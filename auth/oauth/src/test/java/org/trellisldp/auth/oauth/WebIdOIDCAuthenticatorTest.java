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

import static java.util.Base64.getUrlDecoder;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.trellisldp.auth.oauth.WebIdOIDCAuthenticator.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.impl.DefaultClaims;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WebIdOIDCAuthenticatorTest {

    private static final String base64PrivateExponent = "VRPRBm9dCoAJfBbEz5oAHEz7Tnm" +
        "0i0O6m5yj7NwqAZOj9i4ZwgZ8VZQo88oxZQWNaYd1yKeoQhUsJija_vxQEPXO1Q2q6OqMcwTBH0wyGhIFp--z2dAyRlDVLUTQbJUXyq" +
        "ammdh7b16-igH-BB67jfolM-cw-O7YaN7GrxCCYX5bI38IipeYfcroeIUXdLYmmUdNy7c8P2_K4O-iHQ6A4AUtQRUOzt2FGOdmlGZih" +
        "upI9YprshIy9CZq_iA3BcOl4Gcc-ljwwUzT0M_4jt53DCV7oxqWVt9WRdYDNoD62g2FzQ-1nYUqsz4YChk1MuOPV1xFpRklwSpt5dfh" +
        "uldnbQ";
    private static final BigInteger exponent = new BigInteger(1, getUrlDecoder().decode(base64PrivateExponent));

    private static final String base64PublicExponent = "AQAB";

    private static final BigInteger publicExponent =
            new BigInteger(1, getUrlDecoder().decode(base64PublicExponent));

    private static final String base64Modulus = "oMyjaeUbmnqojRpMBDbWbfBFitd_" +
        "dQcFJ96CDWwzsVcyAK3_kp4dEvhc2KLBjrmE69gJ-4HRuPF-kulDEmpC-MVx9eOihdUG9XV0ZA_eYWj9RoI_Gt3TUqTxlQH_nJRADTf" +
        "y82fOCCboKpaQ2idZH55Vb0FDbau2b2462tYRmcnxTFjClP4fDTTubI-3oFJ4tKMjynvUT34mCrZPiM8Q4noxVoyRYpzUTL1USxdUf5" +
        "6IKSB8NduH438zhMXE5VLC6PzhR3i_4KKpe4nq2otsrJ3KlEc7Me6UeiMXxPYz8rrPovW5L3LFWDmntGs5q923fBZFLFg8yBgMdTine" +
        "aahEQ";
    private static final BigInteger modulus = new BigInteger(1, getUrlDecoder().decode(base64Modulus));
    private static final String DEFAULT_KEY_ID = "VRPRBm9dCo";

    private static final String BASE_URL = "https://trellis.org";
    private static final String DEFAULT_ID_TOKEN_SSUER = "https://solid.community";
    private static final String DEFAULT_ISSUER = "client as issuer";

    private static final Map.Entry<String, String> DEFAULT_SUBJECT_ENTRY =
            new WebIdEntry("sub", "https://bob.solid.community/profile/card#me");

    private static final Key privateKey;
    static {
        try {
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Error creating RSA key!", e);
        }
    }

    private static final Key publicKey;
    static {
        try {
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Error creating RSA public key!", e);
        }
    }

    private static final Map<String, Object> headers = new HashMap<>();
    static {
        headers.put("alg", "RS256");
        headers.put(KEY_ID_HEADER, DEFAULT_KEY_ID);
    }

    private static final Map<String, Key> keyCache = new HashMap<>();
    static {
        keyCache.put(DEFAULT_KEY_ID, publicKey);
    }

    private final WebIdOIDCAuthenticator authenticator = new WebIdOIDCAuthenticator(BASE_URL, 50, 30, keyCache);

    private static class WebIdEntry implements Map.Entry<String, String> {
        private final String key;
        private final String value;

        WebIdEntry() {
            this(null, null);
        }

        WebIdEntry(final String key, final String value) {
            this.key = key;
            this.value = value;
        }
        @Override public String getKey() {
            return key;
        }

        @Override public String getValue() {
            return value;
        }

        @Override public String setValue(final String v) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void testAuthenticateNoIdToken() {
        final Claims claims = new DefaultClaims();
        claims.setIssuer("example.com");
        final String token = createJWTToken(headers, claims);

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testAuthenticateNoIssuer() {
        final String token = createPOPToken(null, BASE_URL, POP_TOKEN, headers,
                DEFAULT_ID_TOKEN_SSUER, DEFAULT_ISSUER, DEFAULT_SUBJECT_ENTRY, createCNF(base64Modulus));

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testAuthenticateNoIdTokenAudienceMatch() {
        final String token = createPOPToken(DEFAULT_ISSUER, BASE_URL, POP_TOKEN, headers,
                DEFAULT_ID_TOKEN_SSUER, "wrong aud", DEFAULT_SUBJECT_ENTRY, createCNF(base64Modulus));

        assertThrows(IncorrectClaimException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testAuthenticateNoTokenType() {
        final String token = createPOPToken(DEFAULT_ISSUER, BASE_URL, null, headers,
                DEFAULT_ID_TOKEN_SSUER, DEFAULT_ISSUER, DEFAULT_SUBJECT_ENTRY, createCNF(base64Modulus));

        assertThrows(MissingClaimException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testAuthenticateNoKeyId() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        final String token = createPOPToken(DEFAULT_ISSUER, BASE_URL, POP_TOKEN, headers, DEFAULT_ID_TOKEN_SSUER,
                DEFAULT_ISSUER, new WebIdEntry(OAuthUtils.WEBID, DEFAULT_ID_TOKEN_SSUER + "/bob/profile#i"),
                createCNF(base64Modulus));

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testAuthenticateNoKeyCnf() {
        final String internalJws = Jwts.builder().setHeader(headers).claim("foo", "bar")
                .signWith(privateKey).compact();
        final Claims claims = new DefaultClaims();
        claims.put(ID_TOKEN_CLAIM, internalJws);
        final String token = createJWTToken(headers, claims);

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testWrongTypeInJwk() {
        final String token = createPOPToken(BASE_URL, DEFAULT_ID_TOKEN_SSUER, DEFAULT_SUBJECT_ENTRY,
                createCNF(64));

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testCnfOfWrongType() {
        final String token = createPOPToken(BASE_URL, DEFAULT_ID_TOKEN_SSUER, DEFAULT_SUBJECT_ENTRY, "Wrong");

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testWebIdProviderConfirmation() {
        final String token =
            createPOPToken(BASE_URL, "https://dark.com", DEFAULT_SUBJECT_ENTRY,
                    createCNF(base64Modulus));

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testNoWebIdClaim() {
        final String token =
            createPOPToken(BASE_URL, DEFAULT_ID_TOKEN_SSUER, new WebIdEntry(), createCNF(base64Modulus));

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testNoIdTokenIssuerClaim() {
        final String token =
            createPOPToken(BASE_URL, null, DEFAULT_SUBJECT_ENTRY, createCNF(base64Modulus));

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testNoAudienceClaim() {
        final String token = createPOPToken(null, DEFAULT_ID_TOKEN_SSUER, DEFAULT_SUBJECT_ENTRY,
                createCNF(base64Modulus));

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testWrongAudienceClaim() {
        final String token = createPOPToken("https://attacker.com", DEFAULT_ID_TOKEN_SSUER,
                DEFAULT_SUBJECT_ENTRY, createCNF(base64Modulus));

        assertThrows(WebIdOIDCJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testGreenPathWithSameOrigin() {
        final String token = createPOPToken(BASE_URL, DEFAULT_ID_TOKEN_SSUER,
                new WebIdEntry("sub", DEFAULT_ID_TOKEN_SSUER + "/bob/profile#i"), createCNF(base64Modulus));

        final Principal principal = authenticator.authenticate(token);
        assertNotNull(principal);
    }

    @Test
    void testGreenPathWithSubDomain() {
        final String token = createPOPToken(BASE_URL, DEFAULT_ID_TOKEN_SSUER, DEFAULT_SUBJECT_ENTRY,
                createCNF(base64Modulus));

        final Principal principal = authenticator.authenticate(token);
        assertNotNull(principal);
    }

    @Test
    void testGreenPathWithWebIdClaim() {
        final String token = createPOPToken(BASE_URL, DEFAULT_ID_TOKEN_SSUER,
                new WebIdEntry(OAuthUtils.WEBID, DEFAULT_ID_TOKEN_SSUER + "/bob/profile#i"), createCNF(base64Modulus));

        final Principal principal = authenticator.authenticate(token);
        assertNotNull(principal);
    }

    private static Map<String, Object> createCNF(final Object n) {
        final Map<String, Object> cnf = new HashMap<>();
        final Map<String, Object> jwk = new HashMap<>();
        cnf.put("jwk", jwk);
        jwk.put("alg", "RS256");
        jwk.put("n", n);
        jwk.put("e", base64PublicExponent);
        return cnf;
    }

    private static String createPOPToken(final String audience, final String idTokenIssuer,
                                         final Map.Entry<String, String> webIdClaim, final Object cnf) {
        return createPOPToken(DEFAULT_ISSUER, audience, POP_TOKEN, headers, idTokenIssuer, DEFAULT_ISSUER,
                webIdClaim, cnf);
    }

    private static String createPOPToken(final String issuer, final String audience, final String tokenType,
                                         final Map<String, Object> idTokenHeaders, final String idTokenIssuer,
                                         final String idTokenAudience, final Map.Entry<String, String> webIdClaim,
                                         final Object cnf) {
        final DefaultClaims idTokenClaims = new DefaultClaims();
        idTokenClaims.setIssuer(idTokenIssuer);
        if (idTokenAudience != null) {
            idTokenClaims.setAudience(idTokenAudience);
        }
        idTokenClaims.put(webIdClaim.getKey(), webIdClaim.getValue());
        idTokenClaims.put(CNF_CLAIM, cnf);
        final String internalJws = createJWTToken(idTokenHeaders, idTokenClaims);

        final Claims claims = new DefaultClaims();
        if (issuer != null) {
            claims.setIssuer(issuer);
        }
        claims.setAudience(audience);
        claims.put(ID_TOKEN_CLAIM, internalJws);
        if (tokenType != null) {
            claims.put(TOKEN_TYPE_CLAIM, tokenType);
        }
        return createJWTToken(headers, claims);
    }

    private static String createJWTToken(final Map<String, Object> headers, final Claims claims) {
        return Jwts.builder()
            .setHeader(headers)
            .setClaims(claims)
            .signWith(privateKey)
            .compact();
    }
}