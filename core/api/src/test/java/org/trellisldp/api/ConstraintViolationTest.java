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
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.vocabulary.Trellis.InvalidProperty;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
class ConstraintViolationTest {

    private static final RDF rdf = new SimpleRDF();
    private static final Triple triple = rdf.createTriple(rdf.createIRI("ex:subject"), LDP.contains,
            rdf.createIRI("ex:object"));
    private static final Triple triple2 = rdf.createTriple(rdf.createIRI("ex:subject"), LDP.contains,
            rdf.createIRI("ex:object2"));

    @Test
    void testSingleConstraint() {
        final ConstraintViolation violation = new ConstraintViolation(InvalidProperty, triple);

        assertEquals(InvalidProperty, violation.getConstraint(), "Incorrect constraint IRI");
        assertTrue(violation.getTriples().contains(triple), "Problematic triple not found");
        assertEquals(1L, violation.getTriples().size(), "Incorrect triple count");
    }

    @Test
    void testMultipleConstraint() {
        final ConstraintViolation violation = new ConstraintViolation(InvalidProperty, asList(triple, triple2));

        assertEquals(InvalidProperty, violation.getConstraint(), "Incorrect constraint IRI");
        assertTrue(violation.getTriples().contains(triple), "Problematic triple (1) not found");
        assertTrue(violation.getTriples().contains(triple2), "Problematic triple (2) not found");
        assertEquals(2L, violation.getTriples().size(), "Incorrect triple count");
    }

    @Test
    void testToString() {
        final ConstraintViolation violation = new ConstraintViolation(InvalidProperty, triple);
        assertEquals("http://www.trellisldp.org/ns/trellis#InvalidProperty: " +
                "[<ex:subject> <http://www.w3.org/ns/ldp#contains> <ex:object> .]", violation.toString(),
                "Unexpected serialization of constraint violation");
    }
}
