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
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.TrellisUtils.findFirst;

import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.rdf.api.IRI;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.tamaya.Configuration;
import org.slf4j.Logger;
import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * A Kafka message producer capable of publishing messages to a Kafka cluster.
 */
public class KafkaPublisher implements EventService {

    private static final Configuration config = getConfiguration();
    private static final Logger LOGGER = getLogger(KafkaPublisher.class);
    private static final ActivityStreamService service = findFirst(ActivityStreamService.class)
        .orElseThrow(() -> new RuntimeTrellisException("No ActivityStream service available!"));

    /** The configuration key controlling the name of the kafka topic. **/
    public static final String CONFIG_KAFKA_TOPIC = "trellis.kafka.topic";

    private final Producer<String, String> producer;
    private final String topic;

    /**
     * Create a new Kafka Publisher.
     */
    @Inject
    public KafkaPublisher() {
        this(buildProducer());
    }

    /**
     * Create a new Kafka Publisher.
     * @param producer the producer
     */
    public KafkaPublisher(final Producer<String, String> producer) {
        this(producer, config.get(CONFIG_KAFKA_TOPIC));
    }

    /**
     * Create a new Kafka Publisher.
     * @param producer the producer
     * @param topic the name of the kafka topic
     */
    public KafkaPublisher(final Producer<String, String> producer, final String topic) {
        this.producer = requireNonNull(producer, "Kafka producer may not be null!");
        this.topic = requireNonNull(topic, "Kafka topic name may not be null!");
    }

    @Override
    public void emit(final Event event) {
        requireNonNull(event, "Cannot emit a null event!");

        service.serialize(event).ifPresent(message -> {
            LOGGER.debug("Sending message to Kafka topic: {}", topic);
            producer.send(
                new ProducerRecord<>(topic, event.getTarget().map(IRI::getIRIString).orElse(null),
                        message));
        });
    }

    private static Producer<String, String> buildProducer() {
        final String prefix = "trellis.kafka.";
        final Properties p = new Properties();
        p.setProperty("acks", config.getOrDefault(prefix + "acks", "all"));
        p.setProperty("batch.size", config.getOrDefault(prefix + "batch.size", "16384"));
        p.setProperty("retries", config.getOrDefault(prefix + "retries", "0"));
        p.setProperty("linger.ms", config.getOrDefault(prefix + "linger.ms", "1"));
        p.setProperty("buffer.memory", config.getOrDefault(prefix + "buffer.memory", "33554432"));
        config.getProperties().entrySet().stream().filter(e -> e.getKey().startsWith(prefix))
            .filter(e -> !CONFIG_KAFKA_TOPIC.equals(e.getKey()))
            .forEach(e -> p.setProperty(e.getKey().substring(prefix.length()), e.getValue()));

        p.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.setProperty("bootstrap.servers", config.get("trellis.kafka.bootstrap.servers"));

        return new KafkaProducer<>(p);
    }
}
