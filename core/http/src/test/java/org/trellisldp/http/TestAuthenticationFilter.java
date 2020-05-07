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

import java.security.Principal;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;

/**
 * @author acoburn
 */
@PreMatching
@Priority(500)
class TestAuthenticationFilter implements ContainerRequestFilter {

    private final String principal;
    private final String userRole;

    TestAuthenticationFilter(final String principal, final String role) {
        this.principal = principal;
        this.userRole = role;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> principal;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public boolean isUserInRole(final String role) {
                return userRole.equals(role);
            }

            @Override
            public String getAuthenticationScheme() {
                return "BASIC";
            }
        });
    }
}
