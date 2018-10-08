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
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
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

import org.apache.tamaya.Configuration;
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
        this(authenticator, getConfiguration().getOrDefault(CONFIG_AUTH_REALM, "trellis"));
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

        final Boolean secure = ofNullable(requestContext.getSecurityContext()).filter(SecurityContext::isSecure)
            .isPresent();

        getOAuthToken(requestContext)
            .map(token -> authenticate(token).orElseThrow(() -> new NotAuthorizedException(challenge)))
            .ifPresent(principal -> requestContext.setSecurityContext(new SecurityContext() {
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
                }));
    }

    private Optional<Principal> authenticate(final String token) {
        LOGGER.info("Token: {}", token);
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
        return ofNullable(ctx.getHeaderString(AUTHORIZATION)).map(h -> h.split(" ", 2))
            .filter(pair -> pair[0].equalsIgnoreCase(SCHEME)).filter(pair -> pair.length == 2).map(pair -> pair[1]);
    }

    private static Authenticator buildAuthenticator() {
        final Configuration config = getConfiguration();

        final Authenticator jwksAuthenticator = OAuthUtils.buildAuthenticatorWithJwk(
                config.get(CONFIG_AUTH_OAUTH_JWK_URL));
        if (nonNull(jwksAuthenticator)) {
            return jwksAuthenticator;
        }

        final Authenticator keystoreAuthenticator = OAuthUtils.buildAuthenticatorWithTruststore(
                config.get(CONFIG_AUTH_OAUTH_KEYSTORE_PATH),
                config.getOrDefault(CONFIG_AUTH_OAUTH_KEYSTORE_CREDENTIALS, "").toCharArray(),
                asList(config.getOrDefault(CONFIG_AUTH_OAUTH_KEYSTORE_IDS, "").split(",")));
        if (nonNull(keystoreAuthenticator)) {
            return keystoreAuthenticator;
        }

        final Authenticator sharedKeyAuthenticator = OAuthUtils.buildAuthenticatorWithSharedSecret(
                config.get(CONFIG_AUTH_OAUTH_SHARED_SECRET));
        if (nonNull(sharedKeyAuthenticator)) {
            return sharedKeyAuthenticator;
        }
        return new NullAuthenticator();
    }
}
