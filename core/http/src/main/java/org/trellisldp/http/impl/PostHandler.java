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
package org.trellisldp.http.impl;

import static java.net.URI.create;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Predicate.isEqual;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.http.core.HttpConstants.ACL;
import static org.trellisldp.http.core.HttpConstants.PATCH;
import static org.trellisldp.http.impl.HttpUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.HttpUtils.skolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.io.File;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.core.TrellisRequest;
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
     * @param baseUrl the base URL
     */
    public PostHandler(final TrellisRequest req, final IRI parentIdentifier, final String id, final File entity,
            final ServiceBundler trellis, final String baseUrl) {
        super(req, trellis, baseUrl, entity);

        final String separator = req.getPath().isEmpty() ? "" : "/";

        this.idPath = separator + id;
        this.contentType = req.getContentType();
        this.parentIdentifier = parentIdentifier;
        this.internalId = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath() + idPath);
        this.rdfSyntax = ofNullable(contentType).map(MediaType::valueOf).flatMap(ct ->
                getServices().getIOService().supportedWriteSyntaxes().stream().filter(s ->
                    ct.isCompatible(MediaType.valueOf(s.mediaType()))).findFirst()).orElse(null);

        // Add LDP type (ldp:Resource results in the defaultType)
        this.ldpType = ofNullable(req.getLink())
            .filter(l -> "type".equals(l.getRel())).map(Link::getUri).map(URI::toString)
            .filter(l -> l.startsWith(LDP.getNamespace())).map(rdf::createIRI)
            .filter(isEqual(LDP.Resource).negate())
            .orElseGet(() -> nonNull(contentType) && isNull(rdfSyntax) ? LDP.NonRDFSource : LDP.RDFSource);
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
            throw new WebApplicationException(GONE);
        } else if (ACL.equals(getRequest().getExt())
                || ldpResourceTypes(parent.getInteractionModel()).noneMatch(LDP.Container::equals)) {
            // Can't POST to an ACL resource or non-Container
            throw new NotAllowedException(GET, Stream.of(HEAD, OPTIONS, PATCH, PUT, DELETE).toArray(String[]::new));
        } else if (!MISSING_RESOURCE.equals(child) && !DELETED_RESOURCE.equals(child)) {
            throw new WebApplicationException(CONFLICT);
        } else if (!supportsInteractionModel(ldpType)) {
            throw new BadRequestException("Unsupported interaction model provided", status(BAD_REQUEST)
                .link(UnsupportedInteractionModel.getIRIString(), LDP.constrainedBy.getIRIString()).build());
        } else if (ldpType.equals(LDP.NonRDFSource) && nonNull(rdfSyntax)) {
            LOGGER.error("Cannot save {} as a NonRDFSource with RDF syntax", getIdentifier());
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
    public CompletableFuture<ResponseBuilder> createResource(final ResponseBuilder builder) {
        LOGGER.debug("Creating resource as {}", getIdentifier());

        final TrellisDataset mutable = TrellisDataset.createDataset();
        final TrellisDataset immutable = TrellisDataset.createDataset();

        return handleResourceCreation(mutable, immutable, builder)
            .whenComplete((a, b) -> mutable.close())
            .whenComplete((a, b) -> immutable.close());
    }

    private CompletableFuture<ResponseBuilder> handleResourceCreation(final TrellisDataset mutable,
            final TrellisDataset immutable, final ResponseBuilder builder) {

        final CompletableFuture<Void> persistPromise;
        final Metadata.Builder metadata;

        // Add user-supplied data
        if (ldpType.equals(LDP.NonRDFSource)) {
            // Check the expected digest value
            checkForBadDigest(getRequest().getDigest());

            final String mimeType = ofNullable(contentType).orElse(APPLICATION_OCTET_STREAM);
            final IRI binaryLocation = rdf.createIRI(getServices().getBinaryService().generateIdentifier());

            // Persist the content
            final BinaryMetadata binary = BinaryMetadata.builder(binaryLocation).mimeType(mimeType)
                .size(getEntityLength()).build();
            persistPromise = persistContent(binaryLocation, binary).thenAccept(future ->
                LOGGER.debug("Successfully persisted bitstream with content type {} to {}", mimeType, binaryLocation));

            metadata = metadataBuilder(internalId, ldpType, mutable).container(parentIdentifier).binary(binary);
            builder.link(getIdentifier() + "?ext=description", "describedby");
        } else {
            readEntityIntoDataset(PreferUserManaged, ofNullable(rdfSyntax).orElse(TURTLE), mutable);

            // Check for any constraints
            checkConstraint(mutable.getGraph(PreferUserManaged).orElse(null), ldpType,
                    ofNullable(rdfSyntax).orElse(TURTLE));

            metadata = metadataBuilder(internalId, ldpType, mutable).container(parentIdentifier);
            persistPromise = completedFuture(null);
        }

        // Should this come from the parent resource data?
        getServices().getAuditService().creation(internalId, getSession()).stream()
            .map(skolemizeQuads(getServices().getResourceService(), getBaseUrl())).forEachOrdered(immutable::add);

        return allOf(
                persistPromise,
                getServices().getResourceService().create(metadata.build(), mutable.asDataset()),
                getServices().getResourceService().add(internalId, immutable.asDataset()))
            .thenCompose(future -> emitEvent(internalId, AS.Create, ldpType))
            .thenApply(future -> {
                ldpResourceTypes(ldpType).map(IRI::getIRIString).forEach(type -> builder.link(type, "type"));
                return builder.location(create(getIdentifier()));
            });
    }

    @Override
    protected String getIdentifier() {
        return super.getIdentifier() + idPath;
    }

    @Override
    protected IRI getInternalId() {
        return internalId;
    }
}
