/*
 * Copyright (c) Aaron Coburn and individual contributors
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
import org.trellisldp.api.ResourceService;

/**
 * A convenience class for running the ResourceService tests.
 */
public abstract class AbstractResourceServiceTests implements ResourceServiceTests {

    /**
     * Get the ResourceService.
     * @return the resource service
     */
    @Override
    public abstract ResourceService getResourceService();

    @Test
    @DisplayName("Resource service tests")
    public void testResourceServiceFeatures() throws Exception {
        assertAll("Test resource service features", runTests());
    }
}
