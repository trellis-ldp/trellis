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
package org.trellisldp.app.triplestore;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.trellisldp.app.config.NotificationsConfiguration;

/**
 * @author acoburn
 */
public class TrellisConfigurationTest {

    @Test
    public void testConfigurationGeneral1() throws Exception {
        final AppConfiguration config = new YamlConfigurationFactory<>(AppConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("Trellis", config.getDefaultName(), "Incorrect default name!");
        assertEquals((Integer) 86400, config.getCache().getMaxAge(), "Incorrect maxAge!");
        assertTrue(config.getCache().getMustRevalidate(), "Incorrect cache/mustRevalidate value!");
        assertFalse(config.getCache().getNoCache(), "Incorrect cache/noCache value!");
        assertEquals((Long) 10L, config.getJsonld().getCacheSize(), "Incorrect jsonld/cacheSize value!");
        assertEquals((Long) 48L, config.getJsonld().getCacheExpireHours(), "Incorrect jsonld/cacheExpireHours value!");
        assertTrue(config.getJsonld().getContextDomainWhitelist().isEmpty(), "Incorrect jsonld/contextDomainWhitelist");
        assertTrue(config.getJsonld().getContextWhitelist().contains("http://example.org/context.json"),
                "Incorrect jsonld/contextWhitelist value!");
        assertNull(config.getResources(), "Unexpected resources value!");
        assertEquals("http://hub.example.com/", config.getHubUrl(), "Incorrect hub value!");
        assertEquals((Integer) 2, config.getBinaryHierarchyLevels(), "Incorrect binaryHierarchyLevels value!");
        assertEquals((Integer) 1, config.getBinaryHierarchyLength(), "Incorrect binaryHierarchyLength value!");
        assertEquals("my.cluster.node", config.any().get("cassandraAddress"), "Incorrect custom value!");
        assertEquals((Integer)245993, config.any().get("cassandraPort"), "Incorrect custom value (2)!");
        @SuppressWarnings("unchecked")
        final Map<String, Object> extraConfig = (Map<String, Object>) config.any().get("extraConfigValues");
        assertTrue((Boolean) extraConfig.get("one"), "Invalid nested custom values as boolean!");
        @SuppressWarnings("unchecked")
        final List<String> list = (List<String>) extraConfig.get("two");
        assertEquals(newArrayList("value1", "value2"), list, "Invalid nested custom values as list!");
    }


    @Test
    public void testConfigurationAssets1() throws Exception {
        final AppConfiguration config = new YamlConfigurationFactory<>(AppConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));
        assertEquals("org/trellisldp/rdfa/resource.mustache", config.getAssets().getTemplate(), "Incorrect asset tpl!");
        assertEquals("http://example.org/image.icon", config.getAssets().getIcon(), "Incorrect asset icon!");
        assertTrue(config.getAssets().getJs().contains("http://example.org/scripts1.js"), "Incorrect asset js!");
        assertTrue(config.getAssets().getCss().contains("http://example.org/styles1.css"), "Incorrect asset css!");
    }

