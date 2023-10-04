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

import jakarta.ws.rs.core.Application;

import java.util.Optional;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.common.ServiceBundler;
import org.trellisldp.http.TrellisHttpResource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebDAVTest extends AbstractWebDAVTest {

    @Override
    protected Application configure() {

        openMocks(this);

        final ResourceConfig config = new ResourceConfig();
        final TrellisWebDAV dav = new TrellisWebDAV();
        dav.userBaseUrl = Optional.of(getBaseUri().toString());
        dav.extensionConfig = Optional.empty();
        dav.init();

        config.register(dav);
        config.register(new DebugExceptionMapper());
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

