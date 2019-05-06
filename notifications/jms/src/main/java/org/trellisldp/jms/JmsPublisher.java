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
import static java.util.ServiceLoader.load;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * A JMS message producer capable of publishing messages to a JMS broker such as ActiveMQ.
 *
 * @author acoburn
 */
public class JmsPublisher implements EventService {

    /** The configuration key controlling the JMS queue name. **/
    public static final String CONFIG_JMS_QUEUE_NAME = "trellis.jms.queue";

    /** The configuration key controlling the JMS broker URL. **/
    public static final String CONFIG_JMS_URL = "trellis.jms.url";

    /** The configuration key controlling the JMS username. **/
    public static final String CONFIG_JMS_USERNAME = "trellis.jms.username";

    /** The configuration key controlling the JMS password. **/
    public static final String CONFIG_JMS_PASSWORD = "trellis.jms.password";

    /** The configuration key controlling whether to use a topic or queue. **/
    public static final String CONFIG_JMS_USE_QUEUE = "trellis.jms.use.queue";

    private static final Logger LOGGER = getLogger(JmsPublisher.class);
    private static final ActivityStreamService service = getService(ActivityStreamService.class);

    private final MessageProducer producer;
    private final Session session;

    /**
     * Create a new JMS Publisher.
     * @throws JMSException when there is a connection error
     */
    @Inject
    public JmsPublisher() throws JMSException {
        this(getConfig());
    }

    private JmsPublisher(final Config config) throws JMSException {
        this(buildJmsConnection(config).createSession(false, AUTO_ACKNOWLEDGE),
                config.getValue(CONFIG_JMS_QUEUE_NAME, String.class),
                config.getOptionalValue(CONFIG_JMS_USE_QUEUE, Boolean.class).orElse(Boolean.TRUE));
    }

    /**
     * Create a new JMS Publisher.
     * @param conn the connection
     * @throws JMSException when there is a connection error
     */
    public JmsPublisher(final Connection conn) throws JMSException {
        this(conn.createSession(false, AUTO_ACKNOWLEDGE), getConfig().getValue(CONFIG_JMS_QUEUE_NAME, String.class),
                getConfig().getOptionalValue(CONFIG_JMS_USE_QUEUE, Boolean.class).orElse(Boolean.TRUE));
    }

    /**
     * Create a new JMS Publisher.
     * @param session the JMS session
     * @param queueName the name of the queue
     * @throws JMSException when there is a connection error
     */
    public JmsPublisher(final Session session, final String queueName) throws JMSException {
        this(session, queueName, true);
    }

    /**
     * Create a new JMS Publisher.
     * @param session the JMS session
     * @param queueName the name of the queue
     * @param useQueue whether to use a queue or a topic
     * @throws JMSException when there is a connection error
     */
    public JmsPublisher(final Session session, final String queueName, final boolean useQueue) throws JMSException {
        requireNonNull(queueName, "JMS Queue name may not be null!");
        this.session = requireNonNull(session, "JMS Session may not be null!");
        if (useQueue) {
            this.producer = session.createProducer(session.createQueue(queueName));
        } else {
            this.producer = session.createProducer(session.createTopic(queueName));
        }
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

    private static Connection buildJmsConnection(final Config config) throws JMSException {
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
                config.getValue(CONFIG_JMS_URL, String.class));
        if (config.getOptionalValue(CONFIG_JMS_USERNAME, String.class).isPresent()
                && config.getOptionalValue(CONFIG_JMS_PASSWORD, String.class).isPresent()) {
            factory.setUserName(config.getValue(CONFIG_JMS_USERNAME, String.class));
            factory.setPassword(config.getValue(CONFIG_JMS_PASSWORD, String.class));
        }
        return factory.createConnection();
    }

    /** Package-private. **/
    static <T> T getService(final Class<T> service) {
        final Iterator<T> services = load(service).iterator();
        if (services.hasNext()) {
            return services.next();
        }
        throw new RuntimeTrellisException("No ActivityStream service available!");
    }
}
