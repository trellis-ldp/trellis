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
package org.trellisldp.webapp;

import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;
import static org.glassfish.jersey.client.HttpUrlConnectorProvider.SET_METHOD_WORKAROUND;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_BASE_URL;
import static org.trellisldp.webapp.AppUtils.CONFIG_WEBAPP_RDF_LOCATION;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Application;

import org.apache.commons.text.RandomStringGenerator;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.test.AbstractApplicationMementoTests;

@TestInstance(PER_CLASS)
public class TrellisMementoTest extends JerseyTest {

    private static final RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withinRange('a', 'z').build();

    @Override
    protected Application configure() {
        System.setProperty(CONFIG_HTTP_BASE_URL, "http://localhost:" + getPort() + "/");
        return new TrellisApplication();
    }

    @BeforeAll
    public void before() throws Exception {
        super.setUp();
        final String id = "-" + generator.generate(5);
        System.setProperty(CONFIG_WEBAPP_RDF_LOCATION, System.getProperty("trellis.rdf.location") + id);
    }

    @AfterAll
    public void after() throws Exception {
        super.tearDown();
        System.clearProperty(CONFIG_WEBAPP_RDF_LOCATION);
        System.clearProperty(CONFIG_HTTP_BASE_URL);
    }

    @Nested
    @DisplayName("Memento tests")
    public class MementoTest extends AbstractApplicationMementoTests {

        @Override
        public Client getClient() {
            final Client client = TrellisMementoTest.this.client();
            client.property(CONNECT_TIMEOUT, 5000);
            client.property(READ_TIMEOUT, 5000);
            client.property(SET_METHOD_WORKAROUND, true);
            return client;
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + TrellisMementoTest.this.getPort() + "/";
        }
    }
}
