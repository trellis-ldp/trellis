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

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;
import static io.jsonwebtoken.security.Keys.secretKeyFor;
import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.EllipticCurveProvider;
import io.jsonwebtoken.impl.crypto.RsaProvider;
import io.jsonwebtoken.security.SecurityException;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Principal;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class JwtAuthenticatorTest {

    @Test
    void testAuthenticate() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS256);
        final String token = Jwts.builder().setSubject("https://people.apache.org/~acoburn/#i")
             .signWith(key).compact();

        final Authenticator authenticator = new JwtAuthenticator(key);

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateEC() {
        final KeyPair keypair = EllipticCurveProvider.generateKeyPair(SignatureAlgorithm.ES256);
        final String token = Jwts.builder().setSubject("https://people.apache.org/~acoburn/#i")
             .signWith(keypair.getPrivate(), SignatureAlgorithm.ES256).compact();

        final Authenticator authenticator = new JwtAuthenticator(keypair.getPublic());

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateRSA() {
        final KeyPair keypair = RsaProvider.generateKeyPair();
        final String token = Jwts.builder().setSubject("https://people.apache.org/~acoburn/#i")
             .signWith(keypair.getPrivate(), SignatureAlgorithm.RS256).compact();

        final Authenticator authenticator = new JwtAuthenticator(keypair.getPublic());

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateKeystore() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis", "password".toCharArray());
        final String token = Jwts.builder().setSubject("https://people.apache.org/~acoburn/#i")
             .signWith(privateKey, SignatureAlgorithm.RS256).compact();

        final Authenticator authenticator = new JwtAuthenticator(
                ks.getCertificate("trellis").getPublicKey());

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateKeystoreRSA() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis", "password".toCharArray());
        final String token = Jwts.builder().setSubject("https://people.apache.org/~acoburn/#i")
             .signWith(privateKey, SignatureAlgorithm.RS256).compact();

        final Authenticator authenticator = new JwtAuthenticator(
                ks.getCertificate("trellis-public").getPublicKey());

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateKeystoreEC() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setSubject("https://people.apache.org/~acoburn/#i")
             .signWith(privateKey, SignatureAlgorithm.ES256).compact();

        final Authenticator authenticator = new JwtAuthenticator(
                ks.getCertificate("trellis-ec").getPublicKey());

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateKeystoreECWebsite() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setSubject("acoburn")
             .claim("website", "https://people.apache.org/~acoburn/#i")
             .signWith(privateKey, SignatureAlgorithm.ES256).compact();

        final Authenticator authenticator = new JwtAuthenticator(
                ks.getCertificate("trellis-ec").getPublicKey());

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateNoSub() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS384);
        final String token = Jwts.builder().setIssuer("http://localhost").signWith(key).compact();

        final Authenticator authenticator = new JwtAuthenticator(key);

        final Principal p = authenticator.authenticate(token);
        assertNull(p, "Unexpected principal!");
    }

    @Test
    void testAuthenticateSubIss() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().setSubject("acoburn").setIssuer("http://localhost")
             .signWith(key).compact();

        final Authenticator authenticator = new JwtAuthenticator(key);

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("http://localhost/acoburn", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateSubNoWebIss() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().setSubject("acoburn").setIssuer("some org").signWith(key).compact();

        final Authenticator authenticator = new JwtAuthenticator(key);

        final Principal p = authenticator.authenticate(token);
        assertNull(p, "Unexpected principal!");
    }

    @Test
    void testAuthenticateToken() {
        final String key = "N0NuokWWb5XjMP+V3XLfyLkaSArwxNm17VeAvv7+y4+Y/DmxBLenvwOPO404lfl6UfyyEGgQ02ETDEPRMwV/+Q==";
        final String token = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJodHRwczovL3Blb3BsZS5hcGFjaGUub3JnL35" +
            "hY29idXJuLyNpIn0.n-C7xhjVyn3WEWGfSXfuqrjXVSoAnD08sO5K8mDsBiZF6Z8lwiksGos6lR-6RjD5jI25d1yPJ47LKBWqMlMm_A";

        final Authenticator authenticator = new JwtAuthenticator(hmacShaKeyFor(Base64.getDecoder().decode(key)));

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateTokenIssSub() {
        final String key = "N0NuokWWb5XjMP+V3XLfyLkaSArwxNm17VeAvv7+y4+Y/DmxBLenvwOPO404lfl6UfyyEGgQ02ETDEPRMwV/+Q==";
        final String token = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhY29idXJuIiwibmFtZSI6IkFhcm9uIENvYnVyb" +
            "iIsImlzcyI6Imh0dHA6Ly9leGFtcGxlLm9yZy8ifQ.4Srityp5iPScGyqvkPakD3DmtXYWhkyHjr0K6B7kpcR2ll8MC-hGpYoIDM8ar" +
            "ro3dyZQp0kDhPfYZ6MiAGfGTQ";

        final Authenticator authenticator = new JwtAuthenticator(hmacShaKeyFor(Base64.getDecoder().decode(key)));

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("http://example.org/acoburn", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticationTokenWebid() {
        final String key = "N0NuokWWb5XjMP+V3XLfyLkaSArwxNm17VeAvv7+y4+Y/DmxBLenvwOPO404lfl6UfyyEGgQ02ETDEPRMwV/+Q==";
        final String token = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJ3ZWJpZCI6Imh0dHBzOi8vcGVvcGxlLmFwYWNoZS5vcmcvfm" +
            "Fjb2J1cm4vI2kiLCJzdWIiOiJhY29idXJuIiwibmFtZSI6IkFhcm9uIENvYnVybiIsImlzcyI6Imh0dHA6Ly9leGFtcGxlLm9yZy8ifQ" +
            ".kIHJDSzaisxfIF5fQou2e9rBInsDsl0vZ4QQ60zlZlSufm9nnmC7eL-875WPsVGzPAfptF6MrImrpFeNxdW9ZQ";

        final Authenticator authenticator = new JwtAuthenticator(hmacShaKeyFor(Base64.getDecoder().decode(key)));

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticationTokenWebidBadKey() {
        final String key = "2YuUlb+t36yVzrTkYLl8xBlBJSC41CE7uNF3somMDxdYDfcACv9JYIU54z17s4Ah313uKu/4Ll+vDNKpxx6v4Q==";
        final String token = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJ3ZWJpZCI6Imh0dHBzOi8vcGVvcGxlLmFwYWNoZS5vcmcvfm" +
            "Fjb2J1cm4vI2kiLCJzdWIiOiJhY29idXJuIiwibmFtZSI6IkFhcm9uIENvYnVybiIsImlzcyI6Imh0dHA6Ly9leGFtcGxlLm9yZy8ifQ" +
            ".kIHJDSzaisxfIF5fQou2e9rBInsDsl0vZ4QQ60zlZlSufm9nnmC7eL-875WPsVGzPAfptF6MrImrpFeNxdW9ZQ";

        final Authenticator authenticator = new JwtAuthenticator(hmacShaKeyFor(Base64.getDecoder().decode(key)));

        assertThrows(SecurityException.class, () -> authenticator.authenticate(token), "Parsed bad JWT!");
    }

    @Test
    void testAuthenticationNoPrincipal() {
        final String key = "w8+z9hrcbr3ktQ5WTr9xNZknke3L/RAj8r8RieriWozGu1M4RDgkpJcfTEg90pqYyadbIBLy+qFHu1JJ8O0rjw==";
        final String token = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhY29idXJuIiwibmFtZSI6IkFhcm9" +
            "uIENvYnVybiJ9.srs7gSbix8nLDuFmwYCEN0In-5pa6-59D5nqF1UgRD-hsJBS2UoieYoBJZNGGKj1hO1DaboqtuS_36bE9QGdCw";

        final Authenticator authenticator = new JwtAuthenticator(hmacShaKeyFor(Base64.getDecoder().decode(key)));

        final Principal p = authenticator.authenticate(token);
        assertNull(p, "Unexpected principal!");
    }

    @Test
    void testGarbledToken() {
        final String key = "thj983z1fiqAiaV7Nv4nWpjaDi6eVTd7jOGxbs92mp8=";
        final String token = "blahblah";

        final Authenticator authenticator = new JwtAuthenticator(hmacShaKeyFor(Base64.getDecoder().decode(key)));

        assertThrows(MalformedJwtException.class, () -> authenticator.authenticate(token), "Parsed bad JWT!");
    }
}
