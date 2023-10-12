/*
 * Copyright (c) Aaron Coburn and individual contributors
 *
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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.*;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getContainer;
import static org.trellisldp.common.HttpConstants.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.RedirectionException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.slf4j.Logger;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.StorageConflictException;
import org.trellisldp.api.TrellisRuntimeException;
import org.trellisldp.common.LdpResource;
import org.trellisldp.common.ServiceBundler;
import org.trellisldp.common.TrellisExtensions;
import org.trellisldp.common.TrellisRequest;
import org.trellisldp.http.impl.DeleteHandler;
import org.trellisldp.http.impl.GetConfiguration;
import org.trellisldp.http.impl.GetHandler;
import org.trellisldp.http.impl.MementoResource;
import org.trellisldp.http.impl.OptionsHandler;
import org.trellisldp.http.impl.PatchHandler;
import org.trellisldp.http.impl.PostHandler;
import org.trellisldp.http.impl.PutHandler;
import org.trellisldp.vocabulary.LDP;

/**
 * An HTTP request matcher for path-based HTTP resource operations.
 *
 * @author acoburn
 */
@ApplicationScoped
@Path("{path: .*}")
@LdpResource
@PermitAll
public class TrellisHttpResource {

    private static final Logger LOGGER = getLogger(TrellisHttpResource.class);

    protected static final RDF rdf = RDFFactory.getInstance();

    @Schema(name = "LinkedDataResource", description = "A Linked Data Resource")
    public static class LinkedDataResource {
    }

    protected final Map<String, IRI> extensions;
    protected final String defaultJsonLdProfile;
    protected final boolean weakEtags;
    protected final boolean includeMementoDates;
    protected final boolean preconditionRequired;
    protected final boolean createUncontained;
    protected final boolean supportsCreateOnPatch;

    @Inject
    ServiceBundler services;

    @Context
    Request request;

    @Context
    UriInfo uriInfo;

    @Context
    HttpHeaders headers;

    @Context
    SecurityContext security;

    Optional<String> baseUrl;

    /**
     * Create a new Trellis HTTP resource matcher.
     */
    public TrellisHttpResource() {
        final Config config = getConfig();
        this.baseUrl = config.getOptionalValue(CONFIG_HTTP_BASE_URL, String.class);
        this.extensions = TrellisExtensions.buildExtensionMapFromConfig(config);
        this.defaultJsonLdProfile = config.getOptionalValue(CONFIG_HTTP_JSONLD_PROFILE, String.class).orElse(null);
        this.weakEtags = config.getOptionalValue(CONFIG_HTTP_WEAK_ETAG, Boolean.class).orElse(Boolean.FALSE);
        this.includeMementoDates = config.getOptionalValue(CONFIG_HTTP_MEMENTO_HEADER_DATES, Boolean.class)
            .orElse(Boolean.TRUE);
        this.preconditionRequired = config.getOptionalValue(CONFIG_HTTP_PRECONDITION_REQUIRED, Boolean.class)
            .orElse(Boolean.FALSE);
        this.createUncontained = config.getOptionalValue(CONFIG_HTTP_PUT_UNCONTAINED, Boolean.class)
            .orElse(Boolean.FALSE);
        this.supportsCreateOnPatch = config.getOptionalValue(CONFIG_HTTP_PATCH_CREATE, Boolean.class)
            .orElse(Boolean.TRUE);
    }

    /**
     * Initialize the Trellis backend with a root container quads.
     *
     * @apiNote In a CDI context, this initialization step will be called automatically.
     *          In a Java SE context, however, it may be necessary to invoke this method
     *          in code, though a ResourceService implementation may still choose to
     *          initialize itself independently of this method. In either case, if the
     *          persistence backend has already been initialized with a root resource,
     *          this method will make no changes to the storage layer.
     */
    @PostConstruct
    public void initialize() {
        final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
        try (final Dataset dataset = rdf.createDataset()) {
            LOGGER.debug("Preparing to initialize Trellis at {}", root);
            services.getResourceService().get(root).thenCompose(res -> initialize(root, res, dataset))
                .exceptionally(err -> {
                    LOGGER.warn("Unable to auto-initialize Trellis: {}. See DEBUG log for more info", err.getMessage());
                    LOGGER.debug("Error auto-initializing Trellis", err);
                    return null;
                }).toCompletableFuture().join();
        } catch (final Exception ex) {
            throw new TrellisRuntimeException("Error initializing Trellis HTTP layer", ex);
        }
    }

    private CompletionStage<Void> initialize(final IRI id, final Resource res, final Dataset dataset) {
        if (MISSING_RESOURCE.equals(res) || DELETED_RESOURCE.equals(res)) {
            LOGGER.info("Initializing root container: {}", id);
            return services.getResourceService().create(Metadata.builder(id).interactionModel(LDP.BasicContainer)
                    .build(), dataset);
        }
        return completedFuture(null);
    }

