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
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
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

    /** The configuration key controlling the AMQP exchange name. **/
    public static final String CONFIG_AMQP_EXCHANGE_NAME = "trellis.amqp.exchangename";

    /** The configuration key controlling the AMQP routing key. **/
    public static final String CONFIG_AMQP_ROUTING_KEY = "trellis.amqp.routingkey";

    /** The configuration key controlling whether publishing is mandatory. **/
    public static final String CONFIG_AMQP_MANDATORY = "trellis.amqp.mandatory";

    /** The configuration key controlling whether publishing is immediate. **/
    public static final String CONFIG_AMQP_IMMEDIATE = "trellis.amqp.immediate";

    /** The configuration key controlling the AMQP connection URI. **/
    public static final String CONFIG_AMQP_URI = "trellis.amqp.uri";

    private final ActivityStreamService service;
    private final Channel channel;
    private final String exchangeName;
    private final String routingKey;
    private final boolean mandatory;
    private final boolean immediate;

    /**
     * Create an AMQP publisher.
     * @param serializer the event serializer
     * @param channel the channel
     */
    @Inject
    public AmqpPublisher(final ActivityStreamService serializer, final Channel channel) {
        this(serializer, channel, getConfig());
    }

    private AmqpPublisher(final ActivityStreamService serializer, final Channel channel, final Config config) {
        this(serializer, channel, config.getValue(CONFIG_AMQP_EXCHANGE_NAME, String.class),
                config.getValue(CONFIG_AMQP_ROUTING_KEY, String.class),
            config.getOptionalValue(CONFIG_AMQP_MANDATORY, Boolean.class).orElse(Boolean.TRUE),
            config.getOptionalValue(CONFIG_AMQP_IMMEDIATE, Boolean.class).orElse(Boolean.FALSE));
    }

    /**
     * Create an AMQP publisher.
     * @param serializer the event serializer
     * @param channel the channel
     * @param exchangeName the exchange name
     * @param routingKey the routing key
     */
    public AmqpPublisher(final ActivityStreamService serializer, final Channel channel, final String exchangeName,
            final String routingKey) {
        this(serializer, channel, exchangeName, routingKey, true, false);
    }

    /**
     * Create an AMQP publisher.
     * @param serializer the event serializer
     * @param channel the channel
     * @param exchangeName the exchange name
     * @param routingKey the routing key
     * @param mandatory the mandatory setting
     * @param immediate the immediate setting
     */
    public AmqpPublisher(final ActivityStreamService serializer, final Channel channel, final String exchangeName,
            final String routingKey, final boolean mandatory, final boolean immediate) {
        this.service = requireNonNull(serializer, "Event serializer may not be null!");
        this.channel = requireNonNull(channel, "AMQP Channel may not be null!");
        this.exchangeName = requireNonNull(exchangeName, "AMQP exchange name may not be null!");
        this.routingKey = requireNonNull(routingKey, "AMQP routing key may not be null!");

        this.mandatory = mandatory;
        this.immediate = immediate;
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
