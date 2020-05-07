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
package org.trellisldp.auth.basic;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.SecurityContext.BASIC_AUTH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.Base64;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

class BasicAuthFilterTest {

    @Mock
    private ContainerRequestContext mockContext;

    @Mock
    private SecurityContext mockSecurityContext;

    @Captor
    private ArgumentCaptor<SecurityContext> securityArgument;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(mockContext.getSecurityContext()).thenReturn(mockSecurityContext);
    }

    @Test
    void testCredentials() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        final String webid = "https://people.apache.org/~acoburn/#i";
        final String token = encodeCredentials("acoburn", "secret");
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("BASIC " + token);
        filter.filter(mockContext);
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(BASIC_AUTH, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
        assertFalse(securityArgument.getValue().isUserInRole("admin"), "Unexpectedly in user role!");
    }

    @Test
    void testAdminCredentials() {
        final String webid = "https://people.apache.org/~acoburn/#i";
        final BasicAuthFilter filter = new BasicAuthFilter(new File(getAuthFile()), "trellis", singleton(webid));
        final String token = encodeCredentials("acoburn", "secret");
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("BASIC " + token);
        filter.filter(mockContext);
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(BASIC_AUTH, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
        assertTrue(securityArgument.getValue().isUserInRole("admin"), "Not in user role!");
    }

    @Test
    void testNoHeader() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn(null);
        filter.filter(mockContext);
        verify(mockContext, never()).setSecurityContext(securityArgument.capture());
    }

    @Test
    void testOtherCredentials() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        when(mockContext.getHeaderString(AUTHORIZATION))
            .thenReturn("Basic " + encodeCredentials("user", "password"));
        filter.filter(mockContext);
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals("https://madison.example.com/profile/#me",
                securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(BASIC_AUTH, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
    }

    @Test
    void testSecureCredentials() {
        when(mockSecurityContext.isSecure()).thenReturn(true);
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        final String webid = "https://people.apache.org/~acoburn/#i";
        final String token = encodeCredentials("acoburn", "secret");
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("BASIC " + token);
        filter.filter(mockContext);
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals(webid, securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(BASIC_AUTH, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertTrue(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
    }

    @Test
    void testNoSecurityContext() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        when(mockContext.getSecurityContext()).thenReturn(null);
        when(mockContext.getHeaderString(AUTHORIZATION))
            .thenReturn("Basic " + encodeCredentials("user", "password"));
        filter.filter(mockContext);
        verify(mockContext).setSecurityContext(securityArgument.capture());
        assertEquals("https://madison.example.com/profile/#me",
                securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
        assertEquals(BASIC_AUTH, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
        assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
        assertFalse(securityArgument.getValue().isUserInRole("some role"), "Unexpectedly in user role!");
    }

    @Test
    void testCredentialsViaConfiguration() {
        try {
            System.setProperty(BasicAuthFilter.CONFIG_AUTH_BASIC_CREDENTIALS, getAuthFile());
            final BasicAuthFilter filter = new BasicAuthFilter();
            when(mockContext.getHeaderString(AUTHORIZATION))
                .thenReturn("Basic " + encodeCredentials("user2", "password2"));
            filter.filter(mockContext);
            verify(mockContext).setSecurityContext(securityArgument.capture());
            assertEquals("https://pat.example.com/profile/#me",
                    securityArgument.getValue().getUserPrincipal().getName(), "Unexpected agent IRI!");
            assertEquals(BASIC_AUTH, securityArgument.getValue().getAuthenticationScheme(), "Unexpected scheme!");
            assertFalse(securityArgument.getValue().isSecure(), "Unexpected secure flag!");
            assertFalse(securityArgument.getValue().isUserInRole("some other role"), "Unexpectedly in user role!");
        } finally {
            System.clearProperty(BasicAuthFilter.CONFIG_AUTH_BASIC_CREDENTIALS);
        }
    }

    @Test
    void testNoCredentials() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        final String token = encodeCredentials("acoburn", "secret");
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Bearer " + token);
        filter.filter(mockContext);
        verify(mockContext, never()).setSecurityContext(any());
    }

    @Test
    void testBadCredentials() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        final String token = encodeCredentials("acoburn", "wrong");
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Basic " + token);
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    void testBadCredentialsFile() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile() + ".non-existent");
        final String token = encodeCredentials("acoburn", "secret");
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Basic " + token);
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    void testNoToken() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("BASIC");
        filter.filter(mockContext);
        verify(mockContext, never()).setSecurityContext(any());
    }

    @Test
    void testBadToken() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        final String token = "blahblah";
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Basic " + token);
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    void testTokenWithBadChars() {
        final BasicAuthFilter filter = new BasicAuthFilter(getAuthFile());
        final String token = "&=!*#$";
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Basic " + token);
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    void testUnreadableFile() {
        final File file = new File(getAuthFile(), "nonexistent");
        final BasicAuthFilter filter = new BasicAuthFilter(file);
        final String token = encodeCredentials("acoburn", "secret");
        when(mockContext.getHeaderString(AUTHORIZATION)).thenReturn("Basic " + token);
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }

    @Test
    void testCredentialsObject() {
        final Credentials credentials = new Credentials("acoburn", "secret");
        assertFalse(credentials.toString().contains("secret"));
        assertTrue(credentials.toString().contains("acoburn"));
    }

    private String encodeCredentials(final String username, final String password) {
        final String combined = username + ":" + password;
        return new String(Base64.getEncoder().encode(combined.getBytes(UTF_8)), UTF_8);
    }

    private String getAuthFile() {
        final String prefix = "file:";
        return getClass().getResource("/users.auth").toString().substring(prefix.length());
    }
}
