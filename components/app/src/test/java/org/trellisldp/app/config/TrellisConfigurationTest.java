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
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("Trellis", config.getDefaultName());

        // Cache tests
        assertEquals((Integer) 86400, config.getCache().getMaxAge());
        assertFalse(config.getCache().getNoCache());
        assertTrue(config.getCache().getMustRevalidate());

        // JSON-LD tests
        assertEquals((Long) 48L, config.getJsonld().getCacheExpireHours());
        assertEquals((Long) 10L, config.getJsonld().getCacheSize());
        assertTrue(config.getJsonld().getContextDomainWhitelist().isEmpty());
        assertTrue(config.getJsonld().getContextWhitelist().contains("http://example.com/context.json"));

        // Hub tests
        assertEquals("http://hub.example.com/", config.getHubUrl());

        // Auth tests
        assertTrue(config.getAuth().getAdminUsers().contains("zoyd"));
        assertTrue(config.getAuth().getAdminUsers().contains("wheeler"));

        // Other tests
        assertEquals("my.cluster.address", config.any().get("cassandraAddress"));
        assertEquals((Integer)245994, config.any().get("cassandraPort"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> extraConfig = (Map<String, Object>) config.any().get("extraConfigValues");
        assertTrue((Boolean) extraConfig.get("first"));
        @SuppressWarnings("unchecked")
        final List<String> list = (List<String>) extraConfig.get("second");
        assertEquals(newArrayList("val1", "val2"), list);
    }


    @Test
    public void testConfigurationAssets1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        assertEquals("org/trellisldp/rdfa/resource.mustache", config.getAssets().getTemplate());
        assertEquals("http://example.com/image.icon", config.getAssets().getIcon());
        assertTrue(config.getAssets().getJs().contains("http://example.com/scripts1.js"));
        assertTrue(config.getAssets().getCss().contains("http://example.com/styles1.css"));
    }

    @Test
    public void testConfigurationNotifications() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertFalse(config.getNotifications().getEnabled());
        assertEquals(NotificationsConfiguration.Type.NONE, config.getNotifications().getType());
        assertEquals("example.com:12345", config.getNotifications().getConnectionString());
        assertEquals("foo", config.getNotifications().any().get("some.other.value"));
        assertEquals("test-topic", config.getNotifications().getTopicName());

    }

    @Test
    public void testConfigurationLocations() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("http://localhost:8080/", config.getBaseUrl());
        assertEquals("http://hub.example.com/", config.getHubUrl());
    }

    @Test
    public void testConfigurationAuth1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getAuth().getWebac().getEnabled());
        assertEquals((Long) 200L, config.getAuth().getWebac().getCacheSize());
        assertEquals((Long) 15L, config.getAuth().getWebac().getCacheExpireSeconds());
        assertTrue(config.getAuth().getAnon().getEnabled());
        assertTrue(config.getAuth().getBasic().getEnabled());
        assertEquals("users.auth", config.getAuth().getBasic().getUsersFile());
        assertEquals("trellis", config.getAuth().getBasic().getRealm());

        config.getAuth().getBasic().setRealm("foobar");
        assertEquals("foobar", config.getAuth().getBasic().getRealm());
        assertTrue(config.getAuth().getJwt().getEnabled());
        assertEquals("secret", config.getAuth().getJwt().getKey());
        assertFalse(config.getAuth().getJwt().getBase64Encoded());
        assertEquals("trellis", config.getAuth().getJwt().getRealm());
        config.getAuth().getJwt().setRealm("barbaz");
        assertEquals("barbaz", config.getAuth().getJwt().getRealm());

        assertEquals("password", config.getAuth().getJwt().getKeyStorePassword());
        assertEquals("/tmp/trellisData/keystore.jks", config.getAuth().getJwt().getKeyStore());
        assertTrue(config.getAuth().getJwt().getKeyIds().contains("baz"));
        assertTrue(config.getAuth().getJwt().getKeyIds().contains("bar"));
        assertTrue(config.getAuth().getJwt().getKeyIds().contains("trellis"));
        assertEquals(3, config.getAuth().getJwt().getKeyIds().size());
    }

    @Test
    public void testConfigurationCORS1() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getCors().getEnabled());
        assertTrue(config.getCors().getAllowOrigin().contains("*"));
        assertTrue(config.getCors().getAllowHeaders().contains("Want-Digest"));
        assertTrue(config.getCors().getAllowMethods().contains("PUT"));
        assertTrue(config.getCors().getExposeHeaders().contains("Memento-Datetime"));
        assertEquals((Integer) 180, config.getCors().getMaxAge());
        assertTrue(config.getCors().getAllowCredentials());
    }

}
