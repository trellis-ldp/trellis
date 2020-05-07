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
package org.trellisldp.kafka;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.Instant;
import java.util.List;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventSerializationService;
import org.trellisldp.api.EventService;
import org.trellisldp.event.jackson.DefaultEventSerializationService;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Test the Kafka publisher
 */
class KafkaEventServiceTest {

    private static final RDF rdf = new SimpleRDF();
    private static final EventSerializationService serializer = new DefaultEventSerializationService();

    private final String queueName = "queue";

    private final Instant time = now();

    private final MockProducer<String, String> producer = new MockProducer<>(true, new StringSerializer(),
            new StringSerializer());

    @Mock
    private Event mockEvent;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(mockEvent.getObject()).thenReturn(of(rdf.createIRI("trellis:data/resource")));
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:test"));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
    }

    @Test
    void testKafka() {
        final EventService svc = new KafkaEventService(serializer, producer);
        svc.emit(mockEvent);

        final List<ProducerRecord<String, String>> records = producer.history();
        assertEquals(1L, records.size(), "Incorrect total records size!");
        assertEquals(1L, records.stream().filter(r -> r.topic().equals(queueName)).count(), "Incorrect filtered size!");
    }

    @Test
    void testNoargCtor() {
        final EventService svc = new KafkaEventService();
        assertDoesNotThrow(() -> svc.emit(mockEvent));
    }

    @Test
    void testDefaultKafka() {
        assertDoesNotThrow(() -> new KafkaEventService(serializer));
    }
}
