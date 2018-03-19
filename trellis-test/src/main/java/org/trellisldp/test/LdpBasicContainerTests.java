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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    String ENG = "eng";
    String BASIC_CONTAINER = "/basicContainer.ttl";
    String BASIC_CONTAINER_LABEL = "Basic Container";

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
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

            setContainerLocation(res.getLocation().toString());
        }

        // POST an LDP-BC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

            setChildLocation(res.getLocation().toString());
        }

        // POST an LDP-BC
        try (final Response res = target(getContainerLocation()).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
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
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
            assertFalse(g.contains(identifier, LDP.contains, null));
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
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertFalse(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
            assertTrue(g.contains(identifier, LDP.contains, null));
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
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.size() >= 2);
            setFirstETag(res.getEntityTag());
            assertTrue(getFirstETag().isWeak());
            assertNotEquals(getFirstETag(), getSecondETag());
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
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

            child3 = res.getLocation().toString();
            assertTrue(child3.startsWith(getContainerLocation()));
            assertTrue(child3.length() > getContainerLocation().length());
        }

        // Now fetch the container
        try (final Response res = target(getContainerLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child3)));
            assertTrue(g.size() >= 3);
            setSecondETag(res.getEntityTag());
            assertTrue(getSecondETag().isWeak());
            assertNotEquals(getFirstETag(), getSecondETag());
            assertNotEquals(getThirdETag(), getSecondETag());
            assertNotEquals(getFourthETag(), getSecondETag());
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
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
        }

        // Now fetch the resource
        try (final Response res = target(getContainerLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child4)));
            assertTrue(g.size() >= 3);
            setThirdETag(res.getEntityTag());
            assertTrue(getThirdETag().isWeak());
            assertNotEquals(getFirstETag(), getThirdETag());
            assertNotEquals(getSecondETag(), getThirdETag());
            assertNotEquals(getFourthETag(), getThirdETag());
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
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

            assertEquals(child5, res.getLocation().toString());
        }

        // Now fetch the resource
        try (final Response res = target(getContainerLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child5)));
            assertTrue(g.size() >= 3);
            setFourthETag(res.getEntityTag());
            assertTrue(getFourthETag().isWeak());
            assertNotEquals(getFirstETag(), getFourthETag());
            assertNotEquals(getSecondETag(), getFourthETag());
            assertNotEquals(getThirdETag(), getFourthETag());
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

        try (final Response res = target(getContainerLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI identifier = rdf.createIRI(getContainerLocation());
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(getChildLocation())));
            assertTrue(g.size() >= 3);
            etag = res.getEntityTag();
            assertTrue(etag.isWeak());
        }

        meanwhile();

        // Delete one of the child resources
        try (final Response res = target(getChildLocation()).request().delete()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        // Try fetching the deleted resource
        try (final Response res = target(getChildLocation()).request().get()) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
        }

        try (final Response res = target(getContainerLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertFalse(g.contains(rdf.createIRI(getContainerLocation()), LDP.contains,
                        rdf.createIRI(getChildLocation())));
            assertTrue(res.getEntityTag().isWeak());
            assertNotEquals(etag, res.getEntityTag());
        }
    }
}
