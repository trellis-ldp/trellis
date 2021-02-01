/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
package org.trellisldp.http;

import static org.mockito.MockitoAnnotations.openMocks;
import static org.trellisldp.common.HttpConstants.CONFIG_HTTP_BASE_URL;

import javax.ws.rs.core.Application;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.trellisldp.common.ServiceBundler;

/**
 * @author acoburn
 */
class TrellisHttpResourceAdminTest extends AbstractTrellisHttpResourceTest {

    @Override
    String getBaseUrl() {
        return getBaseUri().toString();
    }

    @Override
    protected Application configure() {

        openMocks(this);

        final String baseUri = getBaseUri().toString();
        System.setProperty(WebSubHeaderFilter.CONFIG_HTTP_WEB_SUB_HUB, HUB);
        System.setProperty(CONFIG_HTTP_BASE_URL, baseUri);

        final ResourceConfig config = new ResourceConfig();
        config.register(new TrellisHttpResource());
        config.register(new TestAuthenticationFilter("testUser", ""));
        config.register(new CacheControlFilter());
        config.register(new WebSubHeaderFilter());
        config.register(new TrellisHttpFilter());
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(mockBundler).to(ServiceBundler.class);
            }
        });
        return config;
    }
}
