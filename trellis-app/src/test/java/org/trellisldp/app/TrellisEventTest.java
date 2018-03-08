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

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
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

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;

/**
 * Audit tests
 *
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class TrellisEventTest implements MessageListener {

    private static final Logger LOGGER = getLogger(TrellisEventTest.class);

    private static final BrokerService BROKER = new BrokerService();

    private static final DropwizardTestSupport<TrellisConfiguration> APP
        = new DropwizardTestSupport<TrellisConfiguration>(TrellisApplication.class,
                resourceFilePath("trellis-config.yml"),
                config("notifications.type", "JMS"),
                config("notifications.connectionString", "vm://localhost"),
                config("binaries", resourceFilePath("data") + "/binaries"),
                config("mementos", resourceFilePath("data") + "/mementos"),
                config("namespaces", resourceFilePath("data/namespaces.json")));

    private static Client client;
    private static String baseURL;
    private static String container;
    private static String JWT_SECRET = "secret";

    private static final NamespaceService nsSvc = new NamespacesJsonContext(resourceFilePath("data/namespaces.json"));
    private static final IOService ioSvc = new JenaIOService(nsSvc);
    private static final RDF rdf = new JenaRDF();

    private final Set<Message> messages = new CopyOnWriteArraySet<>();
    private MessageConsumer consumer;
    private Connection connection;

    @BeforeAll
    public static void setUp() throws Exception {
        BROKER.addConnector("vm://localhost");
        BROKER.setPersistent(false);
        BROKER.start();
        APP.before();
        client = new JerseyClientBuilder(APP.getEnvironment()).build("test client");
        client.property("jersey.config.client.connectTimeout", 5000);
        client.property("jersey.config.client.readTimeout", 5000);
        baseURL = "http://localhost:" + APP.getLocalPort() + "/";

        final String jwt = "Bearer " + Jwts.builder().claim("webid", Trellis.AdministratorAgent.getIRIString())
            .signWith(SignatureAlgorithm.HS512, JWT_SECRET.getBytes(UTF_8)).compact();

        final String containerContent
            = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> skos:prefLabel \"Basic Container\"@eng ; "
            + "   dc:description \"This is a simple Basic Container for testing.\"@eng .";

        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            container = res.getLocation().toString();
        }

    }

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

    @AfterEach
    public void releaseConnection() throws Exception {
        consumer.setMessageListener(msg -> { });
        consumer.close();
        connection.close();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        APP.after();
        BROKER.stop();
    }

    @Test
    @DisplayName("Test receiving a JMS creation message")
    public void testReceiveCreateMessage() throws Exception {
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

    @Test
    @DisplayName("Test receiving an update message")
    public void testReceiveChildMessage() throws Exception {
        final String resource;
        final String agent = "https://people.apache.org/~acoburn/#i";

        final String jwt = "Bearer " + Jwts.builder().claim("webid", agent)
            .signWith(SignatureAlgorithm.HS512, JWT_SECRET.getBytes(UTF_8)).compact();

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

    @Test
    @DisplayName("Test receiving a delete message")
    public void testReceiveDeleteMessage() throws Exception {
        final String resource;
        final String agent1 = "https://madison.example.com/profile#me";

        final String jwt1 = "Bearer " + Jwts.builder().claim("webid", agent1)
            .signWith(SignatureAlgorithm.HS512, JWT_SECRET.getBytes(UTF_8)).compact();

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
        final String jwt2 = "Bearer " + Jwts.builder().claim("webid", agent2)
            .signWith(SignatureAlgorithm.HS512, JWT_SECRET.getBytes(UTF_8)).compact();

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
        final Graph g = rdf.createGraph();
        try {
            final String body = ((TextMessage) msg).getText();
            final InputStream is = new ByteArrayInputStream(body.getBytes(UTF_8));
            ioSvc.read(is, baseURL, JSONLD).forEach(g::add);
        } catch (final Exception ex) {
            LOGGER.error("Error processing message: {}", ex.getMessage());
        }
        return g;
    }

    @Override
    public void onMessage(final Message message) {
        messages.add(message);
    }

    private static WebTarget target() {
        return target(baseURL);
    }

    private static WebTarget target(final String url) {
        return client.target(url);
    }
}

