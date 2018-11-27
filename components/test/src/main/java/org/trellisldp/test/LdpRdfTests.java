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
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.getResourceAsString;
import static org.trellisldp.test.TestUtils.hasConstrainedBy;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;
import static org.trellisldp.test.TestUtils.readEntityAsJson;
import static org.trellisldp.vocabulary.RDF.type;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.trellisldp.vocabulary.Trellis;

/**
 * Test the RDF responses to LDP resources.
 */
@TestInstance(PER_CLASS)
public interface LdpRdfTests extends CommonTests {

    String SIMPLE_RESOURCE = "/simpleResource.ttl";
    String BASIC_CONTAINER = "/basicContainer.ttl";
    String ANNOTATION_RESOURCE = "/annotation.ttl";

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
     * Return a set of valid JSON-LD profiles that the server supports.
     * @return the JSON-LD profiles
     */
    Set<String> supportedJsonLdProfiles();

    /**
     * Initialize the RDF tests.
     */
    @BeforeAll
    @DisplayName("Initialize RDF tests")
    default void beforeAllTests() {
        final String content = getResourceAsString(SIMPLE_RESOURCE);

        // POST an LDP-RS
        try (final Response res = target().request().header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .post(entity(content, TEXT_TURTLE))) {
            setResourceLocation(checkCreateResponseAssumptions(res, LDP.RDFSource));
        }

    }

