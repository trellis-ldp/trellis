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
package org.trellisldp.kafka;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import org.apache.commons.rdf.api.IRI;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventSerializationService;
import org.trellisldp.api.EventService;

/**
 * A Kafka message producer capable of publishing messages to a Kafka cluster.
 */
public class KafkaEventService implements EventService {

    private static final Logger LOGGER = getLogger(KafkaEventService.class);

    /** The configuration key controlling the name of the kafka topic. */
    public static final String CONFIG_KAFKA_TOPIC = "trellis.kafka.topic";

    @Inject
    EventSerializationService serializer;

    @Inject
    Producer<String, String> producer;

    @Inject
    @ConfigProperty(name = CONFIG_KAFKA_TOPIC)
    String topic;

    @Override
    public void emit(final Event event) {
        requireNonNull(event, "Cannot emit a null event!");

        LOGGER.debug("Sending message to Kafka topic: {}", topic);
        producer.send(new ProducerRecord<>(topic, event.getObject().map(IRI::getIRIString).orElse(null),
                    serializer.serialize(event)));
    }
}
