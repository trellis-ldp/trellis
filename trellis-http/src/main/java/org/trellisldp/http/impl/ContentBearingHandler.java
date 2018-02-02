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
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.codec.digest.DigestUtils.getDigest;
import static org.apache.commons.codec.digest.DigestUtils.updateDigest;
import static org.trellisldp.http.impl.RdfUtils.skolemizeTriples;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.api.IOService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

/**
 * A common base class for PUT/POST requests.
 *
 * @author acoburn
 */
class ContentBearingHandler extends BaseLdpHandler {

    protected final BinaryService binaryService;
    protected final IOService ioService;
    protected final File entity;

    /**
     * Create a builder for an LDP POST response.
     *
     * @param baseUrl the base URL
     * @param req the LDP request
     * @param entity the entity
     * @param resourceService the resource service
     * @param auditService an audit service
     * @param ioService the serialization service
     * @param binaryService the binary service
     */
    protected ContentBearingHandler(final LdpRequest req, final File entity, final ResourceService resourceService,
                    final AuditService auditService, final IOService ioService, final BinaryService binaryService,
                    final String baseUrl) {
        super(req, resourceService, auditService, baseUrl);
        this.binaryService = binaryService;
        this.ioService = ioService;
        this.entity = entity;
    }

    protected void readEntityIntoDataset(final String identifier, final String baseUrl, final IRI graphName,
            final RDFSyntax syntax, final TrellisDataset dataset) {
        try (final InputStream input = new FileInputStream(entity)) {
            ioService.read(input, identifier, syntax)
                .map(skolemizeTriples(resourceService, baseUrl))
                .filter(triple -> !LDP.contains.equals(triple.getPredicate()))
                .map(triple -> rdf.createQuad(graphName, triple.getSubject(), triple.getPredicate(),
                            triple.getObject()))
                .forEachOrdered(dataset::add);
        } catch (final RuntimeTrellisException ex) {
            throw new BadRequestException("Invalid RDF content: " + ex.getMessage());
        } catch (final IOException ex) {
            throw new WebApplicationException("Error processing input", ex);
        }
    }

    protected void checkConstraint(final TrellisDataset dataset, final IRI graphName, final IRI type,
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
                    ioService.write(violations.stream().flatMap(v2 -> v2.getTriples().stream()), out, syntax);
                }
            };
            throw new WebApplicationException(err.entity(stream).build());
        }
    }

    protected Boolean isBadDigest(final Digest digest) {
        return nonNull(digest) && !getDigestForEntity(digest).equals(digest.getDigest());
    }

    protected String getDigestForEntity(final Digest digest) {
        try (final InputStream input = new FileInputStream(entity)) {
            return getEncoder().encodeToString(updateDigest(getDigest(digest.getAlgorithm()), input).digest());
        } catch (final IllegalArgumentException ex) {
            throw new BadRequestException("Invalid algorithm provided for digest. " + digest.getAlgorithm() +
                    " is not supported: " + ex.getMessage());
        } catch (final IOException ex) {
            throw new WebApplicationException("Error computing checksum on input", ex);
        }
    }

    protected void persistContent(final IRI contentLocation, final Map<String, String> metadata) {
        try (final InputStream input = new FileInputStream(entity)) {
            binaryService.setContent(contentLocation, input, metadata);
        } catch (final IOException ex) {
            throw new WebApplicationException(ex);
        }
    }
}
