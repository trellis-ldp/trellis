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
    @DisplayName("Memento TimeGate tests")
    public class TimeGateTests implements MementoTimeGateTests {

        private String resource;

        @Override
        public Client getClient() {
            return AbstractApplicationMementoTests.this.getClient();
        }

        @Override
        public String getBaseURL() {
            return AbstractApplicationMementoTests.this.getBaseURL();
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

    @Nested
    @DisplayName("Memento Resource tests")
    public class ResourceTests implements MementoResourceTests {

        private String resource;

        @Override
        public Client getClient() {
            return AbstractApplicationMementoTests.this.getClient();
        }

        @Override
        public String getBaseURL() {
            return AbstractApplicationMementoTests.this.getBaseURL();
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

    @Nested
    @DisplayName("Memento TimeMap tests")
    public class TimeMapTests implements MementoTimeMapTests {

        private String resource;

        @Override
        public Client getClient() {
            return AbstractApplicationMementoTests.this.getClient();
        }

        @Override
        public String getBaseURL() {
            return AbstractApplicationMementoTests.this.getBaseURL();
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
