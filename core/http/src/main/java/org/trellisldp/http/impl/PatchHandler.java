/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
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

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.http.core.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.core.Prefer.PREFER_REPRESENTATION;
import static org.trellisldp.http.impl.HttpUtils.closeDataset;
import static org.trellisldp.http.impl.HttpUtils.exists;
import static org.trellisldp.http.impl.HttpUtils.getDefaultProfile;
import static org.trellisldp.http.impl.HttpUtils.getProfile;
import static org.trellisldp.http.impl.HttpUtils.getSyntax;
import static org.trellisldp.http.impl.HttpUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.HttpUtils.skolemizeTriples;
import static org.trellisldp.http.impl.HttpUtils.unskolemizeTriples;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.TrellisRequest;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;

/**
 * The PATCH response builder.
 *
 * @author acoburn
 */
public class PatchHandler extends MutatingLdpHandler {

    private static final Logger LOGGER = getLogger(PatchHandler.class);

    private final String updateBody;
    private final RDFSyntax syntax;
    private final String preference;
    private final String defaultJsonLdProfile;
    private final boolean supportsCreate;
    private final IRI internalId;

    /**
     * Create a handler for PATCH operations.
     *
     * @param req the LDP request
     * @param updateBody the sparql update body
     * @param trellis the Trellis application bundle
     * @param extensions the extension graph mapping
     * @param supportsCreate whether the handler supports create operations
     * @param defaultJsonLdProfile a user-supplied default JSON-LD profile
     * @param baseUrl the base URL
     */
    public PatchHandler(final TrellisRequest req, final String updateBody, final ServiceBundler trellis,
            final Map<String, IRI> extensions, final boolean supportsCreate, final String defaultJsonLdProfile,
            final String baseUrl) {
        super(req, trellis, extensions, baseUrl);

        this.updateBody = updateBody;
        this.syntax = getServices().getIOService().supportedUpdateSyntaxes().stream()
            .filter(s -> s.mediaType().equalsIgnoreCase(req.getContentType())).findFirst().orElse(null);
        this.defaultJsonLdProfile = defaultJsonLdProfile;
        this.preference = getPreference(req.getPrefer());
        this.supportsCreate = supportsCreate;
        this.internalId = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
    }

    /**
     * Initialze the handler with a Trellis resource.
     *
     * @param parent the parent resource
     * @param resource the Trellis resource
     * @return a response builder
     */
    public ResponseBuilder initialize(final Resource parent, final Resource resource) {

        if (!supportsCreate && MISSING_RESOURCE.equals(resource)) {
            // Can't patch non-existent resources
            throw new NotFoundException();
        } else if (!supportsCreate && DELETED_RESOURCE.equals(resource)) {
            // Can't patch non-existent resources
            throw new ClientErrorException(GONE);
        } else if (updateBody == null) {
            throw new BadRequestException("Missing body for update");
        } else if (!supportsInteractionModel(LDP.RDFSource)) {
            throw new BadRequestException(status(BAD_REQUEST)
                .link(UnsupportedInteractionModel.getIRIString(), LDP.constrainedBy.getIRIString())
                .entity("Unsupported interaction model provided").type(TEXT_PLAIN_TYPE).build());
        } else if (syntax == null) {
            // Get the incoming syntax and check that the underlying I/O service supports it
            LOGGER.warn("Content-Type: {} not supported", getRequest().getContentType());
            throw new NotSupportedException();
        }
        // Check the cache headers
        if (exists(resource)) {
            checkCache(resource.getModified(), generateEtag(resource));

            setResource(resource);
            resource.getContainer().ifPresent(p -> setParent(parent));
        } else {
            setResource(null);
            setParent(parent);
        }
        return ok();
    }

    /**
     * Update the resource in the persistence layer.
     *
     * @param builder the Trellis response builder
     * @return a response builder promise
     */
    public CompletionStage<ResponseBuilder> updateResource(final ResponseBuilder builder) {
        LOGGER.debug("Updating {} via PATCH", getIdentifier());

        // Add the LDP link types
        if (getExtensionGraphName() != null) {
            getLinkTypes(LDP.RDFSource).forEach(type -> builder.link(type, Link.TYPE));
        } else {
            getLinkTypes(getLdpType()).forEach(type -> builder.link(type, Link.TYPE));
        }

        final Dataset mutable = rdf.createDataset();
        final Dataset immutable = rdf.createDataset();

        return assembleResponse(mutable, immutable, builder)
            .whenComplete((a, b) -> closeDataset(mutable))
            .whenComplete((a, b) -> closeDataset(immutable));
    }

    @Override
    protected IRI getInternalId() {
        return getResource() != null ? getResource().getIdentifier() : internalId;
    }

    @Override
    protected String getIdentifier() {
        final boolean isContainer = getResource() != null && HttpUtils.isContainer(getResource().getInteractionModel());
        final String iri = super.getIdentifier();
        return iri + (isContainer && !iri.endsWith("/") ? "/" : "")
            + (getExtensionGraphName() != null ? "?ext=" + getRequest().getExt() : "");
    }

