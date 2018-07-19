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
import static java.util.Date.from;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_SESSION_BASE_URL;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.impl.RdfUtils.buildEtagHash;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.RdfUtils.skolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.File;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

/**
 * The PUT response handler.
 *
 * @author acoburn
 */
public class PutHandler extends ContentBearingHandler {

    private static final Logger LOGGER = getLogger(PutHandler.class);

    /**
     * Create a builder for an LDP PUT response.
     *
     * @param req the LDP request
     * @param entity the entity
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    public PutHandler(final LdpRequest req, final File entity, final ServiceBundler trellis, final String baseUrl) {
        super(req, entity, trellis, baseUrl);
    }

    private IRI getActiveGraphName() {
        return ACL.equals(req.getExt()) ? PreferAccessControl : PreferUserManaged;
    }

    private IRI getInactiveGraphName() {
        return ACL.equals(req.getExt()) ? PreferUserManaged : PreferAccessControl;
    }

    private Boolean isAclAndNonRdfContent(final Optional<RDFSyntax> syntax) {
        return ACL.equals(req.getExt()) && !syntax.isPresent();
    }

    /**
     * Set the data for a resource.
     *
     * @return the response builder
     */
    public ResponseBuilder createResource() {
        return setResource(null);
    }

    /**
     * Set the data for a resource.
     *
     * @param res the resource
     * @return the response builder
     */
    public ResponseBuilder setResource(final Resource res) {
        final String baseUrl = getBaseUrl();
        final String identifier = buildIdentifier(baseUrl);

        // Check the cache
        if (nonNull(res)) {
            final EntityTag etag;
            final Instant modified;
            final Optional<Instant> binaryModification = res.getBinary().map(Binary::getModified);

            if (binaryModification.isPresent() &&
                    !ofNullable(req.getContentType()).flatMap(RDFSyntax::byMediaType).isPresent()) {
                modified = binaryModification.get();
                etag = new EntityTag(buildEtagHash(identifier + "BINARY", modified, null));
            } else {
                modified = res.getModified();
                etag = new EntityTag(buildEtagHash(identifier, modified, req.getPrefer()), true);
            }
            // Check the cache
            try {
                final ResponseBuilder cache = req.getRequest().evaluatePreconditions(from(modified), etag);
                if (nonNull(cache)) {
                    return cache;
                }
            } catch (final Exception ex) {
                LOGGER.warn("Error parsing request: {}", ex.getMessage());
                return status(BAD_REQUEST);
            }
        }

        final Session session = ofNullable(req.getSecurityContext().getUserPrincipal()).map(Principal::getName)
            .map(trellis.getAgentService()::asAgent).map(HttpSession::new).orElseGet(HttpSession::new);
        session.setProperty(TRELLIS_SESSION_BASE_URL, baseUrl);

        final Optional<RDFSyntax> rdfSyntax = ofNullable(req.getContentType()).map(MediaType::valueOf).flatMap(ct ->
                trellis.getIOService().supportedWriteSyntaxes().stream().filter(s ->
                    ct.isCompatible(MediaType.valueOf(s.mediaType()))).findFirst());

        // One cannot put binaries into the ACL graph
        if (isAclAndNonRdfContent(rdfSyntax)) {
            return status(NOT_ACCEPTABLE);
        }

        LOGGER.debug("Setting resource as {}", identifier);

        final IRI heuristicType = getHeuristicType(rdfSyntax);

        final IRI defaultType = ofNullable(res).map(Resource::getInteractionModel).orElse(heuristicType);

        final Boolean isBinaryDescription = nonNull(res) && LDP.NonRDFSource.equals(res.getInteractionModel())
            && rdfSyntax.isPresent();

        final IRI ldpType = isBinaryDescription ? LDP.NonRDFSource : ofNullable(req.getLink())
            .filter(l -> "type".equals(l.getRel())).map(Link::getUri).map(URI::toString)
            .filter(l -> l.startsWith(LDP.getNamespace())).map(rdf::createIRI).filter(l -> !LDP.Resource.equals(l))
            .orElse(defaultType);

        // Verify that the persistence layer supports the given interaction model
        ResponseBuilder error = checkInteractionModel(LDP.RDFSource);
        if (nonNull(error)) {
            return error;
        }

        LOGGER.debug("Using LDP Type: {}", ldpType);
        // It is not possible to change the LDP type to a type that is not a subclass
        error = checkInteractionModelChange(res, ldpType, isBinaryDescription);
        if (nonNull(error)) {
            return error;
        }

        final IRI internalId = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());

