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
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.domain.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.domain.Prefer.PREFER_REPRESENTATION;
import static org.trellisldp.http.impl.RdfUtils.getDefaultProfile;
import static org.trellisldp.http.impl.RdfUtils.getProfile;
import static org.trellisldp.http.impl.RdfUtils.getSyntax;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.RdfUtils.skolemizeQuads;
import static org.trellisldp.http.impl.RdfUtils.skolemizeTriples;
import static org.trellisldp.http.impl.RdfUtils.unskolemizeTriples;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;

import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.XSD;

/**
 * The PATCH response builder.
 *
 * @author acoburn
 */
public class PatchHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(PatchHandler.class);

    private final IOService ioService;
    private final String sparqlUpdate;

    /**
     * Create a handler for PATCH operations.
     *
     * @param req the LDP request
     * @param sparqlUpdate the sparql update body
     * @param resourceService the resource service
     * @param ioService the serialization service
     * @param baseUrl the base URL
     */
    public PatchHandler(final LdpRequest req, final String sparqlUpdate,
            final ResourceService resourceService, final IOService ioService, final String baseUrl) {
        super(req, resourceService, baseUrl);
        this.ioService = ioService;
        this.sparqlUpdate = sparqlUpdate;
    }

    private List<Triple> updateGraph(final Resource res, final IRI graphName) {
        final List<Triple> triples;
        // Update existing graph
        try (final TrellisGraph graph = TrellisGraph.createGraph()) {
            try (final Stream<? extends Triple> stream = res.stream(graphName)) {
                stream.forEachOrdered(graph::add);
            }
            ioService.update(graph.asGraph(), sparqlUpdate, TRELLIS_DATA_PREFIX + req.getPath() +
                    (ACL.equals(req.getExt()) ? "?ext=acl" : ""));
            triples = graph.stream().collect(toList());
        } catch (final RuntimeTrellisException ex) {
            LOGGER.warn(ex.getMessage());
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

        if (isNull(sparqlUpdate)) {
            throw new WebApplicationException("Missing Sparql-Update body", BAD_REQUEST);
        }
        final Session session = ofNullable(req.getSession()).orElseGet(HttpSession::new);

        // Check if this is already deleted
        checkDeleted(res, identifier);

        // Check the cache
        final EntityTag etag = new EntityTag(md5Hex(res.getModified() + identifier));
        checkCache(req.getRequest(), res.getModified(), etag);

        LOGGER.debug("Updating {} via PATCH", identifier);

        final IRI graphName = ACL.equals(req.getExt()) ? PreferAccessControl : PreferUserManaged;
        final IRI otherGraph = ACL.equals(req.getExt()) ? PreferUserManaged : PreferAccessControl;

        // Put triples in buffer
        final List<Triple> triples = updateGraph(res, graphName);

        try (final TrellisDataset dataset = TrellisDataset.createDataset()) {

            triples.stream().map(skolemizeTriples(resourceService, baseUrl))
                .map(t -> rdf.createQuad(graphName, t.getSubject(), t.getPredicate(), t.getObject()))
                .forEachOrdered(dataset::add);

            // Add audit-related triples
            audit.ifPresent(svc -> svc.update(res.getIdentifier(), session).stream()
                    .map(skolemizeQuads(resourceService, baseUrl)).forEachOrdered(dataset::add));

            // Add existing LDP type, other server-managed triples
            dataset.add(rdf.createQuad(PreferServerManaged, res.getIdentifier(), RDF.type, res.getInteractionModel()));
            res.getBinary().ifPresent(b -> {
                dataset.add(rdf.createQuad(PreferServerManaged, res.getIdentifier(), DC.hasPart, b.getIdentifier()));
                dataset.add(rdf.createQuad(PreferServerManaged, b.getIdentifier(), DC.modified,
                            rdf.createLiteral(b.getModified().toString(), XSD.dateTime)));
                dataset.add(rdf.createQuad(PreferServerManaged, b.getIdentifier(), DC.format,
                            rdf.createLiteral(b.getMimeType().orElse(APPLICATION_OCTET_STREAM))));
                b.getSize().ifPresent(size -> dataset.add(rdf.createQuad(PreferServerManaged, b.getIdentifier(),
                                DC.extent, rdf.createLiteral(Long.toString(size), XSD.long_))));
            });

            // Add baseUrl
            dataset.add(rdf.createQuad(null, res.getIdentifier(), DC.isPartOf, rdf.createIRI(baseUrl)));

            // Check any constraints
            final List<ConstraintViolation> violations = constraintServices.stream()
                .flatMap(svc -> dataset.getGraph(graphName).map(Stream::of).orElseGet(Stream::empty)
                    .flatMap(g -> svc.constrainedBy(res.getInteractionModel(), baseUrl, g)))
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
            if (resourceService.put(res.getIdentifier(), res.getInteractionModel(), dataset.asDataset()).get()) {

                final ResponseBuilder builder = ok();

                getLinkTypes(res.getInteractionModel()).forEach(type -> builder.link(type, "type"));

                return ofNullable(req.getPrefer()).flatMap(Prefer::getPreference).filter(PREFER_REPRESENTATION::equals)
                    .map(prefer -> {
                        final RDFSyntax syntax = getSyntax(req.getHeaders().getAcceptableMediaTypes(), empty())
                            .orElseThrow(NotAcceptableException::new);
                        final IRI profile = ofNullable(getProfile(req.getHeaders().getAcceptableMediaTypes(), syntax))
                            .orElseGet(() -> getDefaultProfile(syntax, identifier));

                        final StreamingOutput stream = new StreamingOutput() {
                            @Override
                            public void write(final OutputStream out) throws IOException {
                                ioService.write(triples.stream().map(unskolemizeTriples(resourceService, baseUrl)),
                                        out, syntax, profile);
                            }
                        };

                        return builder.header(PREFERENCE_APPLIED, "return=representation").type(syntax.mediaType())
                               .entity(stream);
                    }).orElseGet(() -> builder.status(NO_CONTENT));
            }
        } catch (final InterruptedException | ExecutionException ex) {
            LOGGER.error("Error persisting data: {}", ex.getMessage());
        }

        LOGGER.error("Unable to persist data to location at {}", res.getIdentifier());
        return serverError().type(TEXT_PLAIN)
            .entity("Unable to persist data. Please consult the logs for more information");
    }

    private static Stream<String> getLinkTypes(final IRI ldpType) {
        if (LDP.NonRDFSource.equals(ldpType)) {
            return ldpResourceTypes(LDP.RDFSource).map(IRI::getIRIString);
        }
        return ldpResourceTypes(ldpType).map(IRI::getIRIString);
    }
}
