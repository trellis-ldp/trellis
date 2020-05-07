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

import static java.time.Instant.now;
import static java.util.Base64.getUrlDecoder;
import static java.util.Date.from;
import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecurityException;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.Principal;
import java.security.spec.RSAPrivateKeySpec;

import org.junit.jupiter.api.Test;

class JwksAuthenticatorTest {

    private static final String url = "https://www.trellisldp.org/tests/jwks.json";
    private static final String keyid = "trellis-test";

    private static final BigInteger exponent = new BigInteger(1, getUrlDecoder().decode("VRPRBm9dCoAJfBbEz5oAHEz7Tnm" +
            "0i0O6m5yj7NwqAZOj9i4ZwgZ8VZQo88oxZQWNaYd1yKeoQhUsJija_vxQEPXO1Q2q6OqMcwTBH0wyGhIFp--z2dAyRlDVLUTQbJUXyq" +
            "ammdh7b16-igH-BB67jfolM-cw-O7YaN7GrxCCYX5bI38IipeYfcroeIUXdLYmmUdNy7c8P2_K4O-iHQ6A4AUtQRUOzt2FGOdmlGZih" +
            "upI9YprshIy9CZq_iA3BcOl4Gcc-ljwwUzT0M_4jt53DCV7oxqWVt9WRdYDNoD62g2FzQ-1nYUqsz4YChk1MuOPV1xFpRklwSpt5dfh" +
            "uldnbQ"));

    private static final BigInteger modulus = new BigInteger(1, getUrlDecoder().decode("oMyjaeUbmnqojRpMBDbWbfBFitd_" +
            "dQcFJ96CDWwzsVcyAK3_kp4dEvhc2KLBjrmE69gJ-4HRuPF-kulDEmpC-MVx9eOihdUG9XV0ZA_eYWj9RoI_Gt3TUqTxlQH_nJRADTf" +
            "y82fOCCboKpaQ2idZH55Vb0FDbau2b2462tYRmcnxTFjClP4fDTTubI-3oFJ4tKMjynvUT34mCrZPiM8Q4noxVoyRYpzUTL1USxdUf5" +
            "6IKSB8NduH438zhMXE5VLC6PzhR3i_4KKpe4nq2otsrJ3KlEc7Me6UeiMXxPYz8rrPovW5L3LFWDmntGs5q923fBZFLFg8yBgMdTine" +
            "aahEQ"));

    @Test
    void testAuthenticateJwks() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";

        final Key key = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, keyid)
            .setSubject(webid).signWith(key).compact();

        final Authenticator authenticator = new JwksAuthenticator(url);

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateJwksAsWebid() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";

        final Key key = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, keyid)
            .claim("webid", webid).signWith(key).compact();

        final Authenticator authenticator = new JwksAuthenticator(url);

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateJwksExpired() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";

        final Key key = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, keyid).claim("webid", webid)
            .setExpiration(from(now().minusSeconds(10))).signWith(key).compact();

        final Authenticator authenticator = new JwksAuthenticator(url);

        assertThrows(ExpiredJwtException.class, () -> authenticator.authenticate(token), "Unexpected principal!");
    }

    @Test
    void testAuthenticateJwksWrongKeyid() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";

        final Key key = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "non-existent")
            .setSubject(webid).signWith(key).compact();

        final Authenticator authenticator = new JwksAuthenticator(url);

        assertThrows(SecurityException.class, () -> authenticator.authenticate(token), "Unexpected principal!");
    }

    @Test
    void testAuthenticateJwksNoKeyid() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";

        final Key key = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String token = Jwts.builder().setSubject(webid).signWith(key).compact();

        final Authenticator authenticator = new JwksAuthenticator(url);

        assertThrows(JwtException.class, () -> authenticator.authenticate(token), "Unexpected principal!");
    }

    @Test
    void testAuthenticateJwksInvalidKeyLocation() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";

        final Key key = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, keyid).setSubject(webid)
            .signWith(key).compact();

        final Authenticator authenticator = new JwksAuthenticator("https://www.trellisldp.org/tests/non-existent");

        assertThrows(SecurityException.class, () -> authenticator.authenticate(token), "Unexpected principal!");
    }
}
