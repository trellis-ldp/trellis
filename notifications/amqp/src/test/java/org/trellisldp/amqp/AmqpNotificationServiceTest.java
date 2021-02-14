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
package org.trellisldp.amqp;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;
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
 * @author acoburn
 */
@ExtendWith(MockitoExtension.class)
class AmqpNotificationServiceTest {

    private static final RDF rdf = new SimpleRDF();
    private static final SystemLauncher broker = new SystemLauncher();
    private static final NotificationSerializationService serializer = new DefaultNotificationSerializationService();

    private final String exchangeName = "exchange";
    private final String queueName = "queue";
    private final Instant time = now();

    @Mock
    private Channel mockChannel;

    @Mock
    private Notification mockNotification;

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

    @Test
    void testAmqp() throws IOException {
        when(mockNotification.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockNotification.getCreated()).thenReturn(time);
        when(mockNotification.getIdentifier()).thenReturn(rdf.createIRI("urn:amqp:test"));
        when(mockNotification.getTypes()).thenReturn(singleton(AS.Update));
        when(mockNotification.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")));
        when(mockNotification.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));
        doNothing().when(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));

        final AmqpNotificationService svc = new AmqpNotificationService();
        svc.serializer = serializer;
        svc.channel = mockChannel;
        svc.exchangeName = exchangeName;
        svc.routingKey = queueName;
        svc.emit(mockNotification);

        verify(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }

    @Test
    void testAmqpConfiguration() throws IOException {
        when(mockNotification.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockNotification.getCreated()).thenReturn(time);
        when(mockNotification.getIdentifier()).thenReturn(rdf.createIRI("urn:amqp:test"));
        when(mockNotification.getTypes()).thenReturn(singleton(AS.Update));
        when(mockNotification.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")));
        when(mockNotification.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));
        doNothing().when(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));

        final AmqpNotificationService svc = new AmqpNotificationService();
        svc.serializer = serializer;
        svc.channel = mockChannel;
        svc.exchangeName = exchangeName;
        svc.routingKey = queueName;
        svc.emit(mockNotification);

        verify(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }

    @Test
    void testError() throws IOException {
        when(mockNotification.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockNotification.getCreated()).thenReturn(time);
        when(mockNotification.getIdentifier()).thenReturn(rdf.createIRI("urn:amqp:test"));
        when(mockNotification.getTypes()).thenReturn(singleton(AS.Update));
        when(mockNotification.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")));
        when(mockNotification.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));

        doThrow(IOException.class).when(mockChannel).basicPublish(eq(exchangeName), eq(queueName),
                anyBoolean(), anyBoolean(), any(BasicProperties.class), any(byte[].class));

        final AmqpNotificationService svc = new AmqpNotificationService();
        svc.serializer = serializer;
        svc.channel = mockChannel;
        svc.exchangeName = exchangeName;
        svc.routingKey = queueName;
        svc.emit(mockNotification);

        verify(mockChannel).basicPublish(eq(exchangeName), eq(queueName), anyBoolean(), anyBoolean(),
                any(BasicProperties.class), any(byte[].class));
    }
}
