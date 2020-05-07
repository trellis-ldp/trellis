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
package org.trellisldp.http;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.status;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.trellisldp.http.core.HttpConstants.*;
import static org.trellisldp.http.core.TrellisExtensions.buildExtensionMapFromConfig;
import static org.trellisldp.vocabulary.LDP.PreferContainment;
import static org.trellisldp.vocabulary.LDP.PreferMembership;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Link;
import javax.ws.rs.ext.Provider;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.http.core.AcceptDatetime;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.http.core.Range;
import org.trellisldp.http.core.Version;

@Provider
@Priority(AUTHORIZATION - 20)
public class TrellisHttpFilter implements ContainerRequestFilter {

    private final List<String> mutatingMethods;
    private final Map<String, IRI> extensions;

    /**
     * Create a simple pre-matching filter.
     */
    @Inject
    public TrellisHttpFilter() {
        this(asList(POST, PUT, DELETE, PATCH), buildExtensionMapFromConfig(getConfig()));
    }

    /**
     * Create a simple pre-matching filter with a custom method list.
     * @param mutatingMethods a list of mutating HTTP methods
     * @param extensions a map of named graph extensions
     */
    public TrellisHttpFilter(final List<String> mutatingMethods, final Map<String, IRI> extensions) {
        this.mutatingMethods = unmodifiableList(mutatingMethods);
        this.extensions = extensions;
    }

    @Override
    public void filter(final ContainerRequestContext ctx) {
        // Validate headers
        validateAcceptDatetime(ctx);
        validateRange(ctx);
        validateLink(ctx);
        // Validate query parameters
        validateVersion(ctx);
        validateTimeMap(ctx);

        // Unconditionally set the Prefer header for extension requests
        final String ext = ctx.getUriInfo().getQueryParameters().getFirst(EXT);
        if (ext != null && extensions.containsKey(ext) && GET.equals(ctx.getMethod())) {
            ctx.getHeaders().putSingle(PREFER, new Prefer(Prefer.PREFER_REPRESENTATION,
                        singletonList(extensions.get(ext).getIRIString()),
                        asList(PreferServerManaged.getIRIString(), PreferUserManaged.getIRIString(),
                            PreferContainment.getIRIString(), PreferMembership.getIRIString()), null, null).toString());
        }
    }

    private void validateAcceptDatetime(final ContainerRequestContext ctx) {
        final String acceptDatetime = ctx.getHeaderString(ACCEPT_DATETIME);
        if (acceptDatetime != null && AcceptDatetime.valueOf(acceptDatetime) == null) {
            ctx.abortWith(status(BAD_REQUEST).build());
        }
    }

    private void validateRange(final ContainerRequestContext ctx) {
        final String range = ctx.getHeaderString(RANGE);
        if (range != null && Range.valueOf(range) == null) {
            ctx.abortWith(status(BAD_REQUEST).build());
        }
    }

    private void validateLink(final ContainerRequestContext ctx) {
        final String link = ctx.getHeaderString(LINK);
        if (link != null) {
            try {
                Link.valueOf(link);
            } catch (final IllegalArgumentException ex) {
                ctx.abortWith(status(BAD_REQUEST).build());
            }
        }
    }

    private void validateVersion(final ContainerRequestContext ctx) {
        final String version = ctx.getUriInfo().getQueryParameters().getFirst("version");
        if (version != null) {
            // Check well-formedness
            if (Version.valueOf(version) == null) {
                ctx.abortWith(status(BAD_REQUEST).build());
            // Do not allow mutating versioned resources
            } else if (mutatingMethods.contains(ctx.getMethod())) {
                ctx.abortWith(status(METHOD_NOT_ALLOWED).build());
            }
        }
    }

    private void validateTimeMap(final ContainerRequestContext ctx) {
        final List<String> exts = ctx.getUriInfo().getQueryParameters().get(EXT);
        // Do not allow direct manipulation of timemaps
        if (exts != null && exts.contains(TIMEMAP) && mutatingMethods.contains(ctx.getMethod())) {
            ctx.abortWith(status(METHOD_NOT_ALLOWED).build());
        }
    }
}
