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
package org.trellisldp.test;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.trellisldp.http.core.HttpConstants.PREFER;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.getResourceAsString;
import static org.trellisldp.test.TestUtils.hasConstrainedBy;
import static org.trellisldp.test.TestUtils.hasType;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;

import java.util.stream.Stream;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Trellis;

/**
 * Test the RDF responses to LDP resources.
 */
public interface LdpIndirectContainerTests extends CommonTests {

    String BASIC_CONTAINER = "/basicContainer.ttl";
    String MEMBER_RESOURCE2 = "members2";
    String MEMBER_RESOURCE_HASH = "#members";
    String SIMPLE_RESOURCE = "/simpleResource.ttl";
    String DIRECT_CONTAINER = "/directContainer.ttl";
    String INDIRECT_CONTAINER = "/indirectContainer.ttl";
    String INDIRECT_CONTAINER_MEMBER_SUBJECT = "/indirectContainerMemberSubject.ttl";

    /**
     * Set the location of the test resource.
     * @param location the location
     */
    void setMemberLocation(String location);

    /**
     * Get the location of the test resource.
     * @return the test resource location
     */
    String getMemberLocation();

    /**
     * Set the location of the first indirect container.
     * @param location the location
     */
    void setIndirectContainerLocation(String location);

    /**
     * Get the location of the first indirect container.
     * @return the test container location
     */
    String getIndirectContainerLocation();

    /**
     * Set the location of the test container.
     * @param location the location
     */
    void setContainerLocation(String location);

    /**
     * Get the location of the test container.
     * @return the test container location
     */
    String getContainerLocation();

