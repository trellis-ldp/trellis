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

import static java.time.Instant.parse;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class BinaryTest {

    private static final RDF rdf = new SimpleRDF();

    private final Long size = 10L;
    private final String mimeType = "text/plain";
    private final Instant modified = parse("2015-09-15T06:14:00.00Z");
    private final IRI identifier = rdf.createIRI("trellis:data/resource");

    @Test
    public void testBinary() {
        final Binary binary = new Binary(identifier, modified, mimeType, size);
        assertEquals(identifier, binary.getIdentifier(), "Identifier did not match");
        assertEquals(of(mimeType), binary.getMimeType(), "MimeType did not match");
        assertEquals(of(size), binary.getSize(), "Size did not match");
        assertEquals(modified, binary.getModified(), "Modification date did not match");
    }

    @Test
    public void testBinaryWithOptionalArgs() {
        final Binary binary = new Binary(identifier, modified, null, null);
        assertEquals(identifier, binary.getIdentifier(), "Identifier did not match");
        assertFalse(binary.getMimeType().isPresent(), "MimeType was not absent");
        assertFalse(binary.getSize().isPresent(), "Size was not absent");
        assertEquals(modified, binary.getModified(), "Modification date did not match");
    }

}
