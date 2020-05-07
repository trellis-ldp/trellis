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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventSerializationService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.NoopEventSerializationService;

/**
 * An AMQP message producer capable of publishing messages to an AMQP broker such as
 * RabbitMQ or Qpid.
 */
public class AmqpEventService implements EventService {

    private static final Logger LOGGER = getLogger(AmqpEventService.class);

    /** The configuration key controlling the AMQP exchange name. */
    public static final String CONFIG_AMQP_EXCHANGE_NAME = "trellis.amqp.exchange-name";

    /** The configuration key controlling the AMQP routing key. */
    public static final String CONFIG_AMQP_ROUTING_KEY = "trellis.amqp.routing-key";

    /** The configuration key controlling whether publishing is mandatory. */
    public static final String CONFIG_AMQP_MANDATORY = "trellis.amqp.mandatory";

    /** The configuration key controlling whether publishing is immediate. */
    public static final String CONFIG_AMQP_IMMEDIATE = "trellis.amqp.immediate";

    /** The configuration key controlling the AMQP connection URI. */
    public static final String CONFIG_AMQP_URI = "trellis.amqp.uri";

    private final EventSerializationService service;
    private final Channel channel;
    private final String exchangeName;
    private final String routingKey;
    private final boolean mandatory;
    private final boolean immediate;

    /**
     * Create an AMQP publisher.
     *
     * @apiNote This construtor is used by CDI runtimes that require a public, no-argument constructor.
     *          It should not be invoked directly in user code.
     */
    public AmqpEventService() {
        this.service = new NoopEventSerializationService();
        this.channel = null;
        this.exchangeName = null;
        this.routingKey = null;
        this.mandatory = false;
        this.immediate = false;
    }

    /**
     * Create an AMQP publisher.
     * @param serializer the event serializer
     * @param channel the channel
     */
    @Inject
    public AmqpEventService(final EventSerializationService serializer, final Channel channel) {
        this(serializer, channel, getConfig());
    }

    private AmqpEventService(final EventSerializationService serializer, final Channel channel, final Config config) {
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
    public AmqpEventService(final EventSerializationService serializer, final Channel channel,
            final String exchangeName, final String routingKey) {
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
    public AmqpEventService(final EventSerializationService serializer, final Channel channel,
            final String exchangeName, final String routingKey, final boolean mandatory, final boolean immediate) {
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

        try {
            channel.basicPublish(exchangeName, routingKey, mandatory, immediate, props,
                    service.serialize(event).getBytes(UTF_8));
        } catch (final IOException ex) {
            LOGGER.error("Error writing to broker: {}", ex.getMessage());
        }
    }
}
