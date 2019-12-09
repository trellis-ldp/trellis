/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.webac;

import static java.lang.String.join;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getContainer;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.api.TrellisUtils.toGraph;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.*;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.vocabulary.VCARD;

/**
 * A WebAc implementation, based on the rules defined by SOLID.
 *
 * @see <a href="https://github.com/solid/web-access-control-spec">SOLID Web Access Control</a>
 *
 * @author acoburn
 */

@ApplicationScoped
public class WebAcService {

    /** The configuration key controlling whether to check member resources at the AuthZ enforcement point. */
    public static final String CONFIG_WEBAC_MEMBERSHIP_CHECK = "trellis.webac.membership.check";

    /** The configuration key controlling the location of a default root ACL. */
    public static final String CONFIG_WEBAC_ROOT_ACL_LOCATION = "trellis.webac.root.acl";

    private static final Logger LOGGER = getLogger(WebAcService.class);
    private static final CompletionStage<Void> DONE = CompletableFuture.completedFuture(null);
    private static final RDF rdf = getInstance();
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final IRI rootAuth = rdf.createIRI(TRELLIS_DATA_PREFIX + "#auth");
    private static final Set<IRI> allModes = new HashSet<>();

    static {
        allModes.add(ACL.Read);
        allModes.add(ACL.Write);
        allModes.add(ACL.Control);
        allModes.add(ACL.Append);
    }
    /** The permissive Authorizations in effect when no ACL is present on the root node. */
    private final List<Authorization> defaultRootAuthorizations;
    private final ResourceService resourceService;
    private final IOService ioService;
    private final CacheService<String, Set<IRI>> cache;
    private final boolean checkMembershipResources;

    /**
     * Create a WebAC-based authorization service.
     */
    public WebAcService() {
        this(new NoopResourceService(), null);
    }

    private List<Authorization> getDefaultRootAuthorizations() {
        try (final Dataset dataset = generateDefaultRootAuthorizationsDataset()) {
            return dataset.getGraph(Trellis.PreferAccessControl).map(graph -> Authorization.from(rootAuth, graph))
                .map(Collections::singletonList).orElse(emptyList());
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error closing dataset", ex);
        }
    }

