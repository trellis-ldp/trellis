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
package org.trellisldp.triplestore;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

/**
 * ResourceService tests.
 */
class TriplestoreUtilsTest {

    @Test
    void testNodeConversion() {
        final Resource s = createResource("http://example.com/Resource");
        final Property p = createProperty("http://example.com/prop");
        final Resource o = createResource("http://example.com/Other");
        assertFalse(TriplestoreUtils.nodesToTriple(null, null, null).isPresent(), "null nodes yield valid triple!");
        assertFalse(TriplestoreUtils.nodesToTriple(s, null, null).isPresent(), "null node results in a valid triple!");
        assertFalse(TriplestoreUtils.nodesToTriple(null, p, null).isPresent(), "null node results in a valid triple!");
        assertFalse(TriplestoreUtils.nodesToTriple(null, null, o).isPresent(), "null node results in a valid triple!");
        assertFalse(TriplestoreUtils.nodesToTriple(s, p, null).isPresent(), "null node results in a valid triple!");
        assertFalse(TriplestoreUtils.nodesToTriple(s, null, o).isPresent(), "null node results in a valid triple!");
        assertFalse(TriplestoreUtils.nodesToTriple(null, p, o).isPresent(), "null node results in a valid triple!");
        assertTrue(TriplestoreUtils.nodesToTriple(s, p, o).isPresent(), "Nodes not converted to triple!");
    }
}
