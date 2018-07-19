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
import static javax.ws.rs.core.Link.TYPE;
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
     * Set the test direct container location.
     * @param location the URL of the direct container
     */
    void setDirectContainerLocation(String location);

    /**
     * Get the test direct container location.
     * @return the LDP-DC location
     */
    String getDirectContainerLocation();

    /**
     * Set the test indirect container location.
     * @param location the URL of the indirect container
     */
    void setIndirectContainerLocation(String location);

    /**
     * Get the test indirect container location.
     * @return the LDP-IC location
     */
    String getIndirectContainerLocation();

    /**
     * Set the member location.
     * @param location the URL of the member resource
     */
    void setMemberLocation(String location);

    /**
     * Get the test member location.
     * @return the LDP-RS location
     */
    String getMemberLocation();

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
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            setContainerLocation(res.getLocation().toString());
        }

        // POST an LDP-C
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.Container.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            setMemberLocation(res.getLocation().toString());
        }
        final String directContainerContent = getResourceAsString("/directContainer.ttl")
                + "<> ldp:membershipResource <" + getMemberLocation() + "> .";

        // POST an LDP-DC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(directContainerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            setDirectContainerLocation(res.getLocation().toString());
        }

        final String indirectContainerContent = getResourceAsString("/indirectContainer.ttl")
                + "<> ldp:membershipResource <" + getMemberLocation() + "> .";

        // POST an LDP-IC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(indirectContainerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            setIndirectContainerLocation(res.getLocation().toString());
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
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
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

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, rdf.createIRI(agent))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
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

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, rdf.createIRI(agent1))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
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

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(obj, type, null))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, AS.actor, rdf.createIRI(agent2))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Delete)
                        && g.contains(obj, type, LDP.Resource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(parent, type, null))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, AS.actor, rdf.createIRI(agent2))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.BasicContainer)));
    }

    /**
     * Test receiving a creation event message in a direct container.
     */
    @Test
    @DisplayName("Test receiving a JMS creation message from a LDP-DC")
    default void testReceiveCreateMessageDC() {
        final String resource;
        final RDF rdf = getInstance();
        final String agent = "http://example.com/pat#i";
        final String jwt1 = buildJwt(agent, getJwtSecret());

        // POST an LDP-RS
        try (final Response res = target(getDirectContainerLocation()).request()
                .header(AUTHORIZATION, jwt1).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }
        final IRI obj = rdf.createIRI(resource);

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        final IRI member = rdf.createIRI(getMemberLocation());
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, rdf.createIRI(getDirectContainerLocation()))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(rdf.createIRI(getDirectContainerLocation()), type, LDP.DirectContainer)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, member)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(member, type, LDP.Container)));
    }

    /**
     * Test receiving a delete message.
     */
    @Test
    @DisplayName("Test receiving a delete message in a LDP-DC")
    default void testReceiveDeleteMessageDC() {
        final String resource;
        final String agent = "http://example.com/george#i";
        final String jwt1 = buildJwt(agent, getJwtSecret());
        final RDF rdf = getInstance();

        // POST an LDP-RS
        try (final Response res = target(getDirectContainerLocation()).request()
                .header(AUTHORIZATION, jwt1).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }
        final IRI obj = rdf.createIRI(resource);
        final IRI parent = rdf.createIRI(getDirectContainerLocation());
        final IRI member = rdf.createIRI(getMemberLocation());

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.DirectContainer)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, member)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(member, type, LDP.Container)));

        final String agent2 = "https://pat.example.com/profile#me";
        final String jwt2 = buildJwt(agent2, getJwtSecret());

        // DELETE the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, jwt2).delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent2)))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Delete)
                        && g.contains(obj, type, LDP.Resource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent2)))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.DirectContainer)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent2)))
                .anyMatch(g -> g.contains(null, AS.object, member)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(member, type, LDP.Container)));
    }

    /**
     * Test receiving a creation event message in an indirect container.
     */
    @Test
    @DisplayName("Test receiving a JMS creation message from a LDP-IC")
    default void testReceiveCreateMessageIC() {
        final String resource;
        final RDF rdf = getInstance();
        final String agent = "http://example.com/sam#i";
        final String jwt1 = buildJwt(agent, getJwtSecret());
        final String childContent = getResourceAsString("/childResource.ttl");

        // POST an LDP-RS
        try (final Response res = target(getIndirectContainerLocation()).request()
                .header(AUTHORIZATION, jwt1).post(entity(childContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }
        final IRI obj = rdf.createIRI(resource);

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        final IRI member = rdf.createIRI(getMemberLocation());
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, rdf.createIRI(getIndirectContainerLocation()))
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(rdf.createIRI(getIndirectContainerLocation()), type, LDP.IndirectContainer)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, member)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(member, type, LDP.Container)));
    }

    /**
     * Test receiving a replace message.
     */
    @Test
    @DisplayName("Test receiving a replace message in a LDP-IC")
    default void testReceiveReplaceMessageIC() {
        final String resource;
        final String agent = "http://example.com/parker#i";
        final String jwt1 = buildJwt(agent, getJwtSecret());
        final RDF rdf = getInstance();
        final String childContent = getResourceAsString("/childResource.ttl");

        // POST an LDP-RS
        try (final Response res = target(getIndirectContainerLocation()).request()
                .header(AUTHORIZATION, jwt1).post(entity(childContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }
        final IRI obj = rdf.createIRI(resource);
        final IRI parent = rdf.createIRI(getIndirectContainerLocation());
        final IRI member = rdf.createIRI(getMemberLocation());

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.IndirectContainer)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, member)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(member, type, LDP.Container)));

        final String agent2 = "https://hayden.example.com/profile#me";
        final String jwt2 = buildJwt(agent2, getJwtSecret());

        // Replace the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, jwt2)
                .put(entity(childContent + "\n<> a <http://example.com/Type3> .", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent2)))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent2)))
                .anyMatch(g -> g.contains(null, AS.object, member)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(member, type, LDP.Container)));
    }

    /**
     * Test receiving a delete message.
     */
    @Test
    @DisplayName("Test receiving a delete message in a LDP-IC")
    default void testReceiveDeleteMessageIC() {
        final String resource;
        final String agent = "http://example.com/addison#i";
        final String jwt1 = buildJwt(agent, getJwtSecret());
        final RDF rdf = getInstance();
        final String childContent = getResourceAsString("/childResource.ttl");

        // POST an LDP-RS
        try (final Response res = target(getIndirectContainerLocation()).request()
                .header(AUTHORIZATION, jwt1).post(entity(childContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }
        final IRI obj = rdf.createIRI(resource);
        final IRI parent = rdf.createIRI(getIndirectContainerLocation());
        final IRI member = rdf.createIRI(getMemberLocation());

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Create)
                        && g.contains(obj, type, LDP.RDFSource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.IndirectContainer)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent)))
                .anyMatch(g -> g.contains(null, AS.object, member)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(member, type, LDP.Container)));

        final String agent2 = "https://daryl.example.com/profile#me";
        final String jwt2 = buildJwt(agent2, getJwtSecret());

        // DELETE the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, jwt2).delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent2)))
                .anyMatch(g -> g.contains(null, AS.object, obj)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Delete)
                        && g.contains(obj, type, LDP.Resource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent2)))
                .anyMatch(g -> g.contains(null, AS.object, parent)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(parent, type, LDP.IndirectContainer)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .filter(g -> g.contains(null, AS.actor, rdf.createIRI(agent2)))
                .anyMatch(g -> g.contains(null, AS.object, member)
                        && g.contains(null, type, PROV.Activity)
                        && g.contains(null, type, AS.Update)
                        && g.contains(member, type, LDP.Container)));
    }
}
