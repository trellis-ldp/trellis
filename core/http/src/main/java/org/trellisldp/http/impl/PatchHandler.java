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

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.core.HttpConstants.ACL;
import static org.trellisldp.http.core.HttpConstants.ACL_QUERY_PARAM;
import static org.trellisldp.http.core.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.core.Prefer.PREFER_REPRESENTATION;
import static org.trellisldp.http.impl.HttpUtils.closeDataset;
import static org.trellisldp.http.impl.HttpUtils.getDefaultProfile;
import static org.trellisldp.http.impl.HttpUtils.getProfile;
import static org.trellisldp.http.impl.HttpUtils.getSyntax;
import static org.trellisldp.http.impl.HttpUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.HttpUtils.skolemizeTriples;
import static org.trellisldp.http.impl.HttpUtils.unskolemizeTriples;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
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
    private final IRI graphName;
    private final IRI otherGraph;
    private final RDFSyntax syntax;
    private final String preference;
    private final String defaultJsonLdProfile;

    /**
     * Create a handler for PATCH operations.
     *
     * @param req the LDP request
     * @param updateBody the sparql update body
     * @param trellis the Trellis application bundle
     * @param defaultJsonLdProfile a user-supplied default JSON-LD profile
     * @param baseUrl the base URL
     */
    public PatchHandler(final TrellisRequest req, final String updateBody, final ServiceBundler trellis,
            final String defaultJsonLdProfile, final String baseUrl) {
        super(req, trellis, baseUrl);

        this.updateBody = updateBody;
        this.graphName = ACL.equals(req.getExt()) ? PreferAccessControl : PreferUserManaged;
        this.otherGraph = ACL.equals(req.getExt()) ? PreferUserManaged : PreferAccessControl;
        this.syntax = getServices().getIOService().supportedUpdateSyntaxes().stream()
            .filter(s -> s.mediaType().equalsIgnoreCase(req.getContentType())).findFirst().orElse(null);
        this.defaultJsonLdProfile = defaultJsonLdProfile;
        this.preference = getPreference(req.getPrefer());
    }

    /**
     * Initialze the handler with a Trellis resource.
     *
     * @param parent the parent resource
     * @param resource the Trellis resource
     * @return a response builder
     */
    public ResponseBuilder initialize(final Resource parent, final Resource resource) {

        if (MISSING_RESOURCE.equals(resource)) {
            // Can't patch non-existent resources
            throw new NotFoundException();
        } else if (DELETED_RESOURCE.equals(resource)) {
            // Can't patch non-existent resources
            throw new ClientErrorException(GONE);
        } else if (updateBody == null) {
            LOGGER.error("Missing body for update: {}", resource.getIdentifier());
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
        checkCache(resource.getModified(), generateEtag(resource));

        setResource(resource);
        resource.getContainer().ifPresent(p -> setParent(parent));
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
        if (isAclRequest()) {
            getLinkTypes(LDP.RDFSource).forEach(type -> builder.link(type, "type"));
        } else {
            getLinkTypes(getResource().getInteractionModel()).forEach(type -> builder.link(type, "type"));
        }

        final Dataset mutable = rdf.createDataset();
        final Dataset immutable = rdf.createDataset();

        return assembleResponse(mutable, immutable, builder)
            .whenComplete((a, b) -> closeDataset(mutable))
            .whenComplete((a, b) -> closeDataset(immutable));
    }

    @Override
    protected String getIdentifier() {
        return super.getIdentifier() + (isAclRequest() ? ACL_QUERY_PARAM : "");
    }

    private List<Triple> updateGraph(final RDFSyntax syntax, final IRI graphName) {
        final List<Triple> triples;
        // Update existing graph
        try (final Graph graph = rdf.createGraph()) {
            try (final Stream<Quad> stream = getResource().stream(graphName)) {
                stream.map(Quad::asTriple)
                      .map(unskolemizeTriples(getServices().getResourceService(), getBaseUrl()))
                      .forEachOrdered(graph::add);
            }

            getServices().getIOService().update(graph, updateBody, syntax,
                getBaseUrl() + getRequest().getPath() + (isAclRequest() ? ACL_QUERY_PARAM : ""));
            triples = graph.stream().filter(triple -> !RDF.type.equals(triple.getPredicate())
                || !triple.getObject().ntriplesString().startsWith("<" + LDP.getNamespace())).collect(toList());
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error closing graph", ex);
        }

        return triples;
    }

    private CompletionStage<ResponseBuilder> assembleResponse(final Dataset mutable,
            final Dataset immutable, final ResponseBuilder builder) {

        // Put triples in buffer, short-circuit on exception
        final List<Triple> triples;
        try {
            triples = updateGraph(syntax, graphName);
        } catch (final RuntimeTrellisException ex) {
            throw new BadRequestException("Invalid RDF: " + ex.getMessage());
        }

        triples.stream().map(skolemizeTriples(getServices().getResourceService(), getBaseUrl()))
            .map(triple -> rdf.createQuad(graphName, triple.getSubject(), triple.getPredicate(), triple.getObject()))
            .forEachOrdered(mutable::add);

        // Check any constraints on the resulting dataset
        final List<ConstraintViolation> violations = new ArrayList<>();
        getServices().getConstraintServices()
            .forEach(svc -> handleConstraintViolation(svc, mutable, graphName, getResource().getInteractionModel())
                    .forEach(violations::add));

        // Short-ciruit if there is a constraint violation
        if (!violations.isEmpty()) {
            final ResponseBuilder err = status(CONFLICT);
            violations.forEach(v -> err.link(v.getConstraint().getIRIString(), LDP.constrainedBy.getIRIString()));
            throw new ClientErrorException(err.build());
        }

        // When updating User or ACL triples, be sure to add the other category to the dataset
        try (final Stream<Quad> remaining = getResource().stream(otherGraph)) {
            remaining.forEachOrdered(mutable::add);
        }

        // Collect the audit data
        getAuditUpdateData().forEachOrdered(immutable::add);
        return handleResourceReplacement(mutable, immutable)
            .thenCompose(future -> emitEvent(getInternalId(), AS.Update,
                        isAclRequest() ? LDP.RDFSource : getResource().getInteractionModel()))
            .thenApply(future -> {
                final RDFSyntax outputSyntax = getSyntax(getServices().getIOService(),
                        getRequest().getAcceptableMediaTypes(), null);
                if (preference != null) {
                    final IRI profile = getResponseProfile(outputSyntax);
                    final StreamingOutput stream = new StreamingOutput() {
                        @Override
                        public void write(final OutputStream out) throws IOException {
                            getServices().getIOService().write(triples.stream()
                                        .map(unskolemizeTriples(getServices().getResourceService(), getBaseUrl())),
                                        out, outputSyntax, profile);
                        }
                    };
                    return builder.header(PREFERENCE_APPLIED, "return=representation")
                        .type(outputSyntax.mediaType()).entity(stream);
                }
                return builder.status(NO_CONTENT);
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
            final Dataset dataset, final IRI graphName, final IRI interactionModel) {
        final IRI model = PreferAccessControl.equals(graphName) ? LDP.RDFSource : interactionModel;
        return dataset.getGraph(graphName).map(Stream::of).orElseGet(Stream::empty)
                .flatMap(g -> service.constrainedBy(model, g));
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