    /**
     * Fetch the default RDF serialization.
     */
    @Test
    @DisplayName("Fetch the default RDF serialization")
    default void testGetDefault() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertAll("Check for an LDP-RS as Turtle", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
        }
    }

    /**
     * Fetch the default JSON-LD serialization.
     */
    @Test
    @DisplayName("Fetch the default JSON-LD serialization")
    default void testGetJsonLdDefault() {
        final String location = createAnnotationResource();

        try (final Response res = target(location).request().accept("application/ld+json").get()) {
            assertAll("Check for an LDP-RS as JSONLD", checkRdfResponse(res, LDP.RDFSource, APPLICATION_LD_JSON_TYPE));
            final Map<String, Object> obj = readEntityAsJson(res.getEntity(),
                    new TypeReference<Map<String, Object>>(){});
            assertTrue(obj.containsKey("@type"), "Check for a @type property");
            assertTrue(obj.containsKey("@id"), "Check for an @id property");
            assertTrue(obj.keySet().stream().anyMatch(key -> key.endsWith("hasTarget")), "Check for a hasTarget prop");
            assertTrue(obj.keySet().stream().anyMatch(key -> key.endsWith("hasBody")), "Check for a hasBody property");
        }
    }

    /**
     * Fetch the expanded JSON-LD serialization.
     */
    @Test
    @DisplayName("Fetch the expanded JSON-LD serialization")
    default void testGetJsonLdExpanded() {
        final String location = createAnnotationResource();

        try (final Response res = target(location).request()
                .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#expanded\"").get()) {
            assertAll("Check for expanded JSONLD", checkRdfResponse(res, LDP.RDFSource, APPLICATION_LD_JSON_TYPE));
            final List<Map<String, Object>> obj = readEntityAsJson(res.getEntity(),
                    new TypeReference<List<Map<String, Object>>>(){});
            assertEquals(1L, obj.size(), "Check json structure");
            assertTrue(obj.get(0).containsKey("@id"), "Check for an @id property");
            assertTrue(obj.get(0).containsKey("@type"), "Check for a @type property");
            assertTrue(obj.get(0).containsKey("http://www.w3.org/ns/oa#hasBody"), "Check for an oa:hasBody prop");
            assertTrue(obj.get(0).containsKey("http://www.w3.org/ns/oa#hasTarget"), "Check for an oa:hasTarget prop");
        }
    }

    /**
     * Fetch the compacted JSON-LD serialization.
     */
    @Test
    @DisplayName("Fetch the compacted JSON-LD serialization")
    default void testGetJsonLdCompacted() {
        final String location = createAnnotationResource();

        try (final Response res = target(location).request()
                .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get()) {
            assertAll("Check for compact JSONLD", checkRdfResponse(res, LDP.RDFSource, APPLICATION_LD_JSON_TYPE));
            final Map<String, Object> obj = readEntityAsJson(res.getEntity(),
                    new TypeReference<Map<String, Object>>(){});
            assertTrue(obj.containsKey("@id"), "Check for an @id property");
            assertTrue(obj.containsKey("@type"), "Check for a @type property");
            assertTrue(obj.keySet().stream().anyMatch(key -> key.endsWith("hasBody")), "Check for a hasBody property");
            assertTrue(obj.keySet().stream().anyMatch(key -> key.endsWith("hasTarget")), "Check for a hasTarget prop");
        }
    }

    /**
     * Fetch a JSON-LD serialization with a custom profile.
     */
    @Test
    @DisplayName("Fetch the JSON-LD serialization with a custom profile")
    default void testGetJsonLdAnnotationProfile() {

        assumeTrue(supportedJsonLdProfiles().contains("http://www.w3.org/ns/anno.jsonld"),
                "Support for the Web Annotation profile is not enabled.");

        final String location = createAnnotationResource();

        try (final Response res = target(location).request()
                .accept("application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\"").get()) {
            assertAll("Check for custom JSONLD", checkRdfResponse(res, LDP.RDFSource, APPLICATION_LD_JSON_TYPE));
            final Map<String, Object> obj = readEntityAsJson(res.getEntity(),
                    new TypeReference<Map<String, Object>>(){});
            assertTrue(obj.containsKey("@context"), "Check for a @context property");
            assertEquals("http://www.w3.org/ns/anno.jsonld", obj.get("@context"), "Check the @context value");
            assertEquals(location, obj.get("id"), "Check the id value");
            assertEquals("Annotation", obj.get("type"), "Check the type value");
            assertEquals("http://example.org/post1", obj.get("body"), "Check the body value");
            assertEquals("http://example.org/page1", obj.get("target"), "Check the target value");
        }
    }

    /**
     * Fetch the N-Triples serialization.
     */
    @Test
    @DisplayName("Fetch the N-Triples serialization")
    default void testGetNTriples() {
        try (final Response res = target(getResourceLocation()).request().accept("application/n-triples").get()) {
            assertAll("Check for N-Triples", checkRdfResponse(res, LDP.RDFSource, APPLICATION_N_TRIPLES_TYPE));
        }
    }

    /**
     * Test POSTing an RDF resource.
     */
    @Test
    @DisplayName("Test POSTing an RDF resource")
    default void testPostRDF() {
        final String content = getResourceAsString(SIMPLE_RESOURCE);

        // POST an LDP-RS
        try (final Response res = target().request().header(SLUG, generateRandomValue(getClass().getSimpleName()))
                    .post(entity(content, TEXT_TURTLE))) {
            assertAll("Check POSTing an RDF resource", checkRdfResponse(res, LDP.RDFSource, null));
            final String location = res.getLocation().toString();
            assertTrue(location.startsWith(getBaseURL()), "Check the Location header");
            assertTrue(location.length() > getBaseURL().length(), "Re-check the Location header");
        }
    }

    /**
     * Test fetching an RDF resource.
     */
    @Test
    @DisplayName("Test fetching an RDF resource")
    default void testGetRDF() {
        final RDF rdf = getInstance();
        // Fetch the new resource
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertAll("Check an LDP-RS resource", checkRdfResponse(res, LDP.RDFSource, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertEquals(3L, g.size(), "Check the size of the resulting graph");
            final IRI identifier = rdf.createIRI(getResourceLocation());
            assertTrue(g.contains(identifier, type, SKOS.Concept), "Check for a rdf:type triple");
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")),
                    "Check for a skos:prefLabel triple");
            assertTrue(g.contains(identifier, DC.subject, rdf.createIRI("http://example.org/subject/1")),
                    "Check for a dc:subject triple");
            assertTrue(res.getEntityTag().isWeak(), "Check that the ETag is weak");
        }
    }

    /**
     * Test modifying an RDF document via PATCH.
     */
    @Test
    @DisplayName("Test modifying an RDF document via PATCH")
    default void testPatchRDF() {
        final RDF rdf = getInstance();
        final EntityTag initialETag = getETag(getResourceLocation());

        // Patch the resource
        try (final Response res = target(getResourceLocation()).request().method("PATCH",
                    entity("INSERT { <> a <http://www.w3.org/ns/ldp#Container> ; "
                        + "<http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertAll("Check an LDP-RS resource", checkRdfResponse(res, LDP.RDFSource, null));
        }

        await().until(() -> !initialETag.equals(getETag(getResourceLocation())));

        // Fetch the updated resource
        try (final Response res = target(getResourceLocation()).request().accept("application/n-triples").get()) {
            assertAll("Check an updated resource", checkRdfResponse(res, LDP.RDFSource, APPLICATION_N_TRIPLES_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), NTRIPLES);
            assertEquals(4L, g.size(), "Check the graph size");
            assertTrue(g.contains(rdf.createIRI(getResourceLocation()), DC.title, rdf.createLiteral("Title")),
                    "Check for a dc:title triple");
            assertTrue(res.getEntityTag().isWeak(), "Check that the ETag is weak");
            assertNotEquals(initialETag, res.getEntityTag(), "Compare the first and second ETags");
        }
    }

    /**
     * Verify that the correct containment triples exist.
     */
    @Test
    @DisplayName("Verify that the correct containment triples exist")
    default void testRdfContainment() {
        final RDF rdf = getInstance();
        // Test the root container, verifying that the containment triple exists
        try (final Response res = target().request().get()) {
            assertAll("Check a container resource", checkRdfResponse(res, LDP.BasicContainer, TEXT_TURTLE_TYPE));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertTrue(g.contains(rdf.createIRI(getBaseURL()), LDP.contains,
                        rdf.createIRI(getResourceLocation())), "Check for an ldp:contains property");
        }
    }

    /**
     * Test creating resource with invalid RDF.
     */
    @Test
    @DisplayName("Test creating resource with invalid RDF")
    default void testWeirdRDF() {
        final String rdf = getResourceAsString(SIMPLE_RESOURCE)
            + "<> a \"skos concept\" .";

        // POST an LDP-RS
        try (final Response res = target().request().header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .post(entity(rdf, TEXT_TURTLE))) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(),
                    "Semantically invalid RDF should throw a 4xx error");
            assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidRange)),
                    "Check for an InvalidRange constraint for weird rdf:type constructs");
        }
    }

    /**
     * Test creating resource with syntactically invalid RDF.
     */
    @Test
    @DisplayName("Test creating resource with syntactically invalid RDF")
    default void testInvalidRDF() {
        final String rdf
            = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> a skos:Concept \n"
            + "   skos:prefLabel \"Resource Name\"@eng \n"
            + "   dc:subject <http://example.org/subject/1> .";

        // POST an LDP-RS
        try (final Response res = target().request().header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .post(entity(rdf, TEXT_TURTLE))) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Syntactically invalid RDF produces a 4xx");
        }
    }

    /**
     * Create an Annotation resource.
     * @return the location of the new resource
     */
    default String createAnnotationResource() {
        // POST an LDP-RS
        try (final Response res = target().request().header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .post(entity(getResourceAsString(ANNOTATION_RESOURCE), TEXT_TURTLE))) {
            assertAll("Check for an LDP-RS", checkRdfResponse(res, LDP.RDFSource, null));
            return res.getLocation().toString();
        }
    }
}
