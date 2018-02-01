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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.app.config.RdfConnectionConfiguration;
import org.trellisldp.app.config.TrellisConfiguration;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class TrellisUtilsTest {

    @Test
    public void testGetAssetConfigurations() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        final Map<String, String> assets = TrellisUtils.getAssetConfiguration(config);
        assertEquals(3L, assets.size());
        assertEquals("http://example.org/image.icon", assets.get("icon"));
        assertEquals("http://example.org/styles1.css,http://example.org/styles2.css",
                assets.get("css"));
        assertEquals("http://example.org/scripts1.js,http://example.org/scripts2.js",
                assets.get("js"));
    }

    @Test
    public void testGetWebacConfig() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));


        assertTrue(TrellisUtils.getWebacConfiguration(config).isPresent());

        config.getAuth().getWebac().setEnabled(false);

        assertFalse(TrellisUtils.getWebacConfiguration(config).isPresent());
    }

    @Test
    public void testGetCORSConfig() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));


        assertTrue(TrellisUtils.getCorsConfiguration(config).isPresent());

        config.getCors().setEnabled(false);

        assertFalse(TrellisUtils.getCorsConfiguration(config).isPresent());
    }

    @Test
    public void testGetRDFConnection() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        assertNotNull(TrellisUtils.getRDFConnection(config));
        assertFalse(TrellisUtils.getRDFConnection(config).isClosed());

        final RdfConnectionConfiguration c = new RdfConnectionConfiguration();
        c.setLocation("http://localhost/sparql");
        config.setRdfstore(c);

        assertNotNull(TrellisUtils.getRDFConnection(config));
        assertFalse(TrellisUtils.getRDFConnection(config).isClosed());

        c.setLocation("https://localhost/sparql");
        assertNotNull(TrellisUtils.getRDFConnection(config));
        assertFalse(TrellisUtils.getRDFConnection(config).isClosed());

        final File dir = new File(new File(getClass().getResource("/data").toURI()), "resources");
        c.setLocation(dir.getAbsolutePath());
        assertNotNull(TrellisUtils.getRDFConnection(config));
        assertFalse(TrellisUtils.getRDFConnection(config).isClosed());
    }



    @Test
    public void testGetAuthFilters() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "")
            .build(new File(getClass().getResource("/config1.yml").toURI()));

        final Optional<List<AuthFilter>> filters = TrellisUtils.getAuthFilters(config);
        assertTrue(filters.isPresent());
        filters.ifPresent(f -> assertEquals(3L, f.size()));

        config.getAuth().getAnon().setEnabled(false);
        config.getAuth().getBasic().setEnabled(false);
        config.getAuth().getJwt().setEnabled(false);

        assertFalse(TrellisUtils.getAuthFilters(config).isPresent());
    }

}
