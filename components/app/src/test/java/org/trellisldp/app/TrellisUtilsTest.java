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

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.auth.oauth.FederatedJwtAuthenticator;
import org.trellisldp.auth.oauth.JwtAuthenticator;
import org.trellisldp.auth.oauth.NullAuthenticator;

/**
 * @author acoburn
 */
public class TrellisUtilsTest {

    @Mock
    private Environment mockEnv;

    @Mock
    private LifecycleEnvironment mockLifecycle;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockEnv.lifecycle()).thenReturn(mockLifecycle);
    }

    @Test
    public void testGetCORSConfig() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));


        assertTrue(TrellisUtils.getCorsConfiguration(config).isPresent(), "CORS configuration is missing!");

        config.getCors().setEnabled(false);

        assertFalse(TrellisUtils.getCorsConfiguration(config).isPresent(), "CORS config persists after disabling it!");
    }

    @Test
    public void testGetWebacCache() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(TrellisUtils.getWebacCache(config).isPresent(), "WebAC configuration not present!");

        config.getAuth().getWebac().setEnabled(false);

        assertFalse(TrellisUtils.getWebacCache(config).isPresent(), "WebAC config persists after disabling it!");
    }

    @Test
    public void testGetAuthFilters() throws Exception {
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
    public void testGetJwtAuthenticator() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setKeyStore(resourceFilePath("keystore.jks"));
        assertTrue(TrellisUtils.getJwtAuthenticator(config.getAuth().getJwt()) instanceof JwtAuthenticator,
                "JWT auth not enabled!");
    }

    @Test
    public void testGetJwtAuthenticatorNoKeyIds() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setKeyStore(resourceFilePath("keystore.jks"));
        config.getAuth().getJwt().setKeyIds(asList("foo", "bar"));
        assertTrue(TrellisUtils.getJwtAuthenticator(config.getAuth().getJwt()) instanceof JwtAuthenticator,
                "JWT auth not disabled!");
    }

    @Test
    public void testGetJwtAuthenticatorFederated() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setKeyStore(resourceFilePath("keystore.jks"));
        config.getAuth().getJwt().setKeyIds(asList("trellis", "trellis-ec", "trellis-public"));
        assertTrue(TrellisUtils.getJwtAuthenticator(config.getAuth().getJwt()) instanceof FederatedJwtAuthenticator,
                "JWT auth not enabled!");
    }

    @Test
    public void testGetJwtAuthenticatorBadKeystore() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setKeyStore(resourceFilePath("config1.yml"));
        assertTrue(TrellisUtils.getJwtAuthenticator(config.getAuth().getJwt()) instanceof JwtAuthenticator,
                "JWT auth not disabled!");
    }

    @Test
    public void testGetJwtAuthenticatorNoKeystore() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        final String nonexistent = resourceFilePath("config1.yml").replaceAll("config1.yml", "nonexistent.yml");
        config.getAuth().getJwt().setKeyStore(nonexistent);
        assertTrue(TrellisUtils.getJwtAuthenticator(config.getAuth().getJwt()) instanceof JwtAuthenticator,
                "JWT auth not disabled!");
    }

    @Test
    public void testGetNoJwtAuthenticator() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        config.getAuth().getJwt().setKeyStore(null);
        config.getAuth().getJwt().setKey("");
        assertTrue(TrellisUtils.getJwtAuthenticator(config.getAuth().getJwt()) instanceof NullAuthenticator,
                "JWT auth not disabled!");
    }
}
