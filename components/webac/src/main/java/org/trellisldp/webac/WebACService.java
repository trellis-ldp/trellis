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
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.api.RDFUtils.toGraph;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.webac.WrappedGraph.wrap;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.CacheService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.vocabulary.VCARD;

/**
 * An {@link AccessControlService} implementation, based on the rules defined by WebAC.
 *
 * @see <a href="https://github.com/solid/web-access-control-spec">SOLID Web Access Control</a>
 *
 * @author acoburn
 */
public class WebACService implements AccessControlService {

    /** The configuration key controlling whether to check member resources at the AuthZ enforcement point. **/
    public static final String CONFIG_WEBAC_MEMBERSHIP_CHECK = "trellis.webac.membership.check";

    private static final Logger LOGGER = getLogger(WebACService.class);
    private static final RDF rdf = getInstance();
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final Set<IRI> allModes = new HashSet<>();

    static {
        allModes.add(ACL.Read);
        allModes.add(ACL.Write);
        allModes.add(ACL.Control);
        allModes.add(ACL.Append);
    }

    private final ResourceService resourceService;
    private final CacheService<String, Set<IRI>> cache;
    private final Boolean checkMembershipResources;

    /**
     * Create a WebAC-based authorization service.
     *
     * @param resourceService the resource service
     */
    public WebACService(final ResourceService resourceService) {
        this(resourceService, new NoopAuthorizationCache());
    }

    /**
     * Create a WebAC-based authorization service.
     *
     * @param resourceService the resource service
     * @param cache a cache
     */
    @Inject
    public WebACService(final ResourceService resourceService,
            @TrellisAuthorizationCache final CacheService<String, Set<IRI>> cache) {
        this(resourceService, cache, getConfiguration()
                .getOrDefault(CONFIG_WEBAC_MEMBERSHIP_CHECK, Boolean.class, false));
    }

    /**
     * Create a WebAC-based authorization service.
     *
     * @param resourceService the resource service
     * @param cache a cache
     * @param checkMembershipResources whether to check membership resource permissions (default=false)
     */
    public WebACService(final ResourceService resourceService,
            final CacheService<String, Set<IRI>> cache, final Boolean checkMembershipResources) {
        requireNonNull(resourceService, "A non-null ResourceService must be provided!");
        requireNonNull(cache, "A non-null Cache must be provided!");
        this.resourceService = resourceService;
        this.cache = cache;
        this.checkMembershipResources = checkMembershipResources;
    }

    @Override
    public Set<IRI> getAccessModes(final IRI identifier, final Session session) {
        requireNonNull(session, "A non-null session must be provided!");

        if (Trellis.AdministratorAgent.equals(session.getAgent())) {
            return unmodifiableSet(allModes);
        }

        final Set<IRI> cachedModes = cache.get(getCacheKey(identifier, session.getAgent()), k ->
                getAuthz(identifier, session.getAgent()));
        return session.getDelegatedBy().map(delegate -> {
                final Set<IRI> delegatedModes = new HashSet<>(cache.get(getCacheKey(identifier, delegate), k ->
                            getAuthz(identifier, delegate)));
                delegatedModes.retainAll(cachedModes);
                return unmodifiableSet(delegatedModes);
            }).orElseGet(() -> unmodifiableSet(cachedModes));
    }

    private String getCacheKey(final IRI identifier, final IRI agent) {
        return join("||", identifier.getIRIString(), agent.getIRIString());
    }

    private Boolean hasWritableMode(final Set<IRI> modes) {
        return modes.contains(ACL.Write) || modes.contains(ACL.Append);
    }

