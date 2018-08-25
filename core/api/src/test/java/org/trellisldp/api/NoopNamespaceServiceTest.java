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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NoopNamespaceServiceTest {

    @Test
    public void noAction() {
        final NamespaceService testService = new NoopNamespaceService();
        testService.setPrefix("foo", "http://bar/");
        assertFalse(testService.getNamespace("foo").isPresent(), "Prefix present in no-op namespace svc!");
        assertTrue(testService.getNamespaces().isEmpty(), "Namespace list not empty in no-op service!");
        assertFalse(testService.getPrefix("http://bar/").isPresent(), "Namespace present in no-op service!");
    }
}
