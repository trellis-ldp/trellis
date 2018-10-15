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

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.util.Set;

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.test.AbstractApplicationLdpTests;

/**
 * Run LDP-Related Tests.
 */
@TestInstance(PER_CLASS)
public class TrellisLdpTest extends BaseTrellisApplicationTest {

    @Nested
    @DisplayName("Trellis LDP Tests")
    public class LdpTests extends AbstractApplicationLdpTests {

        @Override
        public Client getClient() {
            return TrellisLdpTest.this.CLIENT;
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + TrellisLdpTest.this.APP.getLocalPort() + "/";
        }

        @Override
        public Set<String> supportedJsonLdProfiles() {
            return singleton("http://www.w3.org/ns/anno.jsonld");
        }
    }
}
