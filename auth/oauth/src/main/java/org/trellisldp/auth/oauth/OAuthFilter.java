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
package org.trellisldp.auth.oauth;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SecurityException;

import java.security.Principal;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;

/**
 * An OAuth authentication filter that processes JWT-based Bearer tokens
 * from an Authorization HTTP header.
 */
@Provider
@Priority(AUTHENTICATION)
public class OAuthFilter implements ContainerRequestFilter {

    /** The configuration key controlling the list of of admin WebID values. */
    public static final String CONFIG_AUTH_ADMIN_USERS = "trellis.auth.admin-users";
    /** The configuration key controlling the realm used in a WWW-Authenticate header, or 'trellis' by default. */
    public static final String CONFIG_AUTH_REALM = "trellis.auth.realm";
    /** The configuration key controlling the OAuth Keystore path. */
    public static final String CONFIG_AUTH_OAUTH_KEYSTORE_PATH = "trellis.auth.oauth.keystore-path";
    /** The configuration key controlling the OAuth Keystore credentials. */
    public static final String CONFIG_AUTH_OAUTH_KEYSTORE_CREDENTIALS = "trellis.auth.oauth.keystore-credentials";
    /** The configuration key controlling the OAuth Keystore ids. */
    public static final String CONFIG_AUTH_OAUTH_KEYSTORE_IDS = "trellis.auth.oauth.keystore-ids";
    /** The configuration key controlling the OAuth HMAC shared secret. */
    public static final String CONFIG_AUTH_OAUTH_SHARED_SECRET = "trellis.auth.oauth.shared-secret";
    /** The configuration key controlling the OAuth JWK URL. */
    public static final String CONFIG_AUTH_OAUTH_JWK_URL = "trellis.auth.oauth.jwk";
    /** The authentication scheme used by this module. */
    public static final String SCHEME = "Bearer";
    /** The admin role. */
    public static final String ADMIN_ROLE = "admin";

    private static final Logger LOGGER = getLogger(OAuthFilter.class);

    private final Authenticator authenticator;
    private final String challenge;
    private final Set<String> admins;

    /**
     * Create an OAuth filter.
     */
    @Inject
    public OAuthFilter() {
        this(buildAuthenticator());
    }

    /**
     * Create an OAuth filter with a defined authenticator.
     * @param authenticator the authenticator
     */
    public OAuthFilter(final Authenticator authenticator) {
        this(authenticator, getConfig());
    }

    private OAuthFilter(final Authenticator authenticator, final Config config) {
        this(authenticator, config.getOptionalValue(CONFIG_AUTH_REALM, String.class).orElse("trellis"),
                getConfiguredAdmins(config));
    }

    /**
     * Create an OAuth filter with a defined authenticator.
     * @param authenticator the authenticator
     * @param realm the authentication realm
     * @param admins the admin users
     */
    public OAuthFilter(final Authenticator authenticator, final String realm, final Set<String> admins) {
        this.authenticator = authenticator;
        this.challenge = "Bearer realm=\"" + realm + "\"";
        this.admins = unmodifiableSet(requireNonNull(admins, "Admin set may not be null!"));
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {

        final SecurityContext securityContext = requestContext.getSecurityContext();
        final boolean secure = securityContext != null && securityContext.isSecure();

        final String token = getOAuthToken(requestContext);
        if (token != null) {
            final Principal principal = authenticate(token);
            if (principal == null) throw new NotAuthorizedException(challenge);
            requestContext.setSecurityContext(new OAuthSecurityContext(principal, admins, secure));
        }
    }

    private Principal authenticate(final String token) {
        try {
            return authenticator.authenticate(token);
        } catch (final SecurityException ex) {
            LOGGER.debug("Invalid signature, ignoring JWT token: {}", ex.getMessage());
        } catch (final JwtException ex) {
            LOGGER.warn("Problem reading JWT value: {}", ex.getMessage());
        }
        return null;
    }

    private String getOAuthToken(final ContainerRequestContext ctx) {
        final String headerString = ctx.getHeaderString(AUTHORIZATION);
        if (headerString != null) {
            final String[] pair = headerString.split(" ", 2);
            if (pair.length == 2 && pair[0].equalsIgnoreCase(SCHEME)) return pair[1];
        }
        return null;
    }

    private static Authenticator buildAuthenticator() {
        final Config config = getConfig();
        final Authenticator jwksAuthenticator = OAuthUtils.buildAuthenticatorWithJwk(
                config.getOptionalValue(CONFIG_AUTH_OAUTH_JWK_URL, String.class).orElse(null));
        if (jwksAuthenticator != null) {
            return jwksAuthenticator;
        }

        final Authenticator keystoreAuthenticator = OAuthUtils.buildAuthenticatorWithTruststore(
                config.getOptionalValue(CONFIG_AUTH_OAUTH_KEYSTORE_PATH, String.class).orElse(null),
                config.getOptionalValue(CONFIG_AUTH_OAUTH_KEYSTORE_CREDENTIALS, String.class).orElse("").toCharArray(),
                asList(config.getOptionalValue(CONFIG_AUTH_OAUTH_KEYSTORE_IDS, String.class).orElse("").split(",")));
        if (keystoreAuthenticator != null) {
            return keystoreAuthenticator;
        }

        final Authenticator sharedKeyAuthenticator = OAuthUtils.buildAuthenticatorWithSharedSecret(
                config.getOptionalValue(CONFIG_AUTH_OAUTH_SHARED_SECRET, String.class).orElse(null));
        if (sharedKeyAuthenticator != null) {
            return sharedKeyAuthenticator;
        }
        return new NullAuthenticator();
    }

    private static Set<String> getConfiguredAdmins(final Config config) {
        final String admins = config.getOptionalValue(CONFIG_AUTH_ADMIN_USERS, String.class).orElse("");
        return stream(admins.split(",")).map(String::trim).collect(toSet());
    }

    private static final class OAuthSecurityContext implements SecurityContext {
        private final boolean secure;
        private final Principal principal;
        private final Set<String> admins;

        private OAuthSecurityContext(final Principal principal, final Set<String> admins, final boolean secure) {
            this.principal = principal;
            this.admins = admins;
            this.secure = secure;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(final String role) {
            return ADMIN_ROLE.equals(role) && admins.contains(principal.getName());
        }

        @Override
        public boolean isSecure() {
            return secure;
        }

        @Override
        public String getAuthenticationScheme() {
            return SCHEME;
        }
    }
}
