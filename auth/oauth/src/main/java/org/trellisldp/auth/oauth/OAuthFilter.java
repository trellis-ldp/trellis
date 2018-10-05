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

    private static final Logger LOGGER = getLogger(OAuthFilter.class);

    public static final String SCHEME = "Bearer";
    public static final String KEYSTORE_PATH = "trellis.oauth.keystore.path";
    public static final String KEYSTORE_CREDENTIALS = "trellis.oauth.keystore.password";
    public static final String KEY_IDS = "trellis.oauth.keyids";
    public static final String SHARED_SECRET = "trellis.oauth.sharedsecret";
    public static final String JWK_LOCATION = "trellis.oauth.jwk.location";

    private final Authenticator authenticator;

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
        this.authenticator = authenticator;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {

        final Boolean secure = ofNullable(requestContext.getSecurityContext()).filter(SecurityContext::isSecure)
            .isPresent();

        getOAuthToken(requestContext)
            .map(token -> authenticate(token).orElseThrow(() -> new NotAuthorizedException(SCHEME)))
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
                config.get(JWK_LOCATION));
        if (nonNull(jwksAuthenticator)) {
            return jwksAuthenticator;
        }

        final Authenticator keystoreAuthenticator = OAuthUtils.buildAuthenticatorWithTruststore(
                config.get(KEYSTORE_PATH), config.getOrDefault(KEYSTORE_CREDENTIALS, "").toCharArray(),
                asList(config.getOrDefault(KEY_IDS, "").split(",")));
        if (nonNull(keystoreAuthenticator)) {
            return keystoreAuthenticator;
        }

        final Authenticator sharedKeyAuthenticator = OAuthUtils.buildAuthenticatorWithSharedSecret(
                config.get(SHARED_SECRET));
        if (nonNull(sharedKeyAuthenticator)) {
            return sharedKeyAuthenticator;
        }

        return new NullAuthenticator();
    }
}
