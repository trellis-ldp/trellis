/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
 *
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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static javax.ws.rs.core.HttpHeaders.IF_MODIFIED_SINCE;
import static javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH;
import static javax.ws.rs.core.HttpHeaders.IF_UNMODIFIED_SINCE;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.core.EntityTag;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.TrellisRequest;

/**
 * @author acoburn
 */
class BaseLdpHandler {

    protected static final RDF rdf = RDFFactory.getInstance();

    private final String requestBaseUrl;
    private final TrellisRequest request;
    private final ServiceBundler services;
    private final Map<String, IRI> extensions;

    private Resource resource;

    /**
     * A base class for response handling.
     *
     * @param request the LDP request
     * @param services the Trellis service bundle
     * @param extensions the extension graph mapping
     * @param baseUrl the base URL
     */
    protected BaseLdpHandler(final TrellisRequest request, final ServiceBundler services,
            final Map<String, IRI> extensions, final String baseUrl) {
        this.request = requireNonNull(request, "request may not be null!");
        this.services = requireNonNull(services, "services may not be null!");
        this.extensions = requireNonNull(extensions, "extensions may not be null!");
        this.requestBaseUrl = getRequestBaseUrl(request, baseUrl);
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

    /**
     * Get the graph mapping for the ext url, if one exists.
     *
     * <p>Note: for example the "acl" extension may map to trellis:PreferAccessControl
     *
     * @return the graph IRI for the extension or null if the extension is undefined
     */
    protected IRI getExtensionGraphName() {
        final String ext = getRequest().getExt();
        if (ext != null) {
            return extensions.get(ext);
        }
        return null;
    }

    /**
     * Get all the graph names registered graph extension names.
     *
     * @return the graph names not currently being operated upon
     */
    protected Collection<IRI> getNonCurrentGraphNames() {
        final IRI ext = getExtensionGraphName();
        return extensions.values().stream().map(iri -> iri.equals(ext) ? PreferUserManaged : iri).collect(toSet());
    }

    /**
     * Create a strong entity tag from a resource's revision value.
     * @param res the resource
     * @return an etag
     */
    protected EntityTag generateEtag(final Resource res) {
        return generateEtag(res, false);
    }

    /**
     * Create an entity tag from a resource's revision value.
     * @param res the resource
     * @param weakEtag set this as true to generate a weak etag; false for a strong etag
     * @return an etag
     */
    protected EntityTag generateEtag(final Resource res, final boolean weakEtag) {
        return new EntityTag(sha256Hex(res.getRevision()), weakEtag);
    }

    private static String getRequestBaseUrl(final TrellisRequest req, final String baseUrl) {
        final String base = baseUrl != null ? baseUrl : req.getBaseUrl();
        if (base.endsWith("/")) {
            return base;
        }
        return base + "/";
    }
}
