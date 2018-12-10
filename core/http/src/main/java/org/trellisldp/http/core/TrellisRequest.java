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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * A class representing an HTTP request with various LDP-related headers and query parameters.
 *
 * @author acoburn
 */
public class TrellisRequest {

    private final UriInfo uriInfo;

    private final HttpHeaders headers;

    private final Request request;

    private final SecurityContext secCtx;

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
        this.uriInfo = uriInfo;
        this.headers = headers;
        this.secCtx = secCtx;
    }

    /**
     * Get the Content-Type header.
     *
     * @return the Content-Type header
     */
    public String getContentType() {
        return headers.getHeaderString(CONTENT_TYPE);
    }

    /**
     * Get the slug header.
     *
     * @return the value of the slug header
     */
    public String getSlug() {
        return headers.getHeaderString(SLUG);
    }

    /**
     * Get the Link header.
     *
     * @return the Link header
     */
    public Link getLink() {
        return ofNullable(headers.getHeaderString(LINK)).map(Link::valueOf).orElse(null);
    }

    /**
     * Get the Accept-Datetime value.
     *
     * @return the accept-datetime header
     */
    public AcceptDatetime getDatetime() {
        return ofNullable(headers.getHeaderString(ACCEPT_DATETIME)).map(AcceptDatetime::valueOf).orElse(null);
    }

    /**
     * Get the prefer header.
     *
     * @return the Prefer header
     */
    public Prefer getPrefer() {
        return ofNullable(headers.getHeaderString(PREFER)).map(Prefer::valueOf).orElse(null);
    }

    /**
     * Get the Want-Digest header.
     *
     * @return the Want-Digest header
     */
    public WantDigest getWantDigest() {
        return ofNullable(headers.getHeaderString(WANT_DIGEST)).map(WantDigest::new).orElse(null);
    }

    /**
     * Get the Digest header.
     *
     * @return the Digest header
     */
    public Digest getDigest() {
        return ofNullable(headers.getHeaderString(DIGEST)).map(Digest::valueOf).orElse(null);
    }

    /**
     * Get the range header.
     *
     * @return the range header
     */
    public Range getRange() {
        return ofNullable(headers.getHeaderString(RANGE)).map(Range::valueOf).orElse(null);
    }

    /**
     * Get the path.
     *
     * @return the path
     */
    public String getPath() {
        return uriInfo.getPath();
    }

    /**
     * Get the version value.
     *
     * @return the version query parameter
     */
    public Version getVersion() {
        return ofNullable(uriInfo.getQueryParameters().getFirst("version")).map(Version::valueOf).orElse(null);
    }

    /**
     * Get the ext value.
     *
     * @return the ext query parameter
     */
    public String getExt() {
        return uriInfo.getQueryParameters().getFirst("ext");
    }

    /**
     * Get the request value.
     *
     * @return the request
     */
    public Request getRequest() {
        return request;
    }

    /**
     * Get the HTTP headers.
     *
     * @return the http headers
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * Get the subject filter.
     *
     * @return the subject filter
     */
    public String getSubject() {
        return uriInfo.getQueryParameters().getFirst("subject");
    }

    /**
     * Get the predicate filter.
     *
     * @return the predicate filter
     */
    public String getPredicate() {
        return uriInfo.getQueryParameters().getFirst("predicate");
    }

    /**
     * Get the object filter.
     *
     * @return the object filter
     */
    public String getObject() {
        return uriInfo.getQueryParameters().getFirst("object");
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
     * Get a base url value.
     *
     * @return the baseUrl as a string
     */
    public String getBaseUrl() {
        return uriInfo.getBaseUri().toString();
    }
}
