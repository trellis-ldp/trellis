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

import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_BASE_URL;

import javax.ws.rs.core.Application;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.http.TrellisHttpResource;
import org.trellisldp.http.core.ServiceBundler;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebDAVTest extends AbstractWebDAVTest {

    @Override
    public Application configure() {

        initMocks(this);

        final ResourceConfig config = new ResourceConfig();

        try {
            System.setProperty(CONFIG_HTTP_BASE_URL, getBaseUri().toString());
            config.register(new DebugExceptionMapper());
            config.register(new TrellisWebDAVRequestFilter());
            config.register(new TrellisWebDAVResponseFilter());
            config.register(new TrellisWebDAV());
            config.register(new TrellisHttpResource());
            config.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(mockBundler).to(ServiceBundler.class);
                }
            });
            return config;
        } finally {
            System.clearProperty(CONFIG_HTTP_BASE_URL);
        }
    }
}

