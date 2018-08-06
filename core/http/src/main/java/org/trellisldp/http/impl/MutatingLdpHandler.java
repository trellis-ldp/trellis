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
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.codec.digest.DigestUtils.getDigest;
import static org.apache.commons.codec.digest.DigestUtils.updateDigest;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_SESSION_BASE_URL;
import static org.trellisldp.http.impl.RdfUtils.skolemizeTriples;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.ConstraintViolation;
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
        if (!mayContinue()) {
            return completedFuture(builder);
        }

        return getServices().getResourceService()
            .get(getInternalId()).thenAccept(getServices().getMementoService()::put)
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
     * @return a response builder if there was an error; otherwise return null
     */
    protected ResponseBuilder readEntityIntoDataset(final IRI graphName, final RDFSyntax syntax,
            final TrellisDataset dataset) {
        try (final InputStream input = new FileInputStream(entity)) {
            getServices().getIOService().read(input, syntax, getIdentifier())
                .map(skolemizeTriples(getServices().getResourceService(), getBaseUrl()))
                .filter(triple -> !RDF.type.equals(triple.getPredicate())
                        || !triple.getObject().ntriplesString().startsWith("<" + LDP.getNamespace()))
                .filter(triple -> !LDP.contains.equals(triple.getPredicate()))
                .map(triple -> rdf.createQuad(graphName, triple.getSubject(), triple.getPredicate(),
                            triple.getObject()))
                .forEachOrdered(dataset::add);
        } catch (final RuntimeTrellisException ex) {
            LOGGER.error("Invalid RDF content: {}", ex.getMessage());
            return status(BAD_REQUEST);
        } catch (final IOException ex) {
            LOGGER.error("Error processing input", ex);
            return status(INTERNAL_SERVER_ERROR);
        }
        return null;
    }

    /**
     * Check the constraints of a graph.
     * @param graph the graph
     * @param type the LDP interaction model
     * @param syntax the output syntax
     * @return a response builder if an error occurred
     */
    protected ResponseBuilder checkConstraint(final Graph graph, final IRI type, final RDFSyntax syntax) {
        if (nonNull(graph)) {
            final List<ConstraintViolation> violations = constraintServices.stream().parallel()
                .flatMap(svc -> svc.constrainedBy(type, graph)).collect(toList());

            if (!violations.isEmpty()) {
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
            }
        }
        return null;
    }

    /**
     * Check for a bad digest value.
     * @param digest the digest header, if present
     * @return a response builder if there was an error; otherwise null
     */
    protected ResponseBuilder checkForBadDigest(final Digest digest) {
        if (nonNull(digest)) {
            try (final InputStream input = new FileInputStream(entity)) {
                final String d = getEncoder().encodeToString(updateDigest(getDigest(digest.getAlgorithm()), input)
                        .digest());
                if (!d.equals(digest.getDigest())) {
                    LOGGER.error("Supplied digest value does not match the server-computed digest");
                    return status(BAD_REQUEST);
                }
            } catch (final IllegalArgumentException ex) {
                LOGGER.error("Invalid algorithm provided for digest. {} is not supported {}",
                        digest.getAlgorithm(), ex.getMessage());
                return status(BAD_REQUEST);
            } catch (final IOException ex) {
                LOGGER.error("Error computing checksum on input", ex);
                return status(INTERNAL_SERVER_ERROR);
            }
        }
        return null;
    }

    protected ResponseBuilder persistContent(final IRI contentLocation, final Map<String, String> metadata) {
        try (final InputStream input = new FileInputStream(entity)) {
            getServices().getBinaryService().setContent(contentLocation, input, metadata);
        } catch (final IOException ex) {
            LOGGER.error("Error saving binary content", ex);
            return status(INTERNAL_SERVER_ERROR);
        }
        return null;
    }
}
