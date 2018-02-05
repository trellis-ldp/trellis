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
package org.trellisldp.app;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.apache.jena.query.DatasetFactory.createTxnMem;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.apache.jena.tdb2.DatabaseMgr.connectDatasetGraph;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.app.config.NotificationsConfiguration.Type.KAFKA;

import com.google.common.cache.Cache;
import com.rabbitmq.client.ConnectionFactory;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.rdf.api.IRI;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.trellisldp.amqp.AmqpPublisher;
import org.trellisldp.api.CacheService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.app.auth.AnonymousAuthFilter;
import org.trellisldp.app.auth.AnonymousAuthenticator;
import org.trellisldp.app.auth.BasicAuthenticator;
import org.trellisldp.app.auth.JwtAuthenticator;
import org.trellisldp.app.config.AuthConfiguration;
import org.trellisldp.app.config.CORSConfiguration;
import org.trellisldp.app.config.NotificationsConfiguration;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.jms.JmsPublisher;
import org.trellisldp.kafka.KafkaPublisher;

/**
 * @author acoburn
 */
final class TrellisUtils {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final Logger LOGGER = getLogger(TrellisUtils.class);

    public static Map<String, String> getAssetConfiguration(final TrellisConfiguration config) {
        final Map<String, String> assetMap = new HashMap<>();
        assetMap.put("icon", config.getAssets().getIcon());
        assetMap.put("css", config.getAssets().getCss().stream().map(String::trim).collect(joining(",")));
        assetMap.put("js", config.getAssets().getJs().stream().map(String::trim).collect(joining(",")));

        return assetMap;
    }

    public static Optional<List<AuthFilter>> getAuthFilters(final TrellisConfiguration config) {
        // Authentication
        final List<AuthFilter> filters = new ArrayList<>();
        final AuthConfiguration auth = config.getAuth();

        if (auth.getJwt().getEnabled()) {
            filters.add(new OAuthCredentialAuthFilter.Builder<Principal>()
                    .setAuthenticator(new JwtAuthenticator(auth.getJwt().getKey(), auth.getJwt().getBase64Encoded()))
                    .setPrefix("Bearer")
                    .buildAuthFilter());
        }

        if (auth.getBasic().getEnabled()) {
            filters.add(new BasicCredentialAuthFilter.Builder<Principal>()
                    .setAuthenticator(new BasicAuthenticator(auth.getBasic().getUsersFile()))
                    .setRealm("Trellis Basic Authentication")
                    .buildAuthFilter());
        }

        if (auth.getAnon().getEnabled()) {
            filters.add(new AnonymousAuthFilter.Builder()
                .setAuthenticator(new AnonymousAuthenticator())
                .buildAuthFilter());
        }

        if (filters.isEmpty()) {
            return empty();
        }
        return of(filters);
    }

    public static Optional<CacheService<String, Set<IRI>>> getWebacConfiguration(final TrellisConfiguration config) {
        if (config.getAuth().getWebac().getEnabled()) {
            final Cache<String, Set<IRI>> authCache = newBuilder().maximumSize(config.getAuth().getWebac()
                    .getCacheSize()).expireAfterWrite(config.getAuth().getWebac()
                    .getCacheExpireSeconds(), SECONDS).build();
            return of(new TrellisCache<>(authCache));
        }
        return empty();
    }

    public static RDFConnection getRDFConnection(final TrellisConfiguration config) {
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

    public static Optional<CORSConfiguration> getCorsConfiguration(final TrellisConfiguration config) {
        if (config.getCors().getEnabled()) {
            return of(config.getCors());
        }
        return empty();
    }

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
        if (config.any().containsKey(PASSWORD) && config.any().containsKey(USERNAME)) {
            factory.setUserName(config.any().get(USERNAME));
            factory.setPassword(config.any().get(PASSWORD));
        }
        return factory;
    }

    public static ConnectionFactory getAmqpFactory(final NotificationsConfiguration config)
            throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(config.getConnectionString());
        if (config.any().containsKey(PASSWORD) && config.any().containsKey(USERNAME)) {
            factory.setUsername(config.any().get(USERNAME));
            factory.setPassword(config.any().get(PASSWORD));
        }
        return factory;
    }

    public static EventService getEventService(final TrellisConfiguration config) {
        final NotificationsConfiguration c = config.getNotifications();
        try {
            if (c.getEnabled()) {
                switch (c.getType()) {
                    case KAFKA:
                        LOGGER.info("Connecting to Kafka broker at {}", c.getConnectionString());
                        return new KafkaPublisher(new KafkaProducer<>(getKafkaProperties(c)), c.getTopicName());
                    case JMS:
                        LOGGER.info("Connecting to JMS broker at {}", c.getConnectionString());
                        return new JmsPublisher(getJmsFactory(c).createConnection(), c.getTopicName());
                    case AMQP:
                        LOGGER.info("Connecting to AMQP broker at {}", c.getConnectionString());
                        return new AmqpPublisher(getAmqpFactory(c).newConnection().createChannel(), c.getTopicName(),
                                c.any().getOrDefault("routing.key", c.getTopicName()));
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Error establishing broker connection: {}, defaulting to NO-OP event service.",
                    ex.getMessage());
        }
        return new NoopEventService();
    }

    private TrellisUtils() {
        // prevent instantiation
    }
}
