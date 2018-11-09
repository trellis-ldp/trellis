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

import static java.time.Instant.now;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class BinaryTemplateTest {

    private static final RDF rdf = new SimpleRDF();

    private final Long size = 10L;
    private final String mimeType = "text/plain";
    private final IRI identifier = rdf.createIRI("trellis:data/resource");

    @Test
    public void testBinaryTemplate() {
        final BinaryTemplate binary = new BinaryTemplate(identifier, mimeType, size);
        assertEquals(identifier, binary.getIdentifier(), "Identifier did not match");
        assertEquals(of(mimeType), binary.getMimeType(), "MimeType did not match");
        assertEquals(of(size), binary.getSize(), "Size did not match");
        assertFalse(binary.getModified().isPresent(), "Unexpected modification date");
    }

    @Test
    public void testBinaryTemplateWithOptionalArgs() {
        final BinaryTemplate binary = new BinaryTemplate(identifier, null, null, null);
        assertEquals(identifier, binary.getIdentifier(), "Identifier did not match");
        assertFalse(binary.getMimeType().isPresent(), "MimeType was not absent");
        assertFalse(binary.getSize().isPresent(), "Size was not absent");
        assertFalse(binary.getModified().isPresent(), "Modification was not absent");
    }

    @Test
    public void testFromBinary() {
        final Binary binary = new Binary(identifier, now(), mimeType, size);
        final BinaryTemplate template = BinaryTemplate.fromBinary(binary);
        assertEquals(binary.getIdentifier(), template.getIdentifier());
        assertEquals(of(binary.getModified()), template.getModified());
        assertTrue(template.getSize().isPresent());
        assertEquals(binary.getSize(), template.getSize());
        assertTrue(template.getMimeType().isPresent());
        assertEquals(binary.getMimeType(), template.getMimeType());
    }
}
