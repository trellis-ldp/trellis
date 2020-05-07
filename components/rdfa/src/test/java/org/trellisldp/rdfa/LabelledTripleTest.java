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
package org.trellisldp.rdfa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.DC;

/**
 * @author acoburn
 */
class LabelledTripleTest {

    private static final RDF rdf = new JenaRDF();

    @Test
    void testOrdinaryLabelledTriple() {
        final Triple triple = rdf.createTriple(
                rdf.createIRI("test:value"), DC.title, rdf.createLiteral("A title"));
        final LabelledTriple t = new LabelledTriple(triple, "title", null);
        assertEquals("title", t.getPredicateLabel(), "Predicate label doesn't match!");
        assertEquals(DC.title.getIRIString(), t.getPredicate(), "Predicate value doesn't match!");
        assertEquals("A title", t.getObjectLabel(), "Object label doesn't match!");
        assertEquals("A title", t.getObject(), "Object value doesn't match!");
    }

    @Test
    void testOrdinaryLabelledTriple2() {
        final Triple triple = rdf.createTriple(
                rdf.createIRI("test:value"), DC.title, rdf.createLiteral("A title"));
        final LabelledTriple t = new LabelledTriple(triple, null, null);
        assertEquals(DC.title.getIRIString(), t.getPredicateLabel(), "Predicate label doesn't match!");
        assertEquals(DC.title.getIRIString(), t.getPredicate(), "Predicate value doesn't match!");
        assertEquals("A title", t.getObjectLabel(), "Object label doesn't match!");
        assertEquals("A title", t.getObject(), "Object value doesn't match!");
    }

    @Test
    void testOrdinaryLabelledTriple3() {
        final Triple triple = rdf.createTriple(
                rdf.createIRI("test:value"), DC.title, rdf.createLiteral("A title"));
        final LabelledTriple t = new LabelledTriple(triple, null, null);
        assertEquals("test:value", t.getSubject(), "Subject value doesn't match!");
        assertEquals(DC.title.getIRIString(), t.getPredicateLabel(), "Predicate label doesn't match!");
        assertEquals(DC.title.getIRIString(), t.getPredicate(), "Predicate value doesn't match!");
        assertEquals("A title", t.getObjectLabel(), "Object label doesn't match!");
        assertEquals("A title", t.getObject(), "Object value doesn't match!");
    }

    @Test
    void testBnodes() {
        final BlankNode bn1 = rdf.createBlankNode();
        final BlankNode bn2 = rdf.createBlankNode();
        final Triple triple = rdf.createTriple(bn1, DC.subject, bn2);
        final LabelledTriple t = new LabelledTriple(triple, null, null);
        assertEquals(bn1.ntriplesString(), t.getSubject(), "Subject bnode value doesn't match!");
        assertEquals(bn2.ntriplesString(), t.getObject(), "Object bnode value doesn't match!");
        assertEquals(bn2.ntriplesString(), t.getObjectLabel(), "Object bnode label doesn't match!");
    }
}