    private Dataset generateDefaultRootAuthorizationsDataset() {
        final Dataset dataset = rdf.createDataset();
        final String location = getConfig().getOptionalValue(CONFIG_WEBAC_ROOT_ACL_LOCATION, String.class)
            .orElse("/org/trellisldp/webac/defaultAcl.ttl");
        try (final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(location)) {
            if (ioService != null && is != null) {
                ioService.read(is, TURTLE, TRELLIS_DATA_PREFIX)
                    .map(triple -> rdf.createQuad(Trellis.PreferAccessControl, triple.getSubject(),
                                triple.getPredicate(), triple.getObject()))
                    .forEach(dataset::add);
            }
        } catch (final IOException ex) {
            LOGGER.warn("Error reading ACL from {}: {}", location, ex.getMessage());
        }

        if (dataset.size() == 0) {
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.mode, ACL.Read));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.mode, ACL.Write));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.mode, ACL.Control));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.mode, ACL.Append));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.agentClass, FOAF.Agent));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.default_, root));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.accessTo, root));
        }
        return dataset;
    }

    /**
     * Initializes the root ACL, if there is no root ACL.
     */
    @PostConstruct
    public void initialize() {
        try (final Dataset dataset = generateDefaultRootAuthorizationsDataset()) {
            this.resourceService.get(root).thenCompose(res -> initialize(res, dataset))
                .exceptionally(err -> {
                    LOGGER.warn("Unable to auto-initialize Trellis: {}. See DEBUG log for more info", err.getMessage());
                    LOGGER.debug("Error auto-initializing Trellis", err);
                    return null;
                }).toCompletableFuture().join();
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error initializing Trellis ACL", ex);
        }
    }

    private CompletionStage<Void> initialize(final Resource res, final Dataset dataset) {
        if (!res.hasAcl()) {
            LOGGER.info("Initializing root ACL: {}", res.getIdentifier());
            try (final Stream<Quad> quads = res.stream(Trellis.PreferUserManaged)) {
                quads.forEach(dataset::add);
            }
            return this.resourceService.replace(Metadata.builder(res).hasAcl(true).build(), dataset);
        } else {
            LOGGER.info("Root ACL is present, not initializing: {}", res.getIdentifier());
            return DONE;
        }
    }

    /**
     * Create a WebAC-based authorization service.
     *
     * @param services the trellis service bundler
     */
    public WebAcService(final ServiceBundler services) {
        this(services, new NoopAuthorizationCache());
    }

    /**
     * Create a WebAC-based authorization service.
     *
     * @param services the trellis service bundler
     * @param cache a cache
     */
    @Inject
    public WebAcService(final ServiceBundler services,
            @TrellisAuthorizationCache final CacheService<String, Set<IRI>> cache) {
        this(services.getResourceService(), services.getIOService(), cache);
    }

    /**
     * Create a WebAC-based authorization service.
     *
     * @param resourceService the resource service
     * @param ioService the IO service (may be {@code null})
     */
    public WebAcService(final ResourceService resourceService, final IOService ioService) {
        this(resourceService, ioService, new NoopAuthorizationCache());
    }

    /**
     * Create a WebAC-based authorization service.
     *
     * @param resourceService the resource service
     * @param ioService the IO service (may be {@code null})
     * @param cache a cache
     */
    public WebAcService(final ResourceService resourceService, final IOService ioService,
            final CacheService<String, Set<IRI>> cache) {
        this(resourceService, ioService, cache, getConfig()
                .getOptionalValue(CONFIG_WEBAC_MEMBERSHIP_CHECK, Boolean.class).orElse(Boolean.FALSE));
    }

    /**
     * Create a WebAC-based authorization service.
     *
     * @param resourceService the resource service
     * @param ioService the IO service (may be {@code null})
     * @param cache a cache
     * @param checkMembershipResources whether to check membership resource permissions (default=false)
     */
    public WebAcService(final ResourceService resourceService, final IOService ioService,
            final CacheService<String, Set<IRI>> cache, final boolean checkMembershipResources) {
        this.resourceService = requireNonNull(resourceService, "A non-null ResourceService must be provided!");
        this.ioService = ioService;
        this.cache = cache;
        this.checkMembershipResources = checkMembershipResources;
        this.defaultRootAuthorizations = unmodifiableList(getDefaultRootAuthorizations());
    }

    /**
     * Get the allowable access modes for the given session to the specified resource.
     * @param identifier the resource identifier
     * @param session the agent's session
     * @return a set of allowable access modes
     */
    public Set<IRI> getAccessModes(final IRI identifier, final Session session) {
        requireNonNull(session, "A non-null session must be provided!");

        if (Trellis.AdministratorAgent.equals(session.getAgent())) {
            return unmodifiableSet(allModes);
        }

        final Set<IRI> cachedModes = cache.get(generateCacheKey(identifier, session.getAgent()), k ->
                getAuthz(identifier, session.getAgent()));
        return session.getDelegatedBy().map(delegate -> {
                final Set<IRI> delegatedModes = new HashSet<>(cache.get(generateCacheKey(identifier, delegate),
                            k -> getAuthz(identifier, delegate)));
                delegatedModes.retainAll(cachedModes);
                return unmodifiableSet(delegatedModes);
            }).orElseGet(() -> unmodifiableSet(cachedModes));
    }

    /**
     * Generate a key suitable for cache lookups for the given arguments.
     * @param identifier the resource identifier
     * @param agent the agent identifier
     * @return a key for cache lookups
     */
    public static String generateCacheKey(final IRI identifier, final IRI agent) {
        return join("||", identifier.getIRIString(), agent.getIRIString());
    }

    private Set<IRI> getAuthz(final IRI identifier, final IRI agent) {
        final Set<IRI> modes = getModesFor(identifier, agent);
        // consider membership resources, if relevant
        if (checkMembershipResources && hasWritableMode(modes)) {
            getContainer(identifier).map(resourceService::get).map(CompletionStage::toCompletableFuture)
                .map(CompletableFuture::join).flatMap(Resource::getMembershipResource)
                .map(TrellisUtils::normalizeIdentifier).map(member -> getModesFor(member, agent))
                .ifPresent(memberModes -> {
                    if (!memberModes.contains(ACL.Write)) {
                        modes.remove(ACL.Write);
                    }
                    if (!memberModes.contains(ACL.Append)) {
                        modes.remove(ACL.Append);
                    }
                });
        }
        return modes;
    }

    private Set<IRI> getModesFor(final IRI identifier, final IRI agent) {
        return getNearestResource(identifier).map(resource -> getAllAuthorizationsFor(resource, false)
                .filter(agentFilter(agent))).orElseGet(Stream::empty)
            .flatMap(auth -> auth.getMode().stream()).collect(toSet());
    }

    private Optional<Resource> getNearestResource(final IRI identifier) {
        final Resource res = resourceService.get(identifier).toCompletableFuture().join();
        if (resourceExists(res)) {
            return Optional.of(res);
        }
        return getContainer(identifier).flatMap(this::getNearestResource);
    }

    private Predicate<Authorization> agentFilter(final IRI agent) {
        return auth -> auth.getAgentClass().contains(FOAF.Agent) ||
            (auth.getAgentClass().contains(ACL.AuthenticatedAgent) && !Trellis.AnonymousAgent.equals(agent)) ||
            auth.getAgent().contains(agent) || auth.getAgentGroup().stream().anyMatch(isAgentInGroup(agent));
    }

    private Predicate<IRI> isAgentInGroup(final IRI agent) {
        return group -> resourceService.get(TrellisUtils.normalizeIdentifier(group)).thenApply(res -> {
            try (final Stream<RDFTerm> triples = res.stream(Trellis.PreferUserManaged)
                    .filter(t -> t.getSubject().equals(group) && t.getPredicate().equals(VCARD.hasMember))
                    .map(Quad::getObject)) {
                return triples.anyMatch(agent::equals);
            }
        }).toCompletableFuture().join();
    }

    private Stream<Authorization> getAllAuthorizationsFor(final Resource resource, final boolean inherited) {
        LOGGER.debug("Checking ACL for: {}", resource.getIdentifier());
        if (resource.hasAcl()) {
            try (final Graph graph = resource.stream(Trellis.PreferAccessControl).map(Quad::asTriple)
                        .collect(toGraph())) {
                // Get the relevant Authorizations in the ACL resource
                final List<Authorization> authorizations = getAuthorizationFromGraph(resource.getIdentifier(), graph);
                // Check for any acl:default statements if checking for inheritance
                if (inherited) {
                    return authorizations.stream().filter(getInheritedAuth(resource.getIdentifier()));
                }
                // If not inheriting, just return the relevant Authorizations
                return authorizations.stream();
            } catch (final Exception ex) {
                throw new RuntimeTrellisException("Error closing graph", ex);
            }
        } else if (root.equals(resource.getIdentifier())) {
            return defaultRootAuthorizations.stream();
        }
        // Nothing here, check the parent
        LOGGER.debug("No ACL for {}; looking up parent resource", resource.getIdentifier());
        return getContainer(resource.getIdentifier()).map(resourceService::get)
            .map(CompletionStage::toCompletableFuture).map(CompletableFuture::join)
            .map(res -> getAllAuthorizationsFor(res, true)).orElseGet(Stream::empty);
    }

    private static List<Authorization> getAuthorizationFromGraph(final IRI identifier, final Graph graph) {
        return graph.stream().map(Triple::getSubject).distinct().map(subject -> {
                try (final Graph subGraph = graph.stream(subject, null, null).collect(toGraph())) {
                    return Authorization.from(subject, subGraph);
                } catch (final Exception ex) {
                    throw new RuntimeTrellisException("Error closing graph", ex);
                }
            }).filter(auth -> auth.getAccessTo().contains(identifier)).collect(toList());
    }

    private static boolean hasWritableMode(final Set<IRI> modes) {
        return modes.contains(ACL.Write) || modes.contains(ACL.Append);
    }

    private static boolean resourceExists(final Resource res) {
        return !MISSING_RESOURCE.equals(res) && !DELETED_RESOURCE.equals(res);
    }

    private static Predicate<Authorization> getInheritedAuth(final IRI identifier) {
        return auth -> root.equals(identifier) || auth.getDefault().contains(identifier);
    }

    @TrellisAuthorizationCache
    public static class NoopAuthorizationCache implements CacheService<String, Set<IRI>> {

        @Override
        public Set<IRI> get(final String key, final Function<String, Set<IRI>> f) {
            return f.apply(key);
        }
    }

    /**
     * A {@link CacheService} that can be used for authorization information.
     *
     * @author ajs6f
     *
     */
    @java.lang.annotation.Documented
    @java.lang.annotation.Retention(RUNTIME)
    @java.lang.annotation.Target({TYPE, METHOD, FIELD, PARAMETER})
    @javax.inject.Qualifier
    public @interface TrellisAuthorizationCache { }
}
