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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_SESSION_BASE_URL;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.impl.RdfUtils.buildEtagHash;
import static org.trellisldp.http.impl.RdfUtils.skolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

/**
 * The DELETE response builder.
 *
 * @author acoburn
 */
public class DeleteHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(DeleteHandler.class);

    /**
     * Create a builder for an LDP DELETE response.
     *
     * @param req the LDP request
     * @param resourceService the resource service
     * @param auditService an audit service
     * @param baseUrl the base URL
     */
    public DeleteHandler(final LdpRequest req, final ResourceService resourceService, final AuditService auditService,
                    final String baseUrl) {
        super(req, resourceService, auditService, baseUrl);
    }

    /**
     * Delete the given resource.
     *
     * @param res the resource
     * @return a response builder
     */
    public ResponseBuilder deleteResource(final Resource res) {
        final String baseUrl = getBaseUrl();
        final String identifier = baseUrl + req.getPath();

        final Session session = ofNullable(req.getSession()).orElseGet(HttpSession::new);
        session.setProperty(TRELLIS_SESSION_BASE_URL, baseUrl);

        // Check if this is already deleted
        checkDeleted(res, identifier);

        // Check the cache
        final EntityTag etag = new EntityTag(buildEtagHash(identifier, res.getModified()));
        checkCache(req.getRequest(), res.getModified(), etag);

        LOGGER.debug("Deleting {}", identifier);

        try (final TrellisDataset dataset = TrellisDataset.createDataset()) {

            // When deleting just the ACL graph, keep the user managed triples intact
            if (ACL.equals(req.getExt())) {
                try (final Stream<? extends Triple> triples = res.stream(PreferUserManaged)) {
                    triples.map(t -> rdf.createQuad(PreferUserManaged, t.getSubject(), t.getPredicate(), t.getObject()))
                        .forEachOrdered(dataset::add);
                }

                // Note: when deleting ACL resources, the resource itself is not removed and so this is really
                // more of an update operation. As such, the `replace` method is used and an `update` Audit event
                // is generated.

                // update the resource
                if (resourceService.replace(res.getIdentifier(), session, LDP.Resource, dataset.asDataset()).get()) {

                    // Add the audit quads
                    try (final TrellisDataset auditDataset = TrellisDataset.createDataset()) {
                        audit.update(res.getIdentifier(), session).stream()
                            .map(skolemizeQuads(resourceService, baseUrl)).forEachOrdered(auditDataset::add);
                        if (!resourceService.add(res.getIdentifier(), session, auditDataset.asDataset()).get()) {
                            LOGGER.error("Unable to delete ACL resource at {}", res.getIdentifier());
                            LOGGER.error("because unable to write audit quads: \n{}",
                                        auditDataset.asDataset().stream().map(Quad::toString).collect(joining("\n")));
                            throw new BadRequestException("Unable to write audit information. "
                                    + "Please consult the logs for more information.");
                        }
                    }
                    return status(NO_CONTENT);
                }

            } else {
                // delete the resource
                if (resourceService.delete(res.getIdentifier(), session, LDP.Resource, dataset.asDataset()).get()) {

                    // Add the audit quads
                    try (final TrellisDataset auditDataset = TrellisDataset.createDataset()) {
                        audit.deletion(res.getIdentifier(), session).stream()
                            .map(skolemizeQuads(resourceService, baseUrl)).forEachOrdered(auditDataset::add);
                        if (!resourceService.add(res.getIdentifier(), session, auditDataset.asDataset()).get()) {
                            LOGGER.error("Unable to delete resource at {}", res.getIdentifier());
                            LOGGER.error("because unable to write audit quads: \n{}",
                                        auditDataset.asDataset().stream().map(Quad::toString).collect(joining("\n")));
                            throw new BadRequestException("Unable to write audit information. Please consult the logs "
                                    + "for more information.");
                        }
                    }
                    return status(NO_CONTENT);
                }
            }

            throw new BadRequestException("Unable to save resource to persistence layer. Please consult the logs for "
                    + "more information.");

        } catch (final InterruptedException | ExecutionException ex) {
            LOGGER.error("Error deleting resource", ex);
        }

        LOGGER.error("Unable to delete resource at {}", res.getIdentifier());
        return serverError().entity("Unable to delete resource. Please consult the logs for more information");
    }
}
