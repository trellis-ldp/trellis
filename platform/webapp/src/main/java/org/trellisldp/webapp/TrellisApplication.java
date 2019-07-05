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
package org.trellisldp.webapp;

import static java.util.Arrays.asList;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_BASE_URL;

import org.glassfish.jersey.server.ResourceConfig;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.auth.basic.BasicAuthFilter;
import org.trellisldp.auth.oauth.OAuthFilter;
import org.trellisldp.http.AgentAuthorizationFilter;
import org.trellisldp.http.CacheControlFilter;
import org.trellisldp.http.CrossOriginResourceSharingFilter;
import org.trellisldp.http.TrellisHttpFilter;
import org.trellisldp.http.TrellisHttpResource;
import org.trellisldp.http.WebSubHeaderFilter;
import org.trellisldp.webac.WebAcFilter;
import org.trellisldp.webac.WebAcService;

/**
 * A Trellis application.
 */
public class TrellisApplication extends ResourceConfig {

    /**
     * Create a Trellis application.
     */
    public TrellisApplication() {
        super();

        final String baseUrl = getConfig().getOptionalValue(CONFIG_HTTP_BASE_URL, String.class).orElse(null);
        final ServiceBundler serviceBundler = new WebappServiceBundler();
        final TrellisHttpResource ldpResource = new TrellisHttpResource(serviceBundler, baseUrl);
        ldpResource.initialize();

        register(ldpResource);
        register(new TrellisHttpFilter());
        register(new CrossOriginResourceSharingFilter());
        register(new CacheControlFilter());
        register(new WebSubHeaderFilter());
        register(new AgentAuthorizationFilter(serviceBundler.getAgentService()));
        register(new OAuthFilter());
        register(new BasicAuthFilter());
        register(new WebAcFilter(new WebAcService(serviceBundler.getResourceService()), asList("Basic", "Bearer"),
                        "trellis", baseUrl));

        AppUtils.getCacheControlFilter().ifPresent(this::register);
        AppUtils.getCORSFilter().ifPresent(this::register);
    }
}
