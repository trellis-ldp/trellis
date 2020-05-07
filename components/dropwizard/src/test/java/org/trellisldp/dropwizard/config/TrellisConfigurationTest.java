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
package org.trellisldp.dropwizard.config;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.*;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class TrellisConfigurationTest {

    @Test
    void testConfigurationGeneral1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("Trellis", config.getDefaultName(), "Incorrect app name!");

        // Cache tests
        assertEquals(86400, config.getCache().getMaxAge(), "Incorrect cache/maxAge value!");
        assertFalse(config.getCache().getNoCache(), "Unexpected cache/noCache value!");
        assertTrue(config.getCache().getMustRevalidate(), "Missing cache/mustRevalidate value!");

        // JSON-LD tests
        assertEquals(48L, config.getJsonld().getCacheExpireHours(), "Incorrect jsonld/cacheExpireHours");
        assertEquals(10L, config.getJsonld().getCacheSize(), "Incorrect jsonld/cacheSize");
        assertTrue(config.getJsonld().getContextDomainWhitelist().isEmpty(), "Incorrect jsonld/contextDomainWhitelist");
        assertTrue(config.getJsonld().getContextWhitelist().contains("http://example.com/context.json"),
                "Incorrect jsonld/contextWhitelist value!");

        // Hub tests
        assertEquals("http://hub.example.com/", config.getHubUrl(), "Incorrect hubUrl");

        // Relative IRI configuration
        assertTrue(config.getUseRelativeIris(), "Incorrect relative IRI configuration");

        // Auth tests
        assertTrue(config.getAuth().getAdminUsers().contains("zoyd"), "zoyd missing from auth/adminUsers");
        assertTrue(config.getAuth().getAdminUsers().contains("wheeler"), "wheeler missing from auth/adminUsers");

        // Other tests
        assertEquals("my.cluster.address", config.any().get("cassandraAddress"), "Incorrect custom property!");
        assertEquals(245994, config.any().get("cassandraPort"), "Incorrect custom Integer property!");
        @SuppressWarnings("unchecked")
        final Map<String, Object> extraConfig = (Map<String, Object>) config.any().get("extraConfigValues");
        assertTrue((Boolean) extraConfig.get("first"), "Incorrect boolean nested custom properties!");
        @SuppressWarnings("unchecked")
        final List<String> list = (List<String>) extraConfig.get("second");
        assertEquals(newArrayList("val1", "val2"), list, "Incorrect nested custom properties as a List!");
    }


    @Test
    void testConfigurationAssets1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        assertEquals("org/trellisldp/rdfa/resource.mustache", config.getAssets().getTemplate(), "Bad assets/template");
        assertEquals("http://example.com/image.icon", config.getAssets().getIcon(), "Bad assets/icon value!");
        assertTrue(config.getAssets().getJs().contains("http://example.com/scripts1.js"), "Missing assets/js value!");
        assertTrue(config.getAssets().getCss().contains("http://example.com/styles1.css"), "Missing assets/css value!");
    }

    @Test
    void testConfigurationNotifications() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertFalse(config.getNotifications().getEnabled(), "Notifications unexpectedly enabled!");
        assertEquals(NotificationsConfiguration.Type.NONE, config.getNotifications().getType(),
                "Incorrect notification type!");
        assertEquals("example.com:12345", config.getNotifications().getConnectionString(), "Incorrect connection URL!");
        assertEquals("foo", config.getNotifications().any().get("some.other.value"), "Incorrect custom property!");
        assertEquals("test-topic", config.getNotifications().getTopicName(), "Incorrect topic name!");

    }

    @Test
    void testConfigurationLocations() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("http://localhost:8080/", config.getBaseUrl(), "Incorrect baseUrl!");
        assertEquals("http://hub.example.com/", config.getHubUrl(), "Incorrect hubUrl!");
    }

    @Test
    void testConfigurationAuth1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getAuth().getWebac().getEnabled(), "WebAC should be enabled!");
        assertEquals(200L, config.getAuth().getWebac().getCacheSize(), "Incorrect auth/webac/cacheSize");
        assertEquals(15L, config.getAuth().getWebac().getCacheExpireSeconds(), "Bad auth/webac/cache expiry!");
        assertTrue(config.getAuth().getBasic().getEnabled(), "basic auth not enabled!");
        assertEquals("users.auth", config.getAuth().getBasic().getUsersFile(), "Incorrect basic users file!");
        assertEquals("trellis", config.getAuth().getRealm(), "Incorrect auth realm!");
        assertEquals("openid", config.getAuth().getScope(), "Incorrect auth scope!");

        config.getAuth().setRealm("foobar");
        config.getAuth().setScope("baz");
        assertEquals("foobar", config.getAuth().getRealm(), "Incorrect auth/basic/realm value!");
        assertEquals("baz", config.getAuth().getScope(), "Incorrect auth scope!");
        assertTrue(config.getAuth().getJwt().getEnabled(), "auth/jwt not enabled!");
        assertEquals("Mz4DGzFLQysSGC98ESAnSafMLbxa71ls/zzUFOdCIJw9L0J8Q0Gt7+yCM+Ag73Tm5OTwpBemFOqPFiZ5BeBo4Q==",
                config.getAuth().getJwt().getKey(), "Incorrect auth/jwt/key");

        assertEquals("password", config.getAuth().getJwt().getKeyStorePassword(), "Wrong auth/jwt/keyStorePassword");
        assertEquals("/tmp/trellisData/keystore.jks", config.getAuth().getJwt().getKeyStore(), "Wrong keystore path!");
        assertTrue(config.getAuth().getJwt().getKeyIds().contains("baz"), "'baz' not in auth/jwt/keyIds");
        assertTrue(config.getAuth().getJwt().getKeyIds().contains("bar"), "'bar' not in auth/jwt/keyIds");
        assertTrue(config.getAuth().getJwt().getKeyIds().contains("trellis"), "'trellis' not in auth/jwt/keyIds");
        assertEquals(3, config.getAuth().getJwt().getKeyIds().size(), "Incorrect count of auth/jwt/keyIds");
        assertEquals("https://www.trellisldp.org/testing/jwks.json", config.getAuth().getJwt().getJwks(),
                "Wrong jwks value!");
    }

    @Test
    void testConfigurationCORS1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getCors().getEnabled(), "CORS not enabled!");
        assertTrue(config.getCors().getAllowOrigin().contains("*"), "'*' not in CORS allow-origin!");
        assertTrue(config.getCors().getAllowHeaders().contains("Link"), "Link not in CORS allow-headers");
        assertTrue(config.getCors().getAllowMethods().contains("PUT"), "PUT not in CORS allow-methods!");
        assertTrue(config.getCors().getExposeHeaders().contains("Memento-Datetime"),
                "memento-datetime missing from CORS expose-headers!");
        assertEquals(180, config.getCors().getMaxAge(), "Incorrect max-age value in CORS headers!");
        assertTrue(config.getCors().getAllowCredentials(), "Incorrect allow-credentials setting in CORS headers!");
    }
}
