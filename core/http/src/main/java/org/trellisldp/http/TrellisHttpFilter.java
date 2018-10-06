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
package org.trellisldp.http;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.seeOther;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.trellisldp.http.domain.HttpConstants.TIMEMAP;

import java.io.IOException;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Link;

import org.trellisldp.http.domain.AcceptDatetime;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.http.domain.Range;
import org.trellisldp.http.domain.Version;

@PreMatching
@Priority(AUTHORIZATION + 20)
public class TrellisHttpFilter implements ContainerRequestFilter {

    private static final List<String> MUTATING_METHODS = asList("POST", "PUT", "DELETE", "PATCH");

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        final String slash = "/";
        // Check for a trailing slash. If so, redirect
        final String path = ctx.getUriInfo().getPath();
        if (path.endsWith(slash) && !path.equals(slash)) {
            ctx.abortWith(seeOther(fromUri(path.substring(0, path.length() - 1)).build()).build());
        }

        // Validate header/query parameters
        ofNullable(ctx.getHeaderString("Accept-Datetime")).filter(x -> isNull(AcceptDatetime.valueOf(x)))
            .ifPresent(x -> ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getHeaderString("Slug")).filter(s -> s.contains(slash)).ifPresent(x ->
            ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getHeaderString("Prefer")).filter(x -> isNull(Prefer.valueOf(x))).ifPresent(x ->
            ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getHeaderString("Range")).filter(x -> isNull(Range.valueOf(x))).ifPresent(x ->
            ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getHeaderString("Link")).ifPresent(x -> {
            try {
                Link.valueOf(x);
            } catch (final IllegalArgumentException ex) {
                ctx.abortWith(status(BAD_REQUEST).build());
            }
        });

        ofNullable(ctx.getHeaderString("Digest")).filter(x -> isNull(Digest.valueOf(x))).ifPresent(x ->
            ctx.abortWith(status(BAD_REQUEST).build()));

        ofNullable(ctx.getUriInfo().getQueryParameters().getFirst("version")).ifPresent(x -> {
            // Check well-formedness
            if (isNull(Version.valueOf(x))) {
                ctx.abortWith(status(BAD_REQUEST).build());
            // Do not allow mutating versioned resources
            } else if (MUTATING_METHODS.contains(ctx.getMethod())) {
                ctx.abortWith(status(METHOD_NOT_ALLOWED).build());
            }
        });

        // Do not allow direct manipulation of timemaps
        ofNullable(ctx.getUriInfo().getQueryParameters().get("ext")).filter(l -> l.contains(TIMEMAP))
            .filter(x -> MUTATING_METHODS.contains(ctx.getMethod()))
            .ifPresent(x -> ctx.abortWith(status(METHOD_NOT_ALLOWED).build()));
    }


}
