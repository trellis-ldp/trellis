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
package org.trellisldp.http.core;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.core.HttpConstants.PREFER;
import static org.trellisldp.http.core.HttpConstants.RANGE;
import static org.trellisldp.http.core.HttpConstants.SLUG;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * A class representing an HTTP request with various LDP-related headers and query parameters.
 *
 * @author acoburn
 */
public class TrellisRequest {

    private final boolean trailingSlash;
    private final String path;
    private final String baseUrl;
    private final String method;
    private final List<MediaType> acceptableMediaTypes;

    private final MultivaluedMap<String, String> headers;
    private final MultivaluedMap<String, String> parameters;
    private final SecurityContext secCtx;

    /**
     * Bundle together some request contexts.
     * @param request the Request object
     * @param uriInfo the URI information
     * @param headers the HTTP headers
     */
    public TrellisRequest(final Request request, final UriInfo uriInfo, final HttpHeaders headers) {
        this(request, uriInfo, headers, null);
    }

    /**
     * Bundle together some request contexts.
     * @param request the Request object
     * @param uriInfo the URI information
     * @param headers the HTTP headers
     * @param secCtx the security context
     */
    public TrellisRequest(final Request request, final UriInfo uriInfo, final HttpHeaders headers,
            final SecurityContext secCtx) {
        // Extract header values
        this.headers = headers.getRequestHeaders();
        this.acceptableMediaTypes = headers.getAcceptableMediaTypes();

        // Extract URI values
        this.parameters = uriInfo.getQueryParameters();
        this.baseUrl = buildBaseUrl(uriInfo.getBaseUri(), this.headers);
        this.path = uriInfo.getPathParameters().getFirst("path");
        this.trailingSlash = uriInfo.getPath().endsWith("/");

        // Extract request method
        this.method = request.getMethod();

        // Security context value
        this.secCtx = secCtx;
    }

    /**
     * Get the Content-Type header.
     *
     * @return the Content-Type header
     */
    public String getContentType() {
        return headers.getFirst(CONTENT_TYPE);
    }

    /**
     * Get the slug header.
     *
     * @return the decoded value of the slug header
     */
    public String getSlug() {
        final Slug slug = Slug.valueOf(headers.getFirst(SLUG));
        if (slug != null && !slug.getValue().isEmpty()) {
            return slug.getValue();
        }
        return null;
    }

    /**
     * Get the Link header.
     *
     * @return the Link header
     */
    public Link getLink() {
        final String link = headers.getFirst(LINK);
        if (link != null) {
            return Link.valueOf(link);
        }
        return null;
    }

    /**
     * Get the Accept-Datetime value.
     *
     * @return the accept-datetime header
     */
    public AcceptDatetime getDatetime() {
        return AcceptDatetime.valueOf(headers.getFirst(ACCEPT_DATETIME));
    }

    /**
     * Get the prefer header.
     *
     * @return the Prefer header
     */
    public Prefer getPrefer() {
        return Prefer.valueOf(headers.getFirst(PREFER));
    }

    /**
     * Get the range header.
     *
     * @return the range header
     */
    public Range getRange() {
        return Range.valueOf(headers.getFirst(RANGE));
    }

    /**
     * Get the path.
     *
     * <p>This method returns a normalized resource path for use with internal
     * identifiers. That means that any trailing slash will not be present. Please
     * see the {@link #hasTrailingSlash} method to check for a trailing slash.
     *
     * @return the normalized path
     */
    public String getPath() {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Test whether the path has a trailing slash.
     *
     * @return true if the request path ends in a slash
     */
    public boolean hasTrailingSlash() {
        return trailingSlash;
    }

    /**
     * Get the version value.
     *
     * @return the version query parameter
     */
    public Version getVersion() {
        return Version.valueOf(parameters.getFirst("version"));
    }

    /**
     * Get the ext value.
     *
     * @return the ext query parameter
     */
    public String getExt() {
        return parameters.getFirst("ext");
    }

    /**
     * Get a base url value.
     *
     * @return the baseUrl as a string
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Get the security context.
     *
     * @return the security context
     */
    public SecurityContext getSecurityContext() {
        return secCtx;
    }

    /**
     * Get the HTTP method.
     * @return the method name
     */
    public String getMethod() {
        return method;
    }

    /**
     * Get all of the headers.
     * @return the headers
     */
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * Get the HTTP headers.
     *
     * @return the http headers
     */
    public List<MediaType> getAcceptableMediaTypes() {
        return acceptableMediaTypes;
    }

    public static String buildBaseUrl(final URI uri, final MultivaluedMap<String, String> headers) {
        // Start with the baseURI from the request
        final UriBuilder builder = UriBuilder.fromUri(uri);

        // Adjust the protocol, using the non-spec X-Forwarded-* header, if present
        final Forwarded nonSpec = new Forwarded(null, headers.getFirst("X-Forwarded-For"),
                headers.getFirst("X-Forwarded-Host"), headers.getFirst("X-Forwarded-Proto"));
        nonSpec.getHostname().ifPresent(builder::host);
        nonSpec.getPort().ifPresent(builder::port);
        nonSpec.getProto().ifPresent(builder::scheme);

        // The standard Forwarded header overrides these values
        final Forwarded forwarded = Forwarded.valueOf(headers.getFirst("Forwarded"));
        if (forwarded != null) {
            forwarded.getHostname().ifPresent(builder::host);
            forwarded.getPort().ifPresent(builder::port);
            forwarded.getProto().ifPresent(builder::scheme);
        }
        return builder.build().toString();
    }
}