    private List<Triple> updateGraph(final RDFSyntax syntax, final IRI graphName) {
        final List<Triple> triples;
        // Update existing graph
        try (final Graph graph = rdf.createGraph()) {
            if (getResource() != null) {
                try (final Stream<Quad> stream = getResource().stream(graphName)) {
                    stream.map(Quad::asTriple)
                          .map(unskolemizeTriples(getServices().getResourceService(), getBaseUrl()))
                          .forEachOrdered(graph::add);
                }
            }

            getServices().getIOService().update(graph, updateBody, syntax, getIdentifier());
            triples = graph.stream().filter(triple -> !RDF.type.equals(triple.getPredicate())
                || !triple.getObject().ntriplesString().startsWith("<" + LDP.getNamespace())).collect(toList());
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error closing graph", ex);
        }

        return triples;
    }

    private IRI getLdpType() {
        return getResource() != null ? getResource().getInteractionModel() : LDP.RDFSource;
    }

    private CompletionStage<ResponseBuilder> assembleResponse(final Dataset mutable,
            final Dataset immutable, final ResponseBuilder builder) {

        final IRI ext = getExtensionGraphName();
        final IRI graphName = ext != null ? ext : PreferUserManaged;

        // Put triples in buffer, short-circuit on exception
        final List<Triple> triples;
        try {
            triples = updateGraph(syntax, graphName);
        } catch (final RuntimeTrellisException ex) {
            throw new BadRequestException("Invalid RDF: " + ex.getMessage(), ex);
        }

        triples.stream().map(skolemizeTriples(getServices().getResourceService(), getBaseUrl()))
            .map(triple -> rdf.createQuad(graphName, triple.getSubject(), triple.getPredicate(), triple.getObject()))
            .forEachOrdered(mutable::add);

        // Check any constraints on the resulting dataset
        final List<ConstraintViolation> violations = new ArrayList<>();
        getServices().getConstraintServices()
            .forEach(svc -> handleConstraintViolation(svc, getInternalId(), mutable, graphName, getLdpType())
                    .forEach(violations::add));

        // Short-ciruit if there is a constraint violation
        if (!violations.isEmpty()) {
            final ResponseBuilder err = status(CONFLICT);
            violations.forEach(v -> err.link(v.getConstraint().getIRIString(), LDP.constrainedBy.getIRIString()));
            throw new ClientErrorException(err.entity((StreamingOutput) out ->
                    getServices().getIOService().write(violations.stream().flatMap(v2 -> v2.getTriples().stream()),
                            out, RDFSyntax.TURTLE, getIdentifier())).type(RDFSyntax.TURTLE.mediaType()).build());
        }

        // When updating one particular graph, be sure to add the other category to the dataset
        if (getResource() != null) {
            try (final Stream<Quad> remaining = getResource().stream(getNonCurrentGraphNames())) {
                remaining.forEachOrdered(mutable::add);
            }
        }

        // Collect the audit data
        getAuditQuadData().forEachOrdered(immutable::add);

        final Metadata metadata;
        if (getResource() == null) {
            metadata = metadataBuilder(getInternalId(), LDP.RDFSource, mutable)
                .container(getParentIdentifier()).build();
        } else {
            final Metadata.Builder mbuilder = metadataBuilder(getResource().getIdentifier(),
                    getResource().getInteractionModel(), mutable);
            getResource().getContainer().ifPresent(mbuilder::container);
            getResource().getBinaryMetadata().ifPresent(mbuilder::binary);
            mbuilder.revision(getResource().getRevision());
            metadata = mbuilder.build();
        }

        return createOrReplace(metadata, mutable, immutable)
            .thenCompose(future -> emitEvent(metadata.getIdentifier(), getResource() == null ? AS.Create : AS.Update,
                        getExtensionGraphName() != null ? LDP.RDFSource : getLdpType()))
            .thenApply(future -> {
                final RDFSyntax outputSyntax = getSyntax(getServices().getIOService(),
                        getRequest().getAcceptableMediaTypes(), null);
                if (preference != null) {
                    final IRI profile = getResponseProfile(outputSyntax);
                    return builder.header(PREFERENCE_APPLIED, "return=representation")
                        .type(outputSyntax.mediaType()).entity((StreamingOutput) out ->
                            getServices().getIOService().write(triples.stream()
                                        .map(unskolemizeTriples(getServices().getResourceService(), getBaseUrl())),
                                        out, outputSyntax, getIdentifier(), profile));
                }
                return builder.status(getResource() == null ? CREATED : NO_CONTENT);
            });
    }

    private IRI getResponseProfile(final RDFSyntax outputSyntax) {
        final IRI profile = getProfile(getRequest().getAcceptableMediaTypes(), outputSyntax);
        if (profile != null) {
            return profile;
        }
        return getDefaultProfile(outputSyntax, getIdentifier(), defaultJsonLdProfile);
    }

    private static Stream<ConstraintViolation> handleConstraintViolation(final ConstraintService service,
            final IRI identifier, final Dataset dataset, final IRI graphName, final IRI interactionModel) {
        final IRI model = PreferUserManaged.equals(graphName) ? interactionModel : LDP.RDFSource;
        return dataset.getGraph(graphName).map(Stream::of).orElseGet(Stream::empty)
                .flatMap(g -> service.constrainedBy(identifier, model, g));
    }

    private static Stream<String> getLinkTypes(final IRI ldpType) {
        if (LDP.NonRDFSource.equals(ldpType)) {
            return ldpResourceTypes(LDP.RDFSource).map(IRI::getIRIString);
        }
        return ldpResourceTypes(ldpType).map(IRI::getIRIString);
    }

    private static String getPreference(final Prefer prefer) {
        if (prefer != null) {
            return prefer.getPreference().filter(PREFER_REPRESENTATION::equals).orElse(null);
        }
        return null;
    }
}
