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

import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;
import static org.trellisldp.app.triplestore.TestUtils.buildApplication;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.AfterAll;
import org.trellisldp.test.AbstractApplicationAuditTests;

/**
 * Audit tests.
 */
public class TrellisAuditTest extends AbstractApplicationAuditTests {

    private static final DropwizardTestSupport<AppConfiguration> APP = buildApplication();
    private static final Client CLIENT;

    static {
        APP.before();
        CLIENT = new JerseyClientBuilder(APP.getEnvironment()).build("test client");
        CLIENT.property(CONNECT_TIMEOUT, 5000);
        CLIENT.property(READ_TIMEOUT, 5000);
    }

    @Override
    public String getJwtSecret() {
        return "EEPPbd/7llN/chRwY2UgbdcyjFdaGjlzaupd3AIyjcu8hMnmMCViWoPUBb5FphGLxBlUlT/G5WMx0WcDq/iNKA==";
    }

    @Override
    public Client getClient() {
        return CLIENT;
    }

    @Override
    public String getBaseURL() {
        return "http://localhost:" + APP.getLocalPort() + "/";
    }

    @AfterAll
    public static void cleanup() throws Exception {
        APP.after();
    }
}
