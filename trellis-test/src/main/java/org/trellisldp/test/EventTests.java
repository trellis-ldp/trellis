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
package org.trellisldp.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.RDF.type;

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
import javax.ws.rs.core.Response;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;

/**
 * Event tests.
 *
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class EventTests extends BaseCommonTests implements MessageListener {

    private static final Logger LOGGER = getLogger(EventTests.class);

    private static String container;
    private static String JWT_SECRET = "secret";

    private final Set<Message> messages = new CopyOnWriteArraySet<>();
    private MessageConsumer consumer;
    private Connection connection;

    protected static void setUp() throws Exception {

        final String jwt = buildJwt(Trellis.AdministratorAgent.getIRIString(), JWT_SECRET);

        final String containerContent = getResourceAsString("/basicContainer.ttl");

        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            container = res.getLocation().toString();
        }
    }

    /**
     * Aquire a JMS connection.
     *
     * @throws Exception if an error is encountered connecting to the JMS broker
     */
    @BeforeEach
    public void aquireConnection() throws Exception {
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        connection = connectionFactory.createConnection();
        connection.start();
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination destination = session.createQueue("trellis");
        consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);
    }

    /**
     * Release a JMS connection.
     *
     * @throws Exception if an error is encountered disconnecting from the JMS broker
     */
    @AfterEach
    public void releaseConnection() throws Exception {
        consumer.setMessageListener(msg -> { });
        consumer.close();
        connection.close();
    }

    /**
     * Test receiving a JMS creation message.
     */
    @Test
    @DisplayName("Test receiving a JMS creation message")
    public void testReceiveCreateMessage() {
        final IRI obj = rdf.createIRI(container);
        await().atMost(5, SECONDS).until(() -> messages.stream()
                .map(this::convertToGraph)
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, Trellis.AdministratorAgent)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.BasicContainer)));
    }

    /**
     * Test receiving an update message.
     */
    @Test
    @DisplayName("Test receiving an update message")
    public void testReceiveChildMessage() {
        final String resource;
        final String agent = "https://people.apache.org/~acoburn/#i";

        final String jwt = buildJwt(agent, JWT_SECRET);

        // POST an LDP-RS
        try (final Response res = target(container).request()
                .header(AUTHORIZATION, jwt).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }
        final IRI obj = rdf.createIRI(resource);
        final IRI parent = rdf.createIRI(container);

        await().atMost(5, SECONDS).until(() -> messages.stream()
                .map(this::convertToGraph)
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, rdf.createIRI(agent))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(5, SECONDS).until(() -> messages.stream()
                .map(this::convertToGraph)
                .filter(g -> g.contains(parent, type, null))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, AS.actor, rdf.createIRI(agent))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.BasicContainer)));
    }

    /**
     * Test receiving a delete message.
     */
    @Test
    @DisplayName("Test receiving a delete message")
    public void testReceiveDeleteMessage() {
        final String resource;
        final String agent1 = "https://madison.example.com/profile#me";

        final String jwt1 = buildJwt(agent1, JWT_SECRET);

        // POST an LDP-RS
        try (final Response res = target(container).request()
                .header(AUTHORIZATION, jwt1).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }
        final IRI obj = rdf.createIRI(resource);
        final IRI parent = rdf.createIRI(container);

        await().atMost(5, SECONDS).until(() -> messages.stream()
                .map(this::convertToGraph)
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, rdf.createIRI(agent1))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(5, SECONDS).until(() -> messages.stream()
                .map(this::convertToGraph)
                .filter(g -> g.contains(parent, type, null))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, AS.actor, rdf.createIRI(agent1))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.BasicContainer)));

        final String agent2 = "https://pat.example.com/profile#me";
        final String jwt2 = buildJwt(agent2, JWT_SECRET);

        // DELETE the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, jwt2).delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        await().atMost(5, SECONDS).until(() -> messages.stream()
                .map(this::convertToGraph)
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, rdf.createIRI(agent2))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Delete)
                        && g.contains(obj, type, LDP.Resource)));
        await().atMost(5, SECONDS).until(() -> messages.stream()
                .map(this::convertToGraph)
                .filter(g -> g.contains(parent, type, null))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, AS.actor, rdf.createIRI(agent2))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.BasicContainer)));
    }


    private Graph convertToGraph(final Message msg) {
        try {
            final String body = ((TextMessage) msg).getText();
            return readEntityAsGraph(new ByteArrayInputStream(body.getBytes(UTF_8)), JSONLD);
        } catch (final Exception ex) {
            LOGGER.error("Error processing message: {}", ex.getMessage());
        }
        return rdf.createGraph();
    }

    @Override
    public void onMessage(final Message message) {
        messages.add(message);
    }
}