    private Set<IRI> getAuthz(final IRI identifier, final IRI agent) {
        final Set<IRI> modes = getModesFor(identifier, agent);
        // consider membership resources, if relevant
        if (checkMembershipResources && hasWritableMode(modes)) {
            resourceService.getContainer(identifier).map(resourceService::get).map(CompletableFuture::join)
                .flatMap(Resource::getMembershipResource).map(WebACService::cleanIdentifier)
                .map(member -> getModesFor(member, agent)).ifPresent(memberModes -> {
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
            .peek(auth -> LOGGER.debug("Applying Authorization {} to {}", auth.getIdentifier(), identifier))
            .flatMap(auth -> auth.getMode().stream()).collect(toSet());
    }

    private Boolean resourceExists(final Resource res) {
        return !MISSING_RESOURCE.equals(res) && !DELETED_RESOURCE.equals(res);
    }

    private Optional<Resource> getNearestResource(final IRI identifier) {
        final Resource res = resourceService.get(identifier).join();
        if (resourceExists(res)) {
            return of(res);
        }
        return resourceService.getContainer(identifier).flatMap(this::getNearestResource);
    }

    private Predicate<Authorization> agentFilter(final IRI agent) {
        return auth -> auth.getAgentClass().contains(FOAF.Agent) ||
            (auth.getAgentClass().contains(ACL.AuthenticatedAgent) && !Trellis.AnonymousAgent.equals(agent)) ||
            auth.getAgent().contains(agent) || auth.getAgentGroup().stream().anyMatch(isAgentInGroup(agent));
    }

    private Predicate<Authorization> getInheritedAuth(final IRI identifier) {
        return auth -> root.equals(identifier) || auth.getDefault().contains(identifier);
    }

    private Predicate<Authorization> getAccessToAuth(final IRI identifier) {
        return auth -> auth.getAccessTo().contains(identifier);
    }

    private Predicate<IRI> isAgentInGroup(final IRI agent) {
        return group -> resourceService.get(cleanIdentifier(group)).thenApply(res -> {
            try (final Stream<RDFTerm> triples = res.stream(Trellis.PreferUserManaged)
                    .filter(t -> t.getSubject().equals(group) && t.getPredicate().equals(VCARD.hasMember))
                    .map(Triple::getObject)) {
                return triples.anyMatch(agent::equals);
            }
        }).join();
    }

    private List<Authorization> getAuthorizationFromGraph(final Graph graph) {
        return graph.stream().map(Triple::getSubject).distinct().map(subject -> {
                try (final WrappedGraph subGraph = wrap(graph.stream(subject, null, null).collect(toGraph()))) {
                    return Authorization.from(subject, subGraph.getGraph());
                }
            }).collect(toList());
    }

    private Stream<Authorization> getAllAuthorizationsFor(final Resource resource, final Boolean inherited) {
        LOGGER.debug("Checking ACL for: {}", resource.getIdentifier());
        if (resource.hasAcl()) {
            try (final WrappedGraph graph = wrap(resource.stream(Trellis.PreferAccessControl).collect(toGraph()))) {
                final List<Authorization> authorizations = getAuthorizationFromGraph(graph.getGraph());
                // Check for any acl:default statements if checking for inheritance
                if (inherited && authorizations.stream().anyMatch(getInheritedAuth(resource.getIdentifier()))) {
                    return authorizations.stream().filter(getInheritedAuth(resource.getIdentifier()));
                // If not inheriting, just return the relevant Authorizations in the ACL
                } else if (!inherited) {
                    return authorizations.stream().filter(getAccessToAuth(resource.getIdentifier()));
                }
            }
        }
        // Nothing here, check the parent
        LOGGER.debug("No ACL for {}; looking up parent resource", resource.getIdentifier());
        return resourceService.getContainer(resource.getIdentifier()).map(resourceService::get)
            .map(CompletableFuture::join).map(res -> getAllAuthorizationsFor(res, true)).orElseGet(Stream::empty);
    }

    /**
     * Clean the identifier.
     *
     * @param identifier the identifier
     * @return the cleaned identifier
     */
    private static String cleanIdentifier(final String identifier) {
        final String id = identifier.split("#")[0].split("\\?")[0];
        if (id.endsWith("/")) {
            return id.substring(0, id.length() - 1);
        }
        return id;
    }

    /**
     * Clean the identifier.
     *
     * @param identifier the identifier
     * @return the cleaned identifier
     */
    private static IRI cleanIdentifier(final IRI identifier) {
        return rdf.createIRI(cleanIdentifier(identifier.getIRIString()));
    }

    @TrellisAuthorizationCache
    public static class NoopAuthorizationCache implements CacheService<String, Set<IRI>> {

        @Override
        public Set<IRI> get(final String key, final Function<? super String, ? extends Set<IRI>> f) {
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
