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
package org.trellisldp.jwt;

import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.trellisldp.common.TrellisRoles;

/** A WebId-enabled SecurityContext implementation. */
public class WebIdSecurityContext implements SecurityContext {

    private final JsonWebToken principal;
    private final SecurityContext delegate;
    private final Set<String> admins;

    /**
     * Create a WebID-based security context.
     * @param delegate the security context delegate
     * @param principal the principal
     * @param admins a list of admin users
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
        if (principal != null) {
            if (TrellisRoles.ADMIN.equals(role)) {
                return admins.contains(principal.getName());
            }
            return TrellisRoles.USER.equals(role) || principal.getGroups().contains(role);
        }
        return false;
    }
}
