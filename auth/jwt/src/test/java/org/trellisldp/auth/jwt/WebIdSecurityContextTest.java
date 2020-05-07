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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

import javax.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;

class WebIdSecurityContextTest {

    @Test
    void testNullPrincipal() {
        final SecurityContext ctx = new WebIdSecurityContext(null, null, emptySet());
        assertNull(ctx.getUserPrincipal());
    }

    @Test
    void testMockedDelegate() {
        final SecurityContext mockDelegate = mock(SecurityContext.class);
        when(mockDelegate.isSecure()).thenReturn(true);
        when(mockDelegate.getAuthenticationScheme()).thenReturn("Bearer");

        final SecurityContext ctx = new WebIdSecurityContext(mockDelegate, null, emptySet());
        assertTrue(ctx.isSecure());
        assertEquals("Bearer", ctx.getAuthenticationScheme());
    }

    @Test
    void testAdminRoles() {
        final SecurityContext mockDelegate = mock(SecurityContext.class);
        final String iss = "https://example.com/idp/";
        final String sub = "acoburn";
        final JwtClaims claims = new JwtClaims();
        claims.setSubject(sub);
        claims.setIssuer(iss);
        final JsonWebToken principal = new DefaultJWTCallerPrincipal(claims);

        final SecurityContext ctx = new WebIdSecurityContext(mockDelegate, principal, singleton(iss + sub));
        assertTrue(ctx.isUserInRole(WebIdSecurityContext.ADMIN_ROLE));
        assertFalse(ctx.isUserInRole("other-role"));
    }

}
