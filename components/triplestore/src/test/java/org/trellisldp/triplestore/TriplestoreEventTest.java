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
package org.trellisldp.triplestore;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collection;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.Event;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
public class TriplestoreEventTest {

    private static final RDF rdf = new JenaRDF();

    private final String identifier = "trellis:repository/resource";
    private final IRI inbox = rdf.createIRI("http://example.org/resource");
    private final IRI agent = rdf.createIRI("http://example.org/agent");

    @Test
    public void testSimpleSession() {
        final Session s1 = new SimpleSession(Trellis.AdministratorAgent);
        final Session s2 = new SimpleSession(Trellis.AdministratorAgent);
        assertNotEquals(s1.getIdentifier(), s2.getIdentifier());
        assertEquals(s1.getAgent(), s2.getAgent());
        assertFalse(s1.getCreated().isAfter(s2.getCreated()));
        assertFalse(s1.getDelegatedBy().isPresent());
    }

    @Test
    public void testSimpleEvent() {
        final IRI resource = rdf.createIRI(identifier);
        final Instant time = now();

        final Event event = new SimpleEvent(identifier, asList(agent),
                asList(PROV.Activity, AS.Create), asList(LDP.RDFSource, SKOS.Concept), inbox);
        assertFalse(time.isAfter(event.getCreated()));
        assertTrue(event.getIdentifier().getIRIString().startsWith("urn:uuid:"));
        assertEquals(of(resource), event.getTarget());
        assertEquals(of(inbox), event.getInbox());
        assertEquals(1L, event.getAgents().size());
        assertTrue(event.getAgents().contains(agent));
        final Collection<IRI> targetTypes = event.getTargetTypes();
        assertEquals(2L, targetTypes.size());
        assertTrue(targetTypes.contains(LDP.RDFSource));
        assertTrue(targetTypes.contains(SKOS.Concept));
        final Collection<IRI> eventTypes = event.getTypes();
        assertEquals(2L, eventTypes.size());
        assertTrue(eventTypes.contains(AS.Create));
        assertTrue(eventTypes.contains(PROV.Activity));
    }

    @Test
    public void testEmptyEvent() {
        final IRI resource = rdf.createIRI(identifier);

        final Event event = new SimpleEvent(identifier, emptyList(), emptyList(), emptyList(), null);
        assertEquals(of(resource), event.getTarget());
        assertFalse(event.getInbox().isPresent());
        assertTrue(event.getAgents().isEmpty());
        assertTrue(event.getTargetTypes().isEmpty());
        assertTrue(event.getTypes().isEmpty());
    }
}
