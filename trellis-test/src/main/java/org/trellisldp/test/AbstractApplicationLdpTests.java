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
import javax.ws.rs.core.EntityTag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

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
    public Boolean supportWebAnnotationProfile() {
        return true;
    }

    @Nested
    @DisplayName("RDF Serialization tests")
    public class RDFSerializationTests extends LdpCommonTests implements LdpRdfTests {
        private String resource;
        private String annotation;
        private String container;
        private EntityTag etag1;
        private EntityTag etag2;

        @Override
        public Boolean supportWebAnnotationProfile() {
            return AbstractApplicationLdpTests.this.supportWebAnnotationProfile();
        }

        @Override
        public void setResourceLocation(final String location) {
            resource = location;
        }

        @Override
        public String getResourceLocation() {
            return resource;
        }

        @Override
        public void setAnnotationLocation(final String location) {
            annotation = location;
        }

        @Override
        public String getAnnotationLocation() {
            return annotation;
        }

        @Override
        public void setContainerLocation(final String location) {
            container = location;
        }

        @Override
        public String getContainerLocation() {
            return container;
        }

        @Override
        public void setFirstETag(final EntityTag etag) {
            etag1 = etag;
        }

        @Override
        public EntityTag getFirstETag() {
            return etag1;
        }

        @Override
        public void setSecondETag(final EntityTag etag) {
            etag2 = etag;
        }

        @Override
        public EntityTag getSecondETag() {
            return etag2;
        }
    }

    @Nested
    @DisplayName("LDP Binary tests")
    public class BinaryTests extends LdpCommonTests implements LdpBinaryTests {
        private String resource;
        private String container;
        private EntityTag etag1;
        private EntityTag etag2;

        @Override
        public void setResourceLocation(final String location) {
            resource = location;
        }

        @Override
        public String getResourceLocation() {
            return resource;
        }

        @Override
        public void setContainerLocation(final String location) {
            container = location;
        }

        @Override
        public String getContainerLocation() {
            return container;
        }

        @Override
        public void setFirstETag(final EntityTag etag) {
            etag1 = etag;
        }

        @Override
        public EntityTag getFirstETag() {
            return etag1;
        }

        @Override
        public void setSecondETag(final EntityTag etag) {
            etag2 = etag;
        }

        @Override
        public EntityTag getSecondETag() {
            return etag2;
        }
    }

    @Nested
    @DisplayName("LDP Basic Containment tests")
    public class BasicContainmentTests extends LdpCommonTests implements LdpBasicContainerTests {
        private String child;
        private String container;
        private EntityTag etag1;
        private EntityTag etag2;
        private EntityTag etag3;
        private EntityTag etag4;

        @Override
        public void setChildLocation(final String location) {
            child = location;
        }

        @Override
        public String getChildLocation() {
            return child;
        }

        @Override
        public void setContainerLocation(final String location) {
            container = location;
        }

        @Override
        public String getContainerLocation() {
            return container;
        }

        @Override
        public void setFirstETag(final EntityTag etag) {
            etag1 = etag;
        }

        @Override
        public EntityTag getFirstETag() {
            return etag1;
        }

        @Override
        public void setSecondETag(final EntityTag etag) {
            etag2 = etag;
        }

        @Override
        public EntityTag getSecondETag() {
            return etag2;
        }

        @Override
        public void setThirdETag(final EntityTag etag) {
            etag3 = etag;
        }

        @Override
        public EntityTag getThirdETag() {
            return etag3;
        }

        @Override
        public void setFourthETag(final EntityTag etag) {
            etag4 = etag;
        }

        @Override
        public EntityTag getFourthETag() {
            return etag4;
        }
    }

    @Nested
    @DisplayName("LDP Direct Containment tests")
    public class DirectContainmentTests extends LdpCommonTests implements LdpDirectContainerTests {
        private String child;
        private String container;
        private String member;
        private String dc1;
        private String dc2;
        private String dc3;

        @Override
        public void setChildLocation(final String location) {
            child = location;
        }

        @Override
        public String getChildLocation() {
            return child;
        }

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
        public void setFirstDirectContainerLocation(final String location) {
            dc1 = location;
        }

        @Override
        public String getFirstDirectContainerLocation() {
            return dc1;
        }

        @Override
        public void setSecondDirectContainerLocation(final String location) {
            dc2 = location;
        }

        @Override
        public String getSecondDirectContainerLocation() {
            return dc2;
        }

        @Override
        public void setThirdDirectContainerLocation(final String location) {
            dc3 = location;
        }

        @Override
        public String getThirdDirectContainerLocation() {
            return dc3;
        }
    }

    @Nested
    @DisplayName("LDP Indirect Containment tests")
    public class IndirectContainmentTests extends LdpCommonTests implements LdpIndirectContainerTests {
        private String child;
        private String container;
        private String member;
        private String ic1;
        private String ic2;
        private String ic3;

        @Override
        public void setChildLocation(final String location) {
            child = location;
        }

        @Override
        public String getChildLocation() {
            return child;
        }

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
        public void setFirstIndirectContainerLocation(final String location) {
            ic1 = location;
        }

        @Override
        public String getFirstIndirectContainerLocation() {
            return ic1;
        }

        @Override
        public void setSecondIndirectContainerLocation(final String location) {
            ic2 = location;
        }

        @Override
        public String getSecondIndirectContainerLocation() {
            return ic2;
        }

        @Override
        public void setThirdIndirectContainerLocation(final String location) {
            ic3 = location;
        }

        @Override
        public String getThirdIndirectContainerLocation() {
            return ic3;
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
}
