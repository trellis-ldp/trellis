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

import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.api.Syntax.LD_PATCH;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;

import org.junit.jupiter.api.Test;

class SyntaxTest {

    @Test
    void testSparqlUpdate() {
        assertEquals("SPARQL-Update", SPARQL_UPDATE.name(), "Incorrect name for SPARQL-Update!");
        assertEquals("SPARQL 1.1 Update", SPARQL_UPDATE.title(), "Incorrect title for SPARQL-Update!");
        assertEquals("application/sparql-update", SPARQL_UPDATE.mediaType(), "Incorrect mediaType for SPARQL-Update!");
        assertEquals(".ru", SPARQL_UPDATE.fileExtension(), "Incorrect file extension for SPARQL-Update!");
        assertEquals("http://www.w3.org/TR/sparql11-update/", SPARQL_UPDATE.iri().getIRIString(),
                "Incorrect IRI for SPARQL-Update!");
        assertFalse(SPARQL_UPDATE.supportsDataset(), "SPARQL-Update shouldn't be supporting datasets!");
        assertEquals(SPARQL_UPDATE.title(), SPARQL_UPDATE.toString(), "String version isn't the same as the title!");
        assertNotEquals(SPARQL_UPDATE, LD_PATCH, "SPARQL-Update equals LD-PATCH???");
        assertNotEquals(SPARQL_UPDATE, "blah blah", "SPARQL-Update equals blah blah???");
        assertEquals(SPARQL_UPDATE, SPARQL_UPDATE, "SPARQL-Update doesn't act like a singleton!");
        assertEquals(SPARQL_UPDATE.mediaType().hashCode(), SPARQL_UPDATE.hashCode(),
                "SPARQL-Update has an unexpected hash code!");
    }

    @Test
    void testLDPatch() {
        assertEquals("LD-Patch", LD_PATCH.name(), "Incorrect name for LD-Patch");
        assertEquals("Linked Data Patch Format", LD_PATCH.title(), "Incorrect title for LD-Patch!");
        assertEquals("text/ldpatch", LD_PATCH.mediaType(), "Incorrect mediaType for LD-Patch!");
        assertEquals(".ldp", LD_PATCH.fileExtension(), "Incorrect extension for LD-Patch!");
        assertEquals("http://www.w3.org/ns/formats/LD_Patch", LD_PATCH.iri().getIRIString(),
                "Incorrect IRI for LD-Patch!");
        assertFalse(LD_PATCH.supportsDataset(), "LD-Patch shouldn't be supporting datasets!");
        assertEquals(LD_PATCH.title(), LD_PATCH.toString(), "LD-Patch string version isn't the same as the title");
        assertNotEquals(LD_PATCH, SPARQL_UPDATE, "LD-Patch matches SPARQL-Update???");
        assertNotEquals(LD_PATCH, "blah blah", "LD-Patch matches 'blah blah'???");
        assertEquals(LD_PATCH, LD_PATCH, "LD-Patch doesn't act like a singleton!");
        assertEquals(LD_PATCH.mediaType().hashCode(), LD_PATCH.hashCode(), "LD-Patch has unexpected hash code!");
    }
}
