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
package org.trellisldp.auth.jwt;

import java.security.Principal;
import java.util.Set;

import javax.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.JsonWebToken;

/** A WebId-enabled SecurityContext implementation. */
public class WebIdSecurityContext implements SecurityContext {

    /** The admin role. */
    public static final String ADMIN_ROLE = "admin";

    private final JsonWebToken principal;
    private final SecurityContext delegate;
    private final Set<String> admins;

    /**
     * Create a WebID-based security context.
     * @param delegate the security context delegate
     * @param principal the principal
     * @param admins a whitelist of admin users
     */
    public WebIdSecurityContext(final SecurityContext delegate, final JsonWebToken principal,
            final Set<String> admins) {
        this.delegate = delegate;
        this.principal = principal != null ? new WebIdPrincipal(principal) : principal;
        this.admins = admins;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return delegate.getAuthenticationScheme();
    }

    @Override
    public boolean isUserInRole(final String role) {
        if (ADMIN_ROLE.equals(role)) {
            return admins.contains(principal.getName());
        }
        return principal.getGroups().contains(role);
    }
}
