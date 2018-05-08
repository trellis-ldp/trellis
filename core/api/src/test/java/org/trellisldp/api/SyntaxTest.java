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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.trellisldp.api.Syntax.LD_PATCH;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;

import org.junit.jupiter.api.Test;

public class SyntaxTest {

    @Test
    public void testSparqlUpdate() {
        assertEquals("SPARQL-Update", SPARQL_UPDATE.name());
        assertEquals("SPARQL 1.1 Update", SPARQL_UPDATE.title());
        assertEquals("application/sparql-update", SPARQL_UPDATE.mediaType());
        assertEquals(".ru", SPARQL_UPDATE.fileExtension());
        assertEquals("http://www.w3.org/TR/sparql11-update/", SPARQL_UPDATE.iri().getIRIString());
        assertFalse(SPARQL_UPDATE.supportsDataset());
        assertEquals(SPARQL_UPDATE.title(), SPARQL_UPDATE.toString());
        assertNotEquals(SPARQL_UPDATE, LD_PATCH);
        assertNotEquals(SPARQL_UPDATE, "blah blah");
        assertEquals(SPARQL_UPDATE, SPARQL_UPDATE);
        assertEquals(SPARQL_UPDATE.mediaType().hashCode(), SPARQL_UPDATE.hashCode());
    }

    @Test
    public void testLDPatch() {
        assertEquals("LD-Patch", LD_PATCH.name());
        assertEquals("Linked Data Patch Format", LD_PATCH.title());
        assertEquals("text/ldpatch", LD_PATCH.mediaType());
        assertEquals(".ldp", LD_PATCH.fileExtension());
        assertEquals("http://www.w3.org/ns/formats/LD_Patch", LD_PATCH.iri().getIRIString());
        assertFalse(LD_PATCH.supportsDataset());
        assertEquals(LD_PATCH.title(), LD_PATCH.toString());
        assertNotEquals(LD_PATCH, SPARQL_UPDATE);
        assertNotEquals(LD_PATCH, "blah blah");
        assertEquals(LD_PATCH, LD_PATCH);
        assertEquals(LD_PATCH.mediaType().hashCode(), LD_PATCH.hashCode());
    }
}
