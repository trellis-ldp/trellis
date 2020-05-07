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
package org.trellisldp.kafka;

import static java.util.Objects.requireNonNull;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.rdf.api.IRI;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventSerializationService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.NoopEventSerializationService;

/**
 * A Kafka message producer capable of publishing messages to a Kafka cluster.
 */
public class KafkaEventService implements EventService {

    private static final Logger LOGGER = getLogger(KafkaEventService.class);

    /** The configuration key controlling the name of the kafka topic. */
    public static final String CONFIG_KAFKA_TOPIC = "trellis.kafka.topic";

    private final EventSerializationService serializer;
    private final Producer<String, String> producer;
    private final String topic;

    /**
     * Create a new Kafka Event Service with a no-op serializer.
     *
     * @apiNote This construtor is used by CDI runtimes that require a public, no-argument constructor.
     *          It should not be invoked directly in user code.
     */
    public KafkaEventService() {
        this(new NoopEventSerializationService());
    }

    /**
     * Create a new Kafka Event Service.
     * @param serializer the event serializer
     */
    @Inject
    public KafkaEventService(final EventSerializationService serializer) {
        this(serializer, getConfig());
    }

    private KafkaEventService(final EventSerializationService serializer, final Config config) {
        this(serializer, buildProducer(config), config.getValue(CONFIG_KAFKA_TOPIC, String.class));
    }

    /**
     * Create a new Kafka Event Service.
     * @param serializer the event serializer
     * @param producer the producer
     */
    public KafkaEventService(final EventSerializationService serializer, final Producer<String, String> producer) {
        this(serializer, producer, getConfig().getValue(CONFIG_KAFKA_TOPIC, String.class));
    }

    /**
     * Create a new Kafka Event Service.
     * @param serializer the event serializer
     * @param producer the producer
     * @param topic the name of the kafka topic
     */
    public KafkaEventService(final EventSerializationService serializer, final Producer<String, String> producer,
            final String topic) {
        this.serializer = requireNonNull(serializer, "The Event serializer may not be null!");
        this.producer = requireNonNull(producer, "Kafka producer may not be null!");
        this.topic = requireNonNull(topic, "Kafka topic name may not be null!");
    }

    @Override
    public void emit(final Event event) {
        requireNonNull(event, "Cannot emit a null event!");

        LOGGER.debug("Sending message to Kafka topic: {}", topic);
        producer.send(new ProducerRecord<>(topic, event.getObject().map(IRI::getIRIString).orElse(null),
                    serializer.serialize(event)));
    }

    private static Producer<String, String> buildProducer(final Config config) {
        final String prefix = "trellis.kafka.";
        final Properties p = new Properties();
        p.setProperty("acks", config.getOptionalValue(prefix + "acks", String.class).orElse("all"));
        p.setProperty("batch.size", config.getOptionalValue(prefix + "batch.size", String.class).orElse("16384"));
        p.setProperty("retries", config.getOptionalValue(prefix + "retries", String.class).orElse("0"));
        p.setProperty("linger.ms", config.getOptionalValue(prefix + "linger.ms", String.class).orElse("1"));
        p.setProperty("buffer.memory", config.getOptionalValue(prefix + "buffer.memory", String.class)
                .orElse("33554432"));
        config.getPropertyNames().forEach(prop -> {
            if (prop.startsWith(prefix) && !CONFIG_KAFKA_TOPIC.equals(prop)) {
                p.setProperty(prop.substring(prefix.length()), config.getValue(prop, String.class));
            }
        });

        p.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.setProperty("bootstrap.servers", config.getValue("trellis.kafka.bootstrap.servers", String.class));

        return new KafkaProducer<>(p);
    }
}
