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
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static javax.ws.rs.core.HttpHeaders.IF_UNMODIFIED_SINCE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.toQuad;
import static org.trellisldp.http.core.HttpConstants.ACL;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_PRECONDITION_REQUIRED;
import static org.trellisldp.http.impl.HttpUtils.buildEtagHash;
import static org.trellisldp.http.impl.HttpUtils.checkRequiredPreconditions;
import static org.trellisldp.http.impl.HttpUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.HttpUtils.skolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.core.TrellisRequest;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;

/**
 * The PUT response handler.
 *
 * @author acoburn
 */
public class PutHandler extends MutatingLdpHandler {

    private static final Logger LOGGER = getLogger(PutHandler.class);
    private static final boolean DEFAULT_PRECONDITION_REQUIRED = getConfiguration()
        .getOrDefault(CONFIG_HTTP_PRECONDITION_REQUIRED, Boolean.class, false);

    private final IRI internalId;
    private final RDFSyntax rdfSyntax;
    private final IRI heuristicType;
    private final IRI graphName;
    private final IRI otherGraph;

    /**
     * Create a builder for an LDP PUT response.
     *
     * @param req the LDP request
     * @param entity the entity
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    public PutHandler(final TrellisRequest req, final InputStream entity, final ServiceBundler trellis,
                    final String baseUrl) {
        super(req, trellis, baseUrl, entity);
        this.internalId = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        this.rdfSyntax = ofNullable(req.getContentType()).map(MediaType::valueOf).flatMap(ct ->
                getServices().getIOService().supportedWriteSyntaxes().stream().filter(s ->
                    ct.isCompatible(MediaType.valueOf(s.mediaType()))).findFirst()).orElse(null);

        this.heuristicType = nonNull(req.getContentType()) && isNull(rdfSyntax) ? LDP.NonRDFSource : LDP.RDFSource;
        this.graphName = ACL.equals(req.getExt()) ? PreferAccessControl : PreferUserManaged;
        this.otherGraph = ACL.equals(req.getExt()) ? PreferUserManaged : PreferAccessControl;
    }

    /**
     * Initialize the response handler.
     * @param parent the parent resource
     * @param resource the resource
     * @return the response builder
     */
    public ResponseBuilder initialize(final Resource parent, final Resource resource) {
        setResource(DELETED_RESOURCE.equals(resource) || MISSING_RESOURCE.equals(resource) ? null : resource);

        // Check the cache
        if (nonNull(getResource())) {
            final Instant modified = getResource().getModified();
            final EntityTag etag;

            if (getResource().getBinaryMetadata().isPresent() &&
                    !ofNullable(getRequest().getContentType()).flatMap(RDFSyntax::byMediaType).isPresent()) {
                etag = new EntityTag(buildEtagHash(getIdentifier() + "BINARY", modified, null));
            } else {
                etag = new EntityTag(buildEtagHash(getIdentifier(), modified, getRequest().getPrefer()));
            }
            // Check the cache
            checkRequiredPreconditions(DEFAULT_PRECONDITION_REQUIRED, getRequest().getHeaders().getFirst(IF_MATCH),
                    getRequest().getHeaders().getFirst(IF_UNMODIFIED_SINCE));
            checkCache(modified, etag);
        }

        // One cannot put binaries into the ACL graph
        if (ACL.equals(getRequest().getExt()) && isNull(rdfSyntax)) {
            throw new NotAcceptableException();
        }

        ofNullable(resource).flatMap(Resource::getContainer).ifPresent(p -> setParent(parent));
        return status(NO_CONTENT);
    }

    /**
     * Store the resource to the persistence layer.
     * @param builder the response builder
     * @return the response builder
     */
    public CompletableFuture<ResponseBuilder> setResource(final ResponseBuilder builder) {
        LOGGER.debug("Setting resource as {}", getIdentifier());

        final IRI ldpType = isBinaryDescription() ? LDP.NonRDFSource : ofNullable(getRequest().getLink())
            .filter(l -> "type".equals(l.getRel())).map(Link::getUri).map(URI::toString)
            .filter(l -> l.startsWith(LDP.getNamespace())).map(rdf::createIRI).filter(isEqual(LDP.Resource).negate())
            .orElseGet(() -> ofNullable(getResource()).map(Resource::getInteractionModel).orElse(heuristicType));

        // Verify that the persistence layer supports the given interaction model
        if (!supportsInteractionModel(ldpType)) {
            throw new BadRequestException("Unsupported interaction model provided",
                    status(BAD_REQUEST).link(UnsupportedInteractionModel.getIRIString(),
                        LDP.constrainedBy.getIRIString()).build());
        }

        // It is not possible to change the LDP type to a type that is not a subclass
        if (nonNull(getResource()) && !isBinaryDescription()
                && ldpResourceTypes(ldpType).noneMatch(getResource().getInteractionModel()::equals)) {
            LOGGER.error("Cannot change the LDP type to {} for {}", ldpType, getIdentifier());
            throw new ClientErrorException("Cannot change the LDP type to " + ldpType, status(CONFLICT).build());
        }

        LOGGER.debug("Using LDP Type: {}", ldpType);

        final TrellisDataset mutable = TrellisDataset.createDataset();
        final TrellisDataset immutable = TrellisDataset.createDataset();
        LOGGER.debug("Persisting {} with mutable data:\n{}\n and immutable data:\n{}", getIdentifier(), mutable,
                        immutable);
        return handleResourceUpdate(mutable, immutable, builder, ldpType)
            .whenComplete((a, b) -> mutable.close())
            .whenComplete((a, b) -> immutable.close());
    }

