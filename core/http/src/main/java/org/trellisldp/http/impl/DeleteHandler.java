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

import static java.util.concurrent.CompletableFuture.allOf;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.toQuad;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.impl.RdfUtils.buildEtagHash;
import static org.trellisldp.http.impl.RdfUtils.skolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;

/**
 * The DELETE response builder.
 *
 * @author acoburn
 */
public class DeleteHandler extends MutatingLdpHandler {

    private static final Logger LOGGER = getLogger(DeleteHandler.class);

    /**
     * Create a builder for an LDP DELETE response.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    public DeleteHandler(final LdpRequest req, final ServiceBundler trellis, final String baseUrl) {
        super(req, trellis, baseUrl);
    }

    @Override
    protected String getIdentifier() {
        return super.getIdentifier() + (ACL.equals(getRequest().getExt()) ? "?ext=acl" : "");
    }

    /**
     * Initialze the handler with a Trellis resource.
     *
     * @param parent the parent resource
     * @param resource the Trellis resource
     * @return a response builder
     */
    public ResponseBuilder initialize(final Resource parent, final Resource resource) {

        // Check that the persistence layer supports LDP-R
        if (MISSING_RESOURCE.equals(resource)) {
            // Can't delete a non-existent resources
            throw new NotFoundException();
        } else if (DELETED_RESOURCE.equals(resource)) {
            // Can't delete a non-existent resources
            throw new WebApplicationException(GONE);
        } else if (!supportsInteractionModel(LDP.Resource)) {
            throw new WebApplicationException(status(BAD_REQUEST)
                .link(UnsupportedInteractionModel.getIRIString(), LDP.constrainedBy.getIRIString())
                .entity("Unsupported interaction model provided").type(TEXT_PLAIN_TYPE).build());
        }

        // Check the cache
        final EntityTag etag = new EntityTag(buildEtagHash(getIdentifier(), resource.getModified(), null));
        checkCache(resource.getModified(), etag);

        setResource(resource);
        resource.getContainer().ifPresent(p -> setParent(parent));
        return noContent();
    }

    /**
     * Delete the resource in the persistence layer.
     *
     * @param builder the Trellis response builder
     * @return a response builder promise
     */
    public CompletableFuture<ResponseBuilder> deleteResource(final ResponseBuilder builder) {

        LOGGER.debug("Deleting {}", getIdentifier());

        final TrellisDataset mutable = TrellisDataset.createDataset();
        final TrellisDataset immutable = TrellisDataset.createDataset();

        return handleDeletion(mutable, immutable)
            .thenApply(future -> builder)
            .whenComplete((a, b) -> immutable.close())
            .whenComplete((a, b) -> mutable.close());
    }

    private CompletableFuture<Void> handleDeletion(final TrellisDataset mutable,
            final TrellisDataset immutable) {
        if (ACL.equals(getRequest().getExt())) {
            return handleAclDeletion(mutable, immutable);
        }
        return handleResourceDeletion(mutable, immutable).thenCompose(future ->
                emitEvent(getInternalId(), AS.Delete, LDP.Resource));
    }

    private CompletableFuture<Void> handleAclDeletion(final TrellisDataset mutable,
            final TrellisDataset immutable) {

        // When deleting just the ACL graph, keep the user managed triples intact
        try (final Stream<? extends Triple> triples = getResource().stream(PreferUserManaged)) {
            triples.map(toQuad(PreferUserManaged)).forEachOrdered(mutable::add);
        }

        // Note: when deleting ACL resources, the resource itself is not removed and so this is really
        // more of an update operation. As such, the `replace` method is used and an `update` Audit event
        // is generated.

        // Collect the audit data
        getAuditUpdateData().forEachOrdered(immutable::add);
        return handleResourceReplacement(mutable, immutable);
    }

    private CompletableFuture<Void> handleResourceDeletion(final TrellisDataset mutable,
            final TrellisDataset immutable) {
        // Collect the audit data
        getServices().getAuditService().deletion(getResource().getIdentifier(), getSession()).stream()
            .map(skolemizeQuads(getServices().getResourceService(), getBaseUrl()))
            .forEachOrdered(immutable::add);

        // delete the resource
        return allOf(
                getServices().getResourceService().delete(getResource().getIdentifier(), LDP.Resource,
                    mutable.asDataset()),
                getServices().getResourceService().add(getResource().getIdentifier(), immutable.asDataset()));
    }
}
