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
package org.trellisldp.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.http.core.HttpConstants.SESSION_PROPERTY;

import java.security.Principal;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AgentAuthorizationFilterTest {

    @Mock
    private AgentService mockAgentService;

    @Mock
    private ContainerRequestContext mockContext;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private Principal mockPrincipal;

    @Captor
    private ArgumentCaptor<Session> sessionArgument;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockAgentService.asAgent(any())).thenReturn(Trellis.AnonymousAgent);
        when(mockContext.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
    }

    @Test
    public void testFilterMissingAgent() throws Exception {
        when(mockPrincipal.getName()).thenReturn("");
        final AgentAuthorizationFilter filter = new AgentAuthorizationFilter(mockAgentService);
        filter.filter(mockContext);
        verify(mockContext).setProperty(eq(SESSION_PROPERTY), sessionArgument.capture());
        assertEquals(Trellis.AnonymousAgent, sessionArgument.getValue().getAgent(), "Unexpected agent IRI!");
    }

    @Test
    public void testFilterAdminAgent() throws Exception {
        when(mockPrincipal.getName()).thenReturn("admin");
        when(mockAgentService.asAgent(any())).thenReturn(Trellis.AdministratorAgent);
        final AgentAuthorizationFilter filter = new AgentAuthorizationFilter(mockAgentService);
        filter.filter(mockContext);
        verify(mockContext).setProperty(eq(SESSION_PROPERTY), sessionArgument.capture());
        assertEquals(Trellis.AnonymousAgent, sessionArgument.getValue().getAgent(), "Unexpected agent IRI!");
    }

    @Test
    public void testNoParamCtor() throws Exception {
        when(mockPrincipal.getName()).thenReturn("https://example.com/admin");
        final AgentAuthorizationFilter filter = new AgentAuthorizationFilter();
        filter.filter(mockContext);
        verify(mockContext).setProperty(eq(SESSION_PROPERTY), sessionArgument.capture());
        assertEquals(Trellis.AdministratorAgent, sessionArgument.getValue().getAgent(), "Unexpected agent IRI!");
    }
}
