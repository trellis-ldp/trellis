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
package org.trellisldp.app;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.app.TrellisUtils.getAuthFilters;
import static org.trellisldp.app.TrellisUtils.getCorsConfiguration;
import static org.trellisldp.app.TrellisUtils.getWebacConfiguration;

import io.dropwizard.Application;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.setup.Environment;

import java.util.Optional;

import org.slf4j.Logger;
import org.trellisldp.agent.SimpleAgent;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NoopAuditService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.http.AgentAuthorizationFilter;
import org.trellisldp.http.CacheControlFilter;
import org.trellisldp.http.CrossOriginResourceSharingFilter;
import org.trellisldp.http.LdpResource;
import org.trellisldp.http.MultipartUploader;
import org.trellisldp.http.WebAcFilter;
import org.trellisldp.webac.WebACService;

/**
 * A base class for Dropwizard-based Trellis applications.
 */
public abstract class AbstractTrellisApplication<T extends TrellisConfiguration> extends Application<T> {

    private static final Logger LOGGER = getLogger(AbstractTrellisApplication.class);

    /**
     * Get the resource service.
     * @return a resource service
     */
    protected abstract ResourceService getResourceService();

    /**
     * Get the IO Service.
     * @return an IO service
     */
    protected abstract IOService getIOService();

    /**
     * Get the binary service.
     * @return a binary service
     */
    protected abstract BinaryService getBinaryService();

    /**
     * Get a multipart uploader service.
     * @return a multipart uploader service, if one exists
     */
    protected abstract Optional<BinaryService.MultipartCapable> getMultipartUploadService();

    /**
     * Get the audit service.
     * @return an audit service, if one exists
     */
    protected abstract Optional<AuditService> getAuditService();

    /**
     * Setup the trellis application.
     *
     * <p>This method is called at the very beginning of the {@link Application#run} method. It can be used
     * to configure or register any of the Trellis-related services that an implementation instantiates.
     *
     * @param config the configuration
     * @param environment the environment
     */
    protected void initialize(final T config, final Environment environment) {
        LOGGER.debug("Initializing Trellis application with {}", config.getClass());
    }

    @Override
    public String getName() {
        return "Trellis LDP";
    }

    @Override
    public void run(final T config, final Environment environment) throws Exception {
        initialize(config, environment);

        getAuthFilters(config).ifPresent(filters -> environment.jersey().register(new ChainedAuthFilter<>(filters)));

        // Resource matchers
        environment.jersey().register(new LdpResource(getResourceService(), getIOService(), getBinaryService(),
                    getAuditService().orElseGet(NoopAuditService::new), config.getBaseUrl()));
        getMultipartUploadService().ifPresent(uploader -> environment.jersey()
                .register(new MultipartUploader(getResourceService(), uploader, config.getBaseUrl())));

        // Filters
        environment.jersey().register(new AgentAuthorizationFilter(new SimpleAgent()));
        environment.jersey().register(new CacheControlFilter(config.getCacheMaxAge()));

        // Authorization
        getWebacConfiguration(config).ifPresent(webacCache -> {
                final WebAcFilter filter = new WebAcFilter(new WebACService(getResourceService(), webacCache));
                filter.setChallenges(asList("Authorization"));
                environment.jersey().register(filter);
        });

        // CORS
        getCorsConfiguration(config).ifPresent(cors -> environment.jersey().register(
                new CrossOriginResourceSharingFilter(cors.getAllowOrigin(), cors.getAllowMethods(),
                    cors.getAllowHeaders(), cors.getExposeHeaders(), cors.getAllowCredentials(), cors.getMaxAge())));
    }
}
