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

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;

import javax.ws.rs.client.Client;

import org.trellisldp.app.config.TrellisConfiguration;

/**
 * LDP-related tests for Trellis.
 */
public class NoInitTrellisApplicationTest extends TrellisApplicationTest {

    private static final DropwizardTestSupport<TrellisConfiguration> APP
        = new DropwizardTestSupport<TrellisConfiguration>(SimpleNoInitTrellisApp.class,
                    resourceFilePath("trellis-config.yml"));

    private static final Client CLIENT;

    static {
        APP.before();
        CLIENT = new JerseyClientBuilder(APP.getEnvironment()).build("test client 2");
        CLIENT.property(CONNECT_TIMEOUT, 5000);
        CLIENT.property(READ_TIMEOUT, 5000);
    }
}
