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
package org.trellisldp.webdav;

import static org.mockito.MockitoAnnotations.openMocks;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.Optional;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.common.ServiceBundler;
import org.trellisldp.http.TrellisHttpResource;
import org.trellisldp.http.WebApplicationExceptionMapper;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebDAVNoBaseUrlTest extends AbstractWebDAVTest {

    @PreMatching
    @Priority(500)
    static class TestAuthnFilter implements ContainerRequestFilter {
        private final String principal;
        private final String userRole;

        TestAuthnFilter(final String principal, final String role) {
            this.principal = principal;
            this.userRole = role;
        }

        @Override
        public void filter(final ContainerRequestContext requestContext) {
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> principal;
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

        openMocks(this);

        final ResourceConfig config = new ResourceConfig();
        final TrellisWebDAV dav = new TrellisWebDAV();
        dav.userBaseUrl = Optional.empty();
        dav.extensionConfig = Optional.empty();
        dav.init();

        config.register(dav);
        config.register(new WebApplicationExceptionMapper());
        config.register(new TestAuthnFilter("testUser", ""));
        config.register(new TrellisWebDAVRequestFilter());
        config.register(new TrellisWebDAVResponseFilter());
        config.register(new TrellisHttpResource());
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(mockBundler).to(ServiceBundler.class);
            }
        });
        return config;
    }
}

