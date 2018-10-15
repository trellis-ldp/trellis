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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.test.AbstractApplicationMementoTests;

@TestInstance(PER_CLASS)
public class TrellisMementoTest extends BaseTrellisApplication {

    static {
        setDefaultPollInterval(100L, MILLISECONDS);
    }

    @Nested
    @DisplayName("Trellis LDP Tests")
    public class MementoTests extends AbstractApplicationMementoTests {

        @Override
        public Client getClient() {
            return TrellisMementoTest.this.CLIENT;
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + TrellisMementoTest.this.APP.getLocalPort() + "/";
        }
    }
}
