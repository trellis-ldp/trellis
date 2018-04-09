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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class JwtAuthenticatorTest {

    @Test
    public void testAuthenticate() throws AuthenticationException {
         final String key = "c2VjcmV0";
         final String token = Jwts.builder().setSubject("https://people.apache.org/~acoburn/#i")
             .signWith(SignatureAlgorithm.HS512, key).compact();

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, true);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertTrue(result.isPresent());
        result.ifPresent(p -> {
            assertEquals("https://people.apache.org/~acoburn/#i", p.getName());
        });
    }

    @Test
    public void testAuthenticateNoSub() throws AuthenticationException {
         final String key = "c2VjcmV0";
         final String token = Jwts.builder().setIssuer("http://localhost")
             .signWith(SignatureAlgorithm.HS512, key).compact();

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, true);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAuthenticateSubIss() throws AuthenticationException {
         final String key = "c2VjcmV0";
         final String token = Jwts.builder().setSubject("acoburn").setIssuer("http://localhost")
             .signWith(SignatureAlgorithm.HS512, key).compact();

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, true);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertTrue(result.isPresent());
        result.ifPresent(p -> {
            assertEquals("http://localhost/acoburn", p.getName());
        });
    }

    @Test
    public void testAuthenticateSubNoWebIss() throws AuthenticationException {
         final String key = "c2VjcmV0";
         final String token = Jwts.builder().setSubject("acoburn").setIssuer("some org")
             .signWith(SignatureAlgorithm.HS512, key).compact();

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, true);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAuthenticateToken() throws AuthenticationException {
        final String key = "secret";
        final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJodHRwczovL" +
            "3Blb3BsZS5hcGFjaGUub3JnL35hY29idXJuLyNpIn0.Njgb_f5deb5hjr6UP-Q0GAWAz3ykvBW0-A0Sr5thdMo";

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, false);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertTrue(result.isPresent());
        result.ifPresent(p -> {
            assertEquals("https://people.apache.org/~acoburn/#i", p.getName());
        });
    }

    @Test
    public void testAuthenticateTokenIssSub() throws AuthenticationException {
        final String key = "secret";
        final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhY29idXJuIiwibmFtZSI6IkFhcm9" +
            "uIENvYnVybiIsImlzcyI6Imh0dHA6Ly9leGFtcGxlLm9yZy8ifQ.DPb_i9vfI5um2X_g_df2y1uFktThGdDBo-Q7AMqjaWc";

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, false);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertTrue(result.isPresent());
        result.ifPresent(p -> {
            assertEquals("http://example.org/acoburn", p.getName());
        });
    }

    @Test
    public void testAuthenticationTokenWebid() throws AuthenticationException {
        final String key = "secret";
        final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ3ZWJpZCI6Imh0dHBzOi8vcGVvcGxlLmFwYWNo" +
            "ZS5vcmcvfmFjb2J1cm4vI2kiLCJzdWIiOiJhY29idXJuIiwibmFtZSI6IkFhcm9uIENvYnVybiIsImlzcyI6Imh0dHA6Ly" +
            "9leGFtcGxlLm9yZy8ifQ.LqrqNUXIMJD-Qrw7b38c0o6HYaaX_8G3GzCANhPclhk";

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, false);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertTrue(result.isPresent());
        result.ifPresent(p -> {
            assertEquals("https://people.apache.org/~acoburn/#i", p.getName());
        });
    }

    @Test
    public void testAuthenticationTokenWebidBadKey() throws AuthenticationException {
        final String key = "incorrect";
        final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ3ZWJpZCI6Imh0dHBzOi8vYWNvYnVybi5wZW9w" +
            "bGUuYW1oZXJzdC5lZHUvIiwic3ViIjoiYWNvYnVybiIsIm5hbWUiOiJBYXJvbiBDb2J1cm4iLCJpc3MiOiJodHRwOi8vZX" +
            "hhbXBsZS5vcmcvIn0.X-7_VfEuLGzH5ZEqzpkHWp1bo3tMzBiyDcNUwwdLeqw";

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, false);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }

    @Test
    public void testAuthenticationNoPrincipal() throws AuthenticationException {
        final String key = "secret";
        final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhY29idXJuIiwibmFtZSI" +
            "6IkFhcm9uIENvYnVybiJ9.IMuzkEyDDHaLi8wps_W3F6wJkIVwocK4DFb8OFYaADA";

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, false);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }

    @Test
    public void testGarbledToken() throws AuthenticationException {
        final String key = "secret";
        final String token = "blahblah";

        final Authenticator<String, Principal> authenticator = new JwtAuthenticator(key, false);

        final Optional<Principal> result = authenticator.authenticate(token);
        assertFalse(result.isPresent());
    }
}
