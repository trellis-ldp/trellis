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

import static java.util.Date.from;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.slf4j.Logger;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class BaseLdpHandler {

    private static final Logger LOGGER = getLogger(BaseLdpHandler.class);

    protected static final RDF rdf = getInstance();

    protected static final List<ConstraintService> constraintServices = new ArrayList<>();

    static {
        ServiceLoader.load(ConstraintService.class).forEach(constraintServices::add);
    }

    private final String baseUrl;

    protected final ServiceBundler trellis;

    protected final LdpRequest req;

    /**
     * A base class for response handling.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    public BaseLdpHandler(final LdpRequest req, final ServiceBundler trellis, final String baseUrl) {
        this.baseUrl = baseUrl;
        this.req = req;
        this.trellis = trellis;
    }

    /**
     * Check if this is a deleted resource, and if so return an appropriate response.
     *
     * @param res the resource
     * @param identifier the identifier
     * @throws WebApplicationException a 410 Gone exception
     */
    protected void checkDeleted(final Resource res, final String identifier) {
        if (res.isDeleted()) {
            throw new WebApplicationException(status(GONE).links(MementoResource.getMementoLinks(identifier,
                            trellis.getMementoService().list(res.getIdentifier()))
                    .toArray(Link[]::new)).build());
        }
    }

    /**
     * Get the baseUrl for the request.
     *
     * @return the baseUrl
     */
    protected String getBaseUrl() {
        final String base = ofNullable(baseUrl).orElseGet(req::getBaseUrl);
        if (base.endsWith("/")) {
            return base;
        }
        return base + "/";
    }

    /**
     * Check the request for a cache-related response.
     *
     * @param request the request
     * @param modified the modified time
     * @param etag the etag
     * @throws WebApplicationException either a 412 Precondition Failed or a 304 Not Modified, depending on the context.
     */
    protected static void checkCache(final Request request, final Instant modified, final EntityTag etag) {
        final ResponseBuilder builder = request.evaluatePreconditions(from(modified), etag);
        if (nonNull(builder)) {
            throw new WebApplicationException(builder.build());
        }
    }

    /**
     * Check that the given interaction model is supported by the
     * underlying persistence layer.
     *
     * @param interactionModel the interaction model
     * @throws BadRequestException if the interaction model is not supported
     */
    protected void checkInteractionModel(final IRI interactionModel) {
        if (!trellis.getResourceService().supportedInteractionModels().contains(interactionModel)) {
            LOGGER.error("Interaction model not supported: {}", interactionModel);
            throw new BadRequestException("Unsupported interaction model provided: " + interactionModel,
                    status(BAD_REQUEST)
                        .link(UnsupportedInteractionModel.getIRIString(), LDP.constrainedBy.getIRIString())
                        .entity("Unsupported interaction model provided").type(TEXT_PLAIN_TYPE).build());
        }
    }

}
