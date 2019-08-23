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
package org.trellisldp.app.mp.triplestore;

import static java.lang.Integer.getInteger;
import static javax.ws.rs.client.ClientBuilder.newBuilder;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.test.AbstractApplicationLdpTests;
import org.trellisldp.test.AbstractApplicationMementoTests;

@TestInstance(PER_CLASS)
class TrellisApplicationTest {

    protected final Client CLIENT = newBuilder().build();
    protected final String TRELLIS_URL = "http://localhost:" + getInteger("trellis.port", 9080) + "/";

    @Nested
    @DisplayName("Trellis LDP Tests")
    public class LdpTests extends AbstractApplicationLdpTests {

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.CLIENT;
        }

        @Override
        public String getBaseURL() {
            return TrellisApplicationTest.this.TRELLIS_URL;
        }
    }

    @Nested
    @DisplayName("Trellis Memento Tests")
    public class MementoTests extends AbstractApplicationMementoTests {

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.CLIENT;
        }

        @Override
        public String getBaseURL() {
            return TrellisApplicationTest.this.TRELLIS_URL;
        }
    }
}
