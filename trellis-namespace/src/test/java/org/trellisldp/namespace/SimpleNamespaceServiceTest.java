/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
package org.trellisldp.namespace;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class SimpleNamespaceServiceTest {

    @Test
    void testNamespace() {
        final SimpleNamespaceService svc = new SimpleNamespaceService();
        svc.mappingConfig = Optional.of("dc11=http://purl.org/dc/elements/1.1/,,foo= , =bar,baz, = ");
        svc.init();
        assertEquals(11, svc.getNamespaces().size());
        assertTrue(svc.setPrefix("foo", "bar"));
        assertEquals(11, svc.getNamespaces().size());
    }

    @Test
    void testEnvNamespace() {
        final SimpleNamespaceService svc = new SimpleNamespaceService();
        svc.mappingConfig = Optional.of("dc11=http://purl.org/dc/elements/1.1/,,foo= , =bar,baz, = ");
        svc.init();
        final String dc11 = "http://purl.org/dc/elements/1.1/";
        assertEquals(dc11, svc.getNamespaces().get("dc11"));
    }
}
