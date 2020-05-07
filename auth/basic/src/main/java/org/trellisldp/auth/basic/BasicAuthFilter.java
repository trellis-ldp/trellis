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
package org.trellisldp.auth.basic;

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.SecurityContext.BASIC_AUTH;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import java.io.File;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.config.Config;

/**
 * A basic authentication filter using an Authorization HTTP header.
 */
@Priority(AUTHENTICATION)
public class BasicAuthFilter implements ContainerRequestFilter {

    /** The configuration key controlling the location of the basic auth credentials file. */
    public static final String CONFIG_AUTH_BASIC_CREDENTIALS = "trellis.auth.basic.credentials";

    /** The configuration key controlling the realm used in a WWW-Authenticate header, or 'trellis' by default. */
    public static final String CONFIG_AUTH_REALM = "trellis.auth.realm";

    /** The configuration key controlling the list of of admin WebID values. */
    public static final String CONFIG_AUTH_ADMIN_USERS = "trellis.auth.admin-users";

    /** The admin role. */
    public static final String ADMIN_ROLE = "admin";

    private final File file;
    private final String challenge;
    private final Set<String> admins;

    /**
     * Create a basic auth filter.
     */
    @Inject
    public BasicAuthFilter() {
        this(getConfig().getValue(CONFIG_AUTH_BASIC_CREDENTIALS, String.class));
    }

    /**
     * Create a basic auth filter.
     * @param credentialsFile a credentials file
     */
    public BasicAuthFilter(final String credentialsFile) {
        this(new File(credentialsFile));
    }

    /**
     * Create a basic auth filter.
     * @param file the credentials file
     */
    public BasicAuthFilter(final File file) {
        this(file, getConfig());
    }

    private BasicAuthFilter(final File file, final Config config) {
        this(file, config.getOptionalValue(CONFIG_AUTH_REALM, String.class).orElse("trellis"),
                getConfiguredAdmins(config));
    }

    /**
     * Create a basic auth filter.
     * @param file the credentials file
     * @param realm the authentication realm
     * @param admins the admin users
     */
    public BasicAuthFilter(final File file, final String realm, final Set<String> admins) {
        this.file = file;
        this.challenge = "Basic realm=\"" + realm + "\"";
        this.admins = unmodifiableSet(requireNonNull(admins, "admins set may not be null!"));
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {

        final boolean secure = requestContext.getSecurityContext() != null
            && requestContext.getSecurityContext().isSecure();

        final String credentials = getCredentials(requestContext);
        if (credentials != null) {
            final Principal principal = authenticate(credentials);
            if (principal == null) throw new NotAuthorizedException(challenge);
            requestContext.setSecurityContext(new BasicAuthSecurityContext(principal, admins, secure));
        }
    }

    private Principal authenticate(final String credentials) {
        final Credentials creds = Credentials.parse(credentials);
        if (creds != null  && file.exists()) {
            final Path path = file.toPath();
            try (final Stream<String> lineStream = BasicAuthUtils.uncheckedLines(path)) {
                return lineStream.map(String::trim).filter(line -> !line.startsWith("#"))
                    .map(line -> line.split(":", 3)).filter(x -> x.length == 3)
                    .filter(d -> d[0].trim().equals(creds.getUsername()) && d[1].trim().equals(creds.getPassword()))
                    .map(d -> d[2].trim()).findFirst().map(BasicPrincipal::new).orElse(null);
            }
        }
        return null;
    }

    private String getCredentials(final ContainerRequestContext ctx) {
        final String authHeader = ctx.getHeaderString(AUTHORIZATION);
        if (authHeader != null) {
            final String[] pair = authHeader.split(" ", 2);
            if (pair.length == 2 && pair[0].equalsIgnoreCase(BASIC_AUTH)) return pair[1];
        }
        return null;
    }

    private static Set<String> getConfiguredAdmins(final Config config) {
        final String admins = config.getOptionalValue(CONFIG_AUTH_ADMIN_USERS, String.class).orElse("");
        return stream(admins.split(",")).map(String::trim).collect(toSet());
    }

    private static final class BasicAuthSecurityContext implements SecurityContext {
        private final Principal principal;
        private final Set<String> admins;
        private final boolean secure;

        private BasicAuthSecurityContext(final Principal principal, final Set<String> admins, final boolean secure) {
            this.principal = principal;
            this.secure = secure;
            this.admins = admins;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isSecure() {
            return secure;
        }

        @Override
        public String getAuthenticationScheme() {
            return BASIC_AUTH;
        }

        @Override
        public boolean isUserInRole(final String role) {
            return ADMIN_ROLE.equals(role) && admins.contains(principal.getName());
        }
    }
}
