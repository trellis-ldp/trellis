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
import io.jsonwebtoken.RequiredTypeException;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Key;
import java.util.Base64;
import java.util.Map;

/**
 * An authenticator for the Solid WebId-OIDC protocol. This implementation requires no WebId provider configuration,
 * the validation is performed based on the in JWT payload supplied key data.
 */
public class SolidOIDCAuthenticator implements Authenticator {

    /**
     * Own exception to signal a malformed JWT according to the expectation of this authenticator.
     */
    static class SolidOIDCJwtException extends MalformedJwtException {
        public SolidOIDCJwtException(final String message) {
            super(message);
        }

        public SolidOIDCJwtException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Build the authenticator.
     */
    public SolidOIDCAuthenticator() {
    }

    @Override
    public Claims parse(final String token) {
        checkTokenStructure(getTokenParts(token));
        final String idToken = Jwts.parserBuilder().build()
            .parseClaimsJwt(extractUnsignedJWT(token)).getBody().get("id_token", String.class);
        if (idToken == null) {
            throw new SolidOIDCJwtException("Missing the id_token claim in JWT payload");
        }
        checkTokenStructure(getTokenParts(idToken));
        final Claims idTokenClaims = Jwts.parserBuilder().build().parseClaimsJwt(extractUnsignedJWT(idToken)).getBody();
        doWebIdProviderConfirmation(idTokenClaims);
        final Key key = getKey(idTokenClaims);
        // Side effect needed to check the signature at the top level JWT
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        return idTokenClaims;
    }

    private static void doWebIdProviderConfirmation(final Claims idTokenClaims) {
        final String issuer = idTokenClaims.getIssuer();
        final String webId = idTokenClaims.get(OAuthUtils.WEBID, String.class);
        final String subject = idTokenClaims.getSubject();
        if (issuer == null || (webId == null && subject == null)) {
            throw new SolidOIDCJwtException(
                String.format("WebId confirmation failed, missing needed claims: (issuer & webId|sub)=(%s & %s|%s).",
                    issuer, webId, subject));
        }
        if (webId != null) {
            doSameOriginOrSubDomainConfirmation(issuer, webId);
        } else {
            doSameOriginOrSubDomainConfirmation(issuer, subject);
        }
    }

    private static void doSameOriginOrSubDomainConfirmation(final String issuer, final String webIdClaim) {
        try {
            final URI webIdURI = new URI(webIdClaim);
            final URI issuerURI = new URI(issuer);
            final boolean isIssuerHttpURI = issuerURI.getScheme().startsWith("http");
            final boolean isWebIdHttpURI = webIdURI.getScheme().startsWith("http");
            final boolean isSubDomainOrSameOrigin = webIdURI.getHost().endsWith(issuerURI.getHost());
            if (!isIssuerHttpURI || !isWebIdHttpURI || !isSubDomainOrSameOrigin) {
                throw new SolidOIDCJwtException(
                    String.format("WebId provider confirmation failed, (issuer, webId)=(%s, %s).", issuer, webIdClaim));
            }
        } catch (final URISyntaxException e) {
            throw new SolidOIDCJwtException(
                String.format("WebId provider confirmation failed, received invalid URI: (issuer, sub)=(%s, %s).",
                    issuer, webIdClaim), e);
        }
    }

    private static Key getKey(final Claims idTokenClaims) {
        final Map<String, Map<String, String>> cnf;
        try {
            cnf = idTokenClaims.get("cnf", Map.class);
            final Map<String, String> jwk = cnf.get("jwk");
            final String alg = jwk.get("alg");
            final String n = jwk.get("n");
            final String e = jwk.get("e");
            if (alg == null || n == null || e == null) {
                final String msg =
                    String.format("Missing at least one algorithm parameter under cnf.jwk. (alg, n, e): (%s, %s, %s)",
                        alg, n, e);
                throw new SolidOIDCJwtException(msg);
            }
            if (!alg.startsWith("RS")) {
                final String msg = String.format("Expecting RSA algorithm under: %s, but got: %s", "cnf.jwk", alg);
                throw new SolidOIDCJwtException(msg);
            }
            return buildKeyEntry(n, e);
        } catch (ClassCastException | RequiredTypeException e) {
            throw new SolidOIDCJwtException(String.format("Missing cnf or it's data in: %s", idTokenClaims), e);
        }
    }

    private static String extractUnsignedJWT(final String token) {
        return token.substring(0, token.lastIndexOf('.') + 1);
    }

    private static String[] getTokenParts(final String token) {
        return token.split("\\.");
    }

    private static void checkTokenStructure(final String[] tokenParts) {
        if (tokenParts.length < 3) {
            final String msg =
                String.format("JWT strings must contain exactly 2 period characters. Found: %d", tokenParts.length - 1);
            throw new SolidOIDCJwtException(msg);
        }
    }

    private static Key buildKeyEntry(final String n, final String e) {
        final BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        final BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        return OAuthUtils.buildRSAPublicKey("RSA", modulus, exponent);
    }
}
