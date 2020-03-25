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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.security.Key;
import java.util.Base64;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A jwks-based authenticator.
 */
public class JwsIdTokenAuthenticator implements Authenticator {

    private static final Logger LOGGER = getLogger(JwsIdTokenAuthenticator.class);

    /**
     * Build a jws authenticator.
     */
    public JwsIdTokenAuthenticator() {
    }

    @Override
    public Claims parse(final String token) {
        final String[] jtwParts = token.split("\\.");
        if (jtwParts.length < 3) {
            final String msg = "JWT strings must contain exactly 2 period characters. Found: " + (jtwParts.length - 1);
            LOGGER.warn("Malformed JWT, expecting a signed JWT token, but got: {}", token);
            throw new MalformedJwtException(msg);
        }
        final String unsignedJwt = token.substring(0, token.lastIndexOf('.') + 1);
        final String idToken =
                Jwts.parser().parseClaimsJwt(unsignedJwt).getBody().get("id_token", String.class);
        final Claims idTokenClaims =
                Jwts.parser().parseClaimsJwt(idToken.substring(0, idToken.lastIndexOf('.') + 1)).getBody();
        final Map<String, Object> jwk = ((Map<String, Map>) idTokenClaims.get("cnf", Map.class)).get("jwk");
        final String n = (String) jwk.get("n");
        final String e = (String) jwk.get("e");
        // Side effect needed to check the signature at the top level
        Jwts.parser().setSigningKey(buildKeyEntry(n, e)).parseClaimsJws(token);
        return idTokenClaims;
    }

    private static Key buildKeyEntry(final String n, final String e) {
        final BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        final BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        return OAuthUtils.buildRSAPublicKey("RSA", modulus, exponent);
    }
}
