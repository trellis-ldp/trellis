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

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class TrellisConfigurationTest {

    @Test
    public void testConfigurationGeneral1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("Trellis", config.getDefaultName(), "Incorrect app name!");

        // Cache tests
        assertEquals((Integer) 86400, config.getCache().getMaxAge(), "Incorrect cache/maxAge value!");
        assertFalse(config.getCache().getNoCache(), "Unexpected cache/noCache value!");
        assertTrue(config.getCache().getMustRevalidate(), "Missing cache/mustRevalidate value!");

        // JSON-LD tests
        assertEquals((Long) 48L, config.getJsonld().getCacheExpireHours(), "Incorrect jsonld/cacheExpireHours");
        assertEquals((Long) 10L, config.getJsonld().getCacheSize(), "Incorrect jsonld/cacheSize");
        assertTrue(config.getJsonld().getContextDomainWhitelist().isEmpty(), "Incorrect jsonld/contextDomainWhitelist");
        assertTrue(config.getJsonld().getContextWhitelist().contains("http://example.com/context.json"),
                "Incorrect jsonld/contextWhitelist value!");

        // Hub tests
        assertEquals("http://hub.example.com/", config.getHubUrl(), "Incorrect hubUrl");

        // Auth tests
        assertTrue(config.getAuth().getAdminUsers().contains("zoyd"), "zoyd missing from auth/adminUsers");
        assertTrue(config.getAuth().getAdminUsers().contains("wheeler"), "wheeler missing from auth/adminUsers");

        // Other tests
        assertEquals("my.cluster.address", config.any().get("cassandraAddress"), "Incorrect custom property!");
        assertEquals((Integer)245994, config.any().get("cassandraPort"), "Incorrect custom Integer property!");
        @SuppressWarnings("unchecked")
        final Map<String, Object> extraConfig = (Map<String, Object>) config.any().get("extraConfigValues");
        assertTrue((Boolean) extraConfig.get("first"), "Incorrect boolean nested custom properties!");
        @SuppressWarnings("unchecked")
        final List<String> list = (List<String>) extraConfig.get("second");
        assertEquals(newArrayList("val1", "val2"), list, "Incorrect nested custom properties as a List!");
    }


    @Test
    public void testConfigurationAssets1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        assertEquals("org/trellisldp/rdfa/resource.mustache", config.getAssets().getTemplate(), "Bad assets/template");
        assertEquals("http://example.com/image.icon", config.getAssets().getIcon(), "Bad assets/icon value!");
        assertTrue(config.getAssets().getJs().contains("http://example.com/scripts1.js"), "Missing assets/js value!");
        assertTrue(config.getAssets().getCss().contains("http://example.com/styles1.css"), "Missing assets/css value!");
    }

    @Test
    public void testConfigurationNotifications() throws Exception {
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
    public void testConfigurationLocations() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("http://localhost:8080/", config.getBaseUrl(), "Incorrect baseUrl!");
        assertEquals("http://hub.example.com/", config.getHubUrl(), "Incorrect hubUrl!");
    }

    @Test
    public void testConfigurationAuth1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getAuth().getWebac().getEnabled(), "WebAC should be enabled!");
        assertEquals((Long) 200L, config.getAuth().getWebac().getCacheSize(), "Incorrect auth/webac/cacheSize");
        assertEquals((Long) 15L, config.getAuth().getWebac().getCacheExpireSeconds(), "Bad auth/webac/cache expiry!");
        assertTrue(config.getAuth().getBasic().getEnabled(), "basic auth not enabled!");
        assertEquals("users.auth", config.getAuth().getBasic().getUsersFile(), "Incorrect basic users file!");
        assertEquals("trellis", config.getAuth().getBasic().getRealm(), "Incorrect basic auth realm!");

        config.getAuth().getBasic().setRealm("foobar");
        assertEquals("foobar", config.getAuth().getBasic().getRealm(), "Incorrect auth/basic/realm value!");
        assertTrue(config.getAuth().getJwt().getEnabled(), "auth/jwt not enabled!");
        assertEquals("Mz4DGzFLQysSGC98ESAnSafMLbxa71ls/zzUFOdCIJw9L0J8Q0Gt7+yCM+Ag73Tm5OTwpBemFOqPFiZ5BeBo4Q==",
                config.getAuth().getJwt().getKey(), "Incorrect auth/jwt/key");
        assertEquals("trellis", config.getAuth().getJwt().getRealm(), "Incorrect auth/jwt/realm value!");
        config.getAuth().getJwt().setRealm("barbaz");
        assertEquals("barbaz", config.getAuth().getJwt().getRealm(), "Incorrect auth/jwt/realm value after a change!");

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
    public void testConfigurationCORS1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getCors().getEnabled(), "CORS not enabled!");
        assertTrue(config.getCors().getAllowOrigin().contains("*"), "'*' not in CORS allow-origin!");
        assertTrue(config.getCors().getAllowHeaders().contains("Want-Digest"), "want-digest not in CORS allow-headers");
        assertTrue(config.getCors().getAllowMethods().contains("PUT"), "PUT not in CORS allow-methods!");
        assertTrue(config.getCors().getExposeHeaders().contains("Memento-Datetime"),
                "memento-datetime missing from CORS expose-headers!");
        assertEquals((Integer) 180, config.getCors().getMaxAge(), "Incorrect max-age value in CORS headers!");
        assertTrue(config.getCors().getAllowCredentials(), "Incorrect allow-credentials setting in CORS headers!");
    }
}
