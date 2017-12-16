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
package org.trellisldp.webac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.PROV;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class AuthorizationTest {

    private static final RDF rdf = new SimpleRDF();

    private final Graph graph = rdf.createGraph();

    private final IRI subject = rdf.createIRI("trellis:repository/resource");

    @BeforeEach
    public void setUp() {

        final IRI other = rdf.createIRI("trellis:repository/other");

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

        graph.add(rdf.createTriple(subject, ACL.accessTo, rdf.createIRI("trellis:repository/resource2")));
        graph.add(rdf.createTriple(subject, ACL.accessTo, rdf.createIRI("trellis:repository/resource3")));
        graph.add(rdf.createTriple(subject, ACL.accessTo, rdf.createIRI("trellis:repository/resource4")));
        graph.add(rdf.createTriple(subject, ACL.accessTo, rdf.createIRI("trellis:repository/resource4")));
        graph.add(rdf.createTriple(other, ACL.accessTo, rdf.createIRI("trellis:repository/resource5")));

        graph.add(rdf.createTriple(subject, ACL.accessToClass, PROV.Activity));
        graph.add(rdf.createTriple(other, ACL.accessToClass, PROV.Entity));

        graph.add(rdf.createTriple(subject, ACL.default_, rdf.createIRI("trellis:repository/container")));
    }

    @Test
    public void testGraph() {
        final Authorization auth = Authorization.from(subject, graph);

        assertEquals(subject, auth.getIdentifier());

        assertEquals(2, auth.getAgent().size());
        assertTrue(auth.getAgent().contains(rdf.createIRI("info:agent/foo")));
        assertTrue(auth.getAgent().contains(rdf.createIRI("info:agent/bar")));

        assertEquals(1, auth.getAgentClass().size());
        assertTrue(auth.getAgentClass().contains(rdf.createIRI("info:agent/SomeClass")));

        assertEquals(4, auth.getAgentGroup().size());
        assertTrue(auth.getAgentGroup().contains(rdf.createIRI("info:group/group1")));

        assertEquals(1, auth.getMode().size());
        assertTrue(auth.getMode().contains(ACL.Read));

        assertEquals(3, auth.getAccessTo().size());
        assertTrue(auth.getAccessTo().contains(rdf.createIRI("trellis:repository/resource2")));
        assertTrue(auth.getAccessTo().contains(rdf.createIRI("trellis:repository/resource3")));
        assertTrue(auth.getAccessTo().contains(rdf.createIRI("trellis:repository/resource4")));

        assertEquals(1, auth.getDefault().size());
        assertTrue(auth.getDefault().contains(rdf.createIRI("trellis:repository/container")));
    }
}
