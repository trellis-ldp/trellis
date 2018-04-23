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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class FederatedJwtAuthenticatorTest {

    @Test
    public void testAuthenticateKeystore() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis", "password".toCharArray());
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis")
            .setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(SignatureAlgorithm.RS256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis", "foo"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertTrue(result.isPresent());
        result.ifPresent(p -> {
            assertEquals("https://people.apache.org/~acoburn/#i", p.getName());
        });
    }

    @Test
    public void testAuthenticateKeystoreRSA() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis", "password".toCharArray());
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-public")
            .setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(SignatureAlgorithm.RS256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis-public"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertTrue(result.isPresent());
        result.ifPresent(p -> {
            assertEquals("https://people.apache.org/~acoburn/#i", p.getName());
        });
    }

    @Test
    public void testAuthenticateKeystoreEC() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-ec")
            .setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(SignatureAlgorithm.ES256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis-ec"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertTrue(result.isPresent());
        result.ifPresent(p -> {
            assertEquals("https://people.apache.org/~acoburn/#i", p.getName());
        });
    }

    @Test
    public void testAuthenticateNoSub() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-ec")
            .setIssuer("http://localhost").signWith(SignatureAlgorithm.ES256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis-ec"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAuthenticateSubIss() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-ec")
            .setSubject("acoburn").setIssuer("http://localhost")
            .signWith(SignatureAlgorithm.ES256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis-ec"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertTrue(result.isPresent());
        result.ifPresent(p -> {
            assertEquals("http://localhost/acoburn", p.getName());
        });
    }

    @Test
    public void testAuthenticateSubNoWebIss() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-ec")
            .setSubject("acoburn").setIssuer("some org")
            .signWith(SignatureAlgorithm.ES256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis-ec"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAuthenticateKeystoreNoKeyId() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(SignatureAlgorithm.ES256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis-ec"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAuthenticateKeystoreNoMatch() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-ec")
            .setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(SignatureAlgorithm.ES256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(ks,
                asList("trellis", "foo"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAuthenticateKeystoreAnotherNoMatch() throws Exception {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "foo")
            .setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(SignatureAlgorithm.ES256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(ks,
                asList("foo"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKeyStoreException() throws Exception {
        final KeyStore mockKeyStore = mock(KeyStore.class, inv -> {
                throw new KeyStoreException("Expected");
        });

        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());

        final Key privateKey = ks.getKey("trellis-ec", "password".toCharArray());
        final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-ec")
            .setSubject("https://people.apache.org/~acoburn/#i")
            .signWith(SignatureAlgorithm.ES256, privateKey).compact();

        final Authenticator<String, Principal> authenticator = new FederatedJwtAuthenticator(mockKeyStore,
                asList("trellis-ec"));

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }
}
