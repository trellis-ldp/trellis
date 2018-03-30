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
package org.trellisldp.test;

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

/**
 * A class that runs all of the Memento-related tests.
 */
public abstract class AbstractApplicationMementoTests {

    /**
     * Get the HTTP client.
     * @return the HTTP client
     */
    public abstract Client getClient();

    /**
     * Get the baseURL for the LDP server.
     * @return the base URL
     */
    public abstract String getBaseURL();

    @Nested
    @DisplayName("Memento Binary resource tests")
    public class BinaryResourceTests extends BaseMementoTests implements MementoBinaryTests {
    }

    @Nested
    @DisplayName("Memento TimeGate tests")
    public class TimeGateTests extends BaseMementoTests implements MementoTimeGateTests {
    }

    @Nested
    @DisplayName("Memento Resource tests")
    public class ResourceTests extends BaseMementoTests implements MementoResourceTests {
    }

    @Nested
    @DisplayName("Memento TimeMap tests")
    public class TimeMapTests extends BaseMementoTests implements MementoTimeMapTests {
    }

    private class BaseMementoTests implements MementoCommonTests {

        private String resource;
        private String binary;

        @Override
        public Client getClient() {
            return AbstractApplicationMementoTests.this.getClient();
        }

        @Override
        public String getBaseURL() {
            return AbstractApplicationMementoTests.this.getBaseURL();
        }

        @Override
        public void setBinaryLocation(final String location) {
            binary = location;
        }

        @Override
        public String getBinaryLocation() {
            return binary;
        }

        @Override
        public void setResourceLocation(final String location) {
            resource = location;
        }

        @Override
        public String getResourceLocation() {
            return resource;
        }
    }

}
