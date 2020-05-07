/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
 *
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

import static org.junit.jupiter.api.Assertions.assertAll;

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    public class BinaryResourceTests extends BaseMementoTests implements MementoBinaryTests {
    }

    public class TimeGateTests extends BaseMementoTests implements MementoTimeGateTests {
    }

    public class ResourceTests extends BaseMementoTests implements MementoResourceTests {
    }

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

    @Test
    @DisplayName("Memento Binary resource tests")
    public void testMementoBinaryFeatures() {
        final MementoBinaryTests tests = new BinaryResourceTests();
        assertAll("Test binary memento features", tests.runTests());
    }

    @Test
    @DisplayName("Memento TimeGate tests")
    public void testMementoTimeGateFeatures() {
        final MementoTimeGateTests tests = new TimeGateTests();
        assertAll("Test memento timegate features", tests.runTests());
    }

    @Test
    @DisplayName("Memento Resource tests")
    public void testMementoResourceFeatures() {
        final MementoResourceTests tests = new ResourceTests();
        assertAll("Test resource memento features", tests.runTests());
    }

    @Test
    @DisplayName("Memento TimeMap tests")
    public void testMementoTimeMapFeatures() {
        final MementoTimeMapTests tests = new TimeMapTests();
        assertAll("Test memento timemap features", tests.runTests());
    }
}