    /**
     * Initialize Indirect Container tests.
     * @throws Exception in the case of an error
     */
    default void setUp() throws Exception {
        final String containerContent = getResourceAsString(BASIC_CONTAINER);

        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .post(entity(containerContent, TEXT_TURTLE))) {
            assumeTrue(SUCCESSFUL.equals(res.getStatusInfo().getFamily()),
                    "Creation of BasicContainer appears to be unsupported");
            assumeTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)),
                    "Expected LDP:BasicContainer type not present in response");

            setContainerLocation(res.getLocation().toString());
        }

        setMemberLocation(getContainerLocation() + "member");

        final String content = getResourceAsString(INDIRECT_CONTAINER)
            + membershipResource(getMemberLocation());

        // POST an LDP-IC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .post(entity(content, TEXT_TURTLE))) {
            assumeTrue(SUCCESSFUL.equals(res.getStatusInfo().getFamily()),
                    "Creation of IndirectContainer appears to be unsupported");
            assumeTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)),
                    "Expected LDP:IndirectContainer type not present in response");

            setIndirectContainerLocation(res.getLocation().toString());
        }

        // PUT an LDP-RS
        try (final Response res = target(getMemberLocation()).request().put(entity(containerContent, TEXT_TURTLE))) {
            assumeTrue(SUCCESSFUL.equals(res.getStatusInfo().getFamily()),
                    "Creation of RDFSource appears to be unsupported");
            assumeTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)),
                    "Expected LDP:RDFSource type not present in response");
        }

        // POST an LDP-RS
        try (final Response res = target(getIndirectContainerLocation()).request()
                .post(entity(getResourceAsString(SIMPLE_RESOURCE) + "<> foaf:primaryTopic <#it>.", TEXT_TURTLE))) {
            assumeTrue(SUCCESSFUL.equals(res.getStatusInfo().getFamily()),
                    "Creation of RDFSource appears to be unsupported");
        }
    }

    /**
     * Run the tests.
     * @return the tests
     * @throws Exception in the case of an error
     */
    default Stream<Executable> runTests() throws Exception {
        setUp();
        return Stream.of(this::testAddResourceWithMemberSubject,
                this::testCreateIndirectContainerViaPut,
                this::testUpdateIndirectContainerTooManyMemberProps,
                this::testUpdateIndirectContainerMultipleMemberResources,
                this::testUpdateIndirectContainerMissingMemberResource,
                this::testGetInverseEmptyMember);
    }

    /**
     * Test adding resource to the indirect container.
     * @throws Exception if the RDF resource did not close cleanly
     */
    default void testAddResourceWithMemberSubject() throws Exception {
        final RDF rdf = RDFFactory.getInstance();
        final String content = getResourceAsString(INDIRECT_CONTAINER_MEMBER_SUBJECT)
            + membershipResource(MEMBER_RESOURCE_HASH);
        final String memberContent = getResourceAsString(SIMPLE_RESOURCE) + "<> foaf:primaryTopic <#it> .";
        final String location;
        final String child;

        // POST an LDP-IC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .post(entity(content, TEXT_TURTLE))) {
            assertAll("Check the LDP-IC", checkRdfResponse(res, LDP.IndirectContainer, null));
            location = res.getLocation().toString();
        }

        // POST an LDP-RS
        try (final Response res = target(location).request().post(entity(memberContent, TEXT_TURTLE))) {
            assertAll("Check the LDP-RS", checkRdfResponse(res, LDP.RDFSource, null));
            child = res.getLocation().toString();
        }

        //Fetch the member resource
        try (final Response res = target(location).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the member resource", checkRdfResponse(res, LDP.IndirectContainer, TEXT_TURTLE_TYPE));
            assertTrue(g.contains(rdf.createIRI(location), LDP.contains,
                        rdf.createIRI(child)), "Check for an ldp:contains triple");
            assertTrue(g.contains(rdf.createIRI(location + MEMBER_RESOURCE_HASH),
                        LDP.member, rdf.createIRI(child)), "Check for an ldp:member triple");
        }
    }

    /**
     * Test adding resources to the indirect container.
     * @throws Exception if the RDF resources did not exit cleanly
     */
    default void testAddingMemberResources() throws Exception {
        final RDF rdf = RDFFactory.getInstance();
        final String child1;
        final String child2;
        final String hash = "#it";
        final EntityTag etag1;
        final EntityTag etag2;
        final EntityTag etag3;
        final EntityTag etag4;
        final EntityTag etag5;
        final EntityTag etag6;
        final String childContent = getResourceAsString("/childResource.ttl");

        // Fetch the member resource
        try (final Response res = target(getMemberLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the member LDP-RS", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            assertFalse(g.contains(rdf.createIRI(getMemberLocation()), LDP.member, null), "Check for no ldp:member");
            etag1 = res.getEntityTag();
            assertTrue(etag1.isWeak(), "Check that the ETag is weak");
        }

        // Fetch the container resource
        try (final Response res = target(getIndirectContainerLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the container resource", checkRdfResponse(res, LDP.IndirectContainer, TEXT_TURTLE_TYPE));
            assertFalse(g.contains(rdf.createIRI(getIndirectContainerLocation()), LDP.contains, null),
                    "Check for no ldp:contains property");
            etag4 = res.getEntityTag();
            assertTrue(etag4.isWeak(), "Check that ETag 4 is weak");
        }


        // POST an LDP-RS child
        try (final Response res = target(getIndirectContainerLocation()).request()
                .post(entity(childContent, TEXT_TURTLE))) {
            assertAll("Check POSTing a child resource", checkRdfResponse(res, LDP.RDFSource, null));
            child1 = res.getLocation().toString();
            assertTrue(child1.startsWith(getIndirectContainerLocation()), "Check the Location header");
            assertTrue(child1.length() > getIndirectContainerLocation().length(), "Re-check the Location header");
        }

        // POST an LDP-RS child
        try (final Response res = target(getIndirectContainerLocation()).request()
                .post(entity(childContent, TEXT_TURTLE))) {
            assertAll("Check POSTing a child resource", checkRdfResponse(res, LDP.RDFSource, null));
            child2 = res.getLocation().toString();
            assertTrue(child2.startsWith(getIndirectContainerLocation()), "Check the Location header");
            assertTrue(child2.length() > getIndirectContainerLocation().length(), "Re-check the Location header");
        }

        // Fetch the member resource
        try (final Response res = target(getMemberLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the member LDP-RS", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getMemberLocation());
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child1 + hash)), "Check for a member triple");
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2 + hash)), "Check for a member triple");
            etag2 = res.getEntityTag();
            assertTrue(etag2.isWeak(), "Verify that the second ETag is weak");
            assertNotEquals(etag1, etag2, "Compare the first and second ETags");
        }

        // Fetch the container resource
        try (final Response res = target(getIndirectContainerLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the container resource", checkRdfResponse(res, LDP.IndirectContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getIndirectContainerLocation());
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child1)), "Check for first ldp:contains");
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child2)), "Check for second ldp:contains");
            etag5 = res.getEntityTag();
            assertTrue(etag5.isWeak(), "Check that the fifth ETag is weak");
            assertNotEquals(etag4, etag5, "Compare ETags 4 and 5");
        }

        // Delete one of the child resources
        try (final Response res = target(child1).request().delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Verify that the DELETE succeeds");
        }

        await().until(() -> !etag5.equals(getETag(getIndirectContainerLocation())));
        await().until(() -> !etag2.equals(getETag(getMemberLocation())));

        // Try fetching the deleted resource
        try (final Response res = target(child1).request().get()) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Verify that a missing resource throws a 4xx");
        }

        // Fetch the member resource
        try (final Response res = target(getMemberLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the member resource", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getMemberLocation());
            assertFalse(g.contains(identifier, LDP.member, rdf.createIRI(child1 + hash)),
                    "Verify that the child is no longer contained");
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2 + hash)),
                    "Verify that the second child is still contained");
            etag3 = res.getEntityTag();
            assertTrue(etag3.isWeak(), "Check that the third ETag is weak");
            assertNotEquals(etag1, etag3, "Compare the first and third ETags");
            assertNotEquals(etag2, etag3, "Compare the second and third ETags");
        }

        // Fetch the container resource
        try (final Response res = target(getIndirectContainerLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the container resource", checkRdfResponse(res, LDP.IndirectContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getIndirectContainerLocation());
            assertFalse(g.contains(identifier, LDP.contains, rdf.createIRI(child1)),
                    "Check the first child isn't contained");
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child2)),
                    "Check that the second child is contained");
            etag6 = res.getEntityTag();
            assertTrue(etag6.isWeak(), "Verify that the sixth ETag is weak");
            assertNotEquals(etag5, etag6, "Compare ETags 5 and 6");
            assertNotEquals(etag4, etag6, "Compare ETags 4 and 6");
        }

        // Now change the membership property
        final String updateContent
            = "PREFIX dc: <http://purl.org/dc/terms/>\n"
            + "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n\n"
            + "DELETE WHERE { <> ldp:hasMemberRelation ?o };"
            + "INSERT { <> ldp:hasMemberRelation dc:relation } WHERE {}";

        // Patch the indirect container
        try (final Response res = target(getIndirectContainerLocation()).request()
                .method("PATCH", entity(updateContent, APPLICATION_SPARQL_UPDATE))) {
            assertAll("Check PATCHing the container resource", checkRdfResponse(res, LDP.IndirectContainer, null));
        }

        // Fetch the member resource
        try (final Response res = target(getMemberLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the member resource", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getMemberLocation());
            assertTrue(g.contains(identifier, DC.relation, rdf.createIRI(child2 + hash)),
                    "Confirm that a dc:relation triple is present");
        }
    }

    /**
     * Test creating an indirect container via PUT.
     */
    default void testCreateIndirectContainerViaPut() {
        final String content = getResourceAsString(INDIRECT_CONTAINER)
            + membershipResource(getContainerLocation() + MEMBER_RESOURCE2);

        // PUT an LDP-IC
        try (final Response res = target(getContainerLocation() + "/indirectcontainer-put").request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .put(entity(content, TEXT_TURTLE))) {
            assertAll("Check PUTting an LDP-IC", checkRdfResponse(res, LDP.IndirectContainer, null));
        }
    }

    /**
     * Test updating an indirect container via PUT.
     */
    default void testUpdateIndirectContainerViaPut() {
        final String location = createSimpleIndirectContainer(MEMBER_RESOURCE2);
        final String content = getResourceAsString("/indirectContainerInverse.ttl")
            + membershipResource(getContainerLocation() + MEMBER_RESOURCE2);

        // PUT an LDP-IC
        try (final Response res = target(location).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .put(entity(content, TEXT_TURTLE))) {
            assertAll("Check replacing an LDP-IC via PUT", checkRdfResponse(res, LDP.IndirectContainer, null));
        }
    }

    /**
     * Test updating an indirect container with too many member-related properties.
     */
    default void testUpdateIndirectContainerTooManyMemberProps() {
        final String location = createSimpleIndirectContainer(MEMBER_RESOURCE2);
        final String content = getResourceAsString(INDIRECT_CONTAINER)
            + membershipResource(getContainerLocation() + MEMBER_RESOURCE2)
            + "<> ldp:isMemberOfRelation dc:isPartOf .";

        // PUT an LDP-DC
        try (final Response res = target(location).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .put(entity(content, TEXT_TURTLE))) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Check for a 4xx response");
            assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)),
                    "Check for an InvalidCardinality constraint IRI");
        }
    }

    /**
     * Test updating an indirect container with no ldp:insertedContentRelation property.
     */
    default void testUpdateIndirectContainerNoICRProp() {
        final String location = createSimpleIndirectContainer(MEMBER_RESOURCE2);
        final String content = getResourceAsString(DIRECT_CONTAINER)
            + membershipResource(getContainerLocation() + MEMBER_RESOURCE2);

        // PUT an LDP-DC
        try (final Response res = target(location).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .put(entity(content, TEXT_TURTLE))) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Check for a 4xx response");
            assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)),
                    "Check for an InvalidCardinality constraint IRI");
        }
    }

    /**
     * Test updating an indirect container with too many membership resources.
     */
    default void testUpdateIndirectContainerMultipleMemberResources() {
        final String location = createSimpleIndirectContainer(MEMBER_RESOURCE2);
        final String content = getResourceAsString(INDIRECT_CONTAINER)
            + membershipResource(getContainerLocation() + MEMBER_RESOURCE2)
            + membershipResource(getContainerLocation() + "/member3");

        // PUT an LDP-DC
        try (final Response res = target(location).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .put(entity(content, TEXT_TURTLE))) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Check for a 4xx response");
            assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)),
                    "Check for an InvalidCardinality contraint IRI");
        }
    }

    /**
     * Test updating an indirect container with no member relation property.
     */
    default void testUpdateIndirectContainerMissingMemberRelation() {
        final String location = createSimpleIndirectContainer(MEMBER_RESOURCE2);
        final String content = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX ldp: <http://www.w3.org/ns/ldp#> \n"
            + "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> skos:prefLabel \"Indirect Container\"@eng ; "
            + "   ldp:membershipResource <" + getContainerLocation() + MEMBER_RESOURCE2 + "> ; "
            + "   ldp:insertedContentRelation foaf:primaryTopic ; "
            + "   dc:description \"This is an Indirect Container for testing.\"@eng .";

        // PUT an LDP-DC
        try (final Response res = target(location).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .put(entity(content, TEXT_TURTLE))) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Missing member property results in 4xx");
            assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)),
                    "Check that a missing memberRelation results in an InvalidCardinality constraint");
        }
    }

    /**
     * Test updating an indirect container with no member resource.
     */
    default void testUpdateIndirectContainerMissingMemberResource() {
        final String location = createSimpleIndirectContainer(MEMBER_RESOURCE2);
        final String content = getResourceAsString(INDIRECT_CONTAINER);

        // PUT an LDP-DC
        try (final Response res = target(location).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .put(entity(content, TEXT_TURTLE))) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "no membershipResource results in 4xx");
            assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)),
                    "A missing membershipResource property results in an InvalidCardinality constraint");
        }
    }

    /**
     * Test with ldp:PreferMinimalContainer Prefer header.
     * @throws Exception if the RDF resources did not exit cleanly
     */
    default void testGetEmptyMember() throws Exception {
        final RDF rdf = RDFFactory.getInstance();
        try (final Response res = target(getMemberLocation()).request().header(PREFER,
                    "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check a member resource with Prefer", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getMemberLocation());
            assertTrue(g.contains(identifier, SKOS.prefLabel, null), "Check for a skos:prefLabel triple");
            assertFalse(g.contains(identifier, LDP.member, null), "Check for no ldp:member triple");
            assertFalse(g.contains(identifier, DC.relation, null), "Check for no dc:relation triple");
        }
    }

    /**
     * Test with ldp:PreferMinimalContainer Prefer header.
     * @throws Exception if the RDF resources did not exit cleanly
     */
    default void testGetInverseEmptyMember() throws Exception {
        final RDF rdf = RDFFactory.getInstance();
        try (final Response res = target(getMemberLocation()).request().header(PREFER,
                    "return=representation; omit=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check a member resource with Prefer", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getMemberLocation());
            assertFalse(g.contains(identifier, SKOS.prefLabel, null), "Check for no skos:prefLabel triple");
            assertTrue(g.contains(identifier, LDP.member, null) || g.contains(identifier, DC.relation, null),
                    "Check for either an ldp:member or dc:relation triple");
        }
    }

    /**
     * Create an ldp:membershipResource triple.
     * @param iri the object IRI
     * @return the triple
     */
    default String membershipResource(final String iri) {
        return "<> ldp:membershipResource <" + iri + ">.\n";
    }

    /**
     * Create a simple indirect container.
     * @param memberLocation the member resource to use
     * @return the location of the new LDP-IC
     */
    default String createSimpleIndirectContainer(final String memberLocation) {
        final String content = getResourceAsString(INDIRECT_CONTAINER)
            + membershipResource(getContainerLocation() + memberLocation);

        // POST an LDP-DC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                .post(entity(content, TEXT_TURTLE))) {
            assertAll("Check POSTing an LDP-IC", checkRdfResponse(res, LDP.IndirectContainer, null));
            return res.getLocation().toString();
        }
    }
}
