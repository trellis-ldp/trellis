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

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.test.AbstractApplicationLdpTests;

/**
 * Run LDP-Related Tests.
 */
@TestInstance(PER_CLASS)
public class TrellisLdpTest extends BaseTrellisApplication {

    @Nested
    @DisplayName("LDP tests")
    public class LdpTest extends AbstractApplicationLdpTests {

        @Override
        public Client getClient() {
            return TrellisLdpTest.this.buildClient();
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + TrellisLdpTest.this.getPort() + "/";
        }
    }
}
