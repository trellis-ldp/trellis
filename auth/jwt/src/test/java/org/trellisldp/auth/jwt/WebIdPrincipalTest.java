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
package org.trellisldp.auth.jwt;

import static org.junit.jupiter.api.Assertions.*;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;

class WebIdPrincipalTest {

    @Test
    void testBasicPrincipal() {
        final String iss = "https://example.com/idp/";
        final String sub = "acoburn";
        final JwtClaims claims = new JwtClaims();
        claims.setSubject(sub);
        claims.setIssuer(iss);
        final JsonWebToken principal = new WebIdPrincipal(new DefaultJWTCallerPrincipal(claims));
        assertTrue(principal.getClaimNames().contains("sub"));
        assertEquals(iss + sub, principal.getName());
        assertEquals(iss, principal.getIssuer());
        assertEquals(iss, principal.getClaim("iss"));
    }

    @Test
    void testIssNoSlashPrincipal() {
        final String iss = "http://idp.example.com";
        final String sub = "acoburn";
        final JwtClaims claims = new JwtClaims();
        claims.setSubject(sub);
        claims.setIssuer(iss);
        final JsonWebToken principal = new WebIdPrincipal(new DefaultJWTCallerPrincipal(claims));
        assertTrue(principal.getClaimNames().contains("sub"));
        assertEquals(iss + "/" + sub, principal.getName());
        assertEquals(iss, principal.getIssuer());
        assertEquals(iss, principal.getClaim("iss"));
    }

    @Test
    void testWebIdPrincipal() {
        final String iss = "https://example.com/idp/";
        final String sub = "acoburn";
        final String webid = "https://example.com/profile#me";
        final JwtClaims claims = new JwtClaims();
        claims.setSubject(sub);
        claims.setIssuer(iss);
        claims.setClaim("webid", webid);
        final JsonWebToken principal = new WebIdPrincipal(new DefaultJWTCallerPrincipal(claims));
        assertEquals(webid, principal.getName());
        assertEquals(iss, principal.getIssuer());
        assertEquals(iss, principal.getClaim("iss"));
        assertEquals(sub, principal.getClaim("sub"));
    }

    @Test
    void testWebIdSubPrincipal() {
        final String iss = "https://example.com/idp/";
        final String webid = "https://example.com/profile#me";
        final JwtClaims claims = new JwtClaims();
        claims.setSubject(webid);
        claims.setIssuer(iss);
        final JsonWebToken principal = new WebIdPrincipal(new DefaultJWTCallerPrincipal(claims));
        assertEquals(webid, principal.getName());
        assertEquals(iss, principal.getIssuer());
        assertEquals(iss, principal.getClaim("iss"));
    }

    @Test
    void testNoIssuerPrincipal() {
        final String sub = "acoburn";
        final JwtClaims claims = new JwtClaims();
        claims.setSubject(sub);
        final JsonWebToken principal = new WebIdPrincipal(new DefaultJWTCallerPrincipal(claims));
        assertNull(principal.getName());
    }

    @Test
    void testNoSubPrincipal() {
        final String iss = "https://example.com/idp/";
        final JwtClaims claims = new JwtClaims();
        claims.setIssuer(iss);
        final JsonWebToken principal = new WebIdPrincipal(new DefaultJWTCallerPrincipal(claims));
        assertNull(principal.getName());
    }
}
