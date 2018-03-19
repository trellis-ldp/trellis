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

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.test.TestUtils.buildJwt;
import static org.trellisldp.test.TestUtils.getResourceAsString;
import static org.trellisldp.vocabulary.RDF.type;

import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;

/**
 * Event tests.
 *
 * @author acoburn
 */
@TestInstance(PER_CLASS)
public interface EventTests extends CommonTests {

    /**
     * Get the JWT secret.
     * @return the JWT secret
     */
    String getJwtSecret();

    /**
     * Get the location of the test container.
     * @return the container URL
     */
    String getContainerLocation();

    /**
     * Set the test container location.
     * @param location the URL of the test container
     */
    void setContainerLocation(String location);

    /**
     * Get the received messages.
     * @return the messages
     */
    Set<Graph> getMessages();

    /**
     * Initialize a test container.
     */
    @BeforeAll
    default void beforeAllTests() {

        final String jwt = buildJwt(Trellis.AdministratorAgent.getIRIString(), getJwtSecret());

        final String containerContent = getResourceAsString("/basicContainer.ttl");

        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            setContainerLocation(res.getLocation().toString());
        }
    }

    /**
     * Test receiving a creation event message.
     */
    @Test
    @DisplayName("Test receiving a JMS creation message")
    default void testReceiveCreateMessage() {
        final RDF rdf = getInstance();
        final IRI obj = rdf.createIRI(getContainerLocation());
        await().atMost(5, SECONDS).until(() -> getMessages().stream()
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
    default void testReceiveChildMessage() {
        final String resource;
        final String agent = "https://people.apache.org/~acoburn/#i";
        final RDF rdf = getInstance();

        final String jwt = buildJwt(agent, getJwtSecret());

        // POST an LDP-RS
        try (final Response res = target(getContainerLocation()).request()
                .header(AUTHORIZATION, jwt).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }
        final IRI obj = rdf.createIRI(resource);
        final IRI parent = rdf.createIRI(getContainerLocation());

        await().atMost(5, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, rdf.createIRI(agent))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(5, SECONDS).until(() -> getMessages().stream()
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
    default void testReceiveDeleteMessage() {
        final String resource;
        final String agent1 = "https://madison.example.com/profile#me";

        final String jwt1 = buildJwt(agent1, getJwtSecret());
        final RDF rdf = getInstance();

        // POST an LDP-RS
        try (final Response res = target(getContainerLocation()).request()
                .header(AUTHORIZATION, jwt1).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }
        final IRI obj = rdf.createIRI(resource);
        final IRI parent = rdf.createIRI(getContainerLocation());

        await().atMost(5, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, rdf.createIRI(agent1))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(5, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(parent, type, null))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, AS.actor, rdf.createIRI(agent1))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.BasicContainer)));

        final String agent2 = "https://pat.example.com/profile#me";
        final String jwt2 = buildJwt(agent2, getJwtSecret());

        // DELETE the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, jwt2).delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        await().atMost(5, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, rdf.createIRI(agent2))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Delete)
                        && g.contains(obj, type, LDP.Resource)));
        await().atMost(5, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(parent, type, null))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, AS.actor, rdf.createIRI(agent2))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.BasicContainer)));
    }
}