        try (final TrellisDataset dataset = TrellisDataset.createDataset()) {
            final IRI graphName = getActiveGraphName();
            final IRI otherGraph = getInactiveGraphName();
            final Binary binary;

            // Add user-supplied data
            if (isBinaryRequest(ldpType, rdfSyntax)) {
                // Check the expected digest value
                final ResponseBuilder digest = checkForBadDigest(req.getDigest());
                if (nonNull(digest)) {
                    return digest;
                }

                final String mimeType = ofNullable(req.getContentType()).orElse(APPLICATION_OCTET_STREAM);
                final IRI binaryLocation = rdf.createIRI(trellis.getBinaryService().generateIdentifier());

                // Persist the content
                final ResponseBuilder persist = persistContent(binaryLocation, singletonMap(CONTENT_TYPE, mimeType));
                if (nonNull(persist)) {
                    return persist;
                }

                binary = new Binary(binaryLocation, now(), mimeType, entity.length());
            } else {
                error = readEntityIntoDataset(identifier, baseUrl, graphName,
                        rdfSyntax.orElse(TURTLE), dataset);
                if (nonNull(error)) {
                    return error;
                }

                // Check for any constraints
                final ResponseBuilder constraints;
                if (ACL.equals(req.getExt())) {
                    constraints = checkConstraint(dataset, PreferAccessControl, LDP.RDFSource,
                            rdfSyntax.orElse(TURTLE));
                } else {
                    constraints = checkConstraint(dataset, PreferUserManaged, ldpType, rdfSyntax.orElse(TURTLE));
                }
                if (nonNull(constraints)) {
                    return constraints;
                }
                binary = ofNullable(res).flatMap(Resource::getBinary).orElse(null);
            }

            ofNullable(res).ifPresent(r -> {
                try (final Stream<? extends Triple> remaining = res.stream(otherGraph)) {
                    remaining.map(t -> rdf.createQuad(otherGraph, t.getSubject(), t.getPredicate(), t.getObject()))
                        .forEachOrdered(dataset::add);
                }
            });

            final Future<Boolean> success = createOrReplace(res, internalId, session, ldpType, dataset, binary);
            if (success.get()) {
                // Add audit quads
                try (final TrellisDataset auditDataset = TrellisDataset.createDataset()) {
                    auditQuads(res, internalId, session).stream().map(skolemizeQuads(trellis.getResourceService(),
                                baseUrl)).forEachOrdered(auditDataset::add);
                    if (!trellis.getResourceService().add(internalId, session, auditDataset.asDataset()).get()) {
                        LOGGER.error("Unable to place or replace resource at {}", internalId);
                        LOGGER.error("because unable to write audit quads: \n{}",
                                        auditDataset.asDataset().stream().map(Quad::toString).collect(joining("\n")));
                        return status(BAD_REQUEST);
                    }
                }

                // Create a memento
                trellis.getResourceService().get(internalId).thenAccept(trellis.getMementoService()::put)
                    .exceptionally(ex -> {
                        LOGGER.warn("Unable to store memento: {}", ex.getMessage());
                        return null;
                    }).join();

                final ResponseBuilder builder = buildResponse(res, identifier);
                getLdpLinkTypes(ldpType, isBinaryDescription).map(IRI::getIRIString)
                    .forEach(type -> builder.link(type, "type"));
                return builder;
            }
            return status(BAD_REQUEST);

        } catch (final InterruptedException | ExecutionException ex) {
            LOGGER.error("Error persisting data", ex);
        }

        LOGGER.error("Unable to persist data to location at {}", internalId.getIRIString());
        return serverError().type(TEXT_PLAIN_TYPE)
            .entity("Unable to persist data. Please consult the logs for more information");
    }

    private Future<Boolean> createOrReplace(final Resource res, final IRI internalId, final Session session,
            final IRI ldpType, final TrellisDataset dataset, final Binary binary) {
        final IRI container = trellis.getResourceService().getContainer(internalId).orElse(null);
        return nonNull(res)
            ? trellis.getResourceService().replace(internalId, session, ldpType, dataset.asDataset(), container, binary)
            : trellis.getResourceService().create(internalId, session, ldpType, dataset.asDataset(), container, binary);
    }

    private ResponseBuilder checkInteractionModelChange(final Resource res, final IRI ldpType,
            final Boolean isBinaryDescription) {
        if (nonNull(res) && !isBinaryDescription && ldpResourceTypes(ldpType)
                .noneMatch(res.getInteractionModel()::equals)) {
            LOGGER.error("Cannot change the LDP type to {} for {}", ldpType, res.getIdentifier());
            return status(CONFLICT);
        }
        return null;
    }

    private ResponseBuilder buildResponse(final Resource res, final String identifier) {
        if (nonNull(res)) {
            return status(NO_CONTENT);
        }
        return status(CREATED).contentLocation(create(identifier));
    }

    private Boolean isBinaryRequest(final IRI ldpType, final Optional<RDFSyntax> rdfSyntax) {
        return LDP.NonRDFSource.equals(ldpType) && !rdfSyntax.isPresent();
    }

    private String buildIdentifier(final String baseUrl) {
        return baseUrl + req.getPath() + (ACL.equals(req.getExt()) ? "?ext=acl" : "");
    }

    private IRI getHeuristicType(final Optional<RDFSyntax> rdfSyntax) {
        return nonNull(req.getContentType()) && !rdfSyntax.isPresent() ? LDP.NonRDFSource : LDP.RDFSource;
    }

    private static Stream<IRI> getLdpLinkTypes(final IRI ldpType, final Boolean isBinaryDescription) {
        if (LDP.NonRDFSource.equals(ldpType) && isBinaryDescription) {
            return ldpResourceTypes(LDP.RDFSource);
        }
        return ldpResourceTypes(ldpType);
    }

    private List<Quad> auditQuads(final Resource res, final IRI internalId, final Session session) {
        if (nonNull(res)) {
            return trellis.getAuditService().update(internalId, session);
        }
        return trellis.getAuditService().creation(internalId, session);
    }
}
