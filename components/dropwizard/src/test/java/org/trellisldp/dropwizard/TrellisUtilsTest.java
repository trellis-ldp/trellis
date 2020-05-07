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

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;

import java.io.File;
import java.util.List;

import javax.ws.rs.container.ContainerRequestFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.auth.oauth.FederatedJwtAuthenticator;
import org.trellisldp.auth.oauth.JwksAuthenticator;
import org.trellisldp.auth.oauth.JwtAuthenticator;
import org.trellisldp.auth.oauth.NullAuthenticator;
import org.trellisldp.auth.oauth.WebIdOIDCAuthenticator;
import org.trellisldp.dropwizard.config.TrellisConfiguration;
import org.trellisldp.dropwizard.config.WebIdOIDCConfiguration;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
class TrellisUtilsTest {

    @Mock
    private Environment mockEnv;

    @Mock
    private LifecycleEnvironment mockLifecycle;

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private Resource mockResource;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(mockEnv.lifecycle()).thenReturn(mockLifecycle);
    }

    @Test
    void testGetCORSConfig() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));


        assertNotNull(TrellisUtils.getCorsConfiguration(config), "CORS configuration is missing!");

        config.getCors().setEnabled(false);

        assertNull(TrellisUtils.getCorsConfiguration(config), "CORS config persists after disabling it!");
    }

    @Test
    void testGetWebacService() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        when(mockResourceService.get(any())).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResource.getMetadataGraphNames()).thenReturn(singleton(Trellis.PreferAccessControl));

        assertNotNull(TrellisUtils.getWebacService(config, mockResourceService), "WebAC configuration not present!");

        config.getAuth().getWebac().setEnabled(false);

        assertNull(TrellisUtils.getWebacService(config, mockResourceService),
                "WebAC config persists after disabling it!");

        config.getAuth().getWebac().setEnabled(true);

        final ResourceService mockRS = mock(ResourceService.class, inv -> {
            throw new RuntimeTrellisException("expected");
        });
        assertThrows(RuntimeTrellisException.class, () -> TrellisUtils.getWebacService(config, mockRS));
        config.getAuth().getWebac().setEnabled(false);
        assertNull(TrellisUtils.getWebacService(config, mockRS),
                "WebAC config persists after disabling it!");
    }

    @Test
    void testGetAuthFilters() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setKeyStore(null);

        final List<ContainerRequestFilter> filters = TrellisUtils.getAuthFilters(config);
        assertFalse(filters.isEmpty(), "Auth filters are missing!");
        assertEquals(2L, filters.size(), "Incorrect auth filter count!");

        config.getAuth().getBasic().setEnabled(false);
        config.getAuth().getJwt().setEnabled(false);

        assertTrue(TrellisUtils.getAuthFilters(config).isEmpty(), "Auth filters should have been disabled!");
    }

    @Test
    void testGetJwksAuthenticator() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        assertTrue(TrellisUtils.getJwtAuthenticator(config) instanceof JwksAuthenticator,
                "JWT auth not enabled!");
    }

    @Test
    void testGetJwtAuthenticator() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setJwks(null);
        config.getAuth().getJwt().setKeyStore(resourceFilePath("keystore.jks"));
        assertTrue(TrellisUtils.getJwtAuthenticator(config) instanceof JwtAuthenticator,
                "JWT auth not enabled!");
    }

    @Test
    void testGetJwtAuthenticatorNoKeyIds() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setJwks(null);
        config.getAuth().getJwt().setKeyStore(resourceFilePath("keystore.jks"));
        config.getAuth().getJwt().setKeyIds(asList("foo", "bar"));
        assertTrue(TrellisUtils.getJwtAuthenticator(config) instanceof JwtAuthenticator,
                "JWT auth not disabled!");
    }

    @Test
    void testGetJwtAuthenticatorFederated() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setJwks(null);
        config.getAuth().getJwt().setKeyStore(resourceFilePath("keystore.jks"));
        config.getAuth().getJwt().setKeyIds(asList("trellis", "trellis-ec", "trellis-public"));
        assertTrue(TrellisUtils.getJwtAuthenticator(config) instanceof FederatedJwtAuthenticator,
                "JWT auth not enabled!");
    }

    @Test
    void testGetJwtAuthenticatorBadKeystore() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setJwks(null);
        config.getAuth().getJwt().setKeyStore(resourceFilePath("config1.yml"));
        assertTrue(TrellisUtils.getJwtAuthenticator(config) instanceof JwtAuthenticator,
                "JWT auth not disabled!");
    }

    @Test
    void testGetJwtAuthenticatorNoKeystore() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        final String nonexistent = resourceFilePath("config1.yml").replaceAll("config1.yml", "nonexistent.yml");
        config.getAuth().getJwt().setJwks(null);
        config.getAuth().getJwt().setKeyStore(nonexistent);
        assertTrue(TrellisUtils.getJwtAuthenticator(config) instanceof JwtAuthenticator,
                "JWT auth not disabled!");
    }

    @Test
    void testGetJwtAuthenticatorWebIdOIDC() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
            Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setKeyStore(null);
        config.getAuth().getJwt().setKey("");
        config.getAuth().getJwt().setJwks(null);
        final WebIdOIDCConfiguration webIdOIDC = new WebIdOIDCConfiguration();
        webIdOIDC.setEnabled(true);
        config.getAuth().getJwt().setWebIdOIDC(webIdOIDC);
        assertTrue(TrellisUtils.getJwtAuthenticator(config) instanceof WebIdOIDCAuthenticator,
            "JWT WebId-OIDC Authenticator not enabled");
    }

    @Test
    void testGetNoJwtAuthenticator() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setKeyStore(null);
        config.getAuth().getJwt().setKey("");
        config.getAuth().getJwt().setJwks(null);
        assertTrue(TrellisUtils.getJwtAuthenticator(config) instanceof NullAuthenticator,
                "JWT auth not disabled");
    }
}
