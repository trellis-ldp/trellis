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

import static jakarta.ws.rs.HttpMethod.DELETE;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.HEAD;
import static jakarta.ws.rs.HttpMethod.OPTIONS;
import static jakarta.ws.rs.HttpMethod.PATCH;
import static jakarta.ws.rs.HttpMethod.PUT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.GONE;
import static jakarta.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.impl.HttpUtils.closeDataset;
import static org.trellisldp.http.impl.HttpUtils.ldpResourceTypes;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.common.ServiceBundler;
import org.trellisldp.common.TrellisRequest;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;

/**
 * The POST response handler.
 *
 * @author acoburn
 */
public class PostHandler extends MutatingLdpHandler {

    private static final Logger LOGGER = getLogger(PostHandler.class);

    private final String idPath;
    private final IRI internalId;
    private final String contentType;
    private final RDFSyntax rdfSyntax;
    private final IRI ldpType;
    private final IRI parentIdentifier;

    /**
     * Create a builder for an LDP POST response.
     *
     * @param req the LDP request
     * @param parentIdentifier the parent resource
     * @param id the new resource's identifier
     * @param entity the entity
     * @param trellis the Trellis application bundle
     * @param extensions the extension graph mapping
     * @param baseUrl the base URL
     */
    public PostHandler(final TrellisRequest req, final IRI parentIdentifier, final String id, final InputStream entity,
            final ServiceBundler trellis, final Map<String, IRI> extensions, final String baseUrl) {
        super(req, trellis, extensions, baseUrl, entity);

        final String separator = req.getPath().isEmpty() ? "" : "/";
        this.idPath = separator + id;
        this.contentType = req.getContentType();
        this.parentIdentifier = parentIdentifier;
        this.internalId = trellis.getResourceService()
            .getResourceIdentifier(getRequestBaseUrl(req, baseUrl), req.getPath() + idPath);
        this.rdfSyntax = getRdfSyntax(contentType, trellis.getIOService().supportedWriteSyntaxes());

        // Add LDP type (ldp:Resource results in the defaultType)
        this.ldpType = getLdpType(req.getLink(), this.rdfSyntax, this.contentType);
    }

    private static RDFSyntax getRdfSyntax(final String contentType, final List<RDFSyntax> supported) {
        if (contentType != null) {
            final MediaType type = MediaType.valueOf(contentType);
            for (final RDFSyntax s : supported) {
                if (type.isCompatible(MediaType.valueOf(s.mediaType()))) {
                    return s;
                }
            }
        }
        return null;
    }

    private static IRI getLdpType(final Link link, final RDFSyntax syntax, final String contentType) {
        if (link != null) {
            return rdf.createIRI(link.getUri().toString());
        }
        return contentType != null && syntax == null ? LDP.NonRDFSource : LDP.RDFSource;
    }

    /**
     * Initialize the response.
     * @param parent the parent resource
     * @param child the child resource
     * @return a response builder
     */
    public ResponseBuilder initialize(final Resource parent, final Resource child) {
        if (MISSING_RESOURCE.equals(parent)) {
            // Can't POST to a missing resource
            throw new NotFoundException();
        } else if (DELETED_RESOURCE.equals(parent)) {
            // Can't POST to a deleted resource
            throw new ClientErrorException(GONE);
        } else if (getExtensionGraphName() != null
                || ldpResourceTypes(parent.getInteractionModel()).noneMatch(LDP.Container::equals)) {
            // Can't POST to an ACL resource or non-Container
            throw new NotAllowedException(GET, Stream.of(HEAD, OPTIONS, PATCH, PUT, DELETE).toArray(String[]::new));
        } else if (!MISSING_RESOURCE.equals(child) && !DELETED_RESOURCE.equals(child)) {
            throw new ClientErrorException(CONFLICT);
        } else if (!supportsInteractionModel(ldpType)) {
            throw new BadRequestException("Unsupported interaction model provided", status(BAD_REQUEST)
                .link(UnsupportedInteractionModel.getIRIString(), LDP.constrainedBy.getIRIString()).build());
        } else if (ldpType.equals(LDP.NonRDFSource) && rdfSyntax != null) {
            throw new BadRequestException("Cannot save resource as a NonRDFSource with RDF syntax");
        }

        setParent(parent);
        return status(CREATED);
    }

    /**
     * Create a new resource.
     * @param builder the response builder
     * @return the response builder
     */
    public CompletionStage<ResponseBuilder> createResource(final ResponseBuilder builder) {
        LOGGER.debug("Creating resource as {}", getIdentifier());

        final Dataset mutable = rdf.createDataset();
        final Dataset immutable = rdf.createDataset();

        return handleResourceCreation(mutable, immutable, builder)
            .whenComplete((a, b) -> closeDataset(mutable))
            .whenComplete((a, b) -> closeDataset(immutable));
    }

    private CompletionStage<ResponseBuilder> handleResourceCreation(final Dataset mutable,
            final Dataset immutable, final ResponseBuilder builder) {

        final Metadata metadata;
        final BinaryMetadata binary;

        // Add user-supplied data
        if (ldpType.equals(LDP.NonRDFSource)) {
            final String mimeType = contentType != null ? contentType : APPLICATION_OCTET_STREAM;
            final IRI binaryLocation = rdf.createIRI(getServices().getBinaryService().generateIdentifier(internalId));

            // Persist the content
            binary = BinaryMetadata.builder(binaryLocation).mimeType(mimeType)
                            .hints(getRequest().getHeaders()).build();

            metadata = metadataBuilder(internalId, ldpType, mutable).container(parentIdentifier).binary(binary).build();
            builder.link(getIdentifier() + "?ext=description", "describedby");
        } else {
            final RDFSyntax s = rdfSyntax != null ? rdfSyntax : TURTLE;
            readEntityIntoDataset(PreferUserManaged, s, mutable);

            // Check for any constraints
            mutable.getGraph(PreferUserManaged).ifPresent(graph -> checkConstraint(graph, ldpType, s));

            metadata = metadataBuilder(internalId, ldpType, mutable).container(parentIdentifier).build();
            binary = null;
        }

        getAuditQuadData().forEachOrdered(immutable::add);

        return handleResourceCreation(metadata, mutable, immutable)
            .thenCompose(future -> persistBinaryContent(binary))
            .thenCompose(future -> emitNotification(internalId, AS.Create, ldpType, null))
            .thenApply(future -> {
                ldpResourceTypes(ldpType).map(IRI::getIRIString).forEach(type -> builder.link(type, Link.TYPE));
                return builder.location(UriBuilder.fromUri(getIdentifier()).build());
            });
    }

    @Override
    protected String getIdentifier() {
        return super.getIdentifier() + idPath + (HttpUtils.isContainer(ldpType) ? "/" : "");
    }

    @Override
    protected IRI getInternalId() {
        return internalId;
    }
}
