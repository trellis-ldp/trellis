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
package org.trellisldp.amqp;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.apache.qpid.server.SystemLauncher;
import org.apache.qpid.server.model.SystemConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
 * @author acoburn
 */
class AmqpEventServiceTest {

    private static final RDF rdf = new SimpleRDF();
    private static final SystemLauncher broker = new SystemLauncher();
    private static final EventSerializationService serializer = new DefaultEventSerializationService();

    private final String exchangeName = "exchange";
    private final String queueName = "queue";
    private final Instant time = now();

    @Mock
    private Channel mockChannel;

    @Mock
    private Event mockEvent;

    @BeforeAll
    static void initialize() throws Exception {
        final Map<String, Object> brokerOptions = new HashMap<>();
        brokerOptions.put("qpid.broker.defaultPreferenceStoreAttributes", "{\"type\": \"Noop\"}");
        brokerOptions.put(SystemConfig.TYPE, "Memory");
        brokerOptions.put(SystemConfig.STARTUP_LOGGED_TO_SYSTEM_OUT, true);
        brokerOptions.put(SystemConfig.INITIAL_CONFIGURATION_LOCATION, SystemConfig.DEFAULT_INITIAL_CONFIG_LOCATION);
        broker.startup(brokerOptions);
    }

    @AfterAll
    static void cleanup() {
        broker.shutdown();
    }

    @BeforeEach
    void setUp() throws IOException {
        initMocks(this);
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:amqp:test"));
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")));
        when(mockEvent.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
        doNothing().when(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }

    @Test
    void testAmqp() throws IOException {
        final EventService svc = new AmqpEventService(serializer, mockChannel, exchangeName, queueName);
        svc.emit(mockEvent);

        verify(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }

    @Test
    void testAmqpConfiguration() throws IOException {
        final EventService svc = new AmqpEventService(serializer, mockChannel);
        svc.emit(mockEvent);

        verify(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }

    @Test
    void testError() throws IOException {
        doThrow(IOException.class).when(mockChannel).basicPublish(eq(exchangeName), eq(queueName),
                anyBoolean(), anyBoolean(), any(BasicProperties.class), any(byte[].class));

        final EventService svc = new AmqpEventService(serializer, mockChannel, exchangeName, queueName, true, true);
        svc.emit(mockEvent);

        verify(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }

    @Test
    void testNoargCtor() {
        assertDoesNotThrow(() -> new AmqpEventService());
    }
}
