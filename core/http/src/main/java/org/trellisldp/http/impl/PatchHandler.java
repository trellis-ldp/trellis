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
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_SESSION_BASE_URL;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.domain.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.domain.Prefer.PREFER_REPRESENTATION;
import static org.trellisldp.http.impl.RdfUtils.buildEtagHash;
import static org.trellisldp.http.impl.RdfUtils.getDefaultProfile;
import static org.trellisldp.http.impl.RdfUtils.getProfile;
import static org.trellisldp.http.impl.RdfUtils.getSyntax;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.RdfUtils.skolemizeQuads;
import static org.trellisldp.http.impl.RdfUtils.skolemizeTriples;
import static org.trellisldp.http.impl.RdfUtils.unskolemizeTriples;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;

/**
 * The PATCH response builder.
 *
 * @author acoburn
 */
public class PatchHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(PatchHandler.class);

    private final String updateBody;

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
    }

    private List<Triple> updateGraph(final Resource res, final IRI graphName) {
        final List<Triple> triples;
        // Get the incoming syntax and check that the underlying I/O service supports it
        final RDFSyntax syntax = trellis.getIOService().supportedUpdateSyntaxes().stream()
            .filter(s -> s.mediaType().equalsIgnoreCase(req.getContentType())).findFirst()
            .orElseThrow(() -> new NotSupportedException("Content-Type: " + req.getContentType() + " not supported"));

        // Update existing graph
        try (final TrellisGraph graph = TrellisGraph.createGraph()) {
            try (final Stream<? extends Triple> stream = res.stream(graphName)) {
                stream.forEachOrdered(graph::add);
            }
            trellis.getIOService().update(graph.asGraph(), updateBody, syntax, TRELLIS_DATA_PREFIX + req.getPath() +
                    (ACL.equals(req.getExt()) ? "?ext=acl" : ""));
            triples = graph.stream().filter(triple -> !RDF.type.equals(triple.getPredicate())
                    || !triple.getObject().ntriplesString().startsWith("<" + LDP.getNamespace())).collect(toList());
        } catch (final RuntimeTrellisException ex) {
            LOGGER.warn("Invalid RDF: {}", ex.getMessage());
            throw new BadRequestException("Invalid RDF: " + ex.getMessage());
        }

        return triples;
    }

    /**
     * Update a resource with Sparql-Update and build an HTTP response.
     *
     * @param res the resource
     * @return the Response builder
     */
    public ResponseBuilder updateResource(final Resource res) {
        final String baseUrl = getBaseUrl();
        final String identifier = baseUrl + req.getPath() + (ACL.equals(req.getExt()) ? "?ext=acl" : "");

        if (isNull(updateBody)) {
            throw new BadRequestException("Missing body for update");
        }
        final Session session = ofNullable(req.getSecurityContext().getUserPrincipal()).map(Principal::getName)
            .map(trellis.getAgentService()::asAgent).map(HttpSession::new).orElseGet(HttpSession::new);
        session.setProperty(TRELLIS_SESSION_BASE_URL, baseUrl);

        // Check if this is already deleted
        checkDeleted(res, identifier);

        // Check the cache
        final EntityTag etag = new EntityTag(buildEtagHash(identifier, res.getModified(), req.getPrefer()));
        checkCache(req.getRequest(), res.getModified(), etag);

        // Check that the persistence layer supports LDP-RS
        checkInteractionModel(LDP.RDFSource);

        LOGGER.debug("Updating {} via PATCH", identifier);

        final IRI graphName = ACL.equals(req.getExt()) ? PreferAccessControl : PreferUserManaged;
        final IRI otherGraph = ACL.equals(req.getExt()) ? PreferUserManaged : PreferAccessControl;

        // Put triples in buffer
        final List<Triple> triples = updateGraph(res, graphName);

        try (final TrellisDataset dataset = TrellisDataset.createDataset()) {

            triples.stream().map(skolemizeTriples(trellis.getResourceService(), baseUrl))
                .map(t -> rdf.createQuad(graphName, t.getSubject(), t.getPredicate(), t.getObject()))
                .forEachOrdered(dataset::add);

            // Check any constraints
            final List<ConstraintViolation> violations = constraintServices.stream()
                .flatMap(svc -> dataset.getGraph(graphName).map(Stream::of).orElseGet(Stream::empty)
                    .flatMap(g -> {
                        if (PreferAccessControl.equals(graphName)) {
                            return svc.constrainedBy(LDP.RDFSource, g);
                        }
                        return svc.constrainedBy(res.getInteractionModel(), g);
                    }))
                .collect(toList());

            if (!violations.isEmpty()) {
                final ResponseBuilder err = status(CONFLICT);
                violations.forEach(v -> err.link(v.getConstraint().getIRIString(), LDP.constrainedBy.getIRIString()));
                throw new WebApplicationException(err.build());
            }

            // When updating User or ACL triples, be sure to add the other category to the dataset
            try (final Stream<? extends Triple> remaining = res.stream(otherGraph)) {
                remaining.map(t -> rdf.createQuad(otherGraph, t.getSubject(), t.getPredicate(), t.getObject()))
                    .forEachOrdered(dataset::add);
            }

            // Save new dataset
            final IRI resId = res.getIdentifier();
            final IRI container = trellis.getResourceService().getContainer(resId).orElse(null);
            if (trellis.getResourceService().replace(res.getIdentifier(), session, res.getInteractionModel(),
                        dataset.asDataset(), container, res.getBinary().orElse(null)).get()) {

                // Add audit-related triples
                try (final TrellisDataset auditDataset = TrellisDataset.createDataset()) {
                    trellis.getAuditService().update(resId, session).stream()
                        .map(skolemizeQuads(trellis.getResourceService(), baseUrl))
                        .forEachOrdered(auditDataset::add);
                    if (!trellis.getResourceService().add(resId, session, auditDataset.asDataset()).get()) {
                        LOGGER.error("Unable to update resource at {}", resId);
                        LOGGER.error("because unable to write audit quads: \n{}",
                                        auditDataset.asDataset().stream().map(Quad::toString).collect(joining("\n")));
                        throw new BadRequestException("Unable to write audit information. "
                                + "Please consult the logs for more information");
                        }
                }

                // Update the memento
                trellis.getResourceService().get(res.getIdentifier()).ifPresent(trellis.getMementoService()::put);

                final ResponseBuilder builder = ok();

                getLinkTypes(res.getInteractionModel()).forEach(type -> builder.link(type, "type"));

                return ofNullable(req.getPrefer()).flatMap(Prefer::getPreference).filter(PREFER_REPRESENTATION::equals)
                    .map(prefer -> {
                        final RDFSyntax outputSyntax = getSyntax(trellis.getIOService(),
                                req.getHeaders().getAcceptableMediaTypes(), empty())
                            .orElseThrow(NotAcceptableException::new);
                        final IRI profile = ofNullable(getProfile(req.getHeaders().getAcceptableMediaTypes(),
                                    outputSyntax)).orElseGet(() -> getDefaultProfile(outputSyntax, identifier));

                        final StreamingOutput stream = new StreamingOutput() {
                            @Override
                            public void write(final OutputStream out) throws IOException {
                                trellis.getIOService().write(triples.stream()
                                            .map(unskolemizeTriples(trellis.getResourceService(), baseUrl)),
                                        out, outputSyntax, profile);
                            }
                        };

                        return builder.header(PREFERENCE_APPLIED, "return=representation")
                            .type(outputSyntax.mediaType()).entity(stream);
                    }).orElseGet(() -> builder.status(NO_CONTENT));
            }
            throw new BadRequestException("Unable to save resource to persistence layer. "
                    + "Please consult the logs for more information.");

        } catch (final InterruptedException | ExecutionException ex) {
            LOGGER.error("Error persisting data", ex);
        }

        LOGGER.error("Unable to persist data to location at {}", res.getIdentifier());
        return serverError().type(TEXT_PLAIN_TYPE)
            .entity("Unable to persist data. Please consult the logs for more information");
    }

    private static Stream<String> getLinkTypes(final IRI ldpType) {
        if (LDP.NonRDFSource.equals(ldpType)) {
            return ldpResourceTypes(LDP.RDFSource).map(IRI::getIRIString);
        }
        return ldpResourceTypes(ldpType).map(IRI::getIRIString);
    }
}
