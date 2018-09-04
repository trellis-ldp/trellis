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
package org.trellisldp.http;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.seeOther;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.domain.HttpConstants.CONFIGURATION_BASE_URL;
import static org.trellisldp.http.domain.HttpConstants.TIMEMAP;
import static org.trellisldp.http.impl.RdfUtils.toQuad;

import com.codahale.metrics.annotation.Timed;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.Provider;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.AcceptDatetime;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.http.domain.PATCH;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.http.domain.Range;
import org.trellisldp.http.domain.Version;
import org.trellisldp.http.impl.DeleteHandler;
import org.trellisldp.http.impl.GetHandler;
import org.trellisldp.http.impl.HttpSession;
import org.trellisldp.http.impl.MementoResource;
import org.trellisldp.http.impl.OptionsHandler;
import org.trellisldp.http.impl.PatchHandler;
import org.trellisldp.http.impl.PostHandler;
import org.trellisldp.http.impl.PutHandler;
import org.trellisldp.http.impl.TrellisDataset;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * A {@link ContainerRequestFilter} that also matches path-based HTTP resource operations.
 *
 * <p>Requests are pre-filtered to validate incoming request headers and query parameters.
 *
 * @author acoburn
 */
@PreMatching
@Provider
@Priority(AUTHORIZATION + 20)
@ApplicationScoped
@Path("{path: .*}")
public class LdpResource implements ContainerRequestFilter {

    private static final Logger LOGGER = getLogger(LdpResource.class);

    private static final List<String> MUTATING_METHODS = asList("POST", "PUT", "DELETE", "PATCH");

    protected static final RDF rdf = getInstance();

    protected final ServiceBundler trellis;

    protected final String baseUrl;

    /**
     * Create an LdpResource.
     *
     * @param trellis the Trellis application bundle
     */
    @Inject
    public LdpResource(final ServiceBundler trellis) {
        this(trellis, ConfigurationProvider.getConfiguration().get(CONFIGURATION_BASE_URL));
    }

    /**
     * Create an LdpResource.
     *
     * @param trellis the Trellis application bundle
     * @param baseUrl a base URL
     */
    public LdpResource(final ServiceBundler trellis, final String baseUrl) {
        this.baseUrl = baseUrl;
        this.trellis = trellis;
    }

