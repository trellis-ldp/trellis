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

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.lang.Strings;

import java.math.BigInteger;
import java.security.Key;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;

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
        getTokenParts(token);
        final String idToken = Jwts.parserBuilder().build()
            .parseClaimsJwt(extractUnsignedJWT(token)).getBody().get("id_token", String.class);
        if (idToken == null) {
            final String msg = "Expecting the id_token claim";
            if (LOGGER.isWarnEnabled()) LOGGER.warn(msg);
            throw new MalformedJwtException(msg);
        }
        final String[] idTokenParts = getTokenParts(idToken);
        checkRequiredAlgorithm(idTokenParts[1]);
        final Claims idTokenClaims =
            Jwts.parserBuilder().build().parseClaimsJwt(extractUnsignedJWT(idToken)).getBody();
        final Key key = getKey(idTokenClaims);
        // Side effect needed to check the signature at the top level JWT
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        return idTokenClaims;
    }

    private static Key getKey(final Claims idTokenClaims) {
        final Map<String, Map<String, String>> cnf = idTokenClaims.get("cnf", Map.class);
        final Map<String, String> jwk = cnf.get("jwk");
        try {
            final String n = jwk.get("n");
            final String e = jwk.get("e");
            if (n == null || e == null) {
                final String msg = String
                    .format("Missing at least one algorithm parameter, modulus or exponent. (n, e): (%s, %s)", n, e);
                if (LOGGER.isWarnEnabled()) LOGGER.warn(msg);
                throw new MalformedJwtException(msg);
            }
            return buildKeyEntry(n, e);
        } catch (ClassCastException e) {
            throw new MalformedJwtException("The cnf field is malformed: " + cnf, e);
        }
    }

    private static String extractUnsignedJWT(final String token) {
        return token.substring(0, token.lastIndexOf('.') + 1);
    }

    private static String[] getTokenParts(final String token) {
        final String[] tokenParts = token.split("\\.");
        if (tokenParts.length < 3) {
            final String msg =
                "JWT strings must contain exactly 2 period characters. Found: " + (tokenParts.length - 1);
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Malformed JWT, expecting a signed JWT token, but got: {}", token);
            throw new MalformedJwtException(msg);
        }
        return tokenParts;
    }

    private static void checkRequiredAlgorithm(final String body) {
        final String jsonS = new String(Decoders.BASE64URL.decode(body), Strings.UTF_8);
        final JsonNode jsonNode;
        try {
            jsonNode = (new ObjectMapper()).readTree(jsonS);
        } catch (JsonProcessingException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Got invalid JWT JSON: \n" + jsonS);
            throw new MalformedJwtException("Invalid JWT JSON", e);
        }
        final String path = "/cnf/jwk/alg";
        final JsonNode algNode = jsonNode.at(path);
        if (algNode.isMissingNode() || !algNode.asText().startsWith("RS")) {
            final String msg = "Expecting RSA algorithm under the JSON path: " + path + ", but got: " + algNode;
            if (LOGGER.isWarnEnabled()) LOGGER.warn(msg);
            throw new MalformedJwtException(msg);
        }
    }

    private static Key buildKeyEntry(final String n, final String e) {
        final BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        final BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        return OAuthUtils.buildRSAPublicKey("RSA", modulus, exponent);
    }
}
