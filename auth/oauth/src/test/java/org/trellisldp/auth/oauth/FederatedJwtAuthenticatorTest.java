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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SecurityException;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class FederatedJwtAuthenticatorTest {

    private static final char[] passphrase = "password".toCharArray();

    @Test
    void testAuthenticateKeystore() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final Key privateKey = ks.getKey("trellis", passphrase);
        final String jwt = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis")
            .setSubject("https://people.apache.org/~acoburn/#me")
            .signWith(privateKey, SignatureAlgorithm.RS256).compact();

        final Authenticator authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis", "foo"));

        final Principal p = authenticator.authenticate(jwt);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#me", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateKeystoreRSA() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final Key privateKey = ks.getKey("trellis", passphrase);
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-public")
            .setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(privateKey, SignatureAlgorithm.RS256).compact();

        final Authenticator authenticator = new FederatedJwtAuthenticator(ks,
                singletonList("trellis-public"));

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateKeystoreEC() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final String token = buildEcToken(ks.getKey("trellis-ec", passphrase), "trellis-ec");
        final Authenticator authenticator = new FederatedJwtAuthenticator(ks,
                singletonList("trellis-ec"));

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("https://people.apache.org/~acoburn/#i", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateNoSub() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final Key privateKey = ks.getKey("trellis-ec", passphrase);
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-ec")
            .setIssuer("http://localhost").signWith(privateKey, SignatureAlgorithm.ES256).compact();

        final Authenticator authenticator = new FederatedJwtAuthenticator(ks,
                singletonList("trellis-ec"));

        assertNull(authenticator.authenticate(token), "Unexpected principal!");
    }

    @Test
    void testAuthenticateSubIss() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final Key privateKey = ks.getKey("trellis-ec", passphrase);
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-ec")
            .setSubject("acoburn").setIssuer("http://localhost")
            .signWith(privateKey, SignatureAlgorithm.ES256).compact();

        final Authenticator authenticator = new FederatedJwtAuthenticator(ks,
                singletonList("trellis-ec"));

        final Principal p = authenticator.authenticate(token);
        assertNotNull(p, "Missing principal!");
        assertEquals("http://localhost/acoburn", p.getName(), "Incorrect webid!");
    }

    @Test
    void testAuthenticateSubNoWebIss() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final Key privateKey = ks.getKey("trellis-ec", passphrase);
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-ec")
            .setSubject("acoburn").setIssuer("some org")
            .signWith(privateKey, SignatureAlgorithm.ES256).compact();

        final Authenticator authenticator = new FederatedJwtAuthenticator(ks,
                singletonList("trellis-ec"));

        assertNull(authenticator.authenticate(token), "Unexpected principal!");
    }

    @Test
    void testAuthenticateKeystoreNoKeyId() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final Key privateKey = ks.getKey("trellis-ec", passphrase);
        final String token = Jwts.builder().setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(privateKey, SignatureAlgorithm.ES256).compact();
        final Authenticator authenticator = new FederatedJwtAuthenticator(ks,
                singletonList("trellis-ec"));

        assertThrows(JwtException.class, () -> authenticator.authenticate(token), "Unexpected key id field!");
    }

    @Test
    void testAuthenticateKeystoreNoMatch() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final String token = buildEcToken(ks.getKey("trellis-ec", passphrase), "trellis-ec");
        final Authenticator authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis", "foo"));

        assertThrows(SecurityException.class, () -> authenticator.authenticate(token), "Unexpected keystore entry!");
    }

    @Test
    void testAuthenticateKeystoreAnotherNoMatch() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final String token = buildEcToken(ks.getKey("trellis-ec", passphrase), "foo");
        final Authenticator authenticator = new FederatedJwtAuthenticator(ks,
                singletonList("foo"));

        assertThrows(SecurityException.class, () -> authenticator.authenticate(token), "Unexpected keystore entry!");
    }

    @Test
    void testKeyStoreException() throws Exception {
        final KeyStore mockKeyStore = mock(KeyStore.class, inv -> {
            throw new KeyStoreException("Expected");
        });

        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase);

        final String token = buildEcToken(ks.getKey("trellis-ec", passphrase), "trellis-ec");
        final Authenticator authenticator = new FederatedJwtAuthenticator(mockKeyStore,
                singletonList("trellis-ec"));

        assertThrows(SecurityException.class, () -> authenticator.authenticate(token),
                "Unexpectedly functional keystore!");
    }

    private String buildEcToken(final Key key, final String id) {
        return Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, id)
            .setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(key, SignatureAlgorithm.ES256).compact();
    }
}
