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

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getContainer;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.api.TrellisUtils.toQuad;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_BASE_URL;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_JSONLD_PROFILE;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_MEMENTO_HEADER_DATES;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_PRECONDITION_REQUIRED;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_WEAK_ETAG;
import static org.trellisldp.http.core.HttpConstants.TIMEMAP;

import com.codahale.metrics.annotation.Timed;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.apache.tamaya.Configuration;
import org.slf4j.Logger;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.core.PATCH;
import org.trellisldp.http.core.TrellisRequest;
import org.trellisldp.http.core.Version;
import org.trellisldp.http.impl.DeleteHandler;
import org.trellisldp.http.impl.GetHandler;
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
 * An HTTP request matcher for path-based HTTP resource operations.
 *
 * @author acoburn
 */
@Provider
@ApplicationScoped
@Path("{path: .*}")
public class TrellisHttpResource {

    private static final Logger LOGGER = getLogger(TrellisHttpResource.class);

    protected static final RDF rdf = getInstance();

    protected final ServiceBundler trellis;
    protected final String baseUrl;
    protected final String defaultJsonLdProfile;
    protected final boolean weakEtags;
    protected final boolean includeMementoDates;
    protected final boolean preconditionRequired;

    /**
     * Create a Trellis HTTP resource matcher.
     *
     * @param trellis the Trellis application bundle
     */
    @Inject
    public TrellisHttpResource(final ServiceBundler trellis) {
        this(trellis, getConfiguration());
    }

    private TrellisHttpResource(final ServiceBundler trellis, final
            Configuration config) {
        this(trellis, config.get(CONFIG_HTTP_BASE_URL), config);
    }

    /**
     * Create a Trellis HTTP resource matcher.
     *
     * @param trellis the Trellis application bundle
     * @param baseUrl a base URL
     */
    public TrellisHttpResource(final ServiceBundler trellis, final String baseUrl) {
        this(trellis, baseUrl, getConfiguration());
    }

