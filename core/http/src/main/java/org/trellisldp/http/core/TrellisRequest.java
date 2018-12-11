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

import static java.util.Date.from;
import static java.util.Objects.nonNull;
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
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * A class representing an HTTP request with various LDP-related headers and query parameters.
 *
 * @author acoburn
 */
public class TrellisRequest {

    private final Request request;

    private final String contentType;
    private final String slug;
    private final Link link;
    private final AcceptDatetime dateTime;
    private final Prefer prefer;
    private final WantDigest wantDigest;
    private final Digest digest;
    private final Range range;
    private final String path;
    private final String ext;
    private final Version version;
    private final String baseUrl;
    private final String subject;
    private final String predicate;
    private final String object;
    private final String principalName;
    private final List<MediaType> acceptableMediaTypes;
    private final String method;

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
        this.request = request;

        // Extract header values
        this.contentType = headers.getHeaderString(CONTENT_TYPE);
        this.slug = headers.getHeaderString(SLUG);
        this.link = ofNullable(headers.getHeaderString(LINK)).map(Link::valueOf).orElse(null);
        this.dateTime = ofNullable(headers.getHeaderString(ACCEPT_DATETIME)).map(AcceptDatetime::valueOf).orElse(null);
        this.prefer = ofNullable(headers.getHeaderString(PREFER)).map(Prefer::valueOf).orElse(null);
        this.wantDigest = ofNullable(headers.getHeaderString(WANT_DIGEST)).map(WantDigest::new).orElse(null);
        this.digest = ofNullable(headers.getHeaderString(DIGEST)).map(Digest::valueOf).orElse(null);
        this.range = ofNullable(headers.getHeaderString(RANGE)).map(Range::valueOf).orElse(null);
        this.acceptableMediaTypes = headers.getAcceptableMediaTypes();

        // Extract URI values
        this.path = uriInfo.getPath();
        this.version = ofNullable(uriInfo.getQueryParameters().getFirst("version")).map(Version::valueOf).orElse(null);
        this.ext = uriInfo.getQueryParameters().getFirst("ext");
        this.baseUrl = uriInfo.getBaseUri().toString();
        this.subject = uriInfo.getQueryParameters().getFirst("subject");
        this.predicate = uriInfo.getQueryParameters().getFirst("predicate");
        this.object = uriInfo.getQueryParameters().getFirst("object");

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
        return contentType;
    }

    /**
     * Get the slug header.
     *
     * @return the value of the slug header
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Get the Link header.
     *
     * @return the Link header
     */
    public Link getLink() {
        return link;
    }

    /**
     * Get the Accept-Datetime value.
     *
     * @return the accept-datetime header
     */
    public AcceptDatetime getDatetime() {
        return dateTime;
    }

    /**
     * Get the prefer header.
     *
     * @return the Prefer header
     */
    public Prefer getPrefer() {
        return prefer;
    }

    /**
     * Get the Want-Digest header.
     *
     * @return the Want-Digest header
     */
    public WantDigest getWantDigest() {
        return wantDigest;
    }

    /**
     * Get the Digest header.
     *
     * @return the Digest header
     */
    public Digest getDigest() {
        return digest;
    }

    /**
     * Get the range header.
     *
     * @return the range header
     */
    public Range getRange() {
        return range;
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
        return version;
    }

    /**
     * Get the ext value.
     *
     * @return the ext query parameter
     */
    public String getExt() {
        return ext;
    }

    /**
     * Get the subject filter.
     *
     * @return the subject filter
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Get the predicate filter.
     *
     * @return the predicate filter
     */
    public String getPredicate() {
        return predicate;
    }

    /**
     * Get the object filter.
     *
     * @return the object filter
     */
    public String getObject() {
        return object;
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
     * Get the request value.
     *
     * @param time the time of the request
     * @param etag an etag for the resource
     * @return a request builder if the preconditions are not met; null otherwise
     */
    public ResponseBuilder evaluatePreconditions(final Instant time, final EntityTag etag) {
        if (nonNull(request)) {
            return request.evaluatePreconditions(from(time), etag);
        }
        return null;
    }

    /**
     * Get the HTTP headers.
     *
     * @return the http headers
     */
    public List<MediaType> getAcceptableMediaTypes() {
        return acceptableMediaTypes;
    }
}
