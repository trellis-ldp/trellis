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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.Base64.getUrlDecoder;
import static java.util.Collections.singleton;
import static java.util.Date.from;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.spec.RSAPrivateKeySpec;
import java.util.stream.Stream;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OAuthFilterTest {

    private static final String WEBID1 = "https://people.apache.org/~acoburn/#i";
    private static final String WEBID2 = "https://user.example.com/#me";

    @Mock
    private ContainerRequestContext mockContext;

    @Mock
    private SecurityContext mockSecurityContext;

    @Captor
    private ArgumentCaptor<SecurityContext> securityArgument;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void testFilterNoSecCtx() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().setSubject(WEBID1).signWith(key).compact();
        final ContainerRequestContext mockCtx = mock(ContainerRequestContext.class);
        when(mockCtx.getSecurityContext()).thenReturn(null);
        when(mockCtx.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key));
        filter.filter(mockCtx);
        verify(mockCtx).setSecurityContext(securityArgument.capture());
        assertEquals(WEBID1, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(OAuthFilter.SCHEME, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
    }

    @Test
    void testFilterNotSecureSecCtx() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().setSubject(WEBID1).signWith(key).compact();
        final ContainerRequestContext mockCtx = mock(ContainerRequestContext.class);
        when(mockCtx.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockSecurityContext.isSecure()).thenReturn(true);
        when(mockCtx.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key));
        filter.filter(mockCtx);
        verify(mockCtx).setSecurityContext(securityArgument.capture());
        assertEquals(WEBID1, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(OAuthFilter.SCHEME, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertTrue(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
    }

    @Test
    void testFilterAuth() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().setSubject(WEBID1).signWith(key).compact();
        final ContainerRequestContext mockCtx = mock(ContainerRequestContext.class);
        when(mockCtx.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockCtx.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key));
        filter.filter(mockCtx);
        verify(mockCtx).setSecurityContext(securityArgument.capture());
        assertEquals(WEBID1, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(OAuthFilter.SCHEME, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
    }

    @Test
    void testFilterNoAuth() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final ContainerRequestContext mockCtx = mock(ContainerRequestContext.class);
        when(mockCtx.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockCtx.getHeaderString(AUTHORIZATION)).thenReturn(null);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key));
        filter.filter(mockCtx);
        verify(mockCtx, never()).setSecurityContext(securityArgument.capture());
    }

    @Test
    void testFilterWebid() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().claim("webid", WEBID2).signWith(key).compact();
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key));
        filter.filter(mockContext);
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals(WEBID2, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(OAuthFilter.SCHEME, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertFalse(securityArgument.getValue().isUserInRole("admin"), "Unexpectedly in user role!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
    }

    @Test
    void testFilterAdminWebid() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().claim("webid", WEBID2).signWith(key).compact();
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key), "trellis", singleton(WEBID2));
        filter.filter(mockContext);
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals(WEBID2, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(OAuthFilter.SCHEME, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertTrue(securityArgument.getValue().isUserInRole("admin"), "Unexpectedly in user role!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
    }


    @Test
    void testFilterInvalidAuth() {
        final String key = "BdEaIIfv67jl8mRL+/vnuf3RzfVfpkxtel8icx2B8uSudOcwVXr7zpwj92UtKCOkVGi2FaE+O4q55P3p7UE7Eg==";
        final String token = Jwts.builder().setSubject(WEBID1).signWith(hmacShaKeyFor(key.getBytes(UTF_8))).compact();
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(hmacShaKeyFor(key.replaceFirst("B", "A")
                        .getBytes())));
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    void testFilterExpiredJwt() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().claim("webid", WEBID1).setExpiration(from(now().minusSeconds(10)))
            .signWith(key).compact();
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key));
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    void testFilterGenericWebid() {
        try {
            System.setProperty(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET,
                    "y7MCBmoOx7TH70q1fabSGLzOrEYx+liUmLWPkwIPUTfWMXn/J5MDZuepBd8mcRObUDYYQN3MIS8p40ZT5EhvWw==");
            final String token = Jwts.builder().claim("webid", WEBID1).signWith(hmacShaKeyFor(getConfig()
                            .getValue(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET, String.class).getBytes(UTF_8)))
                            .compact();
            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);
            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext).setSecurityContext(securityArgument.capture());
            assertAll("Validate security context", checkSecurityContext(securityArgument.getValue(), WEBID1));
        } finally {
            System.clearProperty(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET);
        }
    }

    @Test
    void testFilterGenericJwtSecret() {
        try {
            System.setProperty(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET,
                    "y7MCBmoOx7TH70q1fabSGLzOrEYx+liUmLWPkwIPUTfWMXn/J5MDZuepBd8mcRObUDYYQN3MIS8p40ZT5EhvWw==");
            final String token = Jwts.builder().claim("webid", WEBID2).signWith(hmacShaKeyFor(getConfig()
                            .getValue(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET, String.class).getBytes(UTF_8)))
                            .compact();
            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("BEARER " + token);
            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext).setSecurityContext(securityArgument.capture());
            assertAll("Validate security context", checkSecurityContext(securityArgument.getValue(), WEBID2));
        } finally {
            System.clearProperty(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET);
        }
    }

    @Test
    void testFilterNotBasicAuth() {
        try {
            System.setProperty(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET,
                    "y7MCBmoOx7TH70q1fabSGLzOrEYx+liUmLWPkwIPUTfWMXn/J5MDZuepBd8mcRObUDYYQN3MIS8p40ZT5EhvWw==");
            final String token = Jwts.builder().claim("webid", WEBID1)
                .signWith(hmacShaKeyFor(getConfig()
                            .getValue(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET, String.class).getBytes(UTF_8)))
                            .compact();
            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Basic " + token);

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext, never()).setSecurityContext(any());
        } finally {
            System.clearProperty(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET);
        }
    }

    @Test
    void testFilterNoToken() {
        try {
            System.setProperty(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET,
                    "y7MCBmoOx7TH70q1fabSGLzOrEYx+liUmLWPkwIPUTfWMXn/J5MDZuepBd8mcRObUDYYQN3MIS8p40ZT5EhvWw==");
            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer");

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext, never()).setSecurityContext(any());
        } finally {
            System.clearProperty(OAuthFilter.CONFIG_AUTH_OAUTH_SHARED_SECRET);
        }
    }


    @Test
    void testFilterGenericNoAuth() {
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().claim("webid", WEBID1).signWith(key).compact();
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter();
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    void testFilterGenericJwks() throws Exception {
        final BigInteger exponent = new BigInteger(1, getUrlDecoder().decode("VRPRBm9dCoAJfBbEz5oAHEz7Tnm0i0O6m5yj7N" +
            "wqAZOj9i4ZwgZ8VZQo88oxZQWNaYd1yKeoQhUsJija_vxQEPXO1Q2q6OqMcwTBH0wyGhIFp--z2dAyRlDVLUTQbJUXyqammdh7b16-i" +
            "gH-BB67jfolM-cw-O7YaN7GrxCCYX5bI38IipeYfcroeIUXdLYmmUdNy7c8P2_K4O-iHQ6A4AUtQRUOzt2FGOdmlGZihupI9YprshIy" +
            "9CZq_iA3BcOl4Gcc-ljwwUzT0M_4jt53DCV7oxqWVt9WRdYDNoD62g2FzQ-1nYUqsz4YChk1MuOPV1xFpRklwSpt5dfhuldnbQ"));

        final BigInteger modulus = new BigInteger(1, getUrlDecoder().decode("oMyjaeUbmnqojRpMBDbWbfBFitd_dQcFJ96CDWw" +
            "zsVcyAK3_kp4dEvhc2KLBjrmE69gJ-4HRuPF-kulDEmpC-MVx9eOihdUG9XV0ZA_eYWj9RoI_Gt3TUqTxlQH_nJRADTfy82fOCCboKp" +
            "aQ2idZH55Vb0FDbau2b2462tYRmcnxTFjClP4fDTTubI-3oFJ4tKMjynvUT34mCrZPiM8Q4noxVoyRYpzUTL1USxdUf56IKSB8NduH4" +
            "38zhMXE5VLC6PzhR3i_4KKpe4nq2otsrJ3KlEc7Me6UeiMXxPYz8rrPovW5L3LFWDmntGs5q923fBZFLFg8yBgMdTineaahEQ"));

        try {
            System.setProperty(OAuthFilter.CONFIG_AUTH_OAUTH_JWK_URL, "https://www.trellisldp.org/tests/jwks.json");

            final Key key = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
            final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-test")
                .setSubject(WEBID2).signWith(key).compact();

            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext).setSecurityContext(securityArgument.capture());
            assertEquals(WEBID2, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        } finally {
            System.clearProperty(OAuthFilter.CONFIG_AUTH_OAUTH_JWK_URL);
        }
    }

    @Test
    void testFilterGenericFederated() throws Exception {
        final String passphrase = "password";
        try {
            final String keystorePath = OAuthUtilsTest.class.getResource("/keystore.jks").getPath();
            System.setProperty(OAuthFilter.CONFIG_AUTH_OAUTH_KEYSTORE_PATH, keystorePath);
            System.setProperty(OAuthFilter.CONFIG_AUTH_OAUTH_KEYSTORE_CREDENTIALS, passphrase);
            System.setProperty(OAuthFilter.CONFIG_AUTH_OAUTH_KEYSTORE_IDS, "trellis,trellis-ec");

            final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase.toCharArray());

            final Key privateKey = ks.getKey("trellis", passphrase.toCharArray());
            final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis")
                .setSubject(WEBID2).signWith(privateKey, SignatureAlgorithm.RS256).compact();

            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext).setSecurityContext(securityArgument.capture());
            assertEquals(WEBID2, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        } finally {
            System.clearProperty(OAuthFilter.CONFIG_AUTH_OAUTH_KEYSTORE_PATH);
            System.clearProperty(OAuthFilter.CONFIG_AUTH_OAUTH_KEYSTORE_CREDENTIALS);
            System.clearProperty(OAuthFilter.CONFIG_AUTH_OAUTH_KEYSTORE_IDS);
        }
    }

    private static Stream<Executable> checkSecurityContext(final SecurityContext ctx, final String webid) {
        return of(
                () -> assertEquals(webid, ctx.getUserPrincipal().getName(), "Unexpected agent IRI!"),
                () -> assertEquals(OAuthFilter.SCHEME, ctx.getAuthenticationScheme(), "Unexpected scheme!"),
                () -> assertFalse(ctx.isSecure(), "Unexpected secure flag!"),
                () -> assertFalse(ctx.isUserInRole("some role"), "Unexpectedly in user role!"));
    }
}
