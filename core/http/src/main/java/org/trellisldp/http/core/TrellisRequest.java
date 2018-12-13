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
package org.trellisldp.http.core;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.core.HttpConstants.DIGEST;
import static org.trellisldp.http.core.HttpConstants.PREFER;
import static org.trellisldp.http.core.HttpConstants.RANGE;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.HttpConstants.WANT_DIGEST;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * A class representing an HTTP request with various LDP-related headers and query parameters.
 *
 * @author acoburn
 */
public class TrellisRequest {

    private final String path;
    private final String baseUrl;
    private final String principalName;
    private final String method;
    private final List<MediaType> acceptableMediaTypes;

    private final MultivaluedMap<String, String> headers;
    private final Map<String, String> parameters = new HashMap<>();

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
        this.acceptableMediaTypes = new ArrayList<>(headers.getAcceptableMediaTypes());
        this.headers = new MultivaluedHashMap<String, String>(headers.getRequestHeaders());

        // Extract URI values
        this.baseUrl = uriInfo.getBaseUri().toString();
        this.path = uriInfo.getPathParameters().getFirst("path");
        uriInfo.getQueryParameters().forEach((key, values) ->
                values.stream().findFirst().ifPresent(v -> parameters.put(key, v)));

        // Extract request method
        this.method = request.getMethod();

        // Security context value
        this.principalName = ofNullable(secCtx).map(SecurityContext::getUserPrincipal)
            .filter(Objects::nonNull).map(Principal::getName).orElse(null);
    }

    /**
     * Get the Content-Type header.
     *
     * @return the Content-Type header
     */
    public String getContentType() {
        return getFirst(CONTENT_TYPE).orElse(null);
    }

    /**
     * Get the slug header.
     *
     * @return the value of the slug header
     */
    public String getSlug() {
        return getFirst(SLUG).orElse(null);
    }

    /**
     * Get the Link header.
     *
     * @return the Link header
     */
    public Link getLink() {
        return getFirst(LINK).map(Link::valueOf).orElse(null);
    }

    /**
     * Get the Accept-Datetime value.
     *
     * @return the accept-datetime header
     */
    public AcceptDatetime getDatetime() {
        return getFirst(ACCEPT_DATETIME).map(AcceptDatetime::valueOf).orElse(null);
    }

    /**
     * Get the prefer header.
     *
     * @return the Prefer header
     */
    public Prefer getPrefer() {
        return getFirst(PREFER).map(Prefer::valueOf).orElse(null);
    }

    /**
     * Get the Want-Digest header.
     *
     * @return the Want-Digest header
     */
    public WantDigest getWantDigest() {
        return getFirst(WANT_DIGEST).map(WantDigest::new).orElse(null);
    }

    /**
     * Get the Digest header.
     *
     * @return the Digest header
     */
    public Digest getDigest() {
        return getFirst(DIGEST).map(Digest::valueOf).orElse(null);
    }

    /**
     * Get the range header.
     *
     * @return the range header
     */
    public Range getRange() {
        return getFirst(RANGE).map(Range::valueOf).orElse(null);
    }

    /**
     * Get the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the version value.
     *
     * @return the version query parameter
     */
    public Version getVersion() {
        return ofNullable(parameters.get("version")).map(Version::valueOf).orElse(null);
    }

    /**
     * Get the ext value.
     *
     * @return the ext query parameter
     */
    public String getExt() {
        return parameters.get("ext");
    }

    /**
     * Get the subject filter.
     *
     * @return the subject filter
     */
    public String getSubject() {
        return parameters.get("subject");
    }

    /**
     * Get the predicate filter.
     *
     * @return the predicate filter
     */
    public String getPredicate() {
        return parameters.get("predicate");
    }

    /**
     * Get the object filter.
     *
     * @return the object filter
     */
    public String getObject() {
        return parameters.get("object");
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
    public String getPrincipalName() {
        return principalName;
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
     * @implNote All header keys will be lower case
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

    private Optional<String> getFirst(final String key) {
        return ofNullable(headers.getFirst(key.toLowerCase()));
    }
}
