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
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.http.core.HttpConstants.DIGEST;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.HttpConstants.WANT_DIGEST;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
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
     * Initialize Binary tests.
     */
    @BeforeAll
    @DisplayName("Initialize Binary tests")
    default void beforeAllTests() {

        // POST an LDP-NR
        try (final Response res = target().request().header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .post(entity(CONTENT, TEXT_PLAIN))) {
            setResourceLocation(checkCreateResponseAssumptions(res, LDP.NonRDFSource));
        }
    }

    /**
     * Test fetching a binary resource.
     */
    @Test
    @DisplayName("Test fetching a binary resource")
    default void testGetBinary() {
        // Fetch the resource
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertAll("Check binary resource", checkNonRdfResponse(res, TEXT_PLAIN_TYPE));
            assertEquals(CONTENT, readEntityAsString(res.getEntity()), "Check for matching content");
            assertFalse(res.getEntityTag().isWeak(), "Check for a strong ETag");
        }
    }

    /**
     * Test fetching a binary description.
     */
    @Test
    @DisplayName("Test fetching a binary description")
    default void testGetBinaryDescription() {
        final EntityTag etag = getETag(getResourceLocation());

        // Fetch the description
        try (final Response res = target(getResourceLocation()).request().accept("text/turtle").get()) {
            assertAll("Check binary description", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertTrue(g.size() >= 0L, "Assert that the graph isn't empty");
            assertTrue(res.getEntityTag().isWeak(), "Check for a weak ETag");
            assertNotEquals(etag, res.getEntityTag(), "Check for different ETag values");
        }
    }

    /**
     * Test creating a new binary via POST.
     */
    @Test
    @DisplayName("Test creating a new binary via POST")
    default void testPostBinary() {
        // POST an LDP-NR
        try (final Response res = target().request().header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .post(entity(CONTENT, TEXT_PLAIN))) {
            assertAll("Check POSTing LDP-NR", checkNonRdfResponse(res, null));
            final String resource = res.getLocation().toString();
            assertTrue(resource.startsWith(getBaseURL()), "Check the response location");
            assertTrue(resource.length() > getBaseURL().length(), "Check for a nested response location");
        }
    }

    /**
     * Test creating a new binary via POST with a digest header.
     */
    @Test
    @DisplayName("Test creating a new binary via POST with a digest header")
    default void testPostBinaryWithDigest() {
        // POST an LDP-NR
        try (final Response res = target().request().header(DIGEST, "md5=bUMuG430lSc5B2PWyoNIgA==")
                .header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .post(entity(CONTENT, TEXT_PLAIN))) {
            assertAll("Check POSTing LDP-NR with digest", checkNonRdfResponse(res, null));
            final String resource = res.getLocation().toString();
            assertTrue(resource.startsWith(getBaseURL()), "Check the response location");
            assertTrue(resource.length() > getBaseURL().length(), "Check for a nested response location");
        }
    }

    /**
     * Test modifying a binary's description via PATCH.
     */
    @Test
    @DisplayName("Test modifying a binary's description via PATCH")
    default void testPatchBinaryDescription() {
        final RDF rdf = getInstance();
        final EntityTag descriptionETag;
        final Long size;

        // Fetch the description
        try (final Response res = target(getResourceLocation()).request().accept("text/turtle").get()) {
            assertAll("Check an LDP-NR description", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            size = g.size();
            descriptionETag = res.getEntityTag();
            assertTrue(descriptionETag.isWeak(), "Check for a weak ETag");
        }

        // Patch the description
        try (final Response res = target(getResourceLocation()).request().method("PATCH",
                    entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertAll("Check PATCHing LDP-NR description", checkRdfResponse(res, LDP.RDFSource, null));
        }

        await().until(() -> !descriptionETag.equals(getETag(getResourceLocation())));

        // Fetch the new description
        try (final Response res = target(getResourceLocation()).request().accept("text/turtle").get()) {
            assertAll("Check the new LDP-NR description", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertTrue(g.size() > size, "Check the graph size is greater than " + size);
            assertTrue(g.contains(rdf.createIRI(getResourceLocation()), DC.title, rdf.createLiteral("Title")),
                    "Check for a dc:title triple");
            assertNotEquals(descriptionETag, res.getEntityTag(), "Check that the ETag values are different");
        }

        // Verify that the binary is still accessible
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertAll("Check the LDP-NR", checkNonRdfResponse(res, TEXT_PLAIN_TYPE));
            assertEquals(CONTENT, readEntityAsString(res.getEntity()), "Check for an expected binary content value");
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
        try (final Response res = target().request().get()) {
            assertAll("Check binary in container", checkRdfResponse(res, LDP.BasicContainer, null));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertTrue(g.contains(rdf.createIRI(getBaseURL()), LDP.contains,
                        rdf.createIRI(getResourceLocation())), "Check for an ldp:contains triple");
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
            assertAll("Check binary with SHA-1 digest", checkNonRdfResponse(res, TEXT_PLAIN_TYPE));
            assertEquals("sha=Z5pg2cWB1IqkKKMjh57cQKAeKp0=", res.getHeaderString(DIGEST), "Check the SHA digest value");
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
            assertAll("Check binary with SHA-256 digest", checkNonRdfResponse(res, TEXT_PLAIN_TYPE));
            assertEquals("sha-256=wZXqBpAjgZLSoADF419CRpJCurDcagOwnb/8VAiiQXA=", res.getHeaderString(DIGEST),
                    "Check the SHA-256 digest value");
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
            assertAll("Check binary with unknown digest", checkNonRdfResponse(res, TEXT_PLAIN_TYPE));
            assertNull(res.getHeaderString(DIGEST), "Check that no Digest header is present");
        }
    }
}
