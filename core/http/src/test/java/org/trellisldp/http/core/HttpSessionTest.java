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
package org.trellisldp.http.core;

import static java.time.Instant.now;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.TrellisUtils.TRELLIS_SESSION_PREFIX;

import java.security.Principal;
import java.time.Instant;

import javax.ws.rs.core.SecurityContext;

import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
class HttpSessionTest {

    private static final RDF rdf = RDFFactory.getInstance();

    @Test
    void testHttpSession() {
        final Instant time = now();
        final Session session = new HttpSession();
        assertEquals(Trellis.AnonymousAgent, session.getAgent(), "Incorrect agent in default session!");
        assertFalse(session.getDelegatedBy().isPresent(), "Unexpected delegatedBy property!");
        assertTrue(session.getIdentifier().getIRIString().startsWith(TRELLIS_SESSION_PREFIX), "ID has wrong prefix!");
        final Session session2 = new HttpSession();
        assertNotEquals(session.getIdentifier(), session2.getIdentifier(), "Session identifiers aren't unique!");
        assertFalse(session.getCreated().isBefore(time), "Session date precedes its creation!");
        assertFalse(session.getCreated().isAfter(session2.getCreated()), "Session date is out of order!");
    }

    @Test
    void testAdminHttpSession() {
        final String agent = "http://example.com/agent";
        final SecurityContext mockSecurityContext = mock(SecurityContext.class);
        final Principal mockPrincipal = mock(Principal.class);

        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(agent);
        when(mockSecurityContext.isUserInRole(eq(HttpSession.ADMIN_ROLE))).thenReturn(true);

        final Session session = HttpSession.from(mockSecurityContext);
        assertEquals(Trellis.AdministratorAgent, session.getAgent(), "Incorrect agent in admin session");
        assertEquals(of(rdf.createIRI(agent)), session.getDelegatedBy(), "Incorrect delegate");
    }

    @Test
    void testNormalHttpSession() {
        final String agent = "http://example.com/agent";
        final SecurityContext mockSecurityContext = mock(SecurityContext.class);
        final Principal mockPrincipal = mock(Principal.class);

        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(agent);
        when(mockSecurityContext.isUserInRole(eq(HttpSession.ADMIN_ROLE))).thenReturn(false);

        final Session session = HttpSession.from(mockSecurityContext);
        assertEquals(rdf.createIRI(agent), session.getAgent(), "Incorrect agent in admin session");
        assertFalse(session.getDelegatedBy().isPresent(), "Incorrect delegate");
    }

    @Test
    void testNullPrincipalNameHttpSession() {
        final SecurityContext mockSecurityContext = mock(SecurityContext.class);
        final Principal mockPrincipal = mock(Principal.class);

        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockSecurityContext.isUserInRole(eq(HttpSession.ADMIN_ROLE))).thenReturn(false);

        final Session session = HttpSession.from(mockSecurityContext);
        assertEquals(Trellis.AnonymousAgent, session.getAgent(), "Incorrect agent");
        assertFalse(session.getDelegatedBy().isPresent(), "Incorrect delegate");
    }
}
