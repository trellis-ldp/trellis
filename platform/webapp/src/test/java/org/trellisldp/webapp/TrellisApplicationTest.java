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

import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;
import static org.glassfish.jersey.client.HttpUrlConnectorProvider.SET_METHOD_WORKAROUND;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_BASE_URL;

import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.test.AbstractApplicationAuditTests;
import org.trellisldp.test.AbstractApplicationAuthTests;
import org.trellisldp.test.AbstractApplicationLdpTests;
import org.trellisldp.test.AbstractApplicationMementoTests;

/**
 * A base class for setting up a Trellis application.
 */
@TestInstance(PER_CLASS)
public class TrellisApplicationTest extends JerseyTest {

    protected Client buildClient() {
        final Client client = this.client();
        client.property(CONNECT_TIMEOUT, 10000);
        client.property(READ_TIMEOUT, 12000);
        client.property(SET_METHOD_WORKAROUND, true);
        return client;
    }

    @Override
    protected Application configure() {
        System.setProperty(CONFIG_HTTP_BASE_URL, "http://localhost:" + getPort() + "/");
        return new TrellisApplication();
    }

    @BeforeAll
    public void before() throws Exception {
        super.setUp();
        setDefaultPollInterval(100L, MILLISECONDS);
    }

    @AfterAll
    public void after() throws Exception {
        super.tearDown();
        System.clearProperty(CONFIG_HTTP_BASE_URL);
    }

    @Nested
    @DisplayName("LDP tests")
    public class LdpTest extends AbstractApplicationLdpTests {

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.buildClient();
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + TrellisApplicationTest.this.getPort() + "/";
        }

        @Override
        public Set<String> supportedJsonLdProfiles() {
            return singleton("http://www.w3.org/ns/anno.jsonld");
        }
    }

    @Nested
    @DisplayName("LDP tests")
    public class AuthorizationTest extends AbstractApplicationAuthTests {

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.buildClient();
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + TrellisApplicationTest.this.getPort() + "/";
        }

        @Override
        public String getUser1Credentials() {
            return "acoburn:secret";
        }

        @Override
        public String getUser2Credentials() {
            return "user:password";
        }

        @Override
        public String getJwtSecret() {
            return "EEPPbd/7llN/chRwY2UgbdcyjFdaGjlzaupd3AIyjcu8hMnmMCViWoPUBb5FphGLxBlUlT/G5WMx0WcDq/iNKA==";
        }

        @Override
        public String getAdminWebId() {
            return "http://admin.example.com/#me";
        }
    }

    @Nested
    @DisplayName("Memento tests")
    public class MementoTest extends AbstractApplicationMementoTests {

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.buildClient();
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + TrellisApplicationTest.this.getPort() + "/";
        }
    }

    @Nested
    @DisplayName("Audit tests")
    public class AuditTest extends AbstractApplicationAuditTests {

        @Override
        public String getJwtSecret() {
            return "EEPPbd/7llN/chRwY2UgbdcyjFdaGjlzaupd3AIyjcu8hMnmMCViWoPUBb5FphGLxBlUlT/G5WMx0WcDq/iNKA==";
        }

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.buildClient();
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + TrellisApplicationTest.this.getPort() + "/";
        }
    }
}
