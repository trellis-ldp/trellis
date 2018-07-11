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
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.seeOther;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.AuditService.none;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.HttpConstants.CONFIGURATION_BASE_URL;
import static org.trellisldp.http.domain.HttpConstants.TIMEMAP;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;

import com.codahale.metrics.annotation.Timed;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.domain.AcceptDatetime;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.http.domain.PATCH;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.http.domain.Range;
import org.trellisldp.http.domain.Version;
import org.trellisldp.http.impl.DeleteHandler;
import org.trellisldp.http.impl.GetHandler;
import org.trellisldp.http.impl.MementoResource;
import org.trellisldp.http.impl.OptionsHandler;
import org.trellisldp.http.impl.PatchHandler;
import org.trellisldp.http.impl.PostHandler;
import org.trellisldp.http.impl.PutHandler;
import org.trellisldp.vocabulary.LDP;

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
@Singleton
@Path("{path: .*}")
public class LdpResource implements ContainerRequestFilter {

    private static final Logger LOGGER = getLogger(LdpResource.class);

    private static final List<String> MUTATING_METHODS = asList("POST", "PUT", "DELETE", "PATCH");

    protected static final RDF rdf = getInstance();

    protected final ResourceService resourceService;

    protected final AuditService auditService;

    protected final IOService ioService;

    protected final BinaryService binaryService;

    protected final AgentService agentService;

    protected final MementoService mementoService;

    protected final String baseUrl;

    /**
     * Create an LdpResource.
     *
     * @param resourceService the resource service
     * @param ioService the i/o service
     * @param binaryService the datastream service
     * @param agentService the user agent service
     */
    public LdpResource(final ResourceService resourceService, final IOService ioService,
            final BinaryService binaryService, final AgentService agentService) {
        this(resourceService, ioService, binaryService, agentService,
                loadFirst(MementoService.class).orElseGet(NoopMementoService::new),
                loadFirst(AuditService.class).orElse(none()));
    }

    /**
     * Create an LdpResource.
     *
     * @param resourceService the resource service
     * @param ioService the i/o service
     * @param binaryService the datastream service
     * @param agentService the user agent service
     * @param mementoService the memento service
     * @param auditService an audit service
     */
    @Inject
    public LdpResource(final ResourceService resourceService, final IOService ioService,
            final BinaryService binaryService, final AgentService agentService,
            final MementoService mementoService, final AuditService auditService) {
        this(resourceService, ioService, binaryService, agentService, mementoService, auditService,
                ConfigurationProvider.getConfiguration().get(CONFIGURATION_BASE_URL));
    }

