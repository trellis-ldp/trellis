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
package org.trellisldp.webac;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.PROV;

/**
 * @author acoburn
 */
class AuthorizationTest {

    private static final RDF rdf = new SimpleRDF();

    private final Graph graph = rdf.createGraph();

    private final IRI subject = rdf.createIRI("trellis:data/resource");

    @BeforeEach
    void setUp() {

        final IRI other = rdf.createIRI("trellis:data/other");

        graph.clear();

        graph.add(rdf.createTriple(subject, ACL.agent, rdf.createIRI("info:agent/foo")));
        graph.add(rdf.createTriple(subject, ACL.agent, rdf.createIRI("info:agent/bar")));
        graph.add(rdf.createTriple(other, ACL.agent, rdf.createIRI("info:agent/baz")));

        graph.add(rdf.createTriple(subject, ACL.agentClass, rdf.createIRI("info:agent/SomeClass")));
        graph.add(rdf.createTriple(other, ACL.agentClass, rdf.createIRI("info:agent/SomeOtherClass")));

        graph.add(rdf.createTriple(subject, ACL.agentGroup, rdf.createIRI("info:group/group1")));
        graph.add(rdf.createTriple(subject, ACL.agentGroup, rdf.createIRI("info:group/group2")));
        graph.add(rdf.createTriple(subject, ACL.agentGroup, rdf.createIRI("info:group/group3")));
        graph.add(rdf.createTriple(subject, ACL.agentGroup, rdf.createIRI("info:group/group4")));

        graph.add(rdf.createTriple(subject, ACL.mode, ACL.Read));

        graph.add(rdf.createTriple(subject, ACL.accessTo, rdf.createIRI("trellis:data/resource2")));
        graph.add(rdf.createTriple(subject, ACL.accessTo, rdf.createIRI("trellis:data/resource3")));
        graph.add(rdf.createTriple(subject, ACL.accessTo, rdf.createIRI("trellis:data/resource4")));
        graph.add(rdf.createTriple(subject, ACL.accessTo, rdf.createIRI("trellis:data/resource4")));
        graph.add(rdf.createTriple(other, ACL.accessTo, rdf.createIRI("trellis:data/resource5")));

        graph.add(rdf.createTriple(subject, ACL.accessToClass, PROV.Activity));
        graph.add(rdf.createTriple(other, ACL.accessToClass, PROV.Entity));

        graph.add(rdf.createTriple(subject, ACL.default_, rdf.createIRI("trellis:data/container")));
    }

    @Test
    void testGraph() {
        final Authorization auth = Authorization.from(subject, graph);

        assertEquals(subject, auth.getIdentifier(), "Incorrect identifier!");
        assertEquals(2, auth.getAgent().size(), "Incorrect number of agents!");
        assertTrue(auth.getAgent().contains(rdf.createIRI("info:agent/foo")), "Expected agent missing!");
        assertTrue(auth.getAgent().contains(rdf.createIRI("info:agent/bar")), "Expected agent missing!");

        assertEquals(1, auth.getAgentClass().size(), "Incorrect number of agentClasses!");
        assertTrue(auth.getAgentClass().contains(rdf.createIRI("info:agent/SomeClass")), "agentClass value missing!");

        assertEquals(4, auth.getAgentGroup().size(), "Incorrect number of agentGroups!");
        assertTrue(auth.getAgentGroup().contains(rdf.createIRI("info:group/group1")), "agentGroup value missing!");

        assertEquals(1, auth.getMode().size(), "Incorrect number of modes!");
        assertTrue(auth.getMode().contains(ACL.Read), "Read mode missing!");

        assertEquals(3, auth.getAccessTo().size(), "Incorrect number of accessTo values!");
        assertTrue(auth.getAccessTo().contains(rdf.createIRI("trellis:data/resource2")), "missing accessTo value!");
        assertTrue(auth.getAccessTo().contains(rdf.createIRI("trellis:data/resource3")), "missing accessTo value!");
        assertTrue(auth.getAccessTo().contains(rdf.createIRI("trellis:data/resource4")), "missing accessTo value!");

        assertEquals(1, auth.getDefault().size(), "Incorrect number of default values!");
        assertTrue(auth.getDefault().contains(rdf.createIRI("trellis:data/container")), "missing default value!");
    }

    @Test
    void testNormalizeIdentifier() {
        final IRI iri = rdf.createIRI("trellis:data/resource");
        final IRI iriSlash = rdf.createIRI("trellis:data/resource/");
        final IRI root = rdf.createIRI("trellis:data/");
        assertEquals(iri, Authorization.normalizeIdentifier(rdf.createTriple(subject, ACL.accessTo, iri)));
        assertEquals(iri, Authorization.normalizeIdentifier(rdf.createTriple(subject, ACL.accessTo, iriSlash)));
        assertEquals(root, Authorization.normalizeIdentifier(rdf.createTriple(subject, ACL.accessTo, root)));
        assertNull(Authorization.normalizeIdentifier(rdf.createTriple(subject, ACL.accessTo, rdf.createBlankNode())));
    }
}
