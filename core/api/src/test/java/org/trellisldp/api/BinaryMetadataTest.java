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

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class BinaryMetadataTest {

    private static final RDF rdf = new SimpleRDF();

    private final String mimeType = "text/plain";
    private final IRI identifier = rdf.createIRI("trellis:data/resource");

    @Test
    public void testBinaryMetadata() {
        final BinaryMetadata binary = BinaryMetadata.builder(identifier).mimeType(mimeType).build();
        assertEquals(identifier, binary.getIdentifier(), "Identifier did not match");
        assertEquals(of(mimeType), binary.getMimeType(), "MimeType did not match");
    }

    @Test
    public void testBinaryMetadataWithOptionalArgs() {
        final BinaryMetadata binary = BinaryMetadata.builder(identifier).build();
        assertEquals(identifier, binary.getIdentifier(), "Identifier did not match");
        assertFalse(binary.getMimeType().isPresent(), "MimeType was not absent");
    }
}