    /**
     * Perform a GET operation on an LDP Resource.
     *
     * @implNote The Memento implemenation pattern exactly follows
     *           <a href="https://tools.ietf.org/html/rfc7089#section-4.2.1">section 4.2.1 of RFC 7089</a>.
     * @return the async response
     */
    @GET
    @Timed
    @Operation(summary = "Get a linked data resource")
    @APIResponse(
        responseCode = "200",
        description = "The linked data resource",
        content = {
            @Content(mediaType = "*/*",
                     schema = @Schema(implementation = LinkedDataResource.class)),
            @Content(mediaType = "text/turtle",
                     schema = @Schema(implementation = LinkedDataResource.class)),
            @Content(mediaType = "application/ld+json",
                     schema = @Schema(implementation = LinkedDataResource.class)),
            @Content(mediaType = "application/n-triples",
                     schema = @Schema(implementation = LinkedDataResource.class))})
    public CompletionStage<Response> getResource() {
        return fetchResource(new TrellisRequest(request, uriInfo, headers))
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException);
    }

    /**
     * Perform a HEAD operation on an LDP Resource.
     *
     * @implNote The Memento implemenation pattern exactly follows
     *           <a href="https://tools.ietf.org/html/rfc7089#section-4.2.1">section 4.2.1 of RFC 7089</a>.
     * @return the async response
     */
    @HEAD
    @Timed
    @Operation(summary = "Get the headers for a linked data resource")
    @APIResponse(
        responseCode = "200",
        description = "The linked data resource",
        content = {})
    public CompletionStage<Response> getResourceHeaders() {
        return fetchResource(new TrellisRequest(request, uriInfo, headers))
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException);
    }

    /**
     * Perform an OPTIONS operation on an LDP Resource.
     *
     * @return the async response
     */
    @OPTIONS
    @Timed
    @Operation(summary = "Fetch the interaction options for a linked data resource")
    @APIResponse(
        responseCode = "204",
        description = "The options available to the linked data resource",
        content = {})
    public CompletionStage<Response> options() {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers);
        final OptionsHandler optionsHandler = new OptionsHandler(req, services, extensions);
        return supplyAsync(optionsHandler::ldpOptions).thenApply(ResponseBuilder::build)
            .exceptionally(this::handleException);
    }

    /**
     * Perform a PATCH operation on an LDP Resource.
     *
     * @param body the body
     * @return the async response
     */
    @PATCH
    @Timed
    @Operation(summary = "Create or update a linked data resource")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "201",
                description = "The linked data resource was successfully created",
                content = {}),
            @APIResponse(
                responseCode = "204",
                description = "The linked data resource was successfully updated",
                content = {})})
    public CompletionStage<Response> updateResource(
            @RequestBody(description = "The update request for RDF resources, typically as SPARQL-Update",
                         required = true,
                         content = @Content(mediaType = "application/sparql-update")) final String body) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, security);
        final String urlBase = getBaseUrl(req);
        final IRI identifier = services.getResourceService().getResourceIdentifier(urlBase, req.getPath());
        final PatchHandler patchHandler = new PatchHandler(req, body, services, extensions, supportsCreateOnPatch,
                defaultJsonLdProfile, urlBase);

        return getParent(identifier)
            .thenCombine(services.getResourceService().get(identifier), patchHandler::initialize)
            .thenCompose(patchHandler::updateResource).thenCompose(patchHandler::updateMemento)
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException);
    }

    /**
     * Perform a DELETE operation on an LDP Resource.
     *
     * @return the async response
     */
    @DELETE
    @Timed
    @Operation(summary = "Delete a linked data resource")
    @APIResponse(
        responseCode = "204",
        description = "The linked data resource was successfully deleted",
        content = {})
    public CompletionStage<Response> deleteResource() {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, security);
        final String urlBase = getBaseUrl(req);
        final IRI identifier = services.getResourceService().getResourceIdentifier(urlBase, req.getPath());
        final DeleteHandler deleteHandler = new DeleteHandler(req, services, extensions, urlBase);

        return getParent(identifier)
            .thenCombine(services.getResourceService().get(identifier), deleteHandler::initialize)
            .thenCompose(deleteHandler::deleteResource).thenApply(ResponseBuilder::build)
            .exceptionally(this::handleException);
    }

    /**
     * Perform a POST operation on a LDP Resource.
     *
     * @param body the body
     * @return the async response
     */
    @POST
    @Timed
    @Operation(summary = "Create a linked data resource")
    @APIResponse(
        responseCode = "201",
        description = "The linked data resource was successfully created",
        content = {})
    public CompletionStage<Response> createResource(
            @RequestBody(description = "The new resource",
                         content = @Content(mediaType = "*/*",
                                            schema = @Schema(implementation = LinkedDataResource.class)))
            final InputStream body) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, security);
        final String urlBase = getBaseUrl(req);
        final String path = req.getPath();
        final String identifier = getIdentifier(req);
        final String separator = path.isEmpty() ? "" : "/";

        final IRI parent = services.getResourceService().getResourceIdentifier(urlBase, path);
        final IRI child = services.getResourceService().getResourceIdentifier(urlBase, path + separator + identifier);
        final PostHandler postHandler = new PostHandler(req, parent, identifier, body, services, extensions, urlBase);

        return services.getResourceService().get(parent)
            .thenCombine(services.getResourceService().get(child), postHandler::initialize)
            .thenCompose(postHandler::createResource).thenCompose(postHandler::updateMemento)
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException);
    }

    /**
     * Perform a PUT operation on a LDP Resource.
     *
     * @param body the body
     * @return the async response
     */
    @PUT
    @Timed
    @Operation(summary = "Create or update a linked data resource")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "201",
                description = "The linked data resource was successfully created",
                content = {}),
            @APIResponse(
                responseCode = "204",
                description = "The linked data resource was successfully updated",
                content = {})})
    public CompletionStage<Response> setResource(
            @RequestBody(description = "The updated resource",
                         content = @Content(mediaType = "*/*",
                                            schema = @Schema(implementation = LinkedDataResource.class)))
            final InputStream body) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, security);
        final String urlBase = getBaseUrl(req);
        final IRI identifier = services.getResourceService().getResourceIdentifier(urlBase, req.getPath());
        final PutHandler putHandler = new PutHandler(req, body, services, extensions, preconditionRequired,
                createUncontained, urlBase);

        return getParent(identifier).thenCombine(services.getResourceService().get(identifier), putHandler::initialize)
            .thenCompose(putHandler::setResource).thenCompose(putHandler::updateMemento)
            .thenApply(ResponseBuilder::build).exceptionally(this::handleException);
    }

    private CompletionStage<? extends Resource> getParent(final IRI identifier) {
        return getContainer(identifier).map(services.getResourceService()::get)
            .orElseGet(() -> completedFuture(MISSING_RESOURCE));
    }

    private String getBaseUrl(final TrellisRequest req) {
        return baseUrl.orElseGet(req::getBaseUrl);
    }

    private CompletionStage<ResponseBuilder> fetchResource(final TrellisRequest req) {
        final String urlBase = getBaseUrl(req);
        final IRI identifier = services.getResourceService().getResourceIdentifier(urlBase, req.getPath());
        final GetConfiguration config = new GetConfiguration(req.getVersion() != null,
                weakEtags, includeMementoDates, defaultJsonLdProfile, urlBase);
        final GetHandler getHandler = new GetHandler(req, services, extensions, config);

        // Fetch a memento
        if (req.getVersion() != null) {
            LOGGER.debug("Getting versioned resource: {}", req.getVersion());
            return services.getMementoService().get(identifier, req.getVersion().getInstant())
                .thenApply(getHandler::initialize).thenApply(getHandler::standardHeaders)
                .thenCombine(services.getMementoService().mementos(identifier), getHandler::addMementoHeaders)
                .thenCompose(getHandler::getRepresentation);

        // Fetch a timemap
        } else if (TIMEMAP.equals(req.getExt())) {
            LOGGER.debug("Getting timemap resource: {}", req.getPath());
            return services.getResourceService().get(identifier)
                .thenCombine(services.getMementoService().mementos(identifier), (res, mementos) -> {
                    if (MISSING_RESOURCE.equals(res)) {
                        throw new NotFoundException();
                    }
                    return new MementoResource(services, includeMementoDates).getTimeMapBuilder(mementos, req, urlBase);
                });

        // Fetch a timegate
        } else if (req.getDatetime() != null) {
            LOGGER.debug("Getting timegate resource: {}", req.getDatetime().getInstant());
            return services.getMementoService().get(identifier, req.getDatetime().getInstant())
                .thenCombine(services.getMementoService().mementos(identifier), (res, mementos) -> {
                    if (MISSING_RESOURCE.equals(res)) {
                        throw new NotAcceptableException();
                    }
                    return new MementoResource(services, includeMementoDates)
                        .getTimeGateBuilder(mementos, req, urlBase);
                });
        }

        // Fetch the current state of the resource
        LOGGER.debug("Getting resource at: {}", identifier);
        return services.getResourceService().get(identifier).thenApply(getHandler::initialize)
            .thenApply(getHandler::standardHeaders)
            .thenCombine(services.getMementoService().mementos(identifier), getHandler::addMementoHeaders)
            .thenCompose(getHandler::getRepresentation);
    }

    private String getIdentifier(final TrellisRequest req) {
        final String slug = req.getSlug();
        if (slug != null) {
            return slug;
        }
        return services.getResourceService().generateIdentifier();
    }

    private Response handleException(final Throwable err) {
        final Throwable cause = err.getCause();
        if (cause instanceof StorageConflictException) {
            LOGGER.debug("Storage conflict error: {}", err.getMessage());
            LOGGER.trace("Storage conflict error: ", err);
            return Response.status(Response.Status.CONFLICT).build();
        } else if (cause instanceof ClientErrorException) {
            LOGGER.debug("Client error: {}", err.getMessage());
            LOGGER.trace("Client error: ", err);
        } else if (cause instanceof RedirectionException) {
            LOGGER.debug("Redirection: {}", err.getMessage());
            LOGGER.trace("Redirection: ", err);
        } else {
            LOGGER.debug("Error: {}", err.getMessage());
            LOGGER.trace("Error: ", err);
        }
        return cause instanceof WebApplicationException
                        ? ((WebApplicationException) cause).getResponse()
                        : new WebApplicationException(err).getResponse();
    }
}
