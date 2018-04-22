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
package org.trellisldp.jms;

import static java.util.Objects.requireNonNull;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ServiceLoader;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;

/**
 * A JMS message producer capable of publishing messages to a JMS broker such as ActiveMQ.
 *
 * @author acoburn
 */
public class JmsPublisher implements EventService {

    public static final String JMS_QUEUE_NAME = "trellis.jms.queue";

    private static final Logger LOGGER = getLogger(JmsPublisher.class);

    // TODO - JDK9 ServiceLoader::findFirst
    private static ActivityStreamService service = ServiceLoader.load(ActivityStreamService.class).iterator().next();

    private final MessageProducer producer;

    private final Session session;

    /**
     * Create a new JMS Publisher.
     * @param conn the connection
     * @throws JMSException when there is a connection error
     */
    @Inject
    public JmsPublisher(final Connection conn) throws JMSException {
        this(conn.createSession(false, AUTO_ACKNOWLEDGE),
                ConfigurationProvider.getConfiguration().get(JMS_QUEUE_NAME));
    }

    /**
     * Create a new JMS Publisher.
     * @param factory a connetion factory
     * @param queueName the name of the queue
     * @throws JMSException when there is a connection error
     */
    public JmsPublisher(final ConnectionFactory factory, final String queueName) throws JMSException {
        this(factory.createConnection().createSession(false, AUTO_ACKNOWLEDGE), queueName);
    }

    /**
     * Create a new JMS Publisher.
     * @param session the JMS session
     * @param queueName the name of the queue
     * @throws JMSException when there is a connection error
     */
    public JmsPublisher(final Session session, final String queueName) throws JMSException {
        requireNonNull(session, "JMS Session may not be null!");
        requireNonNull(queueName, "JMS Queue name may not be null!");

        this.session = session;
        this.producer = session.createProducer(session.createQueue(queueName));
    }

    @Override
    public void emit(final Event event) {
        requireNonNull(event, "Cannot emit a null event!");

        service.serialize(event).ifPresent(json -> {
            try {
                final Message message = session.createTextMessage(json);
                message.setStringProperty("Content-Type", "application/ld+json");
                producer.send(message);
            } catch (final JMSException ex) {
                LOGGER.error("Error writing to broker: {}", ex.getMessage());
            }
        });
    }
}
