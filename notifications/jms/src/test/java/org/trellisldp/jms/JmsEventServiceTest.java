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
package org.trellisldp.jms;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import java.time.Instant;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventSerializationService;
import org.trellisldp.event.jackson.DefaultEventSerializationService;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
@ExtendWith(MockitoExtension.class)
class JmsEventServiceTest {

    private static final RDF rdf = new SimpleRDF();
    private static final BrokerService BROKER = new BrokerService();
    private static final EventSerializationService serializer = new DefaultEventSerializationService();

    private final String queueName = "queue";

    private final Instant time = now();

    @Mock
    private Connection mockConnection;

    @Mock
    private Session mockSession;

    @Mock
    private Event mockEvent;

    @Mock
    private Queue mockQueue;

    @Mock
    private Topic mockTopic;

    @Mock
    private TextMessage mockMessage;

    @Mock
    private MessageProducer mockProducer, mockTopicProducer;

    @BeforeAll
    static void initialize() throws Exception {
        BROKER.setPersistent(false);
        BROKER.start();
    }

    @AfterAll
    static void cleanUp() throws Exception {
        BROKER.stop();
    }

    @Test
    void testJms() throws JMSException {
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:jms:test"));
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "a-resource")));
        when(mockEvent.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
        when(mockSession.createQueue(queueName)).thenReturn(mockQueue);
        when(mockSession.createTextMessage(anyString())).thenReturn(mockMessage);
        when(mockSession.createProducer(any(Queue.class))).thenReturn(mockProducer);
        doNothing().when(mockProducer).send(any(TextMessage.class));

        final JmsEventService svc = new JmsEventService();
        svc.serializer = serializer;
        svc.session = mockSession;
        svc.useQueue = true;
        svc.queueName = queueName;
        svc.emit(mockEvent);

        verify(mockProducer).send(mockMessage);
    }

    @Test
    void testQueue() throws JMSException {
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:jms:test"));
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "a-resource")));
        when(mockEvent.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
        when(mockSession.createQueue(queueName)).thenReturn(mockQueue);
        when(mockSession.createTextMessage(anyString())).thenReturn(mockMessage);
        when(mockSession.createProducer(any(Queue.class))).thenReturn(mockProducer);
        doNothing().when(mockProducer).send(any(TextMessage.class));

        final JmsEventService svc = new JmsEventService();
        svc.serializer = serializer;
        svc.session = mockSession;
        svc.useQueue = true;
        svc.queueName = queueName;
        svc.emit(mockEvent);

        verify(mockProducer).send(mockMessage);
        verify(mockTopicProducer, never()).send(mockMessage);
    }

    @Test
    void testTopic() throws JMSException {
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:jms:test"));
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "a-resource")));
        when(mockEvent.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
        when(mockSession.createTopic(queueName)).thenReturn(mockTopic);
        when(mockSession.createTextMessage(anyString())).thenReturn(mockMessage);
        when(mockSession.createProducer(any(Topic.class))).thenReturn(mockTopicProducer);

        final JmsEventService svc = new JmsEventService();
        svc.serializer = serializer;
        svc.session = mockSession;
        svc.useQueue = false;
        svc.queueName = queueName;
        svc.emit(mockEvent);

        verify(mockTopicProducer).send(mockMessage);
        verify(mockProducer, never()).send(mockMessage);
    }

    @Test
    void testError() throws JMSException {
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:jms:test"));
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "a-resource")));
        when(mockEvent.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
        when(mockSession.createQueue(queueName)).thenReturn(mockQueue);
        when(mockSession.createTextMessage(anyString())).thenReturn(mockMessage);
        when(mockSession.createProducer(any(Queue.class))).thenReturn(mockProducer);

        doThrow(JMSException.class).when(mockProducer).send(mockMessage);

        final JmsEventService svc = new JmsEventService();
        svc.serializer = serializer;
        svc.session = mockSession;
        svc.useQueue = true;
        svc.queueName = queueName;
        svc.emit(mockEvent);

        verify(mockProducer).send(mockMessage);
    }
}
