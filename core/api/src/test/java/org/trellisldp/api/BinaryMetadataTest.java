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
package org.trellisldp.api;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class BinaryMetadataTest {

    private static final RDF rdf = new SimpleRDF();

    private final IRI identifier = rdf.createIRI("trellis:data/resource");

    @Test
    void testBinaryMetadata() {
        final String mimeType = "text/plain";
        final Map<String, List<String>> hints = singletonMap("key", asList("val1", "val2"));
        final BinaryMetadata binary = BinaryMetadata.builder(identifier).mimeType(mimeType).hints(hints).build();
        assertEquals(identifier, binary.getIdentifier(), "Identifier did not match");
        assertEquals(of(mimeType), binary.getMimeType(), "MimeType did not match");
        assertEquals(hints, binary.getHints(), "hints did not match");
    }

    @Test
    void testBinaryMetadataWithOptionalArgs() {
        final BinaryMetadata binary = BinaryMetadata.builder(identifier).build();
        assertEquals(identifier, binary.getIdentifier(), "Identifier did not match");
        assertFalse(binary.getMimeType().isPresent(), "MimeType was not absent");
        assertTrue(binary.getHints().isEmpty(), "Hints are not empty!");
    }
}
