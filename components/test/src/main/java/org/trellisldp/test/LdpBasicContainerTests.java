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

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.HttpConstants.PREFER;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.getResourceAsString;
import static org.trellisldp.test.TestUtils.hasType;
import static org.trellisldp.test.TestUtils.meanwhile;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.SKOS;

/**
 * Run LDP-related tests on a Trellis application.
 */
@TestInstance(PER_CLASS)
@DisplayName("Basic Container Tests")
public interface LdpBasicContainerTests extends CommonTests {

    String BASIC_CONTAINER = "/basicContainer.ttl";

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
     * Set the location of the child resource.
     * @param location the location
     */
    void setChildLocation(String location);

    /**
     * Get the location of the child resource.
     * @return the location
     */
    String getChildLocation();

    /**
     * Get the first etag.
     * @return the etag
     */
    EntityTag getFirstETag();

    /**
     * Get the second etag.
     * @return the etag
     */
    EntityTag getSecondETag();

    /**
     * Get the third etag.
     * @return the etag
     */
    EntityTag getThirdETag();

    /**
     * Get the fourth etag.
     * @return the etag
     */
    EntityTag getFourthETag();

    /**
     * Set the first etag.
     * @param etag the etag
     */
    void setFirstETag(EntityTag etag);

    /**
     * Set the second etag.
     * @param etag the etag
     */
    void setSecondETag(EntityTag etag);

    /**
     * Set the third etag.
     * @param etag the etag
     */
    void setThirdETag(EntityTag etag);

    /**
     * Set the fourth etag.
     * @param etag the etag
     */
    void setFourthETag(EntityTag etag);

    /**
     * Check for a successful creation response.
     * @param res the response
     * @param ldpType the expected type
     * @return the location of the new resource
     */
    default String checkCreateResponseAssumptions(final Response res, final IRI ldpType) {
        assumeTrue(SUCCESSFUL.equals(res.getStatusInfo().getFamily()),
                "Creation of " + ldpType + " appears not to be supported");
        assumeTrue(getLinks(res).stream().anyMatch(hasType(ldpType)),
                "New resource was not of the expected " + ldpType + " type");
        return res.getLocation().toString();
    }

    /**
     * Initialize Basic Containment tests.
     */
    @BeforeAll
    @DisplayName("Initialize Basic Containment tests")
    default void beforeAllTests() {
        final String containerContent = getResourceAsString(BASIC_CONTAINER);
        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            setContainerLocation(checkCreateResponseAssumptions(res, LDP.BasicContainer));
        }

