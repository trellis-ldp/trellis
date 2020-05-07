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
package org.trellisldp.app.triplestore;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;

import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.ws.rs.client.Client;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.test.AbstractApplicationAuditTests;
import org.trellisldp.test.AbstractApplicationAuthTests;
import org.trellisldp.test.AbstractApplicationEventTests;
import org.trellisldp.test.AbstractApplicationLdpTests;
import org.trellisldp.test.AbstractApplicationMementoTests;

/**
 * Integration tests for Trellis.
 */
@TestInstance(PER_CLASS)
class TrellisApplicationTest implements MessageListener {

    private static final Logger LOGGER = getLogger(TrellisApplicationTest.class);
    private static final RDF rdf = RDFFactory.getInstance();
    private static final BrokerService BROKER = new BrokerService();

    private static final DropwizardTestSupport<AppConfiguration> APP = buildApplication();
    private static final String JWT_KEY
        = "EEPPbd/7llN/chRwY2UgbdcyjFdaGjlzaupd3AIyjcu8hMnmMCViWoPUBb5FphGLxBlUlT/G5WMx0WcDq/iNKA==";

    private final Set<Graph> MESSAGES = new CopyOnWriteArraySet<>();

    private Client CLIENT;

    private MessageConsumer consumer;
    private Connection connection;


    @BeforeAll
    void setUp() throws Exception {
        BROKER.setPersistent(false);
        BROKER.start();

        APP.before();
        CLIENT = new JerseyClientBuilder(APP.getEnvironment()).build("test client");
        CLIENT.property(CONNECT_TIMEOUT, 10000);
        CLIENT.property(READ_TIMEOUT, 12000);
        setDefaultPollInterval(100L, MILLISECONDS);
    }

    @AfterAll
    void cleanup() throws Exception {
        APP.after();
        BROKER.stop();
    }

    @BeforeEach
    void aquireConnection() throws Exception {
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        connection = connectionFactory.createConnection();
        connection.start();
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination destination = session.createQueue("trellis");
        consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);
    }

    @AfterEach
    void releaseConnection() throws Exception {
        consumer.setMessageListener(msg -> { });
        consumer.close();
        connection.close();
    }

    @Override
    public void onMessage(final Message message) {
        MESSAGES.add(convertToGraph(message));
    }


    @Test
    void testGetName() {
        final Application<AppConfiguration> app = new TrellisApplication();
        assertEquals("Trellis LDP", app.getName(), "Incorrect application name!");
    }

    @Nested
    @DisplayName("Trellis Audit Tests")
    class AuditTests extends AbstractApplicationAuditTests {

        @Override
        public String getJwtSecret() {
            return JWT_KEY;
        }

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.CLIENT;
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + APP.getLocalPort() + "/";
        }
    }

    @Nested
    @DisplayName("Trellis LDP Tests")
    class LdpTests extends AbstractApplicationLdpTests {

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.CLIENT;
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + APP.getLocalPort() + "/";
        }

        @Override
        public Set<String> supportedJsonLdProfiles() {
            return singleton("http://www.w3.org/ns/anno.jsonld");
        }
    }

    @Nested
    @DisplayName("Trellis Memento Tests")
    class MementoTests extends AbstractApplicationMementoTests {

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.CLIENT;
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + APP.getLocalPort() + "/";
        }
    }

    @Nested
    @DisplayName("Trellis Event Tests")
    class EventTests extends AbstractApplicationEventTests {

        @BeforeEach
        public void before() {
            TrellisApplicationTest.this.MESSAGES.clear();
        }

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.CLIENT;
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + APP.getLocalPort() + "/";
        }

        @Override
        public Set<Graph> getMessages() {
            return TrellisApplicationTest.this.MESSAGES;
        }

        @Override
        public String getJwtSecret() {
            return JWT_KEY;
        }
    }

    @Nested
    @DisplayName("Trellis AuthZ Tests")
    class AuthorizationTests extends AbstractApplicationAuthTests {

        @Override
        public Client getClient() {
            return TrellisApplicationTest.this.CLIENT;
        }

        @Override
        public String getBaseURL() {
            return "http://localhost:" + APP.getLocalPort() + "/";
        }

        @Override
        public String getUser1Credentials() {
            return "acoburn:secret";
        }

        @Override
        public String getUser2Credentials() {
            return "user:password";
        }

        @Override
        public String getAdminWebId() {
            return "http://admin.example.com/#me";
        }

        @Override
        public String getJwtSecret() {
            return JWT_KEY;
        }
    }

    private static DropwizardTestSupport<AppConfiguration> buildApplication() {
        return new DropwizardTestSupport<>(TrellisApplication.class,
                resourceFilePath("trellis-config.yml"),
                config("notifications.type", "JMS"),
                config("notifications.connectionString", "vm://localhost"),
                config("auth.basic.usersFile", resourceFilePath("users.auth")),
                config("binaries", resourceFilePath("app-data") + "/binaries"),
                config("mementos", resourceFilePath("app-data") + "/mementos"),
                config("namespaces", resourceFilePath("app-data/namespaces.json")));
    }

    private Graph convertToGraph(final Message msg) {
        try {
            final String body = ((TextMessage) msg).getText();
            return readEntityAsGraph(new ByteArrayInputStream(body.getBytes(UTF_8)),
                    "http://localhost:" + APP.getLocalPort() + "/", JSONLD);
        } catch (final Exception ex) {
            LOGGER.error("Error processing message: {}", ex.getMessage());
        }
        return rdf.createGraph();
    }
}
