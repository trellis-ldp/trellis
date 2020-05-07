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
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.SKOS;

/**
 * ResourceService tests.
 */
class TriplestoreUtilsTest {

    private static final RDF simpleRdf = new SimpleRDF();
    private static final RDF jenaRdf = new JenaRDF();

    private static final IRI subject = simpleRdf.createIRI("http://example.com");
    private static final Literal literal = simpleRdf.createLiteral("title");

    @Test
    void testDatasetNoConversion() {
        final Dataset dataset = jenaRdf.createDataset();

        dataset.add(jenaRdf.createQuad(PreferUserManaged, subject, SKOS.prefLabel, literal));
        dataset.add(jenaRdf.createQuad(PreferUserManaged, subject, type, SKOS.Concept));
        dataset.add(jenaRdf.createQuad(PreferUserManaged, subject, DC.subject, AS.Activity));
        assertEquals(3L, dataset.size(), "Confirm dataset size");

        assertTrue(TriplestoreUtils.asJenaDataset(dataset).containsNamedModel(PreferUserManaged.getIRIString()),
                "Confirm presence of trellis:PreferUserManaged named graph");
        assertEquals(TriplestoreUtils.asJenaDataset(dataset).asDatasetGraph(),
                TriplestoreUtils.asJenaDataset(dataset).asDatasetGraph(), "Confirm datasets are equal");
    }

    @Test
    void testDatasetConversion() {
        final Dataset dataset = simpleRdf.createDataset();

        dataset.add(simpleRdf.createQuad(PreferUserManaged, subject, SKOS.prefLabel, literal));
        dataset.add(simpleRdf.createQuad(PreferUserManaged, subject, type, SKOS.Concept));
        dataset.add(simpleRdf.createQuad(PreferUserManaged, subject, DC.subject, AS.Activity));
        assertEquals(3L, dataset.size(), "Confirm dataset size");

        assertTrue(TriplestoreUtils.asJenaDataset(dataset).containsNamedModel(PreferUserManaged.getIRIString()),
                "Confirm presence of trellis:PreferUserManaged named graph");
        assertNotEquals(TriplestoreUtils.asJenaDataset(dataset).asDatasetGraph(),
                TriplestoreUtils.asJenaDataset(dataset).asDatasetGraph(), "Confirm dataset has been converted");
    }

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
