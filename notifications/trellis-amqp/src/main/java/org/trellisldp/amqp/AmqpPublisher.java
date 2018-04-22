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
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.ServiceLoader;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;

/**
 * An AMQP message producer capable of publishing messages to an AMQP broker such as
 * RabbitMQ or Qpid.
 */
public class AmqpPublisher implements EventService {

    public static final String AMQP_EXCHANGE_NAME = "trellis.amqp.exchangename";

    public static final String AMQP_ROUTING_KEY = "trellis.amqp.routingkey";

    public static final String AMQP_MANDATORY = "trellis.amqp.mandatory";

    public static final String AMQP_IMMEDIATE = "trellis.amqp.immediate";

    private static final Logger LOGGER = getLogger(AmqpPublisher.class);

    // TODO - JDK9 ServiceLoader::findFirst
    private static ActivityStreamService service = ServiceLoader.load(ActivityStreamService.class).iterator().next();

    private final Channel channel;

    private final String exchangeName;

    private final String routingKey;

    private final Boolean mandatory;

    private final Boolean immediate;

    /**
     * Create an AMQP publisher.
     * @param channel the channel
     */
    @Inject
    public AmqpPublisher(final Channel channel) {
        this(channel, ConfigurationProvider.getConfiguration());
    }

    private AmqpPublisher(final Channel channel, final Configuration config) {
        this(channel, config.get(AMQP_EXCHANGE_NAME), config.get(AMQP_ROUTING_KEY),
            config.getOrDefault(AMQP_MANDATORY, Boolean.class, true),
            config.getOrDefault(AMQP_IMMEDIATE, Boolean.class, false));
    }

    /**
     * Create an AMQP publisher.
     * @param factory the connection factory
     * @param exchangeName the exchange name
     * @param routingKey the routing key
     * @throws IOException if a problem is encountered connecting to the broker
     * @throws TimeoutException if the connection times out
     */
    public AmqpPublisher(final ConnectionFactory factory, final String exchangeName, final String routingKey)
            throws IOException, TimeoutException {
        this(factory.newConnection().createChannel(), exchangeName, routingKey);
    }

    /**
     * Create an AMQP publisher.
     * @param channel the channel
     * @param exchangeName the exchange name
     * @param routingKey the routing key
     */
    public AmqpPublisher(final Channel channel, final String exchangeName, final String routingKey) {
        this(channel, exchangeName, routingKey, null, null);
    }

    /**
     * Create an AMQP publisher.
     * @param channel the channel
     * @param exchangeName the exchange name
     * @param routingKey the routing key
     * @param mandatory the mandatory setting
     * @param immediate the immediate setting
     */
    public AmqpPublisher(final Channel channel, final String exchangeName, final String routingKey,
            final Boolean mandatory, final Boolean immediate) {
        requireNonNull(channel, "AMQP Channel may not be null!");
        requireNonNull(exchangeName, "AMQP exchange name may not be null!");
        requireNonNull(routingKey, "AMQP routing key may not be null!");

        this.channel = channel;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
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
                channel.basicPublish(exchangeName, routingKey, mandatory, immediate, props, message.getBytes());
            } catch (final IOException ex) {
                LOGGER.error("Error writing to broker: {}", ex.getMessage());
            }
        });
    }
}
