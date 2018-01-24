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
import static java.time.Instant.now;
import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.RdfUtils.skolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;

import org.slf4j.Logger;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.XSD;

/**
 * The POST response handler.
 *
 * @author acoburn
 */
public class PostHandler extends ContentBearingHandler {

    private static final Logger LOGGER = getLogger(PostHandler.class);

    private final String id;

    /**
     * Create a builder for an LDP POST response.
     *
     * @param req the LDP request
     * @param id the new resource's identifier
     * @param entity the entity
     * @param resourceService the resource service
     * @param ioService the serialization service
     * @param binaryService the datastream service
     * @param baseUrl the base URL
     */
    public PostHandler(final LdpRequest req, final String id, final File entity, final ResourceService resourceService,
            final IOService ioService, final BinaryService binaryService, final String baseUrl) {
        super(req, entity, resourceService, ioService, binaryService, baseUrl);
        this.id = id;
    }

    /**
     * Create a new resource.
     *
     * @return the response builder
     */
    public ResponseBuilder createResource() {
        final String baseUrl = getBaseUrl();
        final String separator = req.getPath().isEmpty() ? "" : "/";
        final String identifier = baseUrl + req.getPath() + separator + id;
        final String contentType = req.getContentType();
        final Session session = ofNullable(req.getSession()).orElseGet(HttpSession::new);

        LOGGER.info("Creating resource as {}", identifier);

        final Optional<RDFSyntax> rdfSyntax = ofNullable(contentType).flatMap(RDFSyntax::byMediaType)
            .filter(SUPPORTED_RDF_TYPES::contains);

        final IRI defaultType = nonNull(contentType) && !rdfSyntax.isPresent() ? LDP.NonRDFSource : LDP.RDFSource;
        final IRI internalId = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath() + separator + id);

        // Add LDP type (ldp:Resource results in the defaultType)
        final IRI ldpType = ofNullable(req.getLink())
            .filter(l -> "type".equals(l.getRel())).map(Link::getUri).map(URI::toString)
            .filter(l -> l.startsWith(LDP.URI)).map(rdf::createIRI)
            .filter(l -> !LDP.Resource.equals(l)).orElse(defaultType);

        if (ldpType.equals(LDP.NonRDFSource) && rdfSyntax.isPresent()) {
            return status(BAD_REQUEST).type(TEXT_PLAIN).entity("Cannot save a NonRDFSource with RDF syntax");
        }

        try (final TrellisDataset dataset = TrellisDataset.createDataset()) {

            // Add Audit quads
            audit.ifPresent(svc ->
                    svc.creation(internalId, session).stream().map(skolemizeQuads(resourceService, baseUrl))
                .forEachOrdered(dataset::add));

            dataset.add(rdf.createQuad(null, internalId, DC.isPartOf, rdf.createIRI(baseUrl)));
            dataset.add(rdf.createQuad(PreferServerManaged, internalId, RDF.type, ldpType));

            // Add user-supplied data
            if (ldpType.equals(LDP.NonRDFSource)) {
                // Check the expected digest value
                final Digest digest = req.getDigest();
                if (nonNull(digest) && !getDigestForEntity(digest).equals(digest.getDigest())) {
                    return status(BAD_REQUEST);
                }

                final Map<String, String> metadata = singletonMap(CONTENT_TYPE, ofNullable(contentType)
                        .orElse(APPLICATION_OCTET_STREAM));
                final IRI binaryLocation = rdf.createIRI(binaryService.getIdentifierSupplier().get());
                dataset.add(rdf.createQuad(PreferServerManaged, internalId, DC.hasPart, binaryLocation));
                dataset.add(rdf.createQuad(PreferServerManaged, binaryLocation, DC.modified,
                            rdf.createLiteral(now().toString(), XSD.dateTime)));
                dataset.add(rdf.createQuad(PreferServerManaged, binaryLocation, DC.format,
                            rdf.createLiteral(ofNullable(contentType).orElse(APPLICATION_OCTET_STREAM))));
                dataset.add(rdf.createQuad(PreferServerManaged, binaryLocation, DC.extent,
                            rdf.createLiteral(Long.toString(entity.length()), XSD.long_)));

                // Persist the content
                persistContent(binaryLocation, metadata);
            } else {
                readEntityIntoDataset(identifier, baseUrl, PreferUserManaged, rdfSyntax.orElse(TURTLE), dataset);

                // Check for any constraints
                checkConstraint(dataset, PreferUserManaged, ldpType, TRELLIS_DATA_PREFIX, rdfSyntax.orElse(TURTLE));
            }

            if (resourceService.put(internalId, ldpType, dataset.asDataset()).get()) {
                final ResponseBuilder builder = status(CREATED).location(create(identifier));

                // Add LDP types
                ldpResourceTypes(ldpType).map(IRI::getIRIString).forEach(type -> builder.link(type, "type"));

                return builder;
            }
        } catch (final InterruptedException | ExecutionException ex) {
            LOGGER.error("Error persisting data: {}", ex.getMessage());
        }

        LOGGER.error("Unable to persist data to location at {}", internalId.getIRIString());
        return serverError().type(TEXT_PLAIN)
            .entity("Unable to persist data. Please consult the logs for more information");
    }
}
