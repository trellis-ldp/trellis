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
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.getInstance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.slf4j.Logger;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.LdpRequest;

/**
 * @author acoburn
 */
class BaseLdpHandler {

    private static final Logger LOGGER = getLogger(BaseLdpHandler.class);

    protected static final RDF rdf = getInstance();

    protected static final List<ConstraintService> constraintServices = new ArrayList<>();

    static {
        ServiceLoader.load(ConstraintService.class).forEach(constraintServices::add);
    }

    private final String requestBaseUrl;
    private final LdpRequest request;
    private final ServiceBundler services;

    private Resource resource;

    /**
     * A base class for response handling.
     *
     * @param request the LDP request
     * @param services the Trellis service bundle
     * @param baseUrl the base URL
     */
    protected BaseLdpHandler(final LdpRequest request, final ServiceBundler services, final String baseUrl) {
        this.requestBaseUrl = getRequestBaseUrl(request, baseUrl);
        this.request = request;
        this.services = services;
    }

    /**
     * Set the resource for this request.
     * @param resource the Trellis resource
     */
    protected void setResource(final Resource resource) {
        this.resource = resource;
    }

    /**
     * Get the resource for this request.
     * @return the resource or null if not present
     */
    protected Resource getResource() {
        return resource;
    }

    /**
     * Check the cache.
     * @param modified the modification date
     * @param etag the resource's etag
     */
    protected void checkCache(final Instant modified, final EntityTag etag) {
        ResponseBuilder builder = null;
        try {
            builder = getRequest().getRequest().evaluatePreconditions(from(modified), etag);
        } catch (final Exception ex) {
            LOGGER.warn("Error processing cache request: {}", ex.getMessage());
            throw new BadRequestException();
        }
        if (nonNull(builder)) {
            throw new WebApplicationException(builder.build());
        }
    }

    /**
     * Check that the given interaction model is supported by the
     * underlying persistence layer.
     *
     * @param interactionModel the interaction model
     * @return true if the interaction model is supported; false otherwise
     */
    protected Boolean supportsInteractionModel(final IRI interactionModel) {
        return getServices().getResourceService().supportedInteractionModels().contains(interactionModel);
    }

    /**
     * Get the base URL for this request.
     * @return the base URL
     */
    protected String getBaseUrl() {
        return requestBaseUrl;
    }

    /**
     * Get an identifier for the resource in question.
     * @return an identifier string
     */
    protected String getIdentifier() {
        return getBaseUrl() + getRequest().getPath();
    }

    /**
     * Get the LDP Request object.
     * @return the LDP request object
     */
    protected LdpRequest getRequest() {
        return request;
    }

    /**
     * Get the Trellis service bundles.
     * @return the services
     */
    protected ServiceBundler getServices() {
        return services;
    }

    /**
     * Handle the results of a pair of writes of resource and audit triples.
     * @param mutable the mutable data
     * @param immutable the immutable data
     * @return true if the writes both succeeded; false otherwise
     */
    protected Boolean handleWriteResults(final Boolean mutable, final Boolean immutable) {
        if (!mutable) {
            LOGGER.error("Error adding mutable/managed RDF data to {}", getIdentifier());
        }
        if (!immutable) {
            LOGGER.error("Error adding immutable/audit data to {}", getIdentifier());
        }
        final boolean result = mutable && immutable;
        if (result) {
            LOGGER.debug("Successfully persisted data for {}", getIdentifier());
        }
        return result;
    }

    private static String getRequestBaseUrl(final LdpRequest req, final String baseUrl) {
        final String base = ofNullable(baseUrl).orElseGet(req::getBaseUrl);
        if (base.endsWith("/")) {
            return base;
        }
        return base + "/";
    }
}
