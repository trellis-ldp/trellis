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
package org.trellisldp.app.triplestore;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;

import java.net.ServerSocket;
import java.util.Properties;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.EventService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.dropwizard.config.NotificationsConfiguration;
import org.trellisldp.kafka.KafkaEventService;

/**
 * @author acoburn
 */
class AppUtilsTest {

    @Mock
    private Environment mockEnv;

    @Mock
    private LifecycleEnvironment mockLifecycle;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(mockEnv.lifecycle()).thenReturn(mockLifecycle);
    }

    @Test
    void testEventServiceNone() {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.setConnectionString("localhost");
        c.setEnabled(true);
        c.setType(NotificationsConfiguration.Type.NONE);
        final EventService svc = AppUtils.getNotificationService(c, mockEnv);
        assertNotNull(svc, "Missing EventService!");
        assertTrue(svc instanceof NoopEventService, "EventService isn't a NoopEvenService!");
    }

    @Test
    void testEventServiceDisabled() {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.set("batch.size", "1000");
        c.set("retries", "10");
        c.set("key.serializer", "some.bogus.key.serializer");
        c.setConnectionString("localhost:9092");
        c.setEnabled(false);
        c.setType(NotificationsConfiguration.Type.KAFKA);
        final EventService svc = AppUtils.getNotificationService(c, mockEnv);
        assertNotNull(svc, "Missing EventService!");
        assertTrue(svc instanceof NoopEventService, "EventService didn't default to No-op service!");
    }

    @Test
    void testEventServiceKafka() {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.set("batch.size", "1000");
        c.set("retries", "10");
        c.set("key.serializer", "some.bogus.key.serializer");
        c.setConnectionString("localhost:9092");
        c.setEnabled(true);
        c.setType(NotificationsConfiguration.Type.KAFKA);
        final EventService svc = AppUtils.getNotificationService(c, mockEnv);
        assertNotNull(svc, "Missing EventService!");
        assertTrue(svc instanceof KafkaEventService, "EventService isn't a KafkaEventService!");
    }

    @Test
    void testGetKafkaProps() {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.set("batch.size", "1000");
        c.set("retries", "10");
        c.set("some.other", "value");
        c.set("key.serializer", "some.bogus.key.serializer");
        c.setConnectionString("localhost:9092");
        final Properties p = AppUtils.getKafkaProperties(c);
        assertEquals("all", p.getProperty("acks"), "Incorrect kafka acks property!");
        assertEquals("value", p.getProperty("some.other"), "Incorrect custom property!");
        assertEquals("org.apache.kafka.common.serialization.StringSerializer", p.getProperty("key.serializer"),
                "Incorrect serializer class property!");
        assertEquals("localhost:9092", p.getProperty("bootstrap.servers"), "Incorrect bootstrap.servers property!");
    }

    @Test
    void testEventServiceJms() throws Exception {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        final int port = new ServerSocket(0).getLocalPort();
        c.setConnectionString("tcp://localhost:" + port);
        c.setEnabled(true);
        c.setType(NotificationsConfiguration.Type.JMS);
        assertThrows(RuntimeTrellisException.class, () ->
                AppUtils.getNotificationService(c, mockEnv), "No exception when JMS client doesn't connect!");
    }

    @Test
    void testGetJmsFactory() {
        final NotificationsConfiguration c = new NotificationsConfiguration();
        c.setConnectionString("localhost:61616");

        final ActiveMQConnectionFactory factory1 = AppUtils.getJmsFactory(c);
        assertNull(factory1.getUserName(), "Unexpected username!");
        assertNull(factory1.getPassword(), "Unexpected password!");
        assertEquals("localhost:61616", factory1.getBrokerURL(), "Incorrect broker URL!");

        c.set("password", "pass");
        final ActiveMQConnectionFactory factory2 = AppUtils.getJmsFactory(c);
        assertNull(factory2.getUserName(), "Unexpected username!");
        assertNull(factory2.getPassword(), "Unexpected password!");
        assertEquals("localhost:61616", factory2.getBrokerURL(), "Incorrect broker URL!");

        c.set("username", "user");
        final ActiveMQConnectionFactory factory3 = AppUtils.getJmsFactory(c);
        assertEquals("user", factory3.getUserName(), "Incorrect username!");
        assertEquals("pass", factory3.getPassword(), "Incorrect password!");
        assertEquals("localhost:61616", factory3.getBrokerURL(), "Incorrect broker URL!");
    }
}