        // POST an LDP-BC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            setChildLocation(checkCreateResponseAssumptions(res, LDP.BasicContainer));
        }

        // POST an LDP-BC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            checkCreateResponseAssumptions(res, LDP.BasicContainer);
        }
    }

    /**
     * Test with ldp:PreferMinimalContainer Prefer header.
     */
    @Test
    @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
    default void testGetEmptyContainer() {
        final RDF rdf = getInstance();
        try (final Response res = target(getContainerLocation()).request().header(PREFER,
                    "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get()) {
            assertAll("Check a container response", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getContainerLocation());
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertAll("Check the graph for triples", checkRdfGraph(g, identifier));
            assertFalse(g.contains(identifier, LDP.contains, null), "Verify that no ldp:contains triples are present");
        }
    }

    /**
     * Test with ldp:PreferMinimalContainer Prefer header.
     */
    @Test
    @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
    default void testGetInverseEmptyContainer() {
        final RDF rdf = getInstance();
        try (final Response res = target(getContainerLocation()).request().header(PREFER,
                    "return=representation; omit=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get()) {
            assertAll("Check a container response", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertFalse(g.contains(identifier, DC.description, null), "Check for no dc:description triple");
            assertFalse(g.contains(identifier, SKOS.prefLabel, null), "Check for no skos:prefLabel triple");
            assertTrue(g.contains(identifier, LDP.contains, null), "Check for an ldp:contains triple");
        }
    }

    /**
     * Test fetching a basic container.
     */
    @Test
    @DisplayName("Test fetching a basic container")
    default void testGetContainer() {
        final RDF rdf = getInstance();
        try (final Response res = target(getContainerLocation()).request().get()) {
            assertAll("Check an LDP-BC response", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertAll("Check the RDF graph", checkRdfGraph(g, identifier));
            setFirstETag(res.getEntityTag());
            assertTrue(getFirstETag().isWeak(), "Verify that the ETag is weak");
            assertNotEquals(getFirstETag(), getSecondETag(), "Verify that the ETag values are different");
        }
    }

    /**
     * Test creating a basic container via POST.
     */
    @Test
    @DisplayName("Test creating a basic container via POST")
    default void testCreateContainerViaPost() {
        final RDF rdf = getInstance();
        final String containerContent = getResourceAsString(BASIC_CONTAINER);
        final String child3;
        meanwhile();

        // POST an LDP-BC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertAll("Check the LDP-BC response", checkRdfResponse(res, LDP.BasicContainer, null));

            child3 = res.getLocation().toString();
            assertTrue(child3.startsWith(getContainerLocation()), "Check the Location header");
            assertTrue(child3.length() > getContainerLocation().length(), "Check the Location header again");
        }

        // Now fetch the container
        try (final Response res = target(getContainerLocation()).request().get()) {
            assertAll("Check the LDP-BC again", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertAll("Check the LDP-BC graph", checkRdfGraph(g, identifier));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child3)));
            setSecondETag(res.getEntityTag());
            assertTrue(getSecondETag().isWeak(), "Verify that the ETag is weak");
            assertNotEquals(getFirstETag(), getSecondETag(), "Check that the ETag has been changed");
            assertNotEquals(getThirdETag(), getSecondETag(), "Check that the ETags are different");
            assertNotEquals(getFourthETag(), getSecondETag(), "Check that the ETags are different");
        }
    }

    /**
     * Test creating a child resource via PUT.
     */
    @Test
    @DisplayName("Test creating a child resource via PUT")
    default void testCreateContainerViaPut() {
        final RDF rdf = getInstance();
        final String containerContent = getResourceAsString(BASIC_CONTAINER);
        final String child4 = getContainerLocation() + "/child4";
        meanwhile();

        try (final Response res = target(child4).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .put(entity(containerContent, TEXT_TURTLE))) {
            assertAll("Check PUTting an LDP-BC", checkRdfResponse(res, LDP.BasicContainer, null));
        }

        // Now fetch the resource
        try (final Response res = target(getContainerLocation()).request().get()) {
            assertAll("Check an LDP-BC after PUT", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertAll("Check the resulting graph", checkRdfGraph(g, identifier));
            assertFalse(g.contains(identifier, LDP.contains, rdf.createIRI(child4)),
                    "Check for an ldp:contains triple");
            setThirdETag(res.getEntityTag());
            assertTrue(getThirdETag().isWeak(), "Check for a weak ETag");
            assertNotEquals(getFirstETag(), getThirdETag(), "Check ETags 1 and 3");
            assertNotEquals(getSecondETag(), getThirdETag(), "Check ETags 2 and 3");
            assertNotEquals(getFourthETag(), getThirdETag(), "Check ETags 3 and 4");
        }
    }

    /**
     * Test creating a child resource with a Slug header.
     */
    @Test
    @DisplayName("Test creating a child resource with a Slug header")
    default void testCreateContainerWithSlug() {
        final RDF rdf = getInstance();
        final String containerContent = getResourceAsString(BASIC_CONTAINER);
        final String child5 = getContainerLocation() + "/child5";
        // POST an LDP-BC
        try (final Response res = target(getContainerLocation()).request().header("Slug", "child5")
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertAll("Check POSTing an LDP-BC", checkRdfResponse(res, LDP.BasicContainer, null));
            assertEquals(child5, res.getLocation().toString(), "Check the resource location");
        }

        // Now fetch the resource
        try (final Response res = target(getContainerLocation()).request().get()) {
            assertAll("Check GETting the new resource", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertAll("Check the resulting Graph", checkRdfGraph(g, identifier));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child5)), "Check for an ldp:contains triple");
            setFourthETag(res.getEntityTag());
            assertTrue(getFourthETag().isWeak(), "Check for a weak ETag");
            assertNotEquals(getFirstETag(), getFourthETag(), "Compare ETags 1 and 4");
            assertNotEquals(getSecondETag(), getFourthETag(), "Compare ETags 2 and 4");
            assertNotEquals(getThirdETag(), getFourthETag(), "Compare ETags 3 and 4");
        }
    }

    /**
     * Test deleting a basic container.
     */
    @Test
    @DisplayName("Test deleting a basic container")
    default void testDeleteContainer() {
        final RDF rdf = getInstance();
        final EntityTag etag;

        // TODO remove the getChildLocation and setChildLocation methods from this interface
        final String childResource;

        try (final Response res = target(getContainerLocation()).request().post(entity("", TEXT_TURTLE))) {
            assertAll("Check for an LDP-RS", checkRdfResponse(res, LDP.RDFSource, null));
            childResource = res.getLocation().toString();
        }

        meanwhile();

        try (final Response res = target(getContainerLocation()).request().get()) {
            assertAll("Check for an LDP-BC", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getContainerLocation());
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertAll("Verify the resulting graph", checkRdfGraph(g, identifier));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(childResource)),
                    "Check for the presence of an ldp:contains triple");
            etag = res.getEntityTag();
            assertTrue(etag.isWeak(), "Verify that the ETag is weak");
        }

        // Delete one of the child resources
        try (final Response res = target(childResource).request().delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check the response type");
        }

        // Try fetching the deleted resource
        try (final Response res = target(childResource).request().get()) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Check for an expected error");
        }

        try (final Response res = target(getContainerLocation()).request().get()) {
            assertAll("Check the parent container", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertFalse(g.contains(rdf.createIRI(getContainerLocation()), LDP.contains,
                        rdf.createIRI(childResource)), "Check the graph doesn't contain the deleted resource");
            assertTrue(res.getEntityTag().isWeak(), "Check that the ETag is weak");
            assertNotEquals(etag, res.getEntityTag(), "Verify that the ETag value is different");
        }
    }
}
