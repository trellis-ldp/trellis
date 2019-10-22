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
import static org.awaitility.Awaitility.await;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_PUT_UNCONTAINED;
import static org.trellisldp.http.core.HttpConstants.PREFER;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.test.TestUtils.getResourceAsString;
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
     * Initialize Basic Containment tests.
     */
    @BeforeAll
    @DisplayName("Initialize Basic Containment tests")
    default void beforeAllTests() {
        // POST an LDP-BC
        try (final Response res = target().request()
                .header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(getResourceAsString(BASIC_CONTAINER), TEXT_TURTLE))) {
            setContainerLocation(checkCreateResponseAssumptions(res, LDP.BasicContainer));
        }
    }

    /**
     * Test with ldp:PreferMinimalContainer Prefer header.
     * @throws Exception when the RDF resource doesn not close cleanly
     */
    @Test
    @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
    default void testGetEmptyContainer() throws Exception {
        final RDF rdf = getInstance();
        try (final Response res = target(getContainerLocation()).request().header(PREFER,
                    "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check a container response", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertAll("Check the graph for triples", checkRdfGraph(g, identifier));
            assertFalse(g.contains(identifier, LDP.contains, null), "Verify that no ldp:contains triples are present");
        }
    }

    /**
     * Test with ldp:PreferMinimalContainer Prefer header.
     * @throws Exception when the RDF resource doesn not close cleanly
     */
    @Test
    @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
    default void testGetInverseEmptyContainer() throws Exception {
        final RDF rdf = getInstance();
        try (final Response res = target(getContainerLocation()).request().header(PREFER,
                    "return=representation; omit=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check a container response", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertFalse(g.contains(identifier, DC.description, null), "Check for no dc:description triple");
            assertFalse(g.contains(identifier, SKOS.prefLabel, null), "Check for no skos:prefLabel triple");
            assertTrue(g.contains(identifier, LDP.contains, null), "Check for an ldp:contains triple");
        }
    }

    /**
     * Test that no membership triples are present.
     * @throws Exception when the RDF resource doesn not close cleanly
     */
    @Test
    @DisplayName("Test with ldp:PreferMembership Prefer header")
    default void testGetEmptyContainerMembership() throws Exception {
        final long size;
        try (final Response res = target(getContainerLocation()).request().header(PREFER,
                    "return=representation; include=\"" + LDP.PreferMembership.getIRIString() + "\"; " +
                    "omit=\"" + LDP.PreferMinimalContainer.getIRIString() + " " + LDP.PreferContainment.getIRIString() +
                    "\"").get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check a container response (1)", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            size = g.size();
        }
        try (final Response res = target(getContainerLocation()).request().header(PREFER,
                    "return=representation; omit=\"" + LDP.PreferMembership.getIRIString() + " " +
                    LDP.PreferMinimalContainer.getIRIString() + " " + LDP.PreferContainment.getIRIString() +
                    "\"").get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check a container response (2)", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            assertEquals(size, g.size(), "Verify that no membership triples are present");
        }
    }

    /**
     * Test fetching a basic container.
     * @throws Exception when the RDF resource doesn not close cleanly
     */
    @Test
    @DisplayName("Test fetching a basic container")
    default void testGetContainer() throws Exception {
        final RDF rdf = getInstance();
        try (final Response res = target(getContainerLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check an LDP-BC response", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertAll("Check the RDF graph", checkRdfGraph(g, identifier));
            assertTrue(res.getEntityTag().isWeak(), "Verify that the ETag is weak");
        }
    }

    /**
     * Test creating a basic container via POST.
     * @throws Exception when the RDF resource doesn not close cleanly
     */
    @Test
    @DisplayName("Test creating a basic container via POST")
    default void testCreateContainerViaPost() throws Exception {
        final RDF rdf = getInstance();
        final String containerContent = getResourceAsString(BASIC_CONTAINER);
        final String child3;

        // First fetch the container headers to get the initial ETag
        final EntityTag initialETag = getETag(getContainerLocation());

        // POST an LDP-BC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertAll("Check the LDP-BC response", checkRdfResponse(res, LDP.BasicContainer, null));

            child3 = res.getLocation().toString();
            assertTrue(child3.startsWith(getContainerLocation()), "Check the Location header");
            assertTrue(child3.length() > getContainerLocation().length(), "Check the Location header again");
        }

        await().until(() -> !initialETag.equals(getETag(getContainerLocation())));

        // Now fetch the container
        try (final Response res = target(getContainerLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the LDP-BC again", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getContainerLocation());
            final EntityTag etag = res.getEntityTag();
            assertAll("Check the LDP-BC graph", checkRdfGraph(g, identifier));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child3)));
            assertTrue(etag.isWeak(), "Verify that the ETag is weak");
            assertNotEquals(initialETag, etag, "Check that the ETag has been changed");
        }
    }

    /**
     * Test creating a child resource via PUT.
     * @throws Exception when the RDF resource doesn not close cleanly
     */
    @Test
    @DisplayName("Test creating a child resource via PUT")
    default void testCreateContainerViaPut() throws Exception {
        final RDF rdf = getInstance();
        final String containerContent = getResourceAsString(BASIC_CONTAINER);
        final String child4 = getContainerLocation() + "/child4";
        final boolean createUncontained = getConfig().getOptionalValue(CONFIG_HTTP_PUT_UNCONTAINED, Boolean.class)
            .orElse(Boolean.FALSE);

        // First fetch the container headers to get the initial ETag
        final EntityTag initialETag = getETag(getContainerLocation());

        try (final Response res = target(child4).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .put(entity(containerContent, TEXT_TURTLE))) {
            assertAll("Check PUTting an LDP-BC", checkRdfResponse(res, LDP.BasicContainer, null));
        }

        // Now fetch the resource
        try (final Response res = target(getContainerLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check an LDP-BC after PUT", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getContainerLocation());
            final EntityTag etag = res.getEntityTag();
            assertAll("Check the resulting graph", checkRdfGraph(g, identifier));
            if (createUncontained) {
                assertFalse(g.contains(identifier, LDP.contains, rdf.createIRI(child4)),
                        "Check for the absense of an ldp:contains triple");
                assertEquals(initialETag, etag, "Check ETags");
            } else {
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child4)),
                        "Check for the presence of an ldp:contains triple");
                assertNotEquals(initialETag, etag, "Check ETags");
            }
            assertTrue(etag.isWeak(), "Check for a weak ETag");
        }
    }

    /**
     * Test creating a child resource with a Slug header.
     * @throws Exception when the RDF resource doesn not close cleanly
     */
    @Test
    @DisplayName("Test creating a child resource with a Slug header")
    default void testCreateContainerWithSlug() throws Exception {
        final RDF rdf = getInstance();
        final String containerContent = getResourceAsString(BASIC_CONTAINER);
        final String child5 = getContainerLocation() + "/child5";

        // First fetch the container headers to get the initial ETag
        final EntityTag initialETag = getETag(getContainerLocation());

        // POST an LDP-BC
        try (final Response res = target(getContainerLocation()).request().header("Slug", "child5")
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertAll("Check POSTing an LDP-BC", checkRdfResponse(res, LDP.BasicContainer, null));
            assertEquals(child5, res.getLocation().toString(), "Check the resource location");
        }

        await().until(() -> !initialETag.equals(getETag(getContainerLocation())));

        // Now fetch the resource
        try (final Response res = target(getContainerLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check GETting the new resource", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getContainerLocation());
            final EntityTag etag = res.getEntityTag();
            assertAll("Check the resulting Graph", checkRdfGraph(g, identifier));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child5)), "Check for an ldp:contains triple");
            assertTrue(etag.isWeak(), "Check for a weak ETag");
            assertNotEquals(initialETag, etag, "Compare ETags 1 and 4");
        }
    }

    /**
     * Test deleting a basic container.
     * @throws Exception when the RDF resource doesn not close cleanly
     */
    @Test
    @DisplayName("Test deleting a basic container")
    default void testDeleteContainer() throws Exception {
        final RDF rdf = getInstance();
        final String childResource;

        final EntityTag initialETag = getETag(getContainerLocation());

        try (final Response res = target(getContainerLocation()).request().post(entity("", TEXT_TURTLE))) {
            assertAll("Check for an LDP-RS", checkRdfResponse(res, LDP.RDFSource, null));
            childResource = res.getLocation().toString();
        }

        await().until(() -> !initialETag.equals(getETag(getContainerLocation())));

        final EntityTag etag;
        try (final Response res = target(getContainerLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check for an LDP-BC", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final IRI identifier = rdf.createIRI(getContainerLocation());
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

        try (final Response res = target(getContainerLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertAll("Check the parent container", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            assertFalse(g.contains(rdf.createIRI(getContainerLocation()), LDP.contains,
                        rdf.createIRI(childResource)), "Check the graph doesn't contain the deleted resource");
            assertTrue(res.getEntityTag().isWeak(), "Check that the ETag is weak");
            assertNotEquals(etag, res.getEntityTag(), "Verify that the ETag value is different");
        }
    }
}
