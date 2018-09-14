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
import static org.trellisldp.app.TrellisUtils.getWebacCache;

import io.dropwizard.Application;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.setup.Environment;

import java.util.ArrayList;
import java.util.List;

import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.ServiceBundler;
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

    /** The configuration key controlling whether an application should initialize its own root resource. **/
    public static final String APPLICATION_SELF_INITIALIZE = "trellis.app.initialize.root";

    /**
     * Get the Trellis {@link ServiceBundler}. This object collects the various
     * Trellis services used in an application.
     * @return the ServiceBundler
     */
    protected abstract ServiceBundler getServiceBundler();

    /**
     * Get any additional components to register with Jersey.
     *
     * @implSpec By default, this returns an empty list.
     * @return any additional components.
     */
    protected List<Object> getComponents() {
        return emptyList();
    }

    /**
     * Get the LdpResource matcher.
     *
     * @param config the configuration
     * @param initialize true if the LdpResource object should be initialized; false otherwise
     * @return the LDP resource matcher
     */
    protected Object getLdpComponent(final T config, final Boolean initialize) {
        final LdpResource ldpResource = new LdpResource(getServiceBundler(), config.getBaseUrl());
        if (initialize) {
            ldpResource.initialize();
        }
        return ldpResource;
    }

    /**
     * Setup the trellis application.
     *
     * @apiNote This method is called at the very beginning of the {@link Application#run} method. It can be used
     *          to configure or register any of the Trellis-related services that an implementation instantiates.
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
        environment.jersey().register(getLdpComponent(config, ConfigurationProvider.getConfiguration()
                    .getOrDefault(APPLICATION_SELF_INITIALIZE, Boolean.class, true)));

        // Authentication
        final AgentAuthorizationFilter agentFilter
            = new AgentAuthorizationFilter(getServiceBundler().getAgentService());
        agentFilter.setAdminUsers(config.getAuth().getAdminUsers());

        // Filters
        environment.jersey().register(agentFilter);
        environment.jersey().register(new CacheControlFilter(config.getCache().getMaxAge(),
                    config.getCache().getMustRevalidate(), config.getCache().getNoCache()));

        // Authorization
        getWebacCache(config).ifPresent(cache -> {
            final AccessControlService webac = new WebACService(getServiceBundler().getResourceService(), cache);
            final WebAcFilter filter = new WebAcFilter(webac);
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