    @Test
    public void testConfigurationNotifications() throws Exception {
        final AppConfiguration config = new YamlConfigurationFactory<>(AppConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertFalse(config.getNotifications().getEnabled(), "Notifications aren't enabled!");
        assertEquals(NotificationsConfiguration.Type.NONE, config.getNotifications().getType(),
                "Incorrect notification type!");
        assertEquals("example.com:1234", config.getNotifications().getConnectionString(), "Incorrect connect string!");
        assertEquals("foo", config.getNotifications().any().get("some.other.value"), "Incorrect custom value!");
        assertEquals("test", config.getNotifications().getTopicName(), "Incorrect topic name!");

    }

    @Test
    public void testConfigurationLocations() throws Exception {
        final AppConfiguration config = new YamlConfigurationFactory<>(AppConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("/tmp/trellisData/binaries", config.getBinaries(), "Incorrect binary location!");
        assertEquals("/tmp/trellisData/mementos", config.getMementos(), "Incorrect memento location!");
        assertEquals("http://localhost:8080/", config.getBaseUrl(), "Incorrect base URL!");
        assertEquals("http://hub.example.com/", config.getHubUrl(), "Incorrect hub URL!");

        final String resources = "http://triplestore.example.com/";
        config.setResources(resources);
        assertEquals(resources, config.getResources(), "Incorrect resource location!");
    }

    @Test
    public void testConfigurationAuth1() throws Exception {
        final AppConfiguration config = new YamlConfigurationFactory<>(AppConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getAuth().getWebac().getEnabled(), "WebAC wasn't enabled!");
        assertEquals((Long) 100L, config.getAuth().getWebac().getCacheSize(), "Incorrect auth/webac/cacheSize value!");
        assertEquals((Long) 10L, config.getAuth().getWebac().getCacheExpireSeconds(), "Incorrect webac cache expiry!");
        assertTrue(config.getAuth().getBasic().getEnabled(), "Missing basic auth support!");
        assertEquals("users.auth", config.getAuth().getBasic().getUsersFile(), "Incorrect basic auth users file!");
        assertEquals("trellis", config.getAuth().getBasic().getRealm(), "Incorrect basic auth realm!");

        config.getAuth().getBasic().setRealm("foo");
        assertEquals("foo", config.getAuth().getBasic().getRealm(), "Incorrect basic auth realm!");
        assertTrue(config.getAuth().getJwt().getEnabled(), "JWT not enabled!");
        assertEquals("xd1GuAwiP2+M+pyK+GlIUEAumSmFx5DP3dziGtVb1tA+/8oLXfSDMDZFkxVghyAd28rXImy18TmttUi+g0iomQ==",
                config.getAuth().getJwt().getKey(), "Incorrect JWT key!");
        assertEquals("trellis", config.getAuth().getJwt().getRealm(), "Incorrect JWT realm!");
        config.getAuth().getJwt().setRealm("bar");
        assertEquals("bar", config.getAuth().getJwt().getRealm(), "Incorrect JWT realm after reset!");

        assertEquals("password", config.getAuth().getJwt().getKeyStorePassword(), "Incorrect JWT keystore password!");
        assertEquals("/tmp/trellisData/keystore.jks", config.getAuth().getJwt().getKeyStore(), "Wrong keystore loc!");
        assertTrue(config.getAuth().getJwt().getKeyIds().contains("foo"), "'foo' missing from JWT key ids!");
        assertTrue(config.getAuth().getJwt().getKeyIds().contains("bar"), "'bar' missing from JWT key ids!");
        assertTrue(config.getAuth().getJwt().getKeyIds().contains("trellis"), "'trellis' missing from JWT key ids!");
        assertEquals(3, config.getAuth().getJwt().getKeyIds().size(), "Incorrect JWT Key id count!");
    }

    @Test
    public void testConfigurationNamespaces1() throws Exception {
        final AppConfiguration config = new YamlConfigurationFactory<>(AppConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertEquals("/tmp/trellisData/namespaces.json", config.getNamespaces(), "Incorrect namespace location!");
    }

    @Test
    public void testConfigurationCORS1() throws Exception {
        final AppConfiguration config = new YamlConfigurationFactory<>(AppConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertTrue(config.getCors().getEnabled(), "CORS isn't enabled!");
        assertTrue(config.getCors().getAllowOrigin().contains("*"), "CORS origin not '*'");
        assertTrue(config.getCors().getAllowHeaders().contains("Link"), "Link not in CORS allow-headers!");
        assertTrue(config.getCors().getAllowMethods().contains("PATCH"), "PATCH not in CORS allow-methods!");
        assertTrue(config.getCors().getExposeHeaders().contains("Location"), "Location not in CORS expose-headers!");
        assertEquals((Integer) 180, config.getCors().getMaxAge(), "incorrect max-age in CORS headers!");
        assertTrue(config.getCors().getAllowCredentials(), "CORS allow-credentials not set!");
    }
}
