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
package org.trellisldp.io.impl;

import static org.apache.jena.riot.RDFFormat.JSONLD_COMPACT_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD_EXPAND_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD_FLATTEN_FLAT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.vocabulary.JSONLD;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class IOUtilsTest {

    private static final RDF rdf = new JenaRDF();

    @Test
    public void testProfile() {
        assertEquals(JSONLD_COMPACT_FLAT, IOUtils.getJsonLdProfile(JSONLD.compacted));
        assertEquals(JSONLD_EXPAND_FLAT, IOUtils.getJsonLdProfile());
        assertEquals(JSONLD_EXPAND_FLAT, IOUtils.getJsonLdProfile(rdf.createIRI("ex:text")));
        assertEquals(JSONLD_EXPAND_FLAT, IOUtils.getJsonLdProfile(JSONLD.expanded));
        assertEquals(JSONLD_FLATTEN_FLAT, IOUtils.getJsonLdProfile(JSONLD.expanded_flattened));
        assertEquals(JSONLD_FLATTEN_FLAT, IOUtils.getJsonLdProfile(JSONLD.expanded, JSONLD.flattened));
        assertEquals(JSONLD_FLATTEN_FLAT, IOUtils.getJsonLdProfile(JSONLD.flattened));
        assertEquals(JSONLD_FLATTEN_FLAT, IOUtils.getJsonLdProfile(JSONLD.compacted_flattened));
        assertEquals(JSONLD_FLATTEN_FLAT, IOUtils.getJsonLdProfile(JSONLD.compacted, JSONLD.flattened));
    }
}
