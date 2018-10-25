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

import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
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

    @PathParam("path")
    private String path;

    @QueryParam("version")
    private Version version;

    @QueryParam("ext")
    private String ext;

    @QueryParam("subject")
    private String subject;

    @QueryParam("predicate")
    private String predicate;

    @QueryParam("object")
    private String object;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders headers;

    @Context
    private Request request;

    @Context
    private SecurityContext secCtx;

    @HeaderParam("Accept-Datetime")
    private AcceptDatetime datetime;

    @HeaderParam("Prefer")
    private Prefer prefer;

    @HeaderParam("Want-Digest")
    private WantDigest wantDigest;

    @HeaderParam("Range")
    private Range range;

    @HeaderParam("Link")
    private Link link;

    @HeaderParam("Content-Type")
    private String contentType;

    @HeaderParam("Slug")
    private String slug;

    @HeaderParam("Digest")
    private Digest digest;

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
        return datetime;
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
