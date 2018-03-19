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
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.HttpConstants.DIGEST;
import static org.trellisldp.http.domain.HttpConstants.WANT_DIGEST;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.getResourceAsString;
import static org.trellisldp.test.TestUtils.hasType;
import static org.trellisldp.test.TestUtils.meanwhile;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;
import static org.trellisldp.test.TestUtils.readEntityAsString;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;

/**
 * Run LDP Binary-related tests on a Trellis application.
 */
@TestInstance(PER_CLASS)
@DisplayName("Binary resource tests")
public interface LdpBinaryTests extends CommonTests {

    String CONTENT = "This is a file.";
    String BASIC_CONTAINER = "/basicContainer.ttl";

    /**
     * Set the location of the test resource.
     * @param location the location
     */
    void setResourceLocation(String location);

    /**
     * Get the location of the test resource.
     * @return the test resource location
     */
    String getResourceLocation();

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
     * Get the first etag.
     * @return the etag, may be null
     */
    EntityTag getFirstETag();

    /**
     * Set the first etag.
     * @param etag the etag
     */
    void setFirstETag(EntityTag etag);

    /**
     * Get the second etag.
     * @return the etag, may be null
     */
    EntityTag getSecondETag();

    /**
     * Set the second etag.
     * @param etag the etag
     */
    void setSecondETag(EntityTag etag);

    /**
     * Initialize Binary tests.
     */
    @BeforeAll
    @DisplayName("Initialize Binary tests")
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

        // POST an LDP-NR
        try (final Response res = target(getContainerLocation()).request().post(entity(CONTENT, TEXT_PLAIN))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));

            setResourceLocation(res.getLocation().toString());
        }
    }

    /**
     * Test fetching a binary resource.
     */
    @Test
    @DisplayName("Test fetching a binary resource")
    default void testGetBinary() {
        // Fetch the new resource
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
            assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            assertEquals(CONTENT, readEntityAsString(res.getEntity()));
            assertFalse(res.getEntityTag().isWeak());
            setFirstETag(res.getEntityTag());
            assertNotEquals(getFirstETag(), getSecondETag());
        }
    }

    /**
     * Test fetching a binary description.
     */
    @Test
    @DisplayName("Test fetching a binary description")
    default void testGetBinaryDescription() {
        // Fetch the description
        try (final Response res = target(getResourceLocation()).request().accept("text/turtle").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertTrue(g.size() >= 0L);
            assertTrue(res.getEntityTag().isWeak());
            setSecondETag(res.getEntityTag());
            assertNotEquals(getFirstETag(), getSecondETag());
        }
    }

    /**
     * Test creating a new binary via POST.
     */
    @Test
    @DisplayName("Test creating a new binary via POST")
    default void testPostBinary() {
        // POST an LDP-NR
        try (final Response res = target(getContainerLocation()).request().post(entity(CONTENT, TEXT_PLAIN))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            final String location = res.getLocation().toString();
            assertTrue(location.startsWith(getContainerLocation()));
            assertTrue(location.length() > getContainerLocation().length());
        }
    }

    /**
     * Test creating a new binary via POST with a digest header.
     */
    @Test
    @DisplayName("Test creating a new binary via POST with a digest header")
    default void testPostBinaryWithDigest() {
        // POST an LDP-NR
        try (final Response res = target(getContainerLocation()).request()
                .header(DIGEST, "md5=bUMuG430lSc5B2PWyoNIgA==").post(entity(CONTENT, TEXT_PLAIN))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            final String resource = res.getLocation().toString();
            assertTrue(resource.startsWith(getContainerLocation()));
            assertTrue(resource.length() > getContainerLocation().length());
        }
    }

    /**
     * Test modifying a binary's description via PATCH.
     */
    @Test
    @DisplayName("Test modifying a binary's description via PATCH")
    default void testPatchBinaryDescription() {
        final RDF rdf = getInstance();
        final EntityTag etag;

        // Fetch the description
        try (final Response res = target(getResourceLocation()).request().accept("text/turtle").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertEquals(0L, g.size());
            etag = res.getEntityTag();
            assertTrue(etag.isWeak());
            assertNotEquals(getFirstETag(), etag);
        }

        meanwhile();

        // Patch the description
        try (final Response res = target(getResourceLocation()).request().method("PATCH",
                    entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        }

        // Fetch the new description
        try (final Response res = target(getResourceLocation()).request().accept("text/turtle").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertEquals(1L, g.size());
            assertTrue(g.contains(rdf.createIRI(getResourceLocation()), DC.title, rdf.createLiteral("Title")));
            assertNotEquals(etag, res.getEntityTag());
        }

        // Verify that the binary is still accessible
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
            assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertEquals(CONTENT, readEntityAsString(res.getEntity()));
            assertEquals(getFirstETag(), res.getEntityTag());
        }
    }

    /**
     * Test that the binary appears in the parent container.
     */
    @Test
    @DisplayName("Test that the binary appears in the parent container")
    default void testBinaryIsInContainer() {
        final RDF rdf = getInstance();
        // Test the root container, verifying that the containment triple exists
        try (final Response res = target(getContainerLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertTrue(g.contains(rdf.createIRI(getContainerLocation()), LDP.contains,
                        rdf.createIRI(getResourceLocation())));
        }
    }

    /**
     * Test that the SHA digest is generated.
     */
    @Test
    @DisplayName("Test that the SHA digest is generated")
    default void testBinaryWantDigestSha() {
        // Test the SHA-1 algorithm
        try (final Response res = target(getResourceLocation()).request().header(WANT_DIGEST, "SHA,MD5").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
            assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
            assertEquals("sha=Z5pg2cWB1IqkKKMjh57cQKAeKp0=", res.getHeaderString(DIGEST));
        }
    }

    /**
     * Test that the SHA-256 digest is generated.
     */
    @Test
    @DisplayName("Test that the SHA-256 digest is generated")
    default void testBinaryWantDigestSha256() {
        // Test the SHA-256 algorithm
        try (final Response res = target(getResourceLocation()).request().header(WANT_DIGEST, "SHA-256").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
            assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
            assertEquals("sha-256=wZXqBpAjgZLSoADF419CRpJCurDcagOwnb/8VAiiQXA=", res.getHeaderString(DIGEST));
        }
    }

    /**
     * Test that an unknown digest is ignored.
     */
    @Test
    @DisplayName("Test that an unknown digest is ignored")
    default void testBinaryWantDigestUnknown() {
        // Test an unknown digest algorithm
        try (final Response res = target(getResourceLocation()).request().header(WANT_DIGEST, "FOO").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
            assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
            assertNull(res.getHeaderString(DIGEST));
        }
    }
}
