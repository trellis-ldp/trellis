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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;

/** A JWT-based authentication filter. */
@Provider
@Priority(AUTHENTICATION + 10)
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = getLogger(JwtAuthFilter.class);

    /** The configuration key controlling the list of of admin WebID values. */
    public static final String CONFIG_AUTH_ADMIN_USERS = "trellis.auth.admin-users";

    private final Set<String> admins;

    @Inject
    private JsonWebToken jwt;

    /**
     * Create an auth filter that augments MicroProfile-JWT authentication with WebID support.
     */
    public JwtAuthFilter() {
        this.admins = getConfiguredAdmins(getConfig().getOptionalValue(CONFIG_AUTH_ADMIN_USERS, String.class)
                .orElse(""));
    }

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        LOGGER.trace("JWT Auth Token: {}", jwt);
        ctx.setSecurityContext(new WebIdSecurityContext(ctx.getSecurityContext(), jwt, admins));
    }

    static Set<String> getConfiguredAdmins(final String admins) {
        return stream(admins.split(",")).map(String::trim).collect(toSet());
    }
}
