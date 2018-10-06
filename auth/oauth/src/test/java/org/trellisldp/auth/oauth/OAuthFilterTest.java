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

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;
import static io.jsonwebtoken.security.Keys.secretKeyFor;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.Base64.getUrlDecoder;
import static java.util.Date.from;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.spec.RSAPrivateKeySpec;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OAuthFilterTest {

    @Mock
    private ContainerRequestContext mockContext;

    @Mock
    private SecurityContext mockSecurityContext;

    @Captor
    private ArgumentCaptor<SecurityContext> securityArgument;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockContext.getSecurityContext()).thenReturn(mockSecurityContext);
    }

    @Test
    public void testFilterAuth() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().setSubject(webid).signWith(key).compact();
        final ContainerRequestContext mockCtx = mock(ContainerRequestContext.class);
        when(mockCtx.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockCtx.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key));
        filter.filter(mockCtx);
        verify(mockCtx).setSecurityContext(securityArgument.capture());
        assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(OAuthFilter.SCHEME, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertTrue(securityArgument.getValue().isUserInRole("some role"), "Not in user role!");
    }

    @Test
    public void testFilterWebid() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().claim("webid", webid).signWith(key).compact();
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key));
        filter.filter(mockContext);
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(OAuthFilter.SCHEME, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertTrue(securityArgument.getValue().isUserInRole("some role"), "Not in user role!");
    }

    @Test
    public void testFilterInvalidAuth() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";
        final String key = "BdEaIIfv67jl8mRL+/vnuf3RzfVfpkxtel8icx2B8uSudOcwVXr7zpwj92UtKCOkVGi2FaE+O4q55P3p7UE7Eg==";
        final String token = Jwts.builder().setSubject(webid).signWith(hmacShaKeyFor(key.getBytes(UTF_8))).compact();
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(hmacShaKeyFor(key.replaceFirst("B", "A")
                        .getBytes())));
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    public void testFilterExpiredJwt() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().claim("webid", webid).setExpiration(from(now().minusSeconds(10)))
            .signWith(key).compact();
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter(new JwtAuthenticator(key));
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    public void testFilterGenericWebid() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";
        try {
            System.setProperty(OAuthFilter.SHARED_SECRET,
                    "y7MCBmoOx7TH70q1fabSGLzOrEYx+liUmLWPkwIPUTfWMXn/J5MDZuepBd8mcRObUDYYQN3MIS8p40ZT5EhvWw==");
            final String token = Jwts.builder().claim("webid", webid)
                .signWith(hmacShaKeyFor(getConfiguration().get(OAuthFilter.SHARED_SECRET).getBytes(UTF_8))).compact();
            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext).setSecurityContext(securityArgument.capture());
            assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
            assertEquals(OAuthFilter.SCHEME, securityArgument.getValue().getAuthenticationScheme(),
                    "Unexpected scheme!");
            assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
            assertTrue(securityArgument.getValue().isUserInRole("some role"), "Not in user role!");
        } finally {
            System.clearProperty(OAuthFilter.SHARED_SECRET);
        }
    }

    @Test
    public void testFilterGenericJwtSecret() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";
        try {
            System.setProperty(OAuthFilter.SHARED_SECRET,
                    "y7MCBmoOx7TH70q1fabSGLzOrEYx+liUmLWPkwIPUTfWMXn/J5MDZuepBd8mcRObUDYYQN3MIS8p40ZT5EhvWw==");
            final String token = Jwts.builder().claim("webid", webid)
                .signWith(hmacShaKeyFor(getConfiguration().get(OAuthFilter.SHARED_SECRET).getBytes(UTF_8))).compact();
            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("BEARER " + token);

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext).setSecurityContext(securityArgument.capture());
            assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
            assertEquals(OAuthFilter.SCHEME, securityArgument.getValue().getAuthenticationScheme(),
                    "Unexpected scheme!");
            assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
            assertTrue(securityArgument.getValue().isUserInRole("some role"), "Not in user role!");
        } finally {
            System.clearProperty(OAuthFilter.SHARED_SECRET);
        }
    }

    @Test
    public void testFilterNotBasicAuth() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";
        try {
            System.setProperty(OAuthFilter.SHARED_SECRET,
                    "y7MCBmoOx7TH70q1fabSGLzOrEYx+liUmLWPkwIPUTfWMXn/J5MDZuepBd8mcRObUDYYQN3MIS8p40ZT5EhvWw==");
            final String token = Jwts.builder().claim("webid", webid)
                .signWith(hmacShaKeyFor(getConfiguration().get(OAuthFilter.SHARED_SECRET).getBytes(UTF_8))).compact();
            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Basic " + token);

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext, never()).setSecurityContext(any());
        } finally {
            System.clearProperty(OAuthFilter.SHARED_SECRET);
        }
    }

    @Test
    public void testFilterNoToken() throws Exception {
        try {
            System.setProperty(OAuthFilter.SHARED_SECRET,
                    "y7MCBmoOx7TH70q1fabSGLzOrEYx+liUmLWPkwIPUTfWMXn/J5MDZuepBd8mcRObUDYYQN3MIS8p40ZT5EhvWw==");
            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer");

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext, never()).setSecurityContext(any());
        } finally {
            System.clearProperty(OAuthFilter.SHARED_SECRET);
        }
    }


    @Test
    public void testFilterGenericNoAuth() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#i";
        final Key key = secretKeyFor(SignatureAlgorithm.HS512);
        final String token = Jwts.builder().claim("webid", webid).signWith(key).compact();
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

        final OAuthFilter filter = new OAuthFilter();
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    public void testFilterGenericJwks() throws Exception {
        final String webid = "https://people.apache.org/~acoburn/#me";
        final BigInteger exponent = new BigInteger(1, getUrlDecoder().decode("VRPRBm9dCoAJfBbEz5oAHEz7Tnm0i0O6m5yj7N" +
            "wqAZOj9i4ZwgZ8VZQo88oxZQWNaYd1yKeoQhUsJija_vxQEPXO1Q2q6OqMcwTBH0wyGhIFp--z2dAyRlDVLUTQbJUXyqammdh7b16-i" +
            "gH-BB67jfolM-cw-O7YaN7GrxCCYX5bI38IipeYfcroeIUXdLYmmUdNy7c8P2_K4O-iHQ6A4AUtQRUOzt2FGOdmlGZihupI9YprshIy" +
            "9CZq_iA3BcOl4Gcc-ljwwUzT0M_4jt53DCV7oxqWVt9WRdYDNoD62g2FzQ-1nYUqsz4YChk1MuOPV1xFpRklwSpt5dfhuldnbQ"));

        final BigInteger modulus = new BigInteger(1, getUrlDecoder().decode("oMyjaeUbmnqojRpMBDbWbfBFitd_dQcFJ96CDWw" +
            "zsVcyAK3_kp4dEvhc2KLBjrmE69gJ-4HRuPF-kulDEmpC-MVx9eOihdUG9XV0ZA_eYWj9RoI_Gt3TUqTxlQH_nJRADTfy82fOCCboKp" +
            "aQ2idZH55Vb0FDbau2b2462tYRmcnxTFjClP4fDTTubI-3oFJ4tKMjynvUT34mCrZPiM8Q4noxVoyRYpzUTL1USxdUf56IKSB8NduH4" +
            "38zhMXE5VLC6PzhR3i_4KKpe4nq2otsrJ3KlEc7Me6UeiMXxPYz8rrPovW5L3LFWDmntGs5q923fBZFLFg8yBgMdTineaahEQ"));

        try {
            System.setProperty(OAuthFilter.JWK_LOCATION, "https://www.trellisldp.org/tests/jwks.json");

            final Key key = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
            final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis-test")
                .setSubject(webid).signWith(key).compact();

            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext).setSecurityContext(securityArgument.capture());
            assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        } finally {
            System.clearProperty(OAuthFilter.JWK_LOCATION);
        }
    }

    @Test
    public void testFilterGenericFederated() throws Exception {
        final String passphrase = "password";
        final String webid = "https://people.apache.org/~acoburn/#me";
        try {
            final String keystorePath = OAuthUtilsTest.class.getResource("/keystore.jks").getPath();
            System.setProperty(OAuthFilter.KEYSTORE_PATH, keystorePath);
            System.setProperty(OAuthFilter.KEYSTORE_CREDENTIALS, passphrase);
            System.setProperty(OAuthFilter.KEY_IDS, "trellis,trellis-ec");

            final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(getClass().getResourceAsStream("/keystore.jks"), passphrase.toCharArray());

            final Key privateKey = ks.getKey("trellis", passphrase.toCharArray());
            final String token = Jwts.builder().setHeaderParam(JwsHeader.KEY_ID, "trellis")
                .setSubject(webid).signWith(privateKey, SignatureAlgorithm.RS256).compact();

            when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);

            final OAuthFilter filter = new OAuthFilter();
            filter.filter(mockContext);
            verify(mockContext).setSecurityContext(securityArgument.capture());
            assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        } finally {
            System.clearProperty(OAuthFilter.KEYSTORE_PATH);
            System.clearProperty(OAuthFilter.KEYSTORE_CREDENTIALS);
            System.clearProperty(OAuthFilter.KEY_IDS);
        }
    }
}
