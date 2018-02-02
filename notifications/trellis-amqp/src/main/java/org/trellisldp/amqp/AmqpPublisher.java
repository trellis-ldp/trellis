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
package org.trellisldp.amqp;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;

/**
 * An AMQP message producer capable of publishing messages to an AMQP broker such as
 * RabbitMQ or Qpid.
 */
public class AmqpPublisher implements EventService {

    private static final Logger LOGGER = getLogger(AmqpPublisher.class);

    // TODO - JDK9 ServiceLoader::findFirst
    private static ActivityStreamService service = ServiceLoader.load(ActivityStreamService.class).iterator().next();

    private final Channel channel;

    private final String exchangeName;

    private final String queueName;

    private final Boolean mandatory;

    private final Boolean immediate;

    /**
     * Create a an AMQP publisher.
     * @param channel the channel
     * @param exchangeName the exchange name
     * @param queueName the queue name
     */
    public AmqpPublisher(final Channel channel, final String exchangeName, final String queueName) {
        this(channel, exchangeName, queueName, null, null);
    }

    /**
     * Create a an AMQP publisher.
     * @param channel the channel
     * @param exchangeName the exchange name
     * @param queueName the queue name
     * @param mandatory the mandatory setting
     * @param immediate the immediate setting
     */
    public AmqpPublisher(final Channel channel, final String exchangeName, final String queueName,
            final Boolean mandatory, final Boolean immediate) {
        requireNonNull(channel);
        requireNonNull(exchangeName);
        requireNonNull(queueName);

        this.channel = channel;
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.mandatory = ofNullable(mandatory).orElse(true);
        this.immediate = ofNullable(immediate).orElse(false);
    }

    @Override
    public void emit(final Event event) {
        requireNonNull(event, "Cannot emit a null event!");

        final BasicProperties props = new BasicProperties().builder()
                .contentType("application/ld+json").contentEncoding("UTF-8").build();

        service.serialize(event).ifPresent(message -> {
            try {
                channel.basicPublish(exchangeName, queueName, mandatory, immediate, props, message.getBytes());
            } catch (final IOException ex) {
                LOGGER.error("Error writing to broker: {}", ex.getMessage());
            }
        });
    }
}
