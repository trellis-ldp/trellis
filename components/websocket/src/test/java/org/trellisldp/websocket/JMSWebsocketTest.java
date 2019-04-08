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
package org.trellisldp.websocket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;

@TestInstance(PER_CLASS)
public class JMSWebsocketTest implements MessageListener {

    private static final Logger LOGGER = getLogger(JMSWebsocketTest.class);
    private static final BrokerService BROKER = new BrokerService();
    private static final String QUEUE = "testQueue";
    private static final List<String> MESSAGES = new ArrayList<>();

    private Connection connection;

    @BeforeAll
    public void setUp() throws Exception {
        BROKER.setPersistent(false);
        BROKER.start();
    }

    @AfterAll
    public void cleanup() throws Exception {
        BROKER.stop();
    }

    @BeforeEach
    public void acquireConnection() throws Exception {
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        connection = connectionFactory.createConnection();
        connection.start();
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination destination = session.createQueue(QUEUE);
        final MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);
    }

    @AfterEach
    public void closeConnection() throws Exception {
        connection.close();
    }

    @Override
    public void onMessage(final Message message) {
        try {
            final String body = ((TextMessage) message).getText();
            LOGGER.info("Body: {}", body);
            MESSAGES.add(body);
        } catch (final Exception ex) {
            LOGGER.error("Error processing message: {}", ex.getMessage());
        }
    }

    private void observes(final @Observes String update) {
        LOGGER.info("Observing event: {}", update);
    }

    @Test
    public void testMessage() throws Exception {
        final String text = "This is a message";
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Message message = session.createTextMessage(text);
        assertDoesNotThrow(() ->
                session.createProducer(session.createQueue(QUEUE)).send(message));
        await().atMost(15, SECONDS).until(() -> MESSAGES.contains(text));
    }
}
