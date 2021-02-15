/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trellisldp.api.Notification;
import org.trellisldp.api.NotificationSerializationService;
import org.trellisldp.notification.jackson.DefaultNotificationSerializationService;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Test the Kafka publisher
 */
@ExtendWith(MockitoExtension.class)
class KafkaNotificationServiceTest {

    private static final RDF rdf = new SimpleRDF();
    private static final NotificationSerializationService serializer = new DefaultNotificationSerializationService();

    private final String queueName = "queue";

    private final Instant time = now();

    private final MockProducer<String, String> producer = new MockProducer<>(true, new StringSerializer(),
            new StringSerializer());

    @Mock
    private Notification mockNotification;

    @Test
    void testKafka() {
        when(mockNotification.getObject()).thenReturn(of(rdf.createIRI("trellis:data/resource")));
        when(mockNotification.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockNotification.getIdentifier()).thenReturn(rdf.createIRI("urn:test"));
        when(mockNotification.getCreated()).thenReturn(time);
        when(mockNotification.getTypes()).thenReturn(singleton(AS.Update));
        when(mockNotification.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));

        final KafkaNotificationService svc = new KafkaNotificationService();
        svc.serializer = serializer;
        svc.producer = producer;
        svc.topic = queueName;
        svc.emit(mockNotification);

        final List<ProducerRecord<String, String>> records = producer.history();
        assertEquals(1L, records.size(), "Incorrect total records size!");
        assertEquals(1L, records.stream().filter(r -> r.topic().equals(queueName)).count(), "Incorrect filtered size!");
    }
}
