/*
 * Copyright (c) Aaron Coburn and individual contributors
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
package org.trellisldp.webdav;

import static jakarta.ws.rs.HttpMethod.DELETE;
import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;
import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.Link.TYPE;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.status;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.common.HttpConstants.CONFIG_HTTP_BASE_URL;
import static org.trellisldp.common.HttpConstants.CONFIG_HTTP_PUT_UNCONTAINED;
import static org.trellisldp.common.HttpConstants.SLUG;
import static org.trellisldp.webdav.impl.WebDAVUtils.getAllButLastSegment;
import static org.trellisldp.webdav.impl.WebDAVUtils.getLastSegment;
import static org.trellisldp.webdav.impl.WebDAVUtils.recursiveDelete;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

import org.apache.commons.rdf.api.IRI;
import org.eclipse.microprofile.config.Config;
import org.trellisldp.api.Resource;
import org.trellisldp.common.HttpSession;
import org.trellisldp.common.ServiceBundler;
import org.trellisldp.vocabulary.LDP;

@Provider
@PreMatching
public class TrellisWebDAVRequestFilter implements ContainerRequestFilter {

    @Inject
    ServiceBundler services;

    boolean createUncontained;
    String baseUrl;

    /**
     * Create a Trellis HTTP request filter for WebDAV.
     */
    public TrellisWebDAVRequestFilter() {
        final Config config = getConfig();
        this.createUncontained = config.getOptionalValue(CONFIG_HTTP_PUT_UNCONTAINED, Boolean.class)
            .orElse(Boolean.FALSE);
        this.baseUrl = config.getOptionalValue(CONFIG_HTTP_BASE_URL, String.class).orElse(null);
    }

    @Override
    public void filter(final ContainerRequestContext ctx) {
        final IRI identifier = services.getResourceService()
            .getResourceIdentifier(getBaseUrl(baseUrl, ctx.getUriInfo()), ctx.getUriInfo().getPath());
        if (PUT.equals(ctx.getMethod()) && createUncontained) {
            final Resource res = services.getResourceService().get(identifier).toCompletableFuture().join();
            final List<PathSegment> segments = ctx.getUriInfo().getPathSegments();
            if ((MISSING_RESOURCE.equals(res) || DELETED_RESOURCE.equals(res))
                    && segments.stream().map(PathSegment::getPath).anyMatch(s -> !s.isEmpty())) {
                ctx.setMethod(POST);
                ctx.setRequestUri(ctx.getUriInfo().getBaseUriBuilder().path(getAllButLastSegment(segments)).build());
                ctx.getHeaders().putSingle(SLUG, getLastSegment(segments));
            }
        } else if ("MKCOL".equals(ctx.getMethod())) {
            // Note: MKCOL is just a POST with Link: <ldp:BasicContainer>; rel=type and appropriate Slug header
            final List<PathSegment> segments = ctx.getUriInfo().getPathSegments();
            final String slug = getLastSegment(segments);
            if (slug.isEmpty()) {
                // cannot POST a new root resource
                ctx.abortWith(status(CONFLICT).build());
            } else {
                ctx.setRequestUri(ctx.getUriInfo().getBaseUriBuilder().path(getAllButLastSegment(segments)).build());
                ctx.setMethod(POST);
                ctx.getHeaders().putSingle(SLUG, getLastSegment(segments));
                ctx.getHeaders().putSingle(LINK,
                        Link.fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build().toString());
            }
        } else if (DELETE.equals(ctx.getMethod())) {
            recursiveDelete(services, HttpSession.from(ctx.getSecurityContext()), identifier,
                    getBaseUrl(baseUrl, ctx.getUriInfo()));
        }
    }

    private static String getBaseUrl(final String baseUrl, final UriInfo uriInfo) {
        if (baseUrl != null) {
            return baseUrl;
        }
        return uriInfo.getBaseUri().toString();
    }
}
