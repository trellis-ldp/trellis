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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author acoburn
 */
class TrellisHttpResourceBaseUrlTest extends AbstractTrellisHttpResourceTest {

    // The absence of trailing slash is intentional here; it tests configuration overrides
    // that lack a trailing slash.
    private static final String URL = "http://example.com";

    @Override
    String getBaseUrl() {
        return URL + "/";
    }

    @Override
    protected Application configure() {

        // Junit runner doesn't seem to work very well with JerseyTest
        initMocks(this);

        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);

        final ResourceConfig config = new ResourceConfig();
        config.register(new TrellisHttpResource(mockBundler, URL));
        config.register(new TestAuthenticationFilter("testUser", "group"));
        config.register(new CacheControlFilter());
        config.register(new WebSubHeaderFilter(HUB));
        config.register(new TrellisHttpFilter());
        config.register(new CrossOriginResourceSharingFilter(singletonList(origin), asList("PATCH", "POST", "PUT"),
                        asList("Link", "Content-Type", "Accept-Datetime", "Accept"),
                        asList("Link", "Content-Type", "Memento-Datetime"), true, 100));
        return config;
    }
}
