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
package org.trellisldp.auth.oauth;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SecurityException;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;

/**
 * An OAuth authentication filter that processes JWT-based Bearer tokens
 * from an Authorization HTTP header.
 */
@Priority(AUTHENTICATION)
public class OAuthFilter implements ContainerRequestFilter {

    /** The configuration key controlling the realm used in a WWW-Authenticate header, or 'trellis' by default. **/
    public static final String CONFIG_AUTH_REALM = "trellis.auth.realm";
    /** The configuration key controlling the OAuth Keystore path. **/
    public static final String CONFIG_AUTH_OAUTH_KEYSTORE_PATH = "trellis.auth.oauth.keystore.path";
    /** The configuration key controlling the OAuth Keystore credentials. **/
    public static final String CONFIG_AUTH_OAUTH_KEYSTORE_CREDENTIALS = "trellis.auth.oauth.keystore.credentials";
    /** The configuration key controlling the OAuth Keystore ids. **/
    public static final String CONFIG_AUTH_OAUTH_KEYSTORE_IDS = "trellis.auth.oauth.keystore.ids";
    /** The configuration key controlling the OAuth HMAC shared secret. **/
    public static final String CONFIG_AUTH_OAUTH_SHARED_SECRET = "trellis.auth.oauth.sharedsecret";
    /** The configuration key controlling the OAuth JWK URL. **/
    public static final String CONFIG_AUTH_OAUTH_JWK_URL = "trellis.auth.oauth.jwk";
    /** The authentication scheme used by this module. **/
    public static final String SCHEME = "Bearer";

    private static final Logger LOGGER = getLogger(OAuthFilter.class);

    private final Authenticator authenticator;
    private final String challenge;

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
        this(authenticator, getConfig().getOptionalValue(CONFIG_AUTH_REALM, String.class).orElse("trellis"));
    }

    /**
     * Create an OAuth filter with a defined authenticator.
     * @param authenticator the authenticator
     * @param realm the authentication realm
     */
    public OAuthFilter(final Authenticator authenticator, final String realm) {
        this.authenticator = authenticator;
        this.challenge = "Bearer realm=\"" + realm + "\"";
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {

        final SecurityContext securityContext = requestContext.getSecurityContext();
        final boolean secure = securityContext != null && securityContext.isSecure();

        getOAuthToken(requestContext)
                        .map(token -> authenticate(token)
                                        .<RuntimeException>orElseThrow(() -> new NotAuthorizedException(challenge)))
                        .ifPresent(principal -> requestContext
                                        .setSecurityContext(new OAuthSecurityContext(secure, principal)));
    }

    private Optional<Principal> authenticate(final String token) {
        try {
            return authenticator.authenticate(token);
        } catch (final SecurityException ex) {
            LOGGER.debug("Invalid signature, ignoring JWT token: {}", ex.getMessage());
        } catch (final JwtException ex) {
            LOGGER.warn("Problem reading JWT value: {}", ex.getMessage());
        }
        return empty();
    }

    private Optional<String> getOAuthToken(final ContainerRequestContext ctx) {
        final String headerString = ctx.getHeaderString(AUTHORIZATION);
        if (headerString == null) return empty();
        final String[] pair = headerString.split(" ", 2);
        if (pair.length == 2 && pair[0].equalsIgnoreCase(SCHEME)) return of(pair[1]);
        return empty();
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

    private static final class OAuthSecurityContext implements SecurityContext {
        private final boolean secure;
        private final Principal principal;

        private OAuthSecurityContext(final boolean secure, final Principal principal) {
            this.secure = secure;
            this.principal = principal;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(final String role) {
            return true;
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
