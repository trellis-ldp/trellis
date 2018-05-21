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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.trellisldp.app.config.DatasetConnectionConfiguration;
import org.trellisldp.app.config.TrellisConfiguration;

public class JenaRDFConnectionTest {

    @Test
    public void testGetRDFConnection() throws Exception {
        final TrellisConfiguration config = new YamlConfigurationFactory<>(TrellisConfiguration.class,
                Validators.newValidator(), Jackson.newObjectMapper(), "").build(
                new File(getClass().getResource("/config1.yml").toURI()));
        final DatasetConnectionConfiguration rc = new DatasetConnectionConfiguration();
        assertNotNull(JenaRDFConnection.getRDFConnection(config));
        assertFalse(JenaRDFConnection.getRDFConnection(config).getConnection().isClosed());
        rc.setDatasetLocation("http://localhost/sparql");
        config.setResources(rc);

        assertNotNull(JenaRDFConnection.getRDFConnection(config));
        assertFalse(JenaRDFConnection.getRDFConnection(config).getConnection().isClosed());

        rc.setDatasetLocation("https://localhost/sparql");
        config.setResources(rc);
        assertNotNull(JenaRDFConnection.getRDFConnection(config));
        assertFalse((JenaRDFConnection.getRDFConnection(config).getConnection().isClosed()));

        final File dir = new File(new File(getClass().getResource("/data").toURI()), "resources");
        rc.setDatasetLocation(dir.getAbsolutePath());
        config.setResources(rc);
        assertNotNull(JenaRDFConnection.getRDFConnection(config));
        assertFalse(JenaRDFConnection.getRDFConnection(config).getConnection().isClosed());
    }

}
