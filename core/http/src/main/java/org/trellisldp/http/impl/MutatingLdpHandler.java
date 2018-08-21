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

import static java.util.Base64.getEncoder;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.codec.digest.DigestUtils.getDigest;
import static org.apache.commons.codec.digest.DigestUtils.updateDigest;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_SESSION_BASE_URL;
import static org.trellisldp.http.impl.RdfUtils.skolemizeQuads;
import static org.trellisldp.http.impl.RdfUtils.skolemizeTriples;
import static org.trellisldp.http.impl.RdfUtils.toQuad;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;

/**
 * A common base class for PUT/POST requests.
 *
 * @author acoburn
 */
class MutatingLdpHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(MutatingLdpHandler.class);

    private final File entity;
    private final Session session;

    /**
     * Create a base handler for a mutating LDP response.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    protected MutatingLdpHandler(final LdpRequest req, final ServiceBundler trellis, final String baseUrl) {
        this(req, trellis, baseUrl, null);
    }

    /**
     * Create a base handler for a mutating LDP response.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     * @param entity the entity
     */
    protected MutatingLdpHandler(final LdpRequest req, final ServiceBundler trellis,
            final String baseUrl, final File entity) {
        super(req, trellis, baseUrl);
        this.entity = entity;
        this.session = ofNullable(req.getSecurityContext().getUserPrincipal()).map(Principal::getName)
            .map(getServices().getAgentService()::asAgent).map(HttpSession::new).orElseGet(HttpSession::new);
        session.setProperty(TRELLIS_SESSION_BASE_URL, getBaseUrl());
    }

    /**
     * Update the memento resource.
     *
     * @param builder the Trellis response builder
     * @return a response builder promise
     */
    public CompletableFuture<ResponseBuilder> updateMemento(final ResponseBuilder builder) {
        return getServices().getResourceService().get(getInternalId())
            .thenCompose(getServices().getMementoService()::put)
            .exceptionally(ex -> {
                    LOGGER.warn("Unable to store memento for {}: {}", getInternalId(), ex.getMessage());
                    return null;
                })
            .thenApply(stage -> builder);
    }

    /**
     * Get the Trellis session for this interaction.
     * @return the session
     */
    protected Session getSession() {
        return session;
    }

    /**
     * Get the length of the entity, if an entity is present.
     * @return the length of an entity or null if non is present
     */
    protected Long getEntityLength() {
        return ofNullable(entity).map(File::length).orElse(null);
    }

    /**
     * Get the internal IRI for the resource.
     * @return the resource IRI
     */
    protected IRI getInternalId() {
        return ofNullable(getResource()).map(Resource::getIdentifier).orElse(null);
    }

    /**
     * Read an entity into the provided {@link TrellisDataset}.
     * @param graphName the target graph
     * @param syntax the entity syntax
     * @param dataset the dataset
     */
    protected void readEntityIntoDataset(final IRI graphName, final RDFSyntax syntax,
            final TrellisDataset dataset) {
        try (final InputStream input = new FileInputStream(entity)) {
            getServices().getIOService().read(input, syntax, getIdentifier())
                .map(skolemizeTriples(getServices().getResourceService(), getBaseUrl()))
                .filter(triple -> !RDF.type.equals(triple.getPredicate())
                        || !triple.getObject().ntriplesString().startsWith("<" + LDP.getNamespace()))
                .filter(triple -> !LDP.contains.equals(triple.getPredicate()))
                .map(toQuad(graphName)).forEachOrdered(dataset::add);
        } catch (final RuntimeTrellisException ex) {
            LOGGER.error("Invalid RDF content: {}", ex.getMessage());
            throw new BadRequestException("Invalid RDF content: " + ex.getMessage());
        } catch (final IOException ex) {
            LOGGER.error("Error processing input", ex);
            throw new WebApplicationException("Error processing input");
        }
    }

    /**
     * Check the constraints of a graph.
     * @param graph the graph
     * @param type the LDP interaction model
     * @param syntax the output syntax
     */
    protected void checkConstraint(final Graph graph, final IRI type, final RDFSyntax syntax) {
        ofNullable(graph).map(g ->
                constraintServices.stream().parallel().flatMap(svc -> svc.constrainedBy(type, g)).collect(toList()))
            .filter(violations -> !violations.isEmpty())
            .map(violations -> {
                final ResponseBuilder err = status(CONFLICT);
                violations.forEach(v -> err.link(v.getConstraint().getIRIString(), LDP.constrainedBy.getIRIString()));
                final StreamingOutput stream = new StreamingOutput() {
                    @Override
                    public void write(final OutputStream out) throws IOException {
                        getServices().getIOService().write(violations.stream().flatMap(v2 -> v2.getTriples().stream()),
                                out, syntax);
                    }
                };
                return err.entity(stream);
            }).ifPresent(err -> {
                throw new WebApplicationException(err.build());
            });
    }

    /**
     * Check for a bad digest value.
     * @param digest the digest header, if present
     */
    protected void checkForBadDigest(final Digest digest) {
        if (nonNull(digest)) {
            try (final InputStream input = new FileInputStream(entity)) {
                final String d = getEncoder().encodeToString(updateDigest(getDigest(digest.getAlgorithm()), input)
                        .digest());
                if (!d.equals(digest.getDigest())) {
                    LOGGER.error("Supplied digest value does not match the server-computed digest");
                    throw new BadRequestException("Supplied digest value does not match the server-computed digest.");
                }
            } catch (final IllegalArgumentException ex) {
                LOGGER.error("Invalid algorithm provided for digest. {} is not supported {}",
                        digest.getAlgorithm(), ex.getMessage());
                throw new BadRequestException("Invalid/unsupported algorithm provided for digest.");
            } catch (final IOException ex) {
                LOGGER.error("Error computing checksum on input", ex);
                throw new WebApplicationException("Error computing checksum on input.");
            }
        }
    }

    protected void persistContent(final IRI contentLocation, final Map<String, String> metadata) {
        try (final InputStream input = new FileInputStream(entity)) {
            getServices().getBinaryService().setContent(contentLocation, input, metadata).join();
        } catch (final IOException ex) {
            LOGGER.error("Error saving binary content", ex);
            throw new WebApplicationException("Error saving binary content");
        }
    }

    protected CompletableFuture<Boolean> handleResourceReplacement(final TrellisDataset mutable,
            final TrellisDataset immutable) {
        // update the resource
        final IRI parent = getServices().getResourceService().getContainer(getResource().getIdentifier())
            .orElse(null);
        return getServices().getResourceService()
            .replace(getResource().getIdentifier(), getSession(), getResource().getInteractionModel(),
                    mutable.asDataset(), parent, getResource().getBinary().orElse(null))
            .thenCombine(getServices().getResourceService().add(getResource().getIdentifier(), getSession(),
                        immutable.asDataset()), this::handleWriteResults);
    }

    protected Stream<Quad> getAuditUpdateData() {
        return getServices().getAuditService().update(getResource().getIdentifier(), getSession()).stream()
            .map(skolemizeQuads(getServices().getResourceService(), getBaseUrl()));
    }
}