    @Override
    protected IRI getInternalId() {
        return internalId;
    }

    @Override
    protected String getIdentifier() {
        return super.getIdentifier() + (ACL.equals(getRequest().getExt()) ? "?ext=acl" : "");
    }

    private CompletableFuture<ResponseBuilder> handleResourceUpdate(final TrellisDataset mutable,
            final TrellisDataset immutable, final ResponseBuilder builder, final IRI ldpType) {

        final Metadata.Builder metadata;
        final CompletableFuture<Void> persistPromise;

        // Add user-supplied data
        if (LDP.NonRDFSource.equals(ldpType) && isNull(rdfSyntax)) {
            LOGGER.trace("Successfully checked for bad digest value");
            final String mimeType = ofNullable(getRequest().getContentType()).orElse(APPLICATION_OCTET_STREAM);
            final IRI binaryLocation = rdf.createIRI(getServices().getBinaryService().generateIdentifier());

            // Persist the content
            final BinaryMetadata binary = BinaryMetadata.builder(binaryLocation).mimeType(mimeType).build();
            persistPromise = persistContent(binary, getRequest().getDigest());

            metadata = metadataBuilder(internalId, ldpType, mutable).binary(binary);
            builder.link(getIdentifier() + "?ext=description", "describedby");
        } else {
            readEntityIntoDataset(graphName, ofNullable(rdfSyntax).orElse(TURTLE), mutable);

            // Check for any constraints
            if (ACL.equals(getRequest().getExt())) {
                checkConstraint(mutable.getGraph(PreferAccessControl).orElse(null), LDP.RDFSource,
                        ofNullable(rdfSyntax).orElse(TURTLE));
            } else {
                checkConstraint(mutable.getGraph(PreferUserManaged).orElse(null), ldpType,
                        ofNullable(rdfSyntax).orElse(TURTLE));
            }
            LOGGER.trace("Successfully checked for constraint violations");
            metadata = metadataBuilder(internalId, ldpType, mutable);
            ofNullable(getResource()).flatMap(Resource::getBinaryMetadata).ifPresent(metadata::binary);
            persistPromise = completedFuture(null);
        }

        if (nonNull(getResource())) {
            getResource().getContainer().ifPresent(metadata::container);
            LOGGER.debug("Resource {} found in persistence", getIdentifier());
            try (final Stream<Triple> remaining = getResource().stream(otherGraph)) {
                remaining.map(toQuad(otherGraph)).forEachOrdered(mutable::add);
            }
        }

        auditQuads().stream().map(skolemizeQuads(getServices().getResourceService(), getBaseUrl()))
            .forEachOrdered(immutable::add);
        LOGGER.trace("Successfully calculated and skolemized immutable data");

        ldpResourceTypes(effectiveLdpType(ldpType)).map(IRI::getIRIString).forEach(type -> {
                LOGGER.debug("Adding link for type {}", type);
                builder.link(type, "type");
            });
        LOGGER.debug("Persisting mutable data for {} with data: {}", internalId, mutable);

        return persistPromise.thenCompose(future -> allOf(
                createOrReplace(metadata.build(), mutable),
                getServices().getResourceService().add(internalId, immutable.asDataset())))
            .thenCompose(future -> handleUpdateEvent(ldpType))
            .thenApply(future -> decorateResponse(builder));
    }

    private ResponseBuilder decorateResponse(final ResponseBuilder builder) {
        if (isNull(getResource())) {
            return builder.status(CREATED).contentLocation(create(getIdentifier()));
        }
        return builder;
    }

    private CompletableFuture<Void> handleUpdateEvent(final IRI ldpType) {
        if (!ACL.equals(getRequest().getExt())) {
            return emitEvent(getInternalId(), isNull(getResource()) ? AS.Create : AS.Update, ldpType);
        }
        return completedFuture(null);
    }

    private IRI effectiveLdpType(final IRI ldpType) {
        LOGGER.trace("Determining effective LDP type from offered type {}", ldpType.getIRIString());
        return ACL.equals(getRequest().getExt())
            || (LDP.NonRDFSource.equals(ldpType) && isBinaryDescription()) ? LDP.RDFSource : ldpType;
    }

    private CompletableFuture<Void> createOrReplace(final Metadata metadata, final TrellisDataset ds) {
        if (isNull(getResource())) {
            LOGGER.debug("Creating new resource {}", internalId);
            return getServices().getResourceService().create(metadata, ds.asDataset());
        } else {
            LOGGER.debug("Replacing old resource {}", internalId);
            return getServices().getResourceService().replace(metadata, ds.asDataset());
        }
    }

    private List<Quad> auditQuads() {
        if (nonNull(getResource())) {
            return getServices().getAuditService().update(internalId, getSession());
        }
        return getServices().getAuditService().creation(internalId, getSession());
    }

    private boolean isBinaryDescription() {
        return nonNull(getResource()) && LDP.NonRDFSource.equals(getResource().getInteractionModel())
            && nonNull(rdfSyntax);
    }
}
