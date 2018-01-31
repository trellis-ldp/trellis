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
package org.trellisldp.app.auth;

import static java.util.Objects.nonNull;
import static javax.ws.rs.Priorities.AUTHENTICATION;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.PrincipalImpl;

import java.io.IOException;
import java.security.Principal;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.trellisldp.vocabulary.Trellis;

/**
 * An Anonymous auth filter.
 */
@Priority(AUTHENTICATION)
public final class AnonymousAuthFilter extends AuthFilter<String, Principal> {

    private AnonymousAuthFilter() {
    }

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {

        if (nonNull(ctx.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
        }

        final SecurityContext securityContext = ctx.getSecurityContext();
        final boolean secure = securityContext != null && securityContext.isSecure();

        ctx.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return new PrincipalImpl(Trellis.AnonymousAgent.getIRIString());
            }

            @Override
            public boolean isUserInRole(final String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return secure;
            }

            @Override
            public String getAuthenticationScheme() {
                return "NONE";
            }
        });
    }

    /**
     * Builder for an anonymous auth filter.
     */
    public static class Builder
            extends AuthFilterBuilder<String, Principal, AnonymousAuthFilter> {
        @Override
        protected AnonymousAuthFilter newInstance() {
            return new AnonymousAuthFilter();
        }
    }
}
