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

import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static javax.ws.rs.core.HttpHeaders.IF_MODIFIED_SINCE;
import static javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH;
import static javax.ws.rs.core.HttpHeaders.IF_UNMODIFIED_SINCE;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.http.impl.HttpUtils.loadEtagGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.ws.rs.core.EntityTag;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.Resource;
import org.trellisldp.http.core.EtagGenerator;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.TrellisRequest;

/**
 * @author acoburn
 */
class BaseLdpHandler {

    protected static final RDF rdf = getInstance();

    protected static final EtagGenerator etagGenerator = loadEtagGenerator();
    protected static final List<ConstraintService> constraintServices = new ArrayList<>();

    static {
        ServiceLoader.load(ConstraintService.class).forEach(constraintServices::add);
    }

    private final String requestBaseUrl;
    private final TrellisRequest request;
    private final ServiceBundler services;

    private Resource resource;

    /**
     * A base class for response handling.
     *
     * @param request the LDP request
     * @param services the Trellis service bundle
     * @param baseUrl the base URL
     */
    protected BaseLdpHandler(final TrellisRequest request, final ServiceBundler services, final String baseUrl) {
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
        HttpUtils.checkIfMatch(getRequest().getHeaders().getFirst(IF_MATCH), etag);
        HttpUtils.checkIfUnmodifiedSince(getRequest().getHeaders().getFirst(IF_UNMODIFIED_SINCE), modified);
        HttpUtils.checkIfNoneMatch(getRequest().getMethod(), getRequest().getHeaders().getFirst(IF_NONE_MATCH), etag);
        HttpUtils.checkIfModifiedSince(getRequest().getMethod(), getRequest().getHeaders().getFirst(IF_MODIFIED_SINCE),
                modified);
    }

    /**
     * Check that the given interaction model is supported by the
     * underlying persistence layer.
     *
     * @param interactionModel the interaction model
     * @return true if the interaction model is supported; false otherwise
     */
    protected boolean supportsInteractionModel(final IRI interactionModel) {
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
    protected TrellisRequest getRequest() {
        return request;
    }

    /**
     * Get the Trellis service bundles.
     * @return the services
     */
    protected ServiceBundler getServices() {
        return services;
    }

    private static String getRequestBaseUrl(final TrellisRequest req, final String baseUrl) {
        final String base = baseUrl != null ? baseUrl : req.getBaseUrl();
        if (base.endsWith("/")) {
            return base;
        }
        return base + "/";
    }
}
