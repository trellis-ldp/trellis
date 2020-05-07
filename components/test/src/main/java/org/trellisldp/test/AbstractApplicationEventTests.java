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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A class that runs all of the LDP tests.
 */
public abstract class AbstractApplicationEventTests implements EventTests {

    private String container;
    private String member;
    private String directContainer;
    private String indirectContainer;

    @Override
    public String getDirectContainerLocation() {
        return directContainer;
    }

    @Override
    public void setDirectContainerLocation(final String location) {
        directContainer = location;
    }

    @Override
    public String getIndirectContainerLocation() {
        return indirectContainer;
    }

    @Override
    public void setIndirectContainerLocation(final String location) {
        indirectContainer = location;
    }

    @Override
    public String getContainerLocation() {
        return container;
    }

    @Override
    public void setContainerLocation(final String location) {
        container = location;
    }

    @Override
    public String getMemberLocation() {
        return member;
    }

    @Override
    public void setMemberLocation(final String location) {
        member = location;
    }

    @Test
    @DisplayName("Event tests")
    public void testEventFeatures() {
        assertAll("Test event features", runTests());
    }
}
