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
package org.trellisldp.http.impl;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.GONE;
import static jakarta.ws.rs.core.Response.noContent;
import static jakarta.ws.rs.core.Response.status;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.common.HttpConstants.CONFIG_HTTP_PURGE_BINARY_ON_DELETE;
import static org.trellisldp.common.HttpConstants.DESCRIPTION;
import static org.trellisldp.http.impl.HttpUtils.closeDataset;
import static org.trellisldp.http.impl.HttpUtils.skolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.common.ServiceBundler;
import org.trellisldp.common.TrellisRequest;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;

/**
 * The DELETE response builder.
 *
 * @author acoburn
 */
public class DeleteHandler extends MutatingLdpHandler {

    private static final Logger LOGGER = getLogger(DeleteHandler.class);
    private final boolean purgeBinaries = getConfig()
        .getOptionalValue(CONFIG_HTTP_PURGE_BINARY_ON_DELETE, Boolean.class).orElse(Boolean.FALSE);

    /**
     * Create a builder for an LDP DELETE response.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param extensions the extention graph mapping
     * @param baseUrl the base URL
     */
    public DeleteHandler(final TrellisRequest req, final ServiceBundler trellis,
            final Map<String, IRI> extensions, final String baseUrl) {
        super(req, trellis, extensions, baseUrl);
    }

    @Override
    protected String getIdentifier() {
        final String ext = getRequest().getExt();
        return super.getIdentifier() + (ext != null ? "?ext=" + ext : "");
    }

    /**
     * Initialze the handler with a Trellis resource.
     *
     * @param parent the parent resource
     * @param resource the Trellis resource
     * @return a response builder
     */
    public ResponseBuilder initialize(final Resource parent, final Resource resource) {

        // Check that the persistence layer supports LDP-R
        if (MISSING_RESOURCE.equals(resource)) {
            // Can't delete a non-existent resources
            throw new NotFoundException();
        } else if (DELETED_RESOURCE.equals(resource)) {
            // Can't delete a non-existent resources
            throw new ClientErrorException(GONE);
        } else if (!supportsInteractionModel(LDP.Resource)) {
            throw new ClientErrorException(status(BAD_REQUEST)
                .link(UnsupportedInteractionModel.getIRIString(), LDP.constrainedBy.getIRIString())
                .entity("Unsupported interaction model provided").type(TEXT_PLAIN_TYPE).build());
        }

        // Check the cache
        checkCache(resource.getModified(), generateEtag(resource.getRevision()));

        setResource(resource);
        resource.getContainer().ifPresent(p -> setParent(parent));
        return noContent();
    }

    /**
     * Delete the resource in the persistence layer.
     *
     * @param builder the Trellis response builder
     * @return a response builder promise
     */
    public CompletionStage<ResponseBuilder> deleteResource(final ResponseBuilder builder) {

        LOGGER.debug("Deleting {}", getIdentifier());

        final Dataset mutable = rdf.createDataset();
        final Dataset immutable = rdf.createDataset();

        return handleDeletion(mutable, immutable)
            .thenApply(future -> builder)
            .whenComplete((a, b) -> closeDataset(immutable))
            .whenComplete((a, b) -> closeDataset(mutable));
    }

    private CompletionStage<Void> handleDeletion(final Dataset mutable, final Dataset immutable) {
        if (getExtensionGraphName() != null || DESCRIPTION.equals(getRequest().getExt())) {
            return handleSubGraphDeletion(mutable, immutable).thenCompose(future ->
                emitNotification(getInternalId(), AS.Delete, getResource().getInteractionModel(),
                    getResource().getRevision()));
        }
        return handleResourceDeletion(immutable).thenCompose(future ->
                emitNotification(getInternalId(), AS.Delete, getResource().getInteractionModel(),
                    getResource().getRevision()));
    }

    private CompletionStage<Void> handleSubGraphDeletion(final Dataset mutable, final Dataset immutable) {

        // When deleting just an extension graph, keep the other graphs intact
        try (final Stream<Quad> quads = getResource().stream(getNonCurrentGraphNames())) {
            quads.forEachOrdered(mutable::add);
        }

        // Note: when deleting extension resources, the resource itself is not removed and so this is really
        // more of an update operation. As such, the `replace` method is used and an `update` Audit event
        // is generated.

        // Collect the audit data
        getAuditQuadData().forEachOrdered(immutable::add);

        final Metadata.Builder metadata = metadataBuilder(getResource().getIdentifier(),
                getResource().getInteractionModel(), mutable);
        getResource().getContainer().ifPresent(metadata::container);
        getResource().getBinaryMetadata().ifPresent(metadata::binary);
        metadata.revision(getResource().getRevision());

        return handleResourceReplacement(metadata.build(), mutable, immutable);
    }

    private CompletionStage<Void> handleResourceDeletion(final Dataset immutable) {
        // Collect the audit data
        getServices().getAuditService().deletion(getResource().getIdentifier(), getSession()).stream()
            .map(skolemizeQuads(getServices().getResourceService(), getBaseUrl()))
            .forEachOrdered(immutable::add);


        final CompletionStage<Void> binaryPromise = getBinaryPurgePromise();

        // delete the resource
        return allOf(
            binaryPromise.toCompletableFuture(),
            getServices().getResourceService().delete(Metadata.builder(getResource()).build()).toCompletableFuture(),
            getServices().getResourceService().add(getResource().getIdentifier(), immutable).toCompletableFuture());
    }

    private CompletionStage<Void> getBinaryPurgePromise() {
        if (purgeBinaries) {
            return getResource().getBinaryMetadata().map(BinaryMetadata::getIdentifier)
                .map(getServices().getBinaryService()::purgeContent).orElseGet(() -> completedFuture(null));
        }
        return completedFuture(null);
    }
}
