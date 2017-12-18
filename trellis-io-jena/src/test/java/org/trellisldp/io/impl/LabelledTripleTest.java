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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.vocabulary.DC;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class LabelledTripleTest {

    private static final RDF rdf = new JenaRDF();

    @Test
    public void testOrdinaryLabelledTriple() {
        final Triple triple = rdf.createTriple(
                rdf.createIRI("test:value"), DC.title, rdf.createLiteral("A title"));
        final LabelledTriple t = new LabelledTriple(triple, "title", null);
        assertEquals("title", t.getPredicateLabel());
        assertEquals(DC.title.getIRIString(), t.getPredicate());
        assertEquals("A title", t.getObjectLabel());
        assertEquals("A title", t.getObject());
    }

    @Test
    public void testOrdinaryLabelledTriple2() {
        final Triple triple = rdf.createTriple(
                rdf.createIRI("test:value"), DC.title, rdf.createLiteral("A title"));
        final LabelledTriple t = new LabelledTriple(triple, null, null);
        assertEquals(DC.title.getIRIString(), t.getPredicateLabel());
        assertEquals(DC.title.getIRIString(), t.getPredicate());
        assertEquals("A title", t.getObjectLabel());
        assertEquals("A title", t.getObject());
    }

    @Test
    public void testOrdinaryLabelledTriple3() {
        final Triple triple = rdf.createTriple(
                rdf.createIRI("test:value"), DC.title, rdf.createLiteral("A title"));
        final LabelledTriple t = new LabelledTriple(triple, null, null);
        assertEquals("test:value", t.getSubject());
        assertEquals(DC.title.getIRIString(), t.getPredicateLabel());
        assertEquals(DC.title.getIRIString(), t.getPredicate());
        assertEquals("A title", t.getObjectLabel());
        assertEquals("A title", t.getObject());
    }

    @Test
    public void testBnodes() {
        final BlankNode bn1 = rdf.createBlankNode();
        final BlankNode bn2 = rdf.createBlankNode();
        final Triple triple = rdf.createTriple(bn1, DC.subject, bn2);
        final LabelledTriple t = new LabelledTriple(triple, null, null);
        assertEquals(bn1.ntriplesString(), t.getSubject());
        assertEquals(bn2.ntriplesString(), t.getObject());
        assertEquals(bn2.ntriplesString(), t.getObjectLabel());
    }
}
