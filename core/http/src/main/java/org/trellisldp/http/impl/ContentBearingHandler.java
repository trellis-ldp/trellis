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
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.codec.digest.DigestUtils.getDigest;
import static org.apache.commons.codec.digest.DigestUtils.updateDigest;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.http.impl.RdfUtils.skolemizeTriples;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;

/**
 * A common base class for PUT/POST requests.
 *
 * @author acoburn
 */
class ContentBearingHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(ContentBearingHandler.class);

    protected final File entity;

    /**
     * Create a builder for an LDP POST response.
     *
     * @param req the LDP request
     * @param entity the entity
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    protected ContentBearingHandler(final LdpRequest req, final File entity, final ServiceBundler trellis,
            final String baseUrl) {
        super(req, trellis, baseUrl);
        this.entity = entity;
    }

    protected ResponseBuilder readEntityIntoDataset(final String identifier, final String baseUrl, final IRI graphName,
            final RDFSyntax syntax, final TrellisDataset dataset) {
        try (final InputStream input = new FileInputStream(entity)) {
            trellis.getIOService().read(input, syntax, identifier)
                .map(skolemizeTriples(trellis.getResourceService(), baseUrl))
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

    protected ResponseBuilder checkConstraint(final TrellisDataset dataset, final IRI graphName, final IRI type,
            final RDFSyntax syntax) {
        final List<ConstraintViolation> violations = constraintServices.stream().parallel().flatMap(svc ->
                dataset.getGraph(graphName).map(g -> svc.constrainedBy(type, g)).orElseGet(Stream::empty))
            .collect(toList());

        if (!violations.isEmpty()) {
            final ResponseBuilder err = status(CONFLICT);
            violations.forEach(v -> err.link(v.getConstraint().getIRIString(), LDP.constrainedBy.getIRIString()));
            final StreamingOutput stream = new StreamingOutput() {
                @Override
                public void write(final OutputStream out) throws IOException {
                    trellis.getIOService().write(violations.stream().flatMap(v2 -> v2.getTriples().stream()), out,
                            syntax);
                }
            };
            return err.entity(stream);
        }
        return null;
    }

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
            trellis.getBinaryService().setContent(contentLocation, input, metadata);
        } catch (final IOException ex) {
            LOGGER.error("Error saving binary content", ex);
            return status(INTERNAL_SERVER_ERROR);
        }
        return null;
    }
}
