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

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventSerializationService;
import org.trellisldp.api.EventService;

/**
 * A JMS message producer capable of publishing messages to a JMS broker such as ActiveMQ.
 *
 * @author acoburn
 */
public class JmsEventService implements EventService {

    /** The configuration key controlling the JMS queue name. */
    public static final String CONFIG_JMS_QUEUE_NAME = "trellis.jms.queue-name";

    /** The configuration key controlling whether to use a topic or queue. */
    public static final String CONFIG_JMS_USE_QUEUE = "trellis.jms.use-queue";

    private static final Logger LOGGER = getLogger(JmsEventService.class);

    @Inject
    @ConfigProperty(name = CONFIG_JMS_USE_QUEUE,
                    defaultValue = "true")
    boolean useQueue;

    @Inject
    @ConfigProperty(name = CONFIG_JMS_QUEUE_NAME)
    String queueName;

    @Inject
    EventSerializationService serializer;

    @Inject
    Session session;

    @Override
    public void emit(final Event event) {
        requireNonNull(event, "Cannot emit a null event!");

        try {
            final Message message = session.createTextMessage(serializer.serialize(event));
            message.setStringProperty("Content-Type", "application/ld+json");
            if (useQueue) {
                session.createProducer(session.createQueue(queueName)).send(message);
            } else {
                session.createProducer(session.createTopic(queueName)).send(message);
            }
        } catch (final JMSException ex) {
            LOGGER.error("Error writing to broker: {}", ex.getMessage());
        }
    }
}
