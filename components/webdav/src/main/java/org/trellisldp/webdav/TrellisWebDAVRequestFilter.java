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
package org.trellisldp.webdav;

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.webdav.impl.WebDAVUtils.getAllButLastSegment;
import static org.trellisldp.webdav.impl.WebDAVUtils.getLastSegment;
import static org.trellisldp.webdav.impl.WebDAVUtils.recursiveCopy;
import static org.trellisldp.webdav.impl.WebDAVUtils.recursiveDelete;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.ext.Provider;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.vocabulary.LDP;

@Provider
@PreMatching
public class TrellisWebDAVRequestFilter implements ContainerRequestFilter {

    private static final RDF rdf = getInstance();

    private final ServiceBundler services;

    /**
     * Create a Trellis HTTP request filter.
     *
     * @param services the Trellis application bundle
     */
    @Inject
    public TrellisWebDAVRequestFilter(final ServiceBundler services) {
        this.services = services;
    }

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + ctx.getUriInfo().getPath());
        if (PUT.equals(ctx.getMethod())) {
            final Resource res = services.getResourceService().get(identifier).toCompletableFuture().join();
            final List<PathSegment> segments = ctx.getUriInfo().getPathSegments();
            if ((MISSING_RESOURCE.equals(res) || DELETED_RESOURCE.equals(res)) && !segments.isEmpty()) {
                ctx.setMethod(POST);
                ctx.setRequestUri(ctx.getUriInfo().getBaseUriBuilder().path(getAllButLastSegment(segments)).build());
                ctx.getHeaders().putSingle(SLUG, getLastSegment(segments));
            }
        } else if ("MKCOL".equals(ctx.getMethod())) {
            // Note: MKCOL is just a POST with Link: <ldp:BasicContainer>; rel=type and appropriate Slug header
            final List<PathSegment> segments = ctx.getUriInfo().getPathSegments();
            ctx.setRequestUri(ctx.getUriInfo().getBaseUriBuilder().path(getAllButLastSegment(segments)).build());
            ctx.setMethod(POST);
            ctx.getHeaders().putSingle(SLUG, getLastSegment(segments));
            ctx.getHeaders().putSingle(LINK,
                    Link.fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build().toString());
        } else if (DELETE.equals(ctx.getMethod())) {
            recursiveDelete(services.getResourceService(), identifier);
        } else if ("COPY".equals(ctx.getMethod()) || "MOVE".equals(ctx.getMethod())) {
            final IRI destination = null;
            recursiveCopy(services.getResourceService(), identifier, destination);
        }
    }
}
