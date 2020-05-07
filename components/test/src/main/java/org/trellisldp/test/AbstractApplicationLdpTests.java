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

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Set;

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A class that runs all of the LDP tests.
 */
public abstract class AbstractApplicationLdpTests {

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

    /**
     * Define whether the Web Annotation JSON-LD profile is supported.
     *
     * <p>Override this method, having it return {@code false}, if the custom
     * JSON-LD profile test should be skipped.
     *
     * @return true if Web Annotation tests should be run; false otherwise
     */
    public Set<String> supportedJsonLdProfiles() {
        return emptySet();
    }

    public class RDFSerializationTests extends LdpCommonTests implements LdpRdfTests {
        private String resource;

        @Override
        public Set<String> supportedJsonLdProfiles() {
            return AbstractApplicationLdpTests.this.supportedJsonLdProfiles();
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

    public class BinaryTests extends LdpCommonTests implements LdpBinaryTests {
        private String resource;

        @Override
        public void setResourceLocation(final String location) {
            resource = location;
        }

        @Override
        public String getResourceLocation() {
            return resource;
        }
    }

    public class BasicContainmentTests extends LdpCommonTests implements LdpBasicContainerTests {
        private String container;

        @Override
        public void setContainerLocation(final String location) {
            container = location;
        }

        @Override
        public String getContainerLocation() {
            return container;
        }
    }

    public class DirectContainmentTests extends LdpCommonTests implements LdpDirectContainerTests {
        private String container;
        private String member;
        private String dc;

        @Override
        public void setContainerLocation(final String location) {
            container = location;
        }

        @Override
        public String getContainerLocation() {
            return container;
        }

        @Override
        public void setMemberLocation(final String location) {
            member = location;
        }

        @Override
        public String getMemberLocation() {
            return member;
        }

        @Override
        public void setDirectContainerLocation(final String location) {
            dc = location;
        }

        @Override
        public String getDirectContainerLocation() {
            return dc;
        }
    }

    public class IndirectContainmentTests extends LdpCommonTests implements LdpIndirectContainerTests {

        private String container;
        private String member;
        private String ic;

        @Override
        public void setContainerLocation(final String location) {
            container = location;
        }

        @Override
        public String getContainerLocation() {
            return container;
        }

        @Override
        public void setMemberLocation(final String location) {
            member = location;
        }

        @Override
        public String getMemberLocation() {
            return member;
        }

        @Override
        public void setIndirectContainerLocation(final String location) {
            ic = location;
        }

        @Override
        public String getIndirectContainerLocation() {
            return ic;
        }
    }

    private class LdpCommonTests implements CommonTests {
        @Override
        public Client getClient() {
            return AbstractApplicationLdpTests.this.getClient();
        }

        @Override
        public String getBaseURL() {
            return AbstractApplicationLdpTests.this.getBaseURL();
        }
    }

    @Test
    @DisplayName("RDF Serialization tests")
    public void testRdfSerialization() throws Exception {
        final LdpRdfTests tests = new RDFSerializationTests();
        assertAll("Check RDF support", tests.runTests());
    }

    @Test
    @DisplayName("LDP Binary tests")
    public void testLdpBinaries() throws Exception {
        final LdpBinaryTests tests = new BinaryTests();
        assertAll("Check binary support", tests.runTests());
    }

    @Test
    @DisplayName("LDP Basic Containment tests")
    public void testBasicContainment() throws Exception {
        final LdpBasicContainerTests tests = new BasicContainmentTests();
        assertAll("Check basic containment support", tests.runTests());
    }

    @Test
    @DisplayName("LDP Direct Containment tests")
    public void testDirectContainment() throws Exception {
        final LdpDirectContainerTests tests = new DirectContainmentTests();
        assertAll("Check direct containment support", tests.runTests());
    }

    @Test
    @DisplayName("LDP Indirect Containment tests")
    public void testIndirectContainment() throws Exception {
        final LdpIndirectContainerTests tests = new IndirectContainmentTests();
        assertAll("Check indirect containment support", tests.runTests());
    }

}
