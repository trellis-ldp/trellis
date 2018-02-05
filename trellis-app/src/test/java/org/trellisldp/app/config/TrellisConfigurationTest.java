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
package org.trellisldp.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class TrellisConfigurationTest {

    @Test
    public void testConfigurationGeneral1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("Trellis", config.getDefaultName());
        assertEquals((Integer) 86400, config.getCacheMaxAge());
        assertEquals((Long) 10L, config.getJsonld().getCacheSize());
        assertEquals((Long) 48L, config.getJsonld().getCacheExpireHours());
        assertTrue(config.getJsonld().getContextDomainWhitelist().isEmpty());
        assertTrue(config.getJsonld().getContextWhitelist().contains("http://example.org/context.json"));
        assertNull(config.getResources());
    }


    @Test
    public void testConfigurationAssets1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("http://example.org/image.icon", config.getAssets().getIcon());
        assertTrue(config.getAssets().getJs().contains("http://example.org/scripts1.js"));
        assertTrue(config.getAssets().getCss().contains("http://example.org/styles1.css"));
    }

    @Test
    public void testConfigurationLocations() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("/tmp/trellisData/binaries", config.getBinaries());
        assertEquals("/tmp/trellisData/mementos", config.getMementos());
        assertEquals("http://localhost:8080/", config.getBaseUrl());
    }

    @Test
    public void testConfigurationAuth1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getAuth().getWebac().getEnabled());
        assertEquals((Long) 100L, config.getAuth().getWebac().getCacheSize());
        assertEquals((Long) 10L, config.getAuth().getWebac().getCacheExpireSeconds());
        assertTrue(config.getAuth().getAnon().getEnabled());
        assertTrue(config.getAuth().getBasic().getEnabled());
        assertEquals("users.auth", config.getAuth().getBasic().getUsersFile());
        assertTrue(config.getAuth().getJwt().getEnabled());
        assertEquals("secret", config.getAuth().getJwt().getKey());
        assertFalse(config.getAuth().getJwt().getBase64Encoded());
    }

    @Test
    public void testConfigurationNamespaces1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("/tmp/trellisData/namespaces.json", config.getNamespaces());
    }

    @Test
    public void testConfigurationCORS1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getCors().getEnabled());
        assertTrue(config.getCors().getAllowOrigin().contains("*"));
        assertTrue(config.getCors().getAllowHeaders().contains("Link"));
        assertTrue(config.getCors().getAllowMethods().contains("PATCH"));
        assertTrue(config.getCors().getExposeHeaders().contains("Location"));
        assertEquals((Integer) 180, config.getCors().getMaxAge());
        assertTrue(config.getCors().getAllowCredentials());
    }
}
