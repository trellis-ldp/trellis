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
package org.trellisldp.http.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.Trellis;

class TrellisExtensionsTest {

    @Test
    void testExtMapBuilder() {
        final RDF rdf = RDFFactory.getInstance();
        assertTrue(TrellisExtensions.buildExtensionMap("").isEmpty());
        assertTrue(TrellisExtensions.buildExtensionMap("    ").isEmpty());
        assertTrue(TrellisExtensions.buildExtensionMap(", , = ,foo=  ,, =bar,baz").isEmpty());
        final Map<String, IRI> data1 = TrellisExtensions.buildExtensionMap(
                "ex = https://example.com/  ,acl=http://www.trellisldp.org/ns/trellis#PreferAccessControl");
        assertEquals(rdf.createIRI("https://example.com/"), data1.get("ex"));
        assertEquals(Trellis.PreferAccessControl, data1.get("acl"));
    }
}
