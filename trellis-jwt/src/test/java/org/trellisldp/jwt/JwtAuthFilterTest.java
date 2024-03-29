/*
 * Copyright (c) Aaron Coburn and individual contributors
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
package org.trellisldp.jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author acoburn
 */
@ExtendWith(WeldJunit5Extension.class)
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.from(JwtAuthFilter.class, PrincipalProducer.class)
                    .activate(RequestScoped.class).build();

    @Inject
    private PrincipalProducer producer;

    @Inject
    private ContainerRequestFilter filter;

    @Captor
    private ArgumentCaptor<SecurityContext> securityArgument;

    @Test
    void testJwtAuthFilter() {
        final ContainerRequestContext mockContext = mock(ContainerRequestContext.class);
        assertNotNull(filter);
        assertNotNull(producer);

        final String iss = "https://example.com/idp/";
        final String sub = "acoburn";
        final JwtClaims claims = new JwtClaims();
        claims.setSubject(sub);
        claims.setIssuer(iss);

        producer.setJsonWebToken(new DefaultJWTCallerPrincipal(claims));
        assertDoesNotThrow(() -> filter.filter(mockContext));
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals(iss + sub, securityArgument.getValue().getUserPrincipal().getName());
    }

    @Test
    void testJwtAuthWebidFilter() {
        final ContainerRequestContext mockContext = mock(ContainerRequestContext.class);
        assertNotNull(filter);
        assertNotNull(producer);

        final String webid = "https://people.apache.org/~acoburn/#i";
        final String iss = "https://example.com/idp/";
        final String sub = "acoburn";
        final JwtClaims claims = new JwtClaims();
        claims.setSubject(sub);
        claims.setIssuer(iss);
        claims.setClaim("webid", webid);

        producer.setJsonWebToken(new DefaultJWTCallerPrincipal(claims));
        assertDoesNotThrow(() -> filter.filter(mockContext));
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName());
    }

    @Test
    void testJwtAuthNoTokenFilter() {
        final ContainerRequestContext mockContext = mock(ContainerRequestContext.class);
        assertNotNull(filter);
        assertNotNull(producer);

        assertDoesNotThrow(() -> filter.filter(mockContext));
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertNull(securityArgument.getValue().getUserPrincipal().getName());
    }
}
