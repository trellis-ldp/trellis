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
package org.trellisldp.app.triplestore;

import static java.util.Optional.ofNullable;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.apache.jena.query.DatasetFactory.createTxnMem;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.apache.jena.tdb2.DatabaseMgr.connectDatasetGraph;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.app.config.NotificationsConfiguration.Type.JMS;
import static org.trellisldp.app.config.NotificationsConfiguration.Type.KAFKA;

import io.dropwizard.lifecycle.AutoCloseableManager;
import io.dropwizard.setup.Environment;

import java.util.Optional;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.trellisldp.api.EventService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.app.config.NotificationsConfiguration;
import org.trellisldp.jms.JmsPublisher;
import org.trellisldp.kafka.KafkaPublisher;

final class AppUtils {

    private static final String UN_KEY = "username";
    private static final String PW_KEY = "password";
    private static final Logger LOGGER = getLogger(AppUtils.class);

    public static Properties getKafkaProperties(final NotificationsConfiguration config) {
        final Properties p = new Properties();
        p.setProperty("acks", config.any().getOrDefault("acks", "all"));
        p.setProperty("batch.size", config.any().getOrDefault("batch.size", "16384"));
        p.setProperty("retries", config.any().getOrDefault("retries", "0"));
        p.setProperty("linger.ms", config.any().getOrDefault("linger.ms", "1"));
        p.setProperty("buffer.memory", config.any().getOrDefault("buffer.memory", "33554432"));
        config.any().forEach(p::setProperty);
        p.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        p.setProperty("bootstrap.servers", config.getConnectionString());
        return p;
    }

    public static ActiveMQConnectionFactory getJmsFactory(final NotificationsConfiguration config) {
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
                config.getConnectionString());
        if (config.any().containsKey(PW_KEY) && config.any().containsKey(UN_KEY)) {
            factory.setUserName(config.any().get(UN_KEY));
            factory.setPassword(config.any().get(PW_KEY));
        }
        return factory;
    }

    private static EventService buildKafkaPublisher(final NotificationsConfiguration config,
            final Environment environment) {
        LOGGER.info("Connecting to Kafka broker at {}", config.getConnectionString());
        final KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(getKafkaProperties(config));
        environment.lifecycle().manage(new AutoCloseableManager(kafkaProducer));
        return new KafkaPublisher(kafkaProducer, config.getTopicName());
    }

    private static EventService buildJmsPublisher(final NotificationsConfiguration config,
            final Environment environment) throws JMSException {
        LOGGER.info("Connecting to JMS broker at {}", config.getConnectionString());
        final Connection jmsConnection = getJmsFactory(config).createConnection();
        environment.lifecycle().manage(new AutoCloseableManager(jmsConnection));
        return new JmsPublisher(jmsConnection.createSession(false, AUTO_ACKNOWLEDGE), config.getTopicName());
    }

    public static EventService getNotificationService(final NotificationsConfiguration config,
            final Environment environment) {

        if (config.getEnabled()) {
            if (KAFKA.equals(config.getType())) {
                return buildKafkaPublisher(config, environment);

            } else if (JMS.equals(config.getType())) {
                try {
                    return buildJmsPublisher(config, environment);
                } catch (final JMSException ex) {
                    throw new RuntimeTrellisException(ex);
                }
            }
        }
        final String status = "notifications will be disabled";
        LOGGER.info("Using no-op event service: {}", status);
        return new NoopEventService();
    }

    public static RDFConnection getRDFConnection(final AppConfiguration config) {
        final Optional<String> location = ofNullable(config.getResources());
        if (location.isPresent()) {
            final String loc = location.get();
            if (loc.startsWith("http://") || loc.startsWith("https://")) {
                // Remote
                return connect(loc);
            }
            // TDB2
            return connect(wrap(connectDatasetGraph(loc)));
        }
        // in-memory
        return connect(createTxnMem());
    }

    private AppUtils() {
        // prevent instantiation
    }
}