    /**
     * Create an LdpResource.
     *
     * @param resourceService the resource service
     * @param ioService the i/o service
     * @param binaryService the datastream service
     * @param agentService the user agent service
     * @param auditService an audit service
     * @param mementoService the memento service
     * @param baseUrl a base URL
     */
    public LdpResource(final ResourceService resourceService, final IOService ioService,
            final BinaryService binaryService, final AgentService agentService, final MementoService mementoService,
            final AuditService auditService, final String baseUrl) {
        this.baseUrl = baseUrl;
        this.resourceService = resourceService;
        this.ioService = ioService;
        this.binaryService = binaryService;
        this.agentService = agentService;
        this.auditService = auditService;
        this.mementoService = mementoService;
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
        ofNullable(ctx.getHeaderString("Accept-Datetime")).ifPresent(x -> {
            if (isNull(AcceptDatetime.valueOf(x))) {
                ctx.abortWith(status(BAD_REQUEST).build());
            }
        });

        ofNullable(ctx.getHeaderString("Slug")).filter(s -> s.contains(slash)).ifPresent(x ->
            ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getHeaderString("Prefer")).ifPresent(x -> {
            if (isNull(Prefer.valueOf(x))) {
                ctx.abortWith(status(BAD_REQUEST).build());
            }
        });

        ofNullable(ctx.getHeaderString("Range")).ifPresent(x -> {
            if (isNull(Range.valueOf(x))) {
                ctx.abortWith(status(BAD_REQUEST).build());
            }
        });

        ofNullable(ctx.getHeaderString("Link")).ifPresent(x -> {
            try {
                Link.valueOf(x);
            } catch (final IllegalArgumentException ex) {
                ctx.abortWith(status(BAD_REQUEST).build());
            }
        });

        ofNullable(ctx.getHeaderString("Digest")).ifPresent(x -> {
            if (isNull(Digest.valueOf(x))) {
                ctx.abortWith(status(BAD_REQUEST).build());
            }
        });

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
     * @param req the request parameters
     * @return the response
     *
     * Note: The Memento implemenation pattern exactly follows
     * <a href="https://tools.ietf.org/html/rfc7089#section-4.2.1">section 4.2.1 of RFC 7089</a>.
     */
    @GET
    @Timed
    public Response getResource(@BeanParam final LdpRequest req) {
        return fetchResource(req);
    }

    /**
     * Perform a HEAD operation on an LDP Resource.
     *
     * @param req the request parameters
     * @return the response
     *
     * Note: The Memento implemenation pattern exactly follows
     * <a href="https://tools.ietf.org/html/rfc7089#section-4.2.1">section 4.2.1 of RFC 7089</a>.
     */
    @HEAD
    @Timed
    public Response getResourceHeaders(@BeanParam final LdpRequest req) {
        return fetchResource(req);
    }

    private String getBaseUrl(final LdpRequest req) {
        return nonNull(baseUrl) ? baseUrl : req.getBaseUrl();
    }

    private Response fetchResource(final LdpRequest req) {
        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final GetHandler getHandler = new GetHandler(req, resourceService, ioService, binaryService, mementoService,
                urlBase);

        // Fetch a versioned resource
        if (nonNull(req.getVersion())) {
            LOGGER.debug("Getting versioned resource: {}", req.getVersion());
            return mementoService.get(identifier, req.getVersion().getInstant())
                .map(getHandler::getRepresentation).orElseGet(() -> status(NOT_FOUND)).build();

        // Fetch a timemap
        } else if (TIMEMAP.equals(req.getExt())) {
            LOGGER.debug("Getting timemap resource: {}", req.getPath());
            return resourceService.get(identifier)
                .map(res -> new MementoResource(mementoService).getTimeMapBuilder(req, ioService, urlBase))
                .orElseGet(() -> status(NOT_FOUND)).build();

        // Fetch a timegate
        } else if (nonNull(req.getDatetime())) {
            LOGGER.debug("Getting timegate resource: {}", req.getDatetime().getInstant());
            return mementoService.get(identifier, req.getDatetime().getInstant())
                .map(res -> new MementoResource(mementoService).getTimeGateBuilder(req, urlBase))
                .orElseGet(() -> status(NOT_FOUND)).build();
        }

        // Fetch the current state of the resource
        LOGGER.debug("Getting resource at: {}", identifier);
        return resourceService.get(identifier).map(getHandler::getRepresentation)
            .orElseGet(() -> status(NOT_FOUND)).build();
    }

    /**
     * Perform an OPTIONS operation on an LDP Resource.
     *
     * @param req the request
     * @return the response
     */
    @OPTIONS
    @Timed
    public Response options(@BeanParam final LdpRequest req) {

        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final OptionsHandler optionsHandler = new OptionsHandler(req, resourceService, ioService, mementoService,
                urlBase);

        if (nonNull(req.getVersion())) {
            return mementoService.get(identifier, req.getVersion().getInstant()).map(optionsHandler::ldpOptions)
                .orElseGet(() -> status(NOT_FOUND)).build();
        }

        return resourceService.get(identifier).map(optionsHandler::ldpOptions)
            .orElseGet(() -> status(NOT_FOUND)).build();
    }


    /**
     * Perform a PATCH operation on an LDP Resource.
     *
     * @param req the request
     * @param body the body
     * @return the response
     */
    @PATCH
    @Timed
    public Response updateResource(@BeanParam final LdpRequest req, final String body) {

        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final PatchHandler patchHandler = new PatchHandler(req, body, resourceService, auditService, ioService,
                        agentService, mementoService, urlBase);

        return resourceService.get(identifier).map(patchHandler::updateResource)
            .orElseGet(() -> status(NOT_FOUND)).build();
    }

    /**
     * Perform a DELETE operation on an LDP Resource.
     *
     * @param req the request
     * @return the response
     */
    @DELETE
    @Timed
    public Response deleteResource(@BeanParam final LdpRequest req) {

        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final DeleteHandler deleteHandler = new DeleteHandler(req, resourceService, auditService,
                        agentService, mementoService, urlBase);

        return resourceService.get(identifier).map(deleteHandler::deleteResource)
            .orElseGet(() -> status(NOT_FOUND)).build();
    }

    /**
     * Perform a POST operation on a LDP Resource.
     *
     * @param req the request
     * @param body the body
     * @return the response
     */
    @POST
    @Timed
    public Response createResource(@BeanParam final LdpRequest req, final File body) {

        final String urlBase = getBaseUrl(req);
        final String path = req.getPath();
        final String identifier = ofNullable(req.getSlug())
            .orElseGet(resourceService::generateIdentifier);

        final PostHandler postHandler = new PostHandler(req, identifier, body, resourceService, auditService,
                        ioService, binaryService, agentService, mementoService, urlBase);
        final String separator = path.isEmpty() ? "" : "/";

        // First check if this is a container
        final Optional<? extends Resource> parent = resourceService.get(rdf.createIRI(TRELLIS_DATA_PREFIX + path));
        if (parent.isPresent()) {
            final Optional<IRI> ixModel = parent.map(Resource::getInteractionModel);
            if (ixModel.filter(type -> ldpResourceTypes(type).anyMatch(LDP.Container::equals)).isPresent()) {
                return resourceService.get(rdf.createIRI(TRELLIS_DATA_PREFIX + path + separator + identifier))
                    .map(x -> status(CONFLICT)).orElseGet(postHandler::createResource).build();
            } else if (parent.filter(Resource::isDeleted).isPresent()) {
                return status(GONE).build();
            }
            return status(METHOD_NOT_ALLOWED).build();
        }
        return status(NOT_FOUND).build();
    }

    /**
     * Perform a PUT operation on a LDP Resource.
     *
     * @param req the request
     * @param body the body
     * @return the response
     */
    @PUT
    @Timed
    public Response setResource(@BeanParam final LdpRequest req, final File body) {

        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final PutHandler putHandler = new PutHandler(req, body, resourceService, auditService, ioService, binaryService,
                        agentService, mementoService, urlBase);

        return resourceService.get(identifier).filter(res -> !res.isDeleted())
            .map(putHandler::setResource).orElseGet(putHandler::createResource).build();
    }

    // TODO - JDK9 replace with ServiceLoader::loadFirst
    private static <T> Optional<T> loadFirst(final Class<T> service) {
        return of(ServiceLoader.load(service).iterator()).filter(Iterator::hasNext).map(Iterator::next);
    }
}
