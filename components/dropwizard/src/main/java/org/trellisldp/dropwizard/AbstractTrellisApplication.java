/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
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
package org.trellisldp.dropwizard;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.dropwizard.TrellisUtils.getAuthFilters;
import static org.trellisldp.dropwizard.TrellisUtils.getCorsConfiguration;
import static org.trellisldp.dropwizard.TrellisUtils.getWebacService;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.ArrayList;
import java.util.List;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.trellisldp.api.ResourceService;
import org.trellisldp.common.ServiceBundler;
import org.trellisldp.dropwizard.config.BasicAuthConfiguration;
import org.trellisldp.dropwizard.config.JwtAuthConfiguration;
import org.trellisldp.dropwizard.config.TrellisConfiguration;
import org.trellisldp.http.CacheControlFilter;
import org.trellisldp.http.TrellisHttpFilter;
import org.trellisldp.http.TrellisHttpResource;
import org.trellisldp.http.WebSubHeaderFilter;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.webac.WebAcFilter;
import org.trellisldp.webac.WebAcService;

/**
 * A base class for Dropwizard-based Trellis applications.
 */
public abstract class AbstractTrellisApplication<T extends TrellisConfiguration> extends Application<T> {

    private static final Logger LOGGER = getLogger(AbstractTrellisApplication.class);

    /** The configuration key controlling whether an application should initialize its own root resource. */
    public static final String CONFIG_DROPWIZARD_INITIALIZE_ROOT = "trellis.dropwizard.initialize-root";

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
     * Get the TrellisHttpResource matcher.
     *
     * @param config the configuration
     * @param initialize true if the TrellisHttpResource object should be initialized; false otherwise
     * @return the LDP resource matcher
     */
    protected Object getLdpComponent(final T config, final boolean initialize) {
        final TrellisHttpResource ldpResource = new TrellisHttpResource(getServiceBundler(),
                singletonMap("acl", Trellis.PreferAccessControl), config.getBaseUrl());
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
    public void initialize(final Bootstrap<T> bootstrap) {
        // Allow configuration property substitution from environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                    new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(final T config, final Environment environment) throws Exception {
        initialize(config, environment);

        getAuthFilters(config).forEach(environment.jersey()::register);

        // Resource matchers
        environment.jersey().register(getLdpComponent(config, getConfig()
                    .getOptionalValue(CONFIG_DROPWIZARD_INITIALIZE_ROOT, Boolean.class).orElse(Boolean.TRUE)));

        // Filters
        environment.jersey().register(new TrellisHttpFilter());
        final CacheControlFilter cacheFilter = new CacheControlFilter();
        cacheFilter.setMaxAge(config.getCache().getMaxAge());
        cacheFilter.setMustRevalidate(config.getCache().getMustRevalidate());
        cacheFilter.setNoCache(config.getCache().getNoCache());
        environment.jersey().register(cacheFilter);

        // Authorization
        ofNullable(getWebacService(config, getServiceBundler().getResourceService())).ifPresent(webac -> {
            final List<String> challengeTypes = new ArrayList<>();
            of(config.getAuth().getJwt()).filter(JwtAuthConfiguration::getEnabled).map(x -> "Bearer")
                .ifPresent(challengeTypes::add);
            of(config.getAuth().getBasic()).filter(BasicAuthConfiguration::getEnabled).map(x -> "Basic")
                .ifPresent(challengeTypes::add);

            final List<String> challenges = challengeTypes.stream().map(String::trim)
                .map(ch -> TrellisUtils.buildChallenge(ch, config.getAuth().getRealm(), config.getAuth().getScope()))
                .collect(toList());

            final WebAcFilter webacFilter = new WebAcFilter();
            webacFilter.setChallenges(challenges);
            webacFilter.setBaseUrl(config.getBaseUrl());
            environment.jersey().register(webacFilter);
            environment.jersey().register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(webac).to(WebAcService.class);
                    bind(getServiceBundler().getResourceService()).to(ResourceService.class);
                }
            });
        });

        // WebSub
        ofNullable(config.getHubUrl()).ifPresent(hub -> {
            final WebSubHeaderFilter webSubFilter = new WebSubHeaderFilter();
            webSubFilter.setHub(hub);
            environment.jersey().register(webSubFilter);
        });

        // CORS
        ofNullable(getCorsConfiguration(config)).ifPresent(cors ->
            environment.jersey().register(new CrossOriginResourceSharingFilter(cors.getAllowOrigin(),
                        cors.getAllowMethods(), cors.getAllowHeaders(), cors.getExposeHeaders(),
                        cors.getAllowCredentials(), cors.getMaxAge())));

        // Additional components
        getComponents().forEach(environment.jersey()::register);
    }
}
