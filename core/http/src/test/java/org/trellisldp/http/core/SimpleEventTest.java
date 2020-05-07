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
package org.trellisldp.http.core;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Collection;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.Event;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.SKOS;

/**
 * @author acoburn
 */
class SimpleEventTest {

    private static final RDF rdf = RDFFactory.getInstance();

    private final String identifier = "trellis:data/resource";
    private final IRI agent = rdf.createIRI("http://example.org/agent");

    @Test
    void testSimpleEvent() {
        final IRI resource = rdf.createIRI(identifier);
        final Instant time = now();

        final Event event = new SimpleEvent(identifier, agent,
                asList(PROV.Activity, AS.Create), asList(LDP.RDFSource, SKOS.Concept));
        assertFalse(time.isAfter(event.getCreated()), "Non-sequential events!");
        assertTrue(event.getIdentifier().getIRIString().startsWith("urn:uuid:"), "Incorrect ID prefix for event!");
        assertEquals(of(resource), event.getObject(), "Incorrect target resource!");
        assertEquals(1L, event.getAgents().size(), "Incorrect agent count!");
        assertTrue(event.getAgents().contains(agent), "Incorrect agent value!");
        final Collection<IRI> targetTypes = event.getObjectTypes();
        assertEquals(2L, targetTypes.size(), "Incorrect target type size!");
        assertTrue(targetTypes.contains(LDP.RDFSource), "Missing ldp:RDFSource type!");
        assertTrue(targetTypes.contains(SKOS.Concept), "Missing skos:Concept type!");
        final Collection<IRI> eventTypes = event.getTypes();
        assertEquals(2L, eventTypes.size(), "Incorrect event type size!");
        assertTrue(eventTypes.contains(AS.Create), "Missing as:Create from event type!");
        assertTrue(eventTypes.contains(PROV.Activity), "Missing prov:Activity from event type!");
        assertFalse(event.getInbox().isPresent());
    }

    @Test
    void testEmptyEvent() {
        final IRI resource = rdf.createIRI(identifier);

        final Event event = new SimpleEvent(identifier, agent, emptyList(), emptyList());
        assertEquals(of(resource), event.getObject(), "Incorrect target resource!");
        assertTrue(event.getAgents().contains(agent), "Unexpected agent list!");
        assertTrue(event.getObjectTypes().isEmpty(), "Unexpected target types!");
        assertTrue(event.getTypes().isEmpty(), "Unexpected event types!");
        assertFalse(event.getInbox().isPresent());
    }
}
