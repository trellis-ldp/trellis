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
package org.trellisldp.webdav;

import static java.util.Collections.singleton;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.security.Principal;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.agent.DefaultAgentService;
import org.trellisldp.http.AgentAuthorizationFilter;
import org.trellisldp.http.TrellisHttpResource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebDAVNoBaseUrlTest extends AbstractWebDAVTest {

    @PreMatching
    @Priority(500)
    private static class TestAuthnFilter implements ContainerRequestFilter {
        private final String principal;
        private final String userRole;

        public TestAuthnFilter(final String principal, final String role) {
            this.principal = principal;
            this.userRole = role;
        }

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return new Principal() {
                        @Override
                        public String getName() {
                            return principal;
                        }
                    };
                }

                @Override
                public boolean isSecure() {
                    return false;
                }

                @Override
                public boolean isUserInRole(final String role) {
                    return userRole.equals(role);
                }

                @Override
                public String getAuthenticationScheme() {
                    return "BASIC";
                }
            });
        }
    }

    @Override
    public Application configure() {

        initMocks(this);

        final ResourceConfig config = new ResourceConfig();

        config.register(new DebugExceptionMapper());
        config.register(new TestAuthnFilter("testUser", ""));
        config.register(new TrellisWebDAVRequestFilter(mockBundler));
        config.register(new TrellisWebDAVResponseFilter());
        config.register(new TrellisWebDAV(mockBundler));
        config.register(new TrellisHttpResource(mockBundler));
        config.register(new AgentAuthorizationFilter(new DefaultAgentService(), singleton("testUser")));
        return config;
    }
}

