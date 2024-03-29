/*
 * Copyright (c) Aaron Coburn and individual contributors
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
package org.trellisldp.test;

import static jakarta.ws.rs.client.Entity.entity;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.Link.TYPE;
import static jakarta.ws.rs.core.Link.fromUri;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.common.HttpConstants.SLUG;
import static org.trellisldp.common.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.common.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.test.AuthCommonTests.EXT_ACL;
import static org.trellisldp.test.TestUtils.buildJwt;
import static org.trellisldp.test.TestUtils.checkEventGraph;
import static org.trellisldp.test.TestUtils.getResourceAsString;

import jakarta.ws.rs.core.Response;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Event tests.
 *
 * @author acoburn
 */
public interface EventTests extends CommonTests {

    String CHILD_RESOURCE_FILE = "/childResource.ttl";

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
    default void setUp() {

        final String jwt = buildJwt(Trellis.AdministratorAgent.getIRIString());

        final String containerContent = getResourceAsString("/basicContainer.ttl");

        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Verify a successful LDP-BC POST response");
            setContainerLocation(res.getLocation().toString());
        }

        // POST an LDP-C
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.Container.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Verify a successful LDP-C POST response");
            setMemberLocation(res.getLocation().toString());
        }
        final String directContainerContent = getResourceAsString("/directContainer.ttl")
                + "<> ldp:membershipResource <" + getMemberLocation() + "> .";

        // POST an LDP-DC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(directContainerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Verify a successful LDP-DC POST response");
            setDirectContainerLocation(res.getLocation().toString());
        }

        final String indirectContainerContent = getResourceAsString("/indirectContainer.ttl")
                + "<> ldp:membershipResource <" + getMemberLocation() + "> .";

        // POST an LDP-IC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(indirectContainerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Verify a successful LDP-IC POST response");
            setIndirectContainerLocation(res.getLocation().toString());
        }
    }

    /**
     * Run the tests.
     * @return the tests
     */
    default Stream<Executable> runTests() {
        setUp();
        return of(this::testReceiveCreateMessage, this::testReceiveChildMessage, this::testReceiveDeleteMessage,
                this::testReceiveCreateMessageDC, this::testReceiveDeleteMessageDC, this::testReceiveCreateMessageIC,
                this::testReceiveReplaceMessageIC, this::testReceiveDeleteMessageIC, this::testReceiveCreateMessageACL,
                this::testReceiveUpdateMessageACL, this::testReceiveDeleteMessageACL);
    }

    /**
     * Test receiving a creation event message.
     */
    default void testReceiveCreateMessage() {
        await().atMost(15, SECONDS).until(() -> getMessages().stream().anyMatch(checkEventGraph(getContainerLocation(),
                        Trellis.AdministratorAgent, AS.Create, LDP.BasicContainer)));
    }

    /**
     * Test receiving an update message.
     */
    default void testReceiveChildMessage() {
        final String agent = "https://people.apache.org/~acoburn/#i";

        // POST an LDP-RS
        try (final Response res = target(getContainerLocation()).request()
                .header(AUTHORIZATION, buildJwt(agent)).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Verify a successful LDP-RS POST response");
            assertAll("Check the resource parent",
                    checkResourceParentLdpBC(res.getLocation().toString(), agent, AS.Create, LDP.RDFSource));
        }
    }

    /**
     * Test receiving a delete message.
     */
    default void testReceiveDeleteMessage() {
        final String resource;
        final String agent1 = "https://madison.example.com/profile#me";

        // POST an LDP-RS
        try (final Response res = target(getContainerLocation()).request()
                .header(AUTHORIZATION, buildJwt(agent1)).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Verify a successful LDP-RS POST response");
            resource = res.getLocation().toString();
            assertAll("Check the resource parent",
                    checkResourceParentLdpBC(resource, agent1, AS.Create, LDP.RDFSource));
        }

        final String agent2 = "https://pat.example.com/profile#me";

        // DELETE the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, buildJwt(agent2)).delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Verify a successful LDP-RS DELETE response");
            assertAll("Check the LDP-BC parent", checkResourceParentLdpBC(resource, agent2, AS.Delete, LDP.RDFSource));
        }
    }

    /**
     * Test receiving a creation event message in a direct container.
     */
    default void testReceiveCreateMessageDC() {
        final String agent = "http://example.com/pat#i";

        // POST an LDP-RS
        try (final Response res = target(getDirectContainerLocation()).request()
                .header(AUTHORIZATION, buildJwt(agent)).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful POST response");
            assertAll("Check the LDP-DC parent", checkResourceParentLdpDC(res.getLocation().toString(), agent,
                        AS.Create, LDP.RDFSource, LDP.Container));
        }
    }

    /**
     * Test receiving a delete message.
     */
    default void testReceiveDeleteMessageDC() {
        final String resource;
        final String agent = "http://example.com/george#i";

        // POST an LDP-RS
        try (final Response res = target(getDirectContainerLocation()).request()
                .header(AUTHORIZATION, buildJwt(agent)).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful POST in an LDP-DC");
            resource = res.getLocation().toString();
            assertAll("Check the LDP-DC parent",
                    checkResourceParentLdpDC(resource, agent, AS.Create, LDP.RDFSource, LDP.Container));
        }

        final String agent2 = "https://pat.example.com/profile#me";

        // DELETE the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, buildJwt(agent2)).delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful LDP-RS DELETE");
            assertAll("Check the LDP-DC parent resource",
                    checkResourceParentLdpDC(resource, agent2, AS.Delete, LDP.RDFSource, LDP.Container));
        }
    }

    /**
     * Test receiving a creation event message in an indirect container.
     */
    default void testReceiveCreateMessageIC() {
        final String agent = "http://example.com/sam#i";

        // POST an LDP-RS
        try (final Response res = target(getIndirectContainerLocation()).request()
                .header(AUTHORIZATION, buildJwt(agent))
                .post(entity(getResourceAsString(CHILD_RESOURCE_FILE), TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful POST in an LDP-IC");
            assertAll("Check the LDP-IC parent", checkResourceParentLdpIC(res.getLocation().toString(),
                        agent, AS.Create, LDP.RDFSource, LDP.Container));
        }
    }

    /**
     * Test receiving a replace message.
     */
    default void testReceiveReplaceMessageIC() {
        final String resource;
        final String agent = "http://example.com/parker#i";
        final String childContent = getResourceAsString(CHILD_RESOURCE_FILE);

        // POST an LDP-RS
        try (final Response res = target(getIndirectContainerLocation()).request()
                .header(AUTHORIZATION, buildJwt(agent)).post(entity(childContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful POST to an LDP-IC");
            resource = res.getLocation().toString();
        }

        assertAll("Check the LDP-IC parent resource",
                checkResourceParentLdpIC(resource, agent, AS.Create, LDP.RDFSource, LDP.Container));

        final String agent2 = "https://hayden.example.com/profile#me";

        // Replace the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, buildJwt(agent2))
                .put(entity(childContent + "\n<> a <http://example.com/Type3> .", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful PUT in an LDP-IC");
        }

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .anyMatch(checkEventGraph(resource, agent2, AS.Update, LDP.RDFSource)));
        await().atMost(15, SECONDS).until(() -> getMessages().stream()
                .anyMatch(checkEventGraph(getMemberLocation(), agent2, AS.Update, LDP.Container)));
    }

    /**
     * Test receiving a delete message.
     */
    default void testReceiveDeleteMessageIC() {
        final String resource;
        final String agent = "http://example.com/addison#i";
        final String childContent = getResourceAsString(CHILD_RESOURCE_FILE);

        // POST an LDP-RS
        try (final Response res = target(getIndirectContainerLocation()).request()
                .header(AUTHORIZATION, buildJwt(agent)).post(entity(childContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful POST in an LDP-IC");
            resource = res.getLocation().toString();
            assertAll("Check the LDP-IC parent resource",
                    checkResourceParentLdpIC(resource, agent, AS.Create, LDP.RDFSource, LDP.Container));
        }

        final String agent2 = "https://daryl.example.com/profile#me";

        // DELETE the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, buildJwt(agent2)).delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful DELETE in an LDP-IC");
            assertAll("Check the LDP-IC parent resource",
                    checkResourceParentLdpIC(resource, agent2, AS.Delete, LDP.RDFSource, LDP.Container));
        }
    }


    default void testReceiveCreateMessageACL() {

        final String jwt = buildJwt(Trellis.AdministratorAgent.getIRIString());

        final String resourceContent = getResourceAsString(CHILD_RESOURCE_FILE);

        // POST an LDP-RS
        try (final Response res = target().request()
            .header(LINK, fromUri(LDP.RDFSource.getIRIString()).rel(TYPE).build())
            .header(SLUG, generateRandomValue(getClass().getSimpleName()))
            .header(AUTHORIZATION, jwt).post(entity(resourceContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                "Verify a successful LDP-RS POST response for ACL Test");
            setContainerLocation(res.getLocation().toString());
        }

        // Add an ACL for this resource, with no permissions
        final String rootAcl = simpleNoPermissionsACL();

        // Add an ACL for the resource
        try (final Response res = target(getContainerLocation() + EXT_ACL).request()
            .header(AUTHORIZATION, jwt)
            .method(AuthCommonTests.PATCH, entity(rootAcl, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                "Check response for ACL was created");
        }

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
            .anyMatch(checkEventGraph(getContainerLocation() + EXT_ACL,
                Trellis.AdministratorAgent, AS.Update, LDP.RDFSource)));
    }

    default void testReceiveUpdateMessageACL() {

        final String jwt = buildJwt(Trellis.AdministratorAgent.getIRIString());

        final String resourceContent = getResourceAsString(CHILD_RESOURCE_FILE);

        // POST an LDP-RS
        try (final Response res = target().request()
            .header(LINK, fromUri(LDP.RDFSource.getIRIString()).rel(TYPE).build())
            .header(SLUG, generateRandomValue(getClass().getSimpleName()))
            .header(AUTHORIZATION, jwt).post(entity(resourceContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                "Verify a successful LDP-RS POST response for ACL Test");
            setContainerLocation(res.getLocation().toString());
        }

        // Add an ACL for this resource, with no permissions
        final String acl = simpleNoPermissionsACL();

        // Add an ACL for the resource
        try (final Response res = target(getContainerLocation() + EXT_ACL).request()
            .header(AUTHORIZATION, jwt)
            .method(AuthCommonTests.PATCH, entity(acl, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                "Check response for ACL");
        }

        // Update the ACL for this resource, with read and write permissions
        final String readWriteAcl = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
            + "PREFIX acl: <http://www.w3.org/ns/auth/acl#>\n\n"
            + "INSERT DATA { [ acl:accessTo <" + getContainerLocation()
            + ">; acl:agentClass foaf:Agent; \n"
            + "    acl:default <" + getContainerLocation() + ">;\n"
            + "    acl:mode acl:Read, acl:Write]}";

        // Add an ACL for the resource
        try (final Response resUpdateACL = target(getContainerLocation() + EXT_ACL).request()
            .header(AUTHORIZATION, jwt)
            .method(AuthCommonTests.PATCH, entity(readWriteAcl, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, resUpdateACL.getStatusInfo().getFamily(),
                "Check response for ACL was updated");
        }

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
            .anyMatch(checkEventGraph(getContainerLocation() + EXT_ACL,
                Trellis.AdministratorAgent, AS.Update, LDP.RDFSource)));
    }

    default void testReceiveDeleteMessageACL() {

        final String jwt = buildJwt(Trellis.AdministratorAgent.getIRIString());

        final String resourceContent = getResourceAsString(CHILD_RESOURCE_FILE);

        // POST an LDP-RS
        try (final Response res = target().request()
            .header(LINK, fromUri(LDP.RDFSource.getIRIString()).rel(TYPE).build())
            .header(SLUG, generateRandomValue(getClass().getSimpleName()))
            .header(AUTHORIZATION, jwt).post(entity(resourceContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                "Verify a successful LDP-RS POST response for ACL Test");
            setContainerLocation(res.getLocation().toString());
        }

        // Add an ACL for this resource, with no permissions
        final String acl = simpleNoPermissionsACL();

        // Add an ACL for the resource
        try (final Response res = target(getContainerLocation() + EXT_ACL).request()
            .header(AUTHORIZATION, jwt)
            .method(AuthCommonTests.PATCH, entity(acl, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                "Check response for ACL was deleted");
        }

        // Add an ACL for the resource
        try (final Response resUpdateACL = target(getContainerLocation() + EXT_ACL).request()
            .header(AUTHORIZATION, jwt).delete()) {
            assertEquals(SUCCESSFUL, resUpdateACL.getStatusInfo().getFamily(),
                "Check response for ACL was deleted.");
        }

        await().atMost(15, SECONDS).until(() -> getMessages().stream()
            .anyMatch(checkEventGraph(getContainerLocation() + EXT_ACL,
                Trellis.AdministratorAgent, AS.Delete, LDP.RDFSource)));
    }

    default String simpleNoPermissionsACL() {
        return "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
            + "PREFIX acl: <http://www.w3.org/ns/auth/acl#>\n\n"
            + "INSERT DATA { [ acl:accessTo <" + getContainerLocation()
            + ">; acl:agentClass foaf:Agent; \n"
            + "    acl:default <" + getContainerLocation() + ">]}";
    }

    /**
     * Check the activity of a resource and its parent.
     * @param resource the resource IRI
     * @param agent the agent IRI
     * @param activityType the activity type
     * @param resourceType the resource type
     * @return a stream of tests
     */
    default Stream<Executable> checkResourceParentLdpBC(final String resource, final String agent,
            final IRI activityType, final IRI resourceType) {
        return checkResourceParentActivity(resource, getContainerLocation(), agent, activityType, resourceType,
                    LDP.BasicContainer);
    }

    /**
     * Check the activity of a resource and its parent.
     * @param resource the resource IRI
     * @param agent the agent IRI
     * @param activityType the activity type
     * @param resourceType the resource type
     * @param memberType the member type
     * @return a stream of tests
     */
    default Stream<Executable> checkResourceParentLdpDC(final String resource, final String agent,
            final IRI activityType, final IRI resourceType, final IRI memberType) {
        return concat(
                checkResourceParentActivity(resource, getDirectContainerLocation(), agent, activityType, resourceType,
                    LDP.DirectContainer),
                of(() -> await().atMost(15, SECONDS).until(() -> getMessages().stream()
                    .anyMatch(checkEventGraph(getMemberLocation(), agent, AS.Update, memberType)))));
    }

    /**
     * Check the activity of a resource and its parent.
     * @param resource the resource IRI
     * @param agent the agent IRI
     * @param activityType the activity type
     * @param resourceType the resource type
     * @param memberType the member type
     * @return a stream of tests
     */
    default Stream<Executable> checkResourceParentLdpIC(final String resource, final String agent,
            final IRI activityType, final IRI resourceType, final IRI memberType) {
        return concat(
                checkResourceParentActivity(resource, getIndirectContainerLocation(), agent, activityType, resourceType,
                    LDP.IndirectContainer),
                of(() -> await().atMost(15, SECONDS).until(() -> getMessages().stream()
                    .anyMatch(checkEventGraph(getMemberLocation(), agent, AS.Update, memberType)))));
    }

    /**
     * Check the activity of a resource and its parent.
     * @param resource the resource IRI
     * @param parent the parent IRI
     * @param agent the agent IRI
     * @param activityType the activity type
     * @param resourceType the resource type
     * @param parentType the parentType
     * @return a stream of tests
     */
    default Stream<Executable> checkResourceParentActivity(final String resource, final String parent,
            final String agent, final IRI activityType, final IRI resourceType, final IRI parentType) {
        return of(
                () -> await().atMost(15, SECONDS).until(() -> getMessages().stream()
                        .anyMatch(checkEventGraph(resource, agent, activityType, resourceType))),
                () -> await().atMost(15, SECONDS).until(() -> getMessages().stream()
                        .anyMatch(checkEventGraph(parent, agent, AS.Update, parentType))));
    }
}
