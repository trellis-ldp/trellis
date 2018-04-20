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
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.getResourceAsString;
import static org.trellisldp.test.TestUtils.hasConstrainedBy;
import static org.trellisldp.test.TestUtils.hasType;
import static org.trellisldp.test.TestUtils.meanwhile;
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
     * Set the location of the annotation resource.
     * @param location the location
     */
    void setAnnotationLocation(String location);

    /**
     * Get the location of the annotation resource.
     * @return the test annotation location
     */
    String getAnnotationLocation();

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
     * @return the etag
     */
    EntityTag getFirstETag();

    /**
     * Get the second etag.
     * @return the etag
     */
    EntityTag getSecondETag();

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
        final String containerContent = getResourceAsString(BASIC_CONTAINER);
        final String content = getResourceAsString(SIMPLE_RESOURCE);
        final String annotation = getResourceAsString(ANNOTATION_RESOURCE);

        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

            setContainerLocation(res.getLocation().toString());
        }

        // POST an LDP-RS
        try (final Response res = target(getContainerLocation()).request().post(entity(content, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            setResourceLocation(res.getLocation().toString());
        }

        // POST an LDP-RS
        try (final Response res = target(getContainerLocation()).request().post(entity(annotation, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            setAnnotationLocation(res.getLocation().toString());
        }
    }

    /**
     * Fetch the default RDF serialization.
     */
    @Test
    @DisplayName("Fetch the default RDF serialization")
    default void testGetDefault() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        }
    }

    /**
     * Fetch the default JSON-LD serialization.
     */
    @Test
    @DisplayName("Fetch the default JSON-LD serialization")
    default void testGetJsonLdDefault() {
        try (final Response res = target(getAnnotationLocation()).request().accept("application/ld+json").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
            assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            final List<Map<String, Object>> obj = readEntityAsJson(res.getEntity(),
                    new TypeReference<List<Map<String, Object>>>(){});
            assertEquals(1L, obj.size());
            assertTrue(obj.get(0).containsKey("@id"));
            assertTrue(obj.get(0).containsKey("@type"));
            assertTrue(obj.get(0).containsKey("http://www.w3.org/ns/oa#hasBody"));
            assertTrue(obj.get(0).containsKey("http://www.w3.org/ns/oa#hasTarget"));
        }
    }

    /**
     * Fetch the expanded JSON-LD serialization.
     */
    @Test
    @DisplayName("Fetch the expanded JSON-LD serialization")
    default void testGetJsonLdExpanded() {
        try (final Response res = target(getAnnotationLocation()).request()
                .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#expanded\"").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
            assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            final List<Map<String, Object>> obj = readEntityAsJson(res.getEntity(),
                    new TypeReference<List<Map<String, Object>>>(){});
            assertEquals(1L, obj.size());
            assertTrue(obj.get(0).containsKey("@id"));
            assertTrue(obj.get(0).containsKey("@type"));
            assertTrue(obj.get(0).containsKey("http://www.w3.org/ns/oa#hasBody"));
            assertTrue(obj.get(0).containsKey("http://www.w3.org/ns/oa#hasTarget"));
        }
    }

    /**
     * Fetch the compacted JSON-LD serialization.
     */
    @Test
    @DisplayName("Fetch the compacted JSON-LD serialization")
    default void testGetJsonLdCompacted() {
        try (final Response res = target(getAnnotationLocation()).request()
                .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
            assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            final Map<String, Object> obj = readEntityAsJson(res.getEntity(),
                    new TypeReference<Map<String, Object>>(){});
            assertTrue(obj.containsKey("@id"));
            assertTrue(obj.containsKey("@type"));
            assertTrue(obj.keySet().stream().anyMatch(key -> key.endsWith("hasBody")));
            assertTrue(obj.keySet().stream().anyMatch(key -> key.endsWith("hasTarget")));
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

        try (final Response res = target(getAnnotationLocation()).request()
                .accept("application/ld+json; profile=\"http://www.w3.org/ns/anno.jsonld\"").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
            assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            final Map<String, Object> obj = readEntityAsJson(res.getEntity(),
                    new TypeReference<Map<String, Object>>(){});
            assertTrue(obj.containsKey("@context"));
            assertEquals("http://www.w3.org/ns/anno.jsonld", obj.get("@context"));
            assertEquals(getAnnotationLocation(), obj.get("id"));
            assertEquals("Annotation", obj.get("type"));
            assertEquals("http://example.org/post1", obj.get("body"));
            assertEquals("http://example.org/page1", obj.get("target"));
        }
    }

    /**
     * Fetch the N-Triples serialization.
     */
    @Test
    @DisplayName("Fetch the N-Triples serialization")
    default void testGetNTriples() {
        try (final Response res = target(getResourceLocation()).request().accept("application/n-triples").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_N_TRIPLES_TYPE));
            assertTrue(APPLICATION_N_TRIPLES_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
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
        try (final Response res = target(getContainerLocation()).request().post(entity(content, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            final String location = res.getLocation().toString();
            assertTrue(location.startsWith(getContainerLocation()));
            assertTrue(location.length() > getContainerLocation().length());
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
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            assertEquals(3L, g.size());
            final IRI identifier = rdf.createIRI(getResourceLocation());
            assertTrue(g.contains(identifier, type, SKOS.Concept));
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")));
            assertTrue(g.contains(identifier, DC.subject, rdf.createIRI("http://example.org/subject/1")));
            setFirstETag(res.getEntityTag());
            assertTrue(getFirstETag().isWeak());
            assertNotEquals(getFirstETag(), getSecondETag());
        }
    }

    /**
     * Test modifying an RDF document via PATCH.
     */
    @Test
    @DisplayName("Test modifying an RDF document via PATCH")
    default void testPatchRDF() {
        final RDF rdf = getInstance();
        meanwhile();

        // Patch the resource
        try (final Response res = target(getResourceLocation()).request().method("PATCH",
                    entity("INSERT { <> a <http://www.w3.org/ns/ldp#Container> ; "
                        + "<http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        }

        meanwhile();

        // Fetch the updated resource
        try (final Response res = target(getResourceLocation()).request().accept("application/n-triples").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_N_TRIPLES_TYPE));
            assertTrue(APPLICATION_N_TRIPLES_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), NTRIPLES);
            assertEquals(4L, g.size());
            assertTrue(g.contains(rdf.createIRI(getResourceLocation()), DC.title, rdf.createLiteral("Title")));
            setSecondETag(res.getEntityTag());
            assertTrue(getSecondETag().isWeak());
            assertNotEquals(getFirstETag(), getSecondETag());
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
     * Test creating resource with invalid RDF.
     */
    @Test
    @DisplayName("Test creating resource with invalid RDF")
    default void testWeirdRDF() {
        final String rdf = getResourceAsString(SIMPLE_RESOURCE)
            + "<> a \"skos concept\" .";

        // POST an LDP-RS
        try (final Response res = target(getContainerLocation()).request().post(entity(rdf, TEXT_TURTLE))) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidRange)));
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
        try (final Response res = target(getContainerLocation()).request().post(entity(rdf, TEXT_TURTLE))) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
        }
    }
}
