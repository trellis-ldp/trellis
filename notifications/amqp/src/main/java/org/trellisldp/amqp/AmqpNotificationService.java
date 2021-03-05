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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.trellisldp.api.Notification;
import org.trellisldp.api.NotificationSerializationService;
import org.trellisldp.api.NotificationService;

/**
 * An AMQP message producer capable of publishing messages to an AMQP broker such as
 * RabbitMQ or Qpid.
 *
 * @deprecated Please use the trellis-reactive project instead
 */
@Deprecated
public class AmqpNotificationService implements NotificationService {

    private static final Logger LOGGER = getLogger(AmqpNotificationService.class);

    /** The configuration key controlling the AMQP exchange name. */
    public static final String CONFIG_AMQP_EXCHANGE_NAME = "trellis.amqp.exchange-name";

    /** The configuration key controlling the AMQP routing key. */
    public static final String CONFIG_AMQP_ROUTING_KEY = "trellis.amqp.routing-key";

    /** The configuration key controlling whether publishing is mandatory. */
    public static final String CONFIG_AMQP_MANDATORY = "trellis.amqp.mandatory";

    /** The configuration key controlling whether publishing is immediate. */
    public static final String CONFIG_AMQP_IMMEDIATE = "trellis.amqp.immediate";

    @Inject
    Channel channel;

    @Inject
    NotificationSerializationService serializer;

    @Inject
    @ConfigProperty(name = CONFIG_AMQP_EXCHANGE_NAME)
    String exchangeName;

    @Inject
    @ConfigProperty(name = CONFIG_AMQP_ROUTING_KEY)
    String routingKey;

    @Inject
    @ConfigProperty(name = CONFIG_AMQP_IMMEDIATE,
                    defaultValue = "false")
    boolean immediate;

    @Inject
    @ConfigProperty(name = CONFIG_AMQP_MANDATORY,
                    defaultValue = "true")
    boolean mandatory;

    @Override
    public void emit(final Notification notification) {
        requireNonNull(notification, "Cannot emit a null notification!");

        final BasicProperties props = new BasicProperties().builder()
                .contentType("application/ld+json").contentEncoding("UTF-8").build();

        try {
            channel.basicPublish(exchangeName, routingKey, mandatory, immediate, props,
                    serializer.serialize(notification).getBytes(UTF_8));
        } catch (final IOException ex) {
            LOGGER.error("Error writing to broker: {}", ex.getMessage());
        }
    }
}
