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
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_SESSION_BASE_URL;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.RdfUtils.skolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.File;
import java.net.URI;
import java.security.Principal;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

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
     * @param auditService and audit service
     * @param agentService the agent service
     * @param baseUrl the base URL
     */
    public PostHandler(final LdpRequest req, final String id, final File entity, final ResourceService resourceService,
                    final AuditService auditService, final IOService ioService, final BinaryService binaryService,
                    final AgentService agentService, final String baseUrl) {
        super(req, entity, resourceService, auditService, ioService, binaryService, agentService, baseUrl);
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
        final Session session = ofNullable(req.getSecurityContext().getUserPrincipal()).map(Principal::getName)
            .map(agentService::asAgent).map(HttpSession::new).orElseGet(HttpSession::new);
        session.setProperty(TRELLIS_SESSION_BASE_URL, baseUrl);

        LOGGER.debug("Creating resource as {}", identifier);

        final Optional<RDFSyntax> rdfSyntax = ofNullable(contentType).flatMap(RDFSyntax::byMediaType)
            .filter(SUPPORTED_RDF_TYPES::contains);

        final IRI defaultType = nonNull(contentType) && !rdfSyntax.isPresent() ? LDP.NonRDFSource : LDP.RDFSource;
        final IRI internalId = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath() + separator + id);

        // Add LDP type (ldp:Resource results in the defaultType)
        final IRI ldpType = ofNullable(req.getLink())
            .filter(l -> "type".equals(l.getRel())).map(Link::getUri).map(URI::toString)
            .filter(l -> l.startsWith(LDP.getNamespace())).map(rdf::createIRI)
            .filter(l -> !LDP.Resource.equals(l)).orElse(defaultType);

        // Verify that the persistence layer supports the specified IXN model
        checkInteractionModel(ldpType);

        if (ldpType.equals(LDP.NonRDFSource) && rdfSyntax.isPresent()) {
            throw new BadRequestException("Cannot save a NonRDFSource with RDF syntax");
        }

        try (final TrellisDataset dataset = TrellisDataset.createDataset()) {

            final Binary binary;

            // Add user-supplied data
            if (ldpType.equals(LDP.NonRDFSource)) {
                // Check the expected digest value
                checkForBadDigest(req.getDigest());

                final String mimeType = ofNullable(contentType).orElse(APPLICATION_OCTET_STREAM);
                final IRI binaryLocation = rdf.createIRI(binaryService.generateIdentifier());

                // Persist the content
                persistContent(binaryLocation, singletonMap(CONTENT_TYPE, mimeType));

                binary = new Binary(binaryLocation, now(), mimeType, entity.length());
            } else {
                readEntityIntoDataset(identifier, baseUrl, PreferUserManaged, rdfSyntax.orElse(TURTLE), dataset);

                // Check for any constraints
                checkConstraint(dataset, PreferUserManaged, ldpType, rdfSyntax.orElse(TURTLE));

                binary = null;
            }
            final IRI container = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
            final Future<Boolean> success = resourceService.create(internalId, session, ldpType, dataset.asDataset(),
                    container, binary);
            if (success.get()) {

                // Add Audit quads
                try (final TrellisDataset auditDataset = TrellisDataset.createDataset()) {
                    audit.creation(internalId, session).stream().map(skolemizeQuads(resourceService, baseUrl))
                                    .forEachOrdered(auditDataset::add);
                    if (!resourceService.add(internalId, session, auditDataset.asDataset()).get()) {
                        LOGGER.error("Using AuditService {}", audit);
                        LOGGER.error("Unable to act against resource at {}", internalId);
                        LOGGER.error("because unable to write audit quads: \n{}",
                                        auditDataset.asDataset().stream().map(Quad::toString).collect(joining("\n")));
                        throw new BadRequestException("Unable to write audit information. Please consult "
                                + "the logs for more information.");
                        }
                }

                final ResponseBuilder builder = status(CREATED).location(create(identifier));

                // Add LDP types
                ldpResourceTypes(ldpType).map(IRI::getIRIString).forEach(type -> builder.link(type, "type"));

                return builder;
            }
            throw new BadRequestException("Unable to save resource to persistence layer. Please consult the logs "
                    + "for more information.");

        } catch (final InterruptedException | ExecutionException ex) {
            LOGGER.error("Error persisting data", ex);
        }

        LOGGER.error("Unable to persist data to location at {}", internalId.getIRIString());
        return serverError().type(TEXT_PLAIN_TYPE)
            .entity("Unable to persist data. Please consult the logs for more information");
    }
}