    /**
     * Initialize the Trellis backend with a root container and default ACL quads.
     *
     * @apiNote In a CDI context, this initialization step will be called automatically.
     *          In a Java SE context, however, it may be necessary to invoke this method
     *          in code, though a ResourceService implementation may still choose to
     *          initialize itself independently of this method. In either case, if the
     *          persistence backend has already been initialized with a root resoure and
     *          root ACL, this method will make no changes to the storage layer.
     */
    @PostConstruct
    public void initialize() {
        final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
        final IRI rootAuth = rdf.createIRI(TRELLIS_DATA_PREFIX + "#auth");
        try (final TrellisDataset dataset = TrellisDataset.createDataset()) {
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.mode, ACL.Read));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.mode, ACL.Write));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.mode, ACL.Control));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.agentClass, FOAF.Agent));
            dataset.add(rdf.createQuad(Trellis.PreferAccessControl, rootAuth, ACL.accessTo, root));
            trellis.getResourceService().get(root).thenCompose(res -> initialize(root, res, dataset)).join();
        }
    }

    private CompletableFuture<Void> initialize(final IRI id, final Resource res, final TrellisDataset dataset) {
        final Session session = new HttpSession(Trellis.AdministratorAgent);
        if (MISSING_RESOURCE.equals(res) || DELETED_RESOURCE.equals(res)) {
            LOGGER.info("Initializing root container: {}", id);
            return trellis.getResourceService().create(id, session, LDP.BasicContainer, dataset.asDataset(), null,
                    null);
        } else if (!res.hasAcl()) {
            LOGGER.info("Initializeing root ACL: {}", id);
            res.stream(Trellis.PreferUserManaged).map(toQuad(Trellis.PreferUserManaged)).forEach(dataset::add);
            return trellis.getResourceService().replace(res.getIdentifier(), session, res.getInteractionModel(),
                    dataset.asDataset(), null, null);
        }
        return completedFuture(null);
    }

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        final String slash = "/";
        // Check for a trailing slash. If so, redirect
        final String path = ctx.getUriInfo().getPath();
        if (path.endsWith(slash) && !path.equals(slash)) {
            ctx.abortWith(seeOther(fromUri(path.substring(0, path.length() - 1)).build()).build());
        }

        // Validate header/query parameters
        ofNullable(ctx.getHeaderString("Accept-Datetime")).filter(x -> isNull(AcceptDatetime.valueOf(x)))
            .ifPresent(x -> ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getHeaderString("Slug")).filter(s -> s.contains(slash)).ifPresent(x ->
            ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getHeaderString("Prefer")).filter(x -> isNull(Prefer.valueOf(x))).ifPresent(x ->
            ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getHeaderString("Range")).filter(x -> isNull(Range.valueOf(x))).ifPresent(x ->
            ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getHeaderString("Link")).ifPresent(x -> {
            try {
                Link.valueOf(x);
            } catch (final IllegalArgumentException ex) {
                ctx.abortWith(status(BAD_REQUEST).build());
            }
        });

        ofNullable(ctx.getHeaderString("Digest")).filter(x -> isNull(Digest.valueOf(x))).ifPresent(x ->
            ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getUriInfo().getQueryParameters().getFirst("version")).ifPresent(x -> {
            // Check well-formedness
            if (isNull(Version.valueOf(x))) {
                ctx.abortWith(status(BAD_REQUEST).build());
            // Do not allow mutating versioned resources
            } else if (MUTATING_METHODS.contains(ctx.getMethod())) {
                ctx.abortWith(status(METHOD_NOT_ALLOWED).build());
            }
        });

        // Do not allow direct manipulation of timemaps
        ofNullable(ctx.getUriInfo().getQueryParameters().get("ext")).filter(l -> l.contains(TIMEMAP))
            .filter(x -> MUTATING_METHODS.contains(ctx.getMethod()))
            .ifPresent(x -> ctx.abortWith(status(METHOD_NOT_ALLOWED).build()));
    }

    /**
     * Perform a GET operation on an LDP Resource.
     *
     * @param request the request parameters
     * @param response the async response
     *
     * Note: The Memento implemenation pattern exactly follows
     * <a href="https://tools.ietf.org/html/rfc7089#section-4.2.1">section 4.2.1 of RFC 7089</a>.
     */
    @GET
    @Timed
    public void getResource(@Suspended final AsyncResponse response, @BeanParam final LdpRequest request) {
        fetchResource(request).thenApply(ResponseBuilder::build).exceptionally(this::handleException)
            .thenApply(response::resume);
    }

    /**
     * Perform a HEAD operation on an LDP Resource.
     *
     * @param request the request parameters
     * @param response the async response
     *
     * Note: The Memento implemenation pattern exactly follows
     * <a href="https://tools.ietf.org/html/rfc7089#section-4.2.1">section 4.2.1 of RFC 7089</a>.
     */
    @HEAD
    @Timed
    public void getResourceHeaders(@Suspended final AsyncResponse response, @BeanParam final LdpRequest request) {
        fetchResource(request).thenApply(ResponseBuilder::build).exceptionally(this::handleException)
            .thenApply(response::resume);
    }

    /**
     * Perform an OPTIONS operation on an LDP Resource.
     *
     * @param response the async response
     * @param req the request
     */
    @OPTIONS
    @Timed
    public void options(@Suspended final AsyncResponse response, @BeanParam final LdpRequest req) {

        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final OptionsHandler optionsHandler = new OptionsHandler(req, trellis, nonNull(req.getVersion()), urlBase);

        fetchTrellisResource(identifier, req.getVersion()).thenApply(optionsHandler::initialize)
            .thenApply(optionsHandler::ldpOptions).thenApply(ResponseBuilder::build)
            .exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Perform a PATCH operation on an LDP Resource.
     *
     * @param response the async response
     * @param req the request
     * @param body the body
     */
    @PATCH
    @Timed
    public void updateResource(@Suspended final AsyncResponse response, @BeanParam final LdpRequest req,
            final String body) {

        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final PatchHandler patchHandler = new PatchHandler(req, body, trellis, urlBase);

        trellis.getResourceService().get(identifier).thenApply(patchHandler::initialize)
            .thenCompose(patchHandler::updateResource).thenCompose(patchHandler::updateMemento)
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Perform a DELETE operation on an LDP Resource.
     *
     * @param response the async response
     * @param req the request
     */
    @DELETE
    @Timed
    public void deleteResource(@Suspended final AsyncResponse response, @BeanParam final LdpRequest req) {

        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final DeleteHandler deleteHandler = new DeleteHandler(req, trellis, urlBase);

        trellis.getResourceService().get(identifier).thenApply(deleteHandler::initialize)
            .thenCompose(deleteHandler::deleteResource).thenApply(ResponseBuilder::build)
            .exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Perform a POST operation on a LDP Resource.
     *
     * @param response the async response
     * @param req the request
     * @param body the body
     */
    @POST
    @Timed
    public void createResource(@Suspended final AsyncResponse response, @BeanParam final LdpRequest req,
            final File body) {

        final String urlBase = getBaseUrl(req);
        final String path = req.getPath();
        final String identifier = ofNullable(req.getSlug())
            .orElseGet(trellis.getResourceService()::generateIdentifier);

        final String separator = path.isEmpty() ? "" : "/";

        final IRI parent = rdf.createIRI(TRELLIS_DATA_PREFIX + path);
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + path + separator + identifier);
        final PostHandler postHandler = new PostHandler(req, parent, identifier, body, trellis, urlBase);

        // First try to fetch the parent and child resources, and then create a new child resource
        trellis.getResourceService().get(parent)
            .thenCombine(trellis.getResourceService().get(child), postHandler::initialize)
            .thenCompose(postHandler::createResource).thenCompose(postHandler::updateMemento)
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Perform a PUT operation on a LDP Resource.
     *
     * @param response the async response
     * @param req the request
     * @param body the body
     */
    @PUT
    @Timed
    public void setResource(@Suspended final AsyncResponse response, @BeanParam final LdpRequest req, final File body) {

        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final PutHandler putHandler = new PutHandler(req, body, trellis, urlBase);

        trellis.getResourceService().get(identifier).thenApply(putHandler::initialize)
            .thenCompose(putHandler::setResource).thenCompose(putHandler::updateMemento)
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException).thenApply(response::resume);
    }

    private String getBaseUrl(final LdpRequest req) {
        return nonNull(baseUrl) ? baseUrl : req.getBaseUrl();
    }

    private CompletableFuture<ResponseBuilder> fetchResource(final LdpRequest req) {
        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final GetHandler getHandler = new GetHandler(req, trellis, nonNull(req.getVersion()), urlBase);

        // Fetch a memento
        if (nonNull(req.getVersion())) {
            LOGGER.debug("Getting versioned resource: {}", req.getVersion());
            return trellis.getMementoService().get(identifier, req.getVersion().getInstant())
                .thenApply(getHandler::initialize).thenApply(getHandler::standardHeaders)
                .thenCombine(trellis.getMementoService().list(identifier), getHandler::addMementoHeaders)
                .thenCompose(getHandler::getRepresentation);

        // Fetch a timemap
        } else if (TIMEMAP.equals(req.getExt())) {
            LOGGER.debug("Getting timemap resource: {}", req.getPath());
            return trellis.getResourceService().get(identifier)
                .thenCombine(trellis.getMementoService().list(identifier), (res, mementos) -> {
                    if (MISSING_RESOURCE.equals(res)) {
                        throw new NotFoundException();
                    }
                    return new MementoResource(trellis).getTimeMapBuilder(mementos, req, urlBase);
                });

        // Fetch a timegate
        } else if (nonNull(req.getDatetime())) {
            LOGGER.debug("Getting timegate resource: {}", req.getDatetime().getInstant());
            return trellis.getMementoService().get(identifier, req.getDatetime().getInstant())
                .thenCombine(trellis.getMementoService().list(identifier), (res, mementos) -> {
                    if (MISSING_RESOURCE.equals(res)) {
                        throw new NotAcceptableException();
                    }
                    return new MementoResource(trellis).getTimeGateBuilder(mementos, req, urlBase);
                });
        }

        // Fetch the current state of the resource
        LOGGER.debug("Getting resource at: {}", identifier);
        return trellis.getResourceService().get(identifier).thenApply(getHandler::initialize)
            .thenApply(getHandler::standardHeaders)
            .thenCombine(trellis.getMementoService().list(identifier), getHandler::addMementoHeaders)
            .thenCompose(getHandler::getRepresentation);
    }

    private CompletableFuture<? extends Resource> fetchTrellisResource(final IRI identifier, final Version version) {
        if (nonNull(version)) {
            return trellis.getMementoService().get(identifier, version.getInstant());
        }
        return trellis.getResourceService().get(identifier);
    }

    private Response handleException(final Throwable err) {
        return of(err).map(Throwable::getCause).filter(ex -> ex instanceof WebApplicationException)
            .map(ex -> (WebApplicationException) ex).orElseGet(() -> new WebApplicationException(err)).getResponse();
    }
}
