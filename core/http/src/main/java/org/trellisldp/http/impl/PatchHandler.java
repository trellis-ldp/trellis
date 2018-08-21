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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.domain.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.domain.Prefer.PREFER_REPRESENTATION;
import static org.trellisldp.http.impl.RdfUtils.buildEtagHash;
import static org.trellisldp.http.impl.RdfUtils.getDefaultProfile;
import static org.trellisldp.http.impl.RdfUtils.getProfile;
import static org.trellisldp.http.impl.RdfUtils.getSyntax;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.RdfUtils.skolemizeTriples;
import static org.trellisldp.http.impl.RdfUtils.toQuad;
import static org.trellisldp.http.impl.RdfUtils.unskolemizeTriples;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.http.domain.Prefer;
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
    private final IRI graphName;
    private final IRI otherGraph;
    private final RDFSyntax syntax;
    private final String preference;

    /**
     * Create a handler for PATCH operations.
     *
     * @param req the LDP request
     * @param updateBody the sparql update body
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    public PatchHandler(final LdpRequest req, final String updateBody, final ServiceBundler trellis,
            final String baseUrl) {
        super(req, trellis, baseUrl);

        this.updateBody = updateBody;
        this.graphName = ACL.equals(req.getExt()) ? PreferAccessControl : PreferUserManaged;
        this.otherGraph = ACL.equals(req.getExt()) ? PreferUserManaged : PreferAccessControl;
        this.syntax = getServices().getIOService().supportedUpdateSyntaxes().stream()
            .filter(s -> s.mediaType().equalsIgnoreCase(req.getContentType())).findFirst().orElse(null);
        this.preference = ofNullable(req.getPrefer()).flatMap(Prefer::getPreference)
            .filter(PREFER_REPRESENTATION::equals).orElse(null);
    }

    /**
     * Initialze the handler with a Trellis resource.
     *
     * @param resource the Trellis resource
     * @return a response builder
     */
    public ResponseBuilder initialize(final Resource resource) {

        if (MISSING_RESOURCE.equals(resource)) {
            // Can't patch non-existent resources
            throw new NotFoundException();
        } else if (DELETED_RESOURCE.equals(resource)) {
            // Can't patch non-existent resources
            throw new WebApplicationException(GONE);
        } else if (isNull(updateBody)) {
            LOGGER.error("Missing body for update: {}", resource.getIdentifier());
            throw new BadRequestException("Missing body for update");
        } else if (!supportsInteractionModel(LDP.RDFSource)) {
            throw new BadRequestException(status(BAD_REQUEST)
                .link(UnsupportedInteractionModel.getIRIString(), LDP.constrainedBy.getIRIString())
                .entity("Unsupported interaction model provided").type(TEXT_PLAIN_TYPE).build());
        } else if (isNull(syntax)) {
            // Get the incoming syntax and check that the underlying I/O service supports it
            LOGGER.warn("Content-Type: {} not supported", getRequest().getContentType());
            throw new NotSupportedException();
        }
        // Check the cache headers
        final EntityTag etag = new EntityTag(buildEtagHash(getIdentifier(), resource.getModified(),
                    getRequest().getPrefer()));
        checkCache(resource.getModified(), etag);

        setResource(resource);
        return ok();
    }

    /**
     * Update the resource in the persistence layer.
     *
     * @param builder the Trellis response builder
     * @return a response builder promise
     */
    public CompletableFuture<ResponseBuilder> updateResource(final ResponseBuilder builder) {
        LOGGER.debug("Updating {} via PATCH", getIdentifier());

        // Add the LDP link types
        getLinkTypes(getResource().getInteractionModel()).forEach(type -> builder.link(type, "type"));

        final TrellisDataset mutable = TrellisDataset.createDataset();
        final TrellisDataset immutable = TrellisDataset.createDataset();

        return assembleResponse(mutable, immutable, builder)
            .whenComplete((a, b) -> mutable.close())
            .whenComplete((a, b) -> immutable.close());
    }

    @Override
    protected String getIdentifier() {
        return super.getIdentifier() + (ACL.equals(getRequest().getExt()) ? "?ext=acl" : "");
    }

    private Function<Boolean, ResponseBuilder> handleResponse(final ResponseBuilder builder,
            final List<Triple> triples) {
        final RDFSyntax outputSyntax = getSyntax(getServices().getIOService(),
                getRequest().getHeaders().getAcceptableMediaTypes(), empty()).orElse(null);
        final IRI profile = ofNullable(getProfile(getRequest().getHeaders().getAcceptableMediaTypes(), outputSyntax))
            .orElseGet(() -> getDefaultProfile(outputSyntax, getIdentifier()));
        return success -> {
            if (success) {
                if (nonNull(preference)) {
                    final StreamingOutput stream = new StreamingOutput() {
                        @Override
                        public void write(final OutputStream out) throws IOException {
                            getServices().getIOService().write(triples.stream()
                                        .map(unskolemizeTriples(getServices().getResourceService(), getBaseUrl())),
                                        out, outputSyntax, profile);
                        }
                    };
                    builder.header(PREFERENCE_APPLIED, "return=representation")
                        .type(outputSyntax.mediaType()).entity(stream);
                } else {
                    builder.status(NO_CONTENT);
                }
                return builder;
            }
            throw new WebApplicationException("Unable to persist data. Please consult the logs for more information");
        };
    }

    private List<Triple> updateGraph(final RDFSyntax syntax, final IRI graphName) {
        final List<Triple> triples;
        // Update existing graph
        try (final TrellisGraph graph = TrellisGraph.createGraph()) {
            try (final Stream<? extends Triple> stream = getResource().stream(graphName)) {
                stream.forEachOrdered(graph::add);
            }
            getServices().getIOService().update(graph.asGraph(), updateBody, syntax,
                TRELLIS_DATA_PREFIX + getRequest().getPath() + (ACL.equals(getRequest().getExt()) ? "?ext=acl" : ""));
            triples = graph.stream().filter(triple -> !RDF.type.equals(triple.getPredicate())
                || !triple.getObject().ntriplesString().startsWith("<" + LDP.getNamespace())).collect(toList());
        }

        return triples;
    }

    private static Function<ConstraintService, Stream<ConstraintViolation>> handleConstraintViolations(
            final TrellisDataset dataset, final IRI graphName, final IRI interactionModel) {
        final IRI model = PreferAccessControl.equals(graphName) ? LDP.RDFSource : interactionModel;
        return service -> dataset.getGraph(graphName).map(Stream::of).orElseGet(Stream::empty)
                .flatMap(g -> service.constrainedBy(model, g));
    }

    private static Stream<String> getLinkTypes(final IRI ldpType) {
        if (LDP.NonRDFSource.equals(ldpType)) {
            return ldpResourceTypes(LDP.RDFSource).map(IRI::getIRIString);
        }
        return ldpResourceTypes(ldpType).map(IRI::getIRIString);
    }

    private CompletableFuture<ResponseBuilder> assembleResponse(final TrellisDataset mutable,
            final TrellisDataset immutable, final ResponseBuilder builder) {

        // Put triples in buffer, short-circuit on exception
        final List<Triple> triples;
        try {
            triples = updateGraph(syntax, graphName);
        } catch (final RuntimeTrellisException ex) {
            LOGGER.warn("Invalid RDF: {}", ex.getMessage());
            throw new BadRequestException("Invalid RDF: " + ex.getMessage());
        }

        triples.stream().map(skolemizeTriples(getServices().getResourceService(), getBaseUrl()))
            .map(toQuad(graphName)).forEachOrdered(mutable::add);

        // Check any constraints on the resulting dataset
        final List<ConstraintViolation> violations = constraintServices.stream()
            .flatMap(handleConstraintViolations(mutable, graphName, getResource().getInteractionModel()))
            .collect(toList());

        // Short-ciruit if there is a constraint violation
        if (!violations.isEmpty()) {
            final ResponseBuilder err = status(CONFLICT);
            violations.forEach(v -> err.link(v.getConstraint().getIRIString(), LDP.constrainedBy.getIRIString()));
            throw new WebApplicationException(err.build());
        }

        // When updating User or ACL triples, be sure to add the other category to the dataset
        try (final Stream<? extends Triple> remaining = getResource().stream(otherGraph)) {
            remaining.map(toQuad(otherGraph)).forEachOrdered(mutable::add);
        }

        // Collect the audit data
        getAuditUpdateData().forEachOrdered(immutable::add);
        return handleResourceReplacement(mutable, immutable).thenApply(handleResponse(builder, triples));
    }
}
