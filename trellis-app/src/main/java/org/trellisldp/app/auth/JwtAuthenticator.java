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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.PrincipalImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

import java.security.Principal;
import java.util.Optional;

import org.slf4j.Logger;

/**
 * A JWT-based authenticator.
 */
public class JwtAuthenticator implements Authenticator<String, Principal> {

    private static final Logger LOGGER = getLogger(JwtAuthenticator.class);

    public static final String WEBID = "webid";

    private final String key;

    /**
     * Create a JWT-based authenticator.
     * @param key a secret key
     * @param encoded whether the key is encoded as base64
     */
    public JwtAuthenticator(final String key, final Boolean encoded) {
        this.key = encoded ? key : getEncoder().encodeToString(key.getBytes(UTF_8));
    }

    @Override
    public Optional<Principal> authenticate(final String credentials) throws AuthenticationException {
        try {
            // Parse the JWT claims
            final Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(credentials).getBody();

            // Use a webid claim, if one exists
            if (claims.containsKey(WEBID)) {
                LOGGER.debug("Using JWT claim with webid: {}", claims.get(WEBID, String.class));
                return ofNullable(claims.get(WEBID, String.class)).map(PrincipalImpl::new);
            }

            // Try generating a webid from other elements
            final String sub = claims.getSubject();
            if (nonNull(sub)) {
                // use the sub claim if it looks like a webid
                if (isUrl(sub)) {
                    LOGGER.debug("Using JWT claim with sub: {}", sub);
                    return of(new PrincipalImpl(sub));
                }
                final String iss = claims.getIssuer();
                // combine the iss and sub fields if that appears possible
                if (nonNull(iss) && isUrl(iss)) {
                    final String webid = iss.endsWith("/") ? iss + sub : iss + "/" + sub;
                    LOGGER.debug("Using JWT claim with generated webid: {}", webid);
                    return of(new PrincipalImpl(webid));
                }
            }
        } catch (final SignatureException ex) {
            LOGGER.debug("Invalid signature, ignoring JWT token: {}", ex.getMessage());
        } catch (final JwtException ex) {
            LOGGER.warn("Problem reading JWT value: {}", ex.getMessage());
        }
        return empty();
    }

    private Boolean isUrl(final String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
