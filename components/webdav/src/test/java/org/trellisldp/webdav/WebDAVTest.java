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

import java.io.IOException;
import java.net.ServerSocket;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.http.TrellisHttpResource;
import org.trellisldp.http.WebAcFilter;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebDAVTest extends AbstractWebDAVTest {

    @Override
    public Application configure() {

        initMocks(this);

        try {
            // Use a random free port for testing
            final String port = Integer.toString(new ServerSocket(0).getLocalPort());
            forceSet(TestProperties.CONTAINER_PORT, port);

            final String baseUri = "http://localhost:" + port + "/";
            final ResourceConfig config = new ResourceConfig();

            System.setProperty(CONFIG_HTTP_BASE_URL, baseUri);
            config.register(new DebugExceptionMapper());
            config.register(new TrellisWebDAVRequestFilter(mockBundler));
            config.register(new TrellisWebDAVResponseFilter());
            config.register(new TrellisWebDAV(mockBundler));
            config.register(new TrellisWebDAVAuthzFilter(accessControlService));
            config.register(new TrellisHttpResource(mockBundler));
            config.register(new WebAcFilter(accessControlService));
            return config;
        } catch (final IOException ex) {
            throw new RuntimeTrellisException("Could not acquire free port!", ex);
        } finally {
            System.clearProperty(CONFIG_HTTP_BASE_URL);
        }
    }
}

