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

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.app.TrellisUtils.getAuthFilters;
import static org.trellisldp.app.TrellisUtils.getCorsConfiguration;
import static org.trellisldp.app.TrellisUtils.getWebacConfiguration;

import io.dropwizard.Application;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.setup.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopAuditService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.app.config.BasicAuthConfiguration;
import org.trellisldp.app.config.JwtAuthConfiguration;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.http.AgentAuthorizationFilter;
import org.trellisldp.http.CacheControlFilter;
import org.trellisldp.http.CrossOriginResourceSharingFilter;
import org.trellisldp.http.LdpResource;
import org.trellisldp.http.WebAcFilter;
import org.trellisldp.http.WebSubHeaderFilter;
import org.trellisldp.webac.WebACService;

/**
 * A base class for Dropwizard-based Trellis applications.
 */
public abstract class AbstractTrellisApplication<T extends TrellisConfiguration> extends Application<T> {

    private static final Logger LOGGER = getLogger(AbstractTrellisApplication.class);

    private final AgentService agentService = new SimpleAgentService();

    /**
     * Get the {@link ResourceService}. There should be one and only one
     * {@code ResourceService} in a deployed Trellis instance. This method should
     * return the same {@code ResourceService} every time it is called.
     * 
     * @return the {@code ResourceService},
     */
    protected abstract ResourceService getResourceService();

    /**
     * Get the {@link IOService}. There should be one and only one
     * {@code IOService} in a deployed Trellis instance. This method should
     * return the same {@code IOService} every time it is called.
     * @return the {@code IOService}
     */
    protected abstract IOService getIOService();

    /**
     * Get the {@link BinaryService}. There should be one and only one
     * {@code BinaryService} in a deployed Trellis instance. This method should
     * return the same {@code BinaryService} every time it is called.
     * @return the {@code BinaryService}
     */
    protected abstract BinaryService getBinaryService();

    /**
     * Get an optional {@link MementoService}. There should be at most one
     * {@code MementoService} in a deployed Trellis instance. If there is one, this
     * method should return the same {@code MementosService} every time it is called.
     *
     * @return an {@code MementoService}, if one exists
     */
    protected abstract Optional<MementoService> getMementoService();

    /**
     * Get an optional {@link AuditService}. There should be at most one
     * {@code AuditService} in a deployed Trellis instance. If there is one, this
     * method should return the same {@code AuditService} every time it is called.
     * 
     * @return an {@code AuditService}, if one exists
     */
    protected abstract Optional<AuditService> getAuditService();

    /**
     * Get any additional components to register with Jersey.
     *
     * <p>Note: by default, this returns an empty list.
     *
     * @return any additional components.
     */
    protected List<Object> getComponents() {
        return emptyList();
    }

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
                    agentService, getMementoService().orElseGet(NoopMementoService::new),
                    getAuditService().orElseGet(NoopAuditService::new), config.getBaseUrl()));

        // Authentication
        final AgentAuthorizationFilter agentFilter = new AgentAuthorizationFilter(agentService);
        agentFilter.setAdminUsers(config.getAuth().getAdminUsers());

        // Filters
        environment.jersey().register(agentFilter);
        environment.jersey().register(new CacheControlFilter(config.getCache().getMaxAge(),
                    config.getCache().getMustRevalidate(), config.getCache().getNoCache()));

        // Authorization
        getWebacConfiguration(config).ifPresent(webacCache -> {
                final WebAcFilter filter = new WebAcFilter(new WebACService(getResourceService(), webacCache));
                final List<String> challenges = new ArrayList<>();
                of(config.getAuth().getJwt()).filter(JwtAuthConfiguration::getEnabled)
                    .map(c -> "Bearer realm=\"" + c.getRealm() + "\"").ifPresent(challenges::add);
                of(config.getAuth().getBasic()).filter(BasicAuthConfiguration::getEnabled)
                    .map(c -> "Basic realm=\"" + c.getRealm() + "\"").ifPresent(challenges::add);
                filter.setChallenges(challenges);
                environment.jersey().register(filter);
        });

        // WebSub
        ofNullable(config.getHubUrl()).ifPresent(hub -> environment.jersey().register(new WebSubHeaderFilter(hub)));

        // CORS
        getCorsConfiguration(config).ifPresent(cors -> environment.jersey().register(
                new CrossOriginResourceSharingFilter(cors.getAllowOrigin(), cors.getAllowMethods(),
                    cors.getAllowHeaders(), cors.getExposeHeaders(), cors.getAllowCredentials(), cors.getMaxAge())));

        // Additional components
        getComponents().forEach(environment.jersey()::register);
    }
}