    private TrellisHttpResource(final ServiceBundler trellis, final String baseUrl, final Configuration config) {
        this.baseUrl = baseUrl;
        this.trellis = trellis;
        this.defaultJsonLdProfile = config.get(CONFIG_HTTP_JSONLD_PROFILE);
        this.weakEtags = config.getOrDefault(CONFIG_HTTP_WEAK_ETAG, Boolean.class, Boolean.TRUE);
        this.includeMementoDates = config.getOrDefault(CONFIG_HTTP_MEMENTO_HEADER_DATES, Boolean.class, Boolean.TRUE);
        this.preconditionRequired = config.getOrDefault(CONFIG_HTTP_PRECONDITION_REQUIRED, Boolean.class,
                Boolean.FALSE);
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
            LOGGER.debug("Preparing to initialize Trellis at {}", root);
            trellis.getResourceService().get(root).thenCompose(res -> initialize(root, res, dataset))
                .exceptionally(err -> {
                    LOGGER.warn("Unable to auto-initialize Trellis: {}. See DEBUG log for more info", err.getMessage());
                    LOGGER.debug("Error auto-initializing Trellis", err);
                    return null;
                }).join();
        }
    }

    private CompletableFuture<Void> initialize(final IRI id, final Resource res, final TrellisDataset dataset) {
        if (MISSING_RESOURCE.equals(res) || DELETED_RESOURCE.equals(res)) {
            LOGGER.info("Initializing root container: {}", id);
            return trellis.getResourceService().create(Metadata.builder(id).interactionModel(LDP.BasicContainer)
                    .build(), dataset.asDataset());
        } else if (!res.hasAcl()) {
            LOGGER.info("Initializeing root ACL: {}", id);
            try (final Stream<Triple> triples = res.stream(Trellis.PreferUserManaged)) {
                triples.map(toQuad(Trellis.PreferUserManaged)).forEach(dataset::add);
            }
            return trellis.getResourceService().replace(Metadata.builder(res).build(), dataset.asDataset());
        }
        return completedFuture(null);
    }

    /**
     * Perform a GET operation on an LDP Resource.
     *
     * @implNote The Memento implemenation pattern exactly follows
     *           <a href="https://tools.ietf.org/html/rfc7089#section-4.2.1">section 4.2.1 of RFC 7089</a>.
     * @param response the async response
     * @param uriInfo the URI info
     * @param headers the HTTP headers
     * @param request the request
     */
    @GET
    @Timed
    public void getResource(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers) {
        fetchResource(new TrellisRequest(request, uriInfo, headers))
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Perform a HEAD operation on an LDP Resource.
     *
     * @implNote The Memento implemenation pattern exactly follows
     *           <a href="https://tools.ietf.org/html/rfc7089#section-4.2.1">section 4.2.1 of RFC 7089</a>.
     * @param response the async response
     * @param uriInfo the URI info
     * @param headers the HTTP headers
     * @param request the request
     */
    @HEAD
    @Timed
    public void getResourceHeaders(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers) {
        fetchResource(new TrellisRequest(request, uriInfo, headers))
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Perform an OPTIONS operation on an LDP Resource.
     *
     * @param response the async response
     * @param uriInfo the URI info
     * @param headers the HTTP headers
     * @param request the request
     */
    @OPTIONS
    @Timed
    public void options(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers);
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
     * @param uriInfo the URI info
     * @param secContext the security context
     * @param headers the HTTP headers
     * @param request the request
     * @param body the body
     */
    @PATCH
    @Timed
    public void updateResource(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers,
            @Context final SecurityContext secContext, final String body) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, secContext);
        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final PatchHandler patchHandler = new PatchHandler(req, body, trellis, defaultJsonLdProfile, urlBase);

        getParent(identifier).thenCombine(trellis.getResourceService().get(identifier), patchHandler::initialize)
            .thenCompose(patchHandler::updateResource).thenCompose(patchHandler::updateMemento)
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Perform a DELETE operation on an LDP Resource.
     *
     * @param response the async response
     * @param uriInfo the URI info
     * @param secContext the security context
     * @param headers the HTTP headers
     * @param request the request
     */
    @DELETE
    @Timed
    public void deleteResource(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers,
            @Context final SecurityContext secContext) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, secContext);
        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final DeleteHandler deleteHandler = new DeleteHandler(req, trellis, urlBase);

        getParent(identifier).thenCombine(trellis.getResourceService().get(identifier), deleteHandler::initialize)
            .thenCompose(deleteHandler::deleteResource).thenApply(ResponseBuilder::build)
            .exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Perform a POST operation on a LDP Resource.
     *
     * @param response the async response
     * @param uriInfo the URI info
     * @param secContext the security context
     * @param headers the HTTP headers
     * @param request the request
     * @param body the body
     */
    @POST
    @Timed
    public void createResource(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers,
            @Context final SecurityContext secContext, final InputStream body) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, secContext);
        final String urlBase = getBaseUrl(req);
        final String path = req.getPath();
        final String identifier = ofNullable(req.getSlug())
            .orElseGet(trellis.getResourceService()::generateIdentifier);

        final String separator = path.isEmpty() ? "" : "/";

        final IRI parent = rdf.createIRI(TRELLIS_DATA_PREFIX + path);
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + path + separator + identifier);
        final PostHandler postHandler = new PostHandler(req, parent, identifier, body, trellis, urlBase);

        trellis.getResourceService().get(parent)
            .thenCombine(trellis.getResourceService().get(child), postHandler::initialize)
            .thenCompose(postHandler::createResource).thenCompose(postHandler::updateMemento)
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Perform a PUT operation on a LDP Resource.
     *
     * @param response the async response
     * @param uriInfo the URI info
     * @param secContext the security context
     * @param headers the HTTP headers
     * @param request the request
     * @param body the body
     */
    @PUT
    @Timed
    public void setResource(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers,
            @Context final SecurityContext secContext, final InputStream body) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, secContext);
        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final PutHandler putHandler = new PutHandler(req, body, trellis, preconditionRequired, urlBase);

        getParent(identifier).thenCombine(trellis.getResourceService().get(identifier), putHandler::initialize)
            .thenCompose(putHandler::setResource).thenCompose(putHandler::updateMemento)
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException).thenApply(response::resume);
    }

    private CompletableFuture<? extends Resource> getParent(final IRI identifier) {
        final Optional<IRI> parent = getContainer(identifier);
        if (parent.isPresent()) {
            return trellis.getResourceService().get(parent.get());
        }
        return completedFuture(MISSING_RESOURCE);
    }

    private String getBaseUrl(final TrellisRequest req) {
        return nonNull(baseUrl) ? baseUrl : req.getBaseUrl();
    }

    private CompletableFuture<ResponseBuilder> fetchResource(final TrellisRequest req) {
        final String urlBase = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final GetHandler getHandler = new GetHandler(req, trellis, nonNull(req.getVersion()), weakEtags,
                includeMementoDates, defaultJsonLdProfile, urlBase);

        // Fetch a memento
        if (nonNull(req.getVersion())) {
            LOGGER.debug("Getting versioned resource: {}", req.getVersion());
            return trellis.getMementoService().get(identifier, req.getVersion().getInstant())
                .thenApply(getHandler::initialize).thenApply(getHandler::standardHeaders)
                .thenCombine(trellis.getMementoService().mementos(identifier), getHandler::addMementoHeaders)
                .thenCompose(getHandler::getRepresentation);

        // Fetch a timemap
        } else if (TIMEMAP.equals(req.getExt())) {
            LOGGER.debug("Getting timemap resource: {}", req.getPath());
            return trellis.getResourceService().get(identifier)
                .thenCombine(trellis.getMementoService().mementos(identifier), (res, mementos) -> {
                    if (MISSING_RESOURCE.equals(res)) {
                        throw new NotFoundException();
                    }
                    return new MementoResource(trellis, includeMementoDates).getTimeMapBuilder(mementos, req, urlBase);
                });

        // Fetch a timegate
        } else if (nonNull(req.getDatetime())) {
            LOGGER.debug("Getting timegate resource: {}", req.getDatetime().getInstant());
            return trellis.getMementoService().get(identifier, req.getDatetime().getInstant())
                .thenCombine(trellis.getMementoService().mementos(identifier), (res, mementos) -> {
                    if (MISSING_RESOURCE.equals(res)) {
                        throw new NotAcceptableException();
                    }
                    return new MementoResource(trellis, includeMementoDates).getTimeGateBuilder(mementos, req, urlBase);
                });
        }

        // Fetch the current state of the resource
        LOGGER.debug("Getting resource at: {}", identifier);
        return trellis.getResourceService().get(identifier).thenApply(getHandler::initialize)
            .thenApply(getHandler::standardHeaders)
            .thenCombine(trellis.getMementoService().mementos(identifier), getHandler::addMementoHeaders)
            .thenCompose(getHandler::getRepresentation);
    }

    private CompletableFuture<? extends Resource> fetchTrellisResource(final IRI identifier, final Version version) {
        if (nonNull(version)) {
            return trellis.getMementoService().get(identifier, version.getInstant());
        }
        return trellis.getResourceService().get(identifier);
    }

    private Response handleException(final Throwable err) {
        if (!(err.getCause() instanceof ClientErrorException || err.getCause() instanceof RedirectionException)) {
            LOGGER.error("Trellis Error:", err);
        }
        return of(err).map(Throwable::getCause).filter(WebApplicationException.class::isInstance)
            .map(WebApplicationException.class::cast).orElseGet(() -> new WebApplicationException(err)).getResponse();
    }
}
