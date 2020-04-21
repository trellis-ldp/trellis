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

import org.apache.commons.lang3.StringUtils;

/**
 * An authenticator for Solid's WebId-OIDC protocol, <a href="https://github.com/solid/webid-oidc-spec">WebId-OIDC</a>.
 * This implementation requires no WebId provider configuration, the validation is performed based on the in JWT payload
 * supplied key data.
 */
public class WebIdOIDCAuthenticator implements Authenticator {
    private final String baseUrl;

    /**
     * Own exception to signal a malformed JWT according to the expectations of this authenticator.
     */
    static class WebIdOIDCJwtException extends MalformedJwtException {
        public WebIdOIDCJwtException(final String message) {
            super(message);
        }

        public WebIdOIDCJwtException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Build the authenticator.
     * @param baseUrl the server's base URL.
     */
    public WebIdOIDCAuthenticator(final String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("Received null as baseUrl, it is required for the WebId-OIDC support.");
        }
        this.baseUrl = baseUrl;
    }

    @Override
    public Claims parse(final String token) {
        checkTokenStructure(token);
        final String idToken = Jwts.parserBuilder().build()
            .parseClaimsJwt(extractUnsignedJWT(token)).getBody().get("id_token", String.class);
        if (idToken == null) {
            throw new WebIdOIDCJwtException("Missing the id_token claim in JWT payload");
        }
        checkTokenStructure(idToken);
        final Claims idTokenClaims = Jwts.parserBuilder().build().parseClaimsJwt(extractUnsignedJWT(idToken)).getBody();
        doWebIdProviderConfirmation(idTokenClaims);
        final Key key = getKey(idTokenClaims);
        final String audience = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getAudience();
        if (audience == null || !audience.endsWith(baseUrl)) {
            throw new WebIdOIDCJwtException(
                    String.format("Proof of Possession failed, wrong audience claim: aud=%s", audience));
        }
        return idTokenClaims;
    }

    private static void doWebIdProviderConfirmation(final Claims idTokenClaims) {
        final String issuer = idTokenClaims.getIssuer();
        if (issuer == null) {
            throw new WebIdOIDCJwtException("WebId provider confirmation failed. Missing needed issuer claim.");
        }
        final String webId = idTokenClaims.get(OAuthUtils.WEBID, String.class);
        final String subject = idTokenClaims.getSubject();
        if (webId == null && subject == null) {
            final String msg =
                "WebId provider confirmation failed. At least the webid or the sub claim mus be present, found none.";
            throw new WebIdOIDCJwtException(msg);
        }
        final String webIdClaim = webId != null ? webId : subject;
        doSameOriginOrSubDomainConfirmation(issuer, webIdClaim);
    }

    private static void doSameOriginOrSubDomainConfirmation(final String issuer, final String webIdClaim) {
        try {
            final URI webIdURI = new URI(webIdClaim);
            final URI issuerURI = new URI(issuer);
            final boolean isIssuerHttpURI = issuerURI.getScheme().startsWith("http");
            final boolean isWebIdHttpURI = webIdURI.getScheme().startsWith("http");
            final boolean isSubDomainOrSameOrigin = webIdURI.getHost().endsWith(issuerURI.getHost());
            if (!isIssuerHttpURI || !isWebIdHttpURI || !isSubDomainOrSameOrigin) {
                throw new WebIdOIDCJwtException(
                    String.format("WebId provider confirmation failed, (iss, webid)=(%s, %s).", issuer, webIdClaim));
            }
        } catch (final URISyntaxException e) {
            throw new WebIdOIDCJwtException(
                String.format("WebId provider confirmation failed, received invalid URI: (iss, sub)=(%s, %s).",
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
                throw new WebIdOIDCJwtException(msg);
            }
            if (!alg.startsWith("RS")) {
                final String msg = String.format("Expecting RSA algorithm under: %s, but got: %s", "cnf.jwk", alg);
                throw new WebIdOIDCJwtException(msg);
            }
            return buildKeyEntry(n, e);
        } catch (ClassCastException | RequiredTypeException e) {
            throw new WebIdOIDCJwtException(String.format("Missing cnf or it's data in: %s", idTokenClaims), e);
        }
    }

    private static String extractUnsignedJWT(final String token) {
        return token.substring(0, token.lastIndexOf('.') + 1);
    }

    private static void checkTokenStructure(final String token) {
        final int dotCount = StringUtils.countMatches(token, '.');
        if (dotCount != 2) {
            final String msg =
                String.format("JWT strings must contain exactly 2 period characters. Found: %d", dotCount);
            throw new WebIdOIDCJwtException(msg);
        }
    }

    private static Key buildKeyEntry(final String n, final String e) {
        final BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        final BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        return OAuthUtils.buildRSAPublicKey("RSA", modulus, exponent);
    }
}
