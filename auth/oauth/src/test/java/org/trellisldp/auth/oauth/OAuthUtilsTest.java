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
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

class OAuthUtilsTest {

    private static final char[] passphrase = "password".toCharArray();

    @Test
    void testBuilderSimpleDefault() {
        assertNull(OAuthUtils.buildAuthenticatorWithSharedSecret(null));
        assertNull(OAuthUtils.buildAuthenticatorWithSharedSecret(""));
    }

    @Test
    void testBuildFederatedNull() {
        assertNull(OAuthUtils.buildAuthenticatorWithTruststore(null, "test".toCharArray(),
                    asList("one,two".split(","))));
    }

    @Test
    void testOAuthFederatedBuilder() {
        final String keystorePath = OAuthUtilsTest.class.getResource("/keystore.jks").getPath();
        final List<String> ids = asList("trellis,foo".split(","));
        final Authenticator authenticator = OAuthUtils.buildAuthenticatorWithTruststore(keystorePath, passphrase, ids);
        assertTrue(authenticator instanceof JwtAuthenticator);
    }

    @Test
    void testOAuthBuilderNoKeystore() {
        final String keystorePath = OAuthUtilsTest.class.getResource("/keystore.jks").getPath();
        final List<String> ids = asList("trellis,foo".split(","));
        assertNull(OAuthUtils.buildAuthenticatorWithTruststore(keystorePath + "foo", passphrase, ids));
    }

    @Test
    void testOAuthBuilderNoIds() {
        final String keystorePath = OAuthUtilsTest.class.getResource("/keystore.jks").getPath();
        final List<String> ids = asList("foo,bar".split(","));
        assertNull(OAuthUtils.buildAuthenticatorWithTruststore(keystorePath, passphrase, ids));
    }

    @Test
    void testOAuthFederatedBuilderMultipleIds() {
        final String keystorePath = OAuthUtilsTest.class.getResource("/keystore.jks").getPath();
        final List<String> ids = asList("trellis,trellis-ec".split(","));
        final Authenticator authenticator = OAuthUtils.buildAuthenticatorWithTruststore(keystorePath, passphrase, ids);
        assertTrue(authenticator instanceof FederatedJwtAuthenticator);
    }

    @Test
    void testOAuthFederatedBuilderBadPassphrase() {
        final String keystorePath = OAuthUtilsTest.class.getResource("/keystore.jks").getPath();
        final List<String> ids = asList("trellis,trellis-ec".split(","));
        assertNull(OAuthUtils.buildAuthenticatorWithTruststore(keystorePath, "foo".toCharArray(), ids));
    }

    @Test
    void testOAuthJwkBuilder() {
        final String url = "https://www.trellisldp.org/tests/jwks.json";
        final Authenticator authenticator = OAuthUtils.buildAuthenticatorWithJwk(url);
        assertTrue(authenticator instanceof JwksAuthenticator);
    }

    @Test
    void testOAuthJwkBuilderNull() {
        assertNull(OAuthUtils.buildAuthenticatorWithJwk(null));
    }

    @Test
    void testOAuthJwkBuilderNotUrl() {
        assertNull(OAuthUtils.buildAuthenticatorWithJwk("some text"));
    }

    @Test
    void testInvalidRSAKey() {
        assertNull(OAuthUtils.buildRSAPublicKey("EC", BigInteger.ONE, BigInteger.TEN));
    }
}
