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
package org.trellisldp.api;

import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class SpecialResourcesTest {

    @Test
    public void testSingletons() {
        assertEquals(SpecialResources.MISSING_RESOURCE, SpecialResources.MISSING_RESOURCE);
        assertEquals(SpecialResources.DELETED_RESOURCE, SpecialResources.DELETED_RESOURCE);
        assertNotEquals(SpecialResources.MISSING_RESOURCE, SpecialResources.DELETED_RESOURCE);
    }

    @Test
    public void testMissingResource() {
        assertNull(SpecialResources.MISSING_RESOURCE.getIdentifier());
        assertNull(SpecialResources.MISSING_RESOURCE.getInteractionModel());
        assertEquals(EPOCH, SpecialResources.MISSING_RESOURCE.getModified());
        assertEquals(0L, SpecialResources.MISSING_RESOURCE.stream().count());
    }

    @Test
    public void testDeletedResource() {
        assertNull(SpecialResources.DELETED_RESOURCE.getIdentifier());
        assertNull(SpecialResources.DELETED_RESOURCE.getInteractionModel());
        assertEquals(EPOCH, SpecialResources.DELETED_RESOURCE.getModified());
        assertEquals(0L, SpecialResources.DELETED_RESOURCE.stream().count());
    }
}
