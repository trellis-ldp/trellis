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
package org.trellisldp.kafka;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ServiceLoader;

import org.apache.commons.rdf.api.IRI;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;

/**
 * A Kafka message producer capable of publishing messages to a Kafka cluster.
 */
public class KafkaPublisher implements EventService {

    private static final Logger LOGGER = getLogger(KafkaPublisher.class);

    // TODO - JDK9 ServiceLoader::findFirst
    private static ActivityStreamService service = ServiceLoader.load(ActivityStreamService.class).iterator().next();

    private final Producer<String, String> producer;
    private final String topicName;

    /**
     * Create a new Kafka Publisher.
     * @param producer the producer
     * @param topicName the name of the topic
     */
    public KafkaPublisher(final Producer<String, String> producer, final String topicName) {
        requireNonNull(producer);
        requireNonNull(topicName);

        this.producer = producer;
        this.topicName = topicName;
    }

    @Override
    public void emit(final Event event) {
        requireNonNull(event, "Cannot emit a null event!");

        service.serialize(event).ifPresent(message -> {
            LOGGER.debug("Sending message to Kafka topic: {}", topicName);
            producer.send(
                new ProducerRecord<>(topicName, event.getTarget().map(IRI::getIRIString).orElse(null),
                        message));
        });
    }
}
