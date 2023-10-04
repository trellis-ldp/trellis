/*
 * Copyright (c) Aaron Coburn and individual contributors
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
package org.trellisldp.common;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.Notification;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.SKOS;

/**
 * @author acoburn
 */
class SimpleNotificationTest {

    private static final RDF rdf = RDFFactory.getInstance();

    private final String identifier = "trellis:data/resource";
    private final IRI agent = rdf.createIRI("http://example.org/agent");

    @Test
    void testSimpleNotification() {
        final IRI resource = rdf.createIRI(identifier);
        final Instant time = now();
        final String state = "etag:123456";

        final Notification notification = new SimpleNotification(identifier, agent,
                List.of(PROV.Activity, AS.Create), List.of(LDP.RDFSource, SKOS.Concept), state);
        assertFalse(time.isAfter(notification.getCreated()), "Non-sequential notifications!");
        assertTrue(notification.getIdentifier().getIRIString()
                .startsWith("urn:uuid:"), "Incorrect ID prefix for notification!");
        assertEquals(of(resource), notification.getObject(), "Incorrect target resource!");
        assertEquals(1L, notification.getAgents().size(), "Incorrect agent count!");
        assertTrue(notification.getAgents().contains(agent), "Incorrect agent value!");
        final Collection<IRI> targetTypes = notification.getObjectTypes();
        assertEquals(2L, targetTypes.size(), "Incorrect target type size!");
        assertTrue(targetTypes.contains(LDP.RDFSource), "Missing ldp:RDFSource type!");
        assertTrue(targetTypes.contains(SKOS.Concept), "Missing skos:Concept type!");
        final Collection<IRI> notificationTypes = notification.getTypes();
        assertEquals(2L, notificationTypes.size(), "Incorrect notification type size!");
        assertTrue(notificationTypes.contains(AS.Create), "Missing as:Create from notification type!");
        assertTrue(notificationTypes.contains(PROV.Activity), "Missing prov:Activity from notification type!");
        assertEquals(of(state), notification.getObjectState());
    }

    @Test
    void testEmptyNotification() {
        final IRI resource = rdf.createIRI(identifier);

        final Notification notification = new SimpleNotification(identifier, agent, emptyList(), emptyList(), null);
        assertEquals(of(resource), notification.getObject(), "Incorrect target resource!");
        assertTrue(notification.getAgents().contains(agent), "Unexpected agent list!");
        assertTrue(notification.getObjectTypes().isEmpty(), "Unexpected target types!");
        assertTrue(notification.getTypes().isEmpty(), "Unexpected notification types!");
    }
}
