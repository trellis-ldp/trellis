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

import static java.time.Instant.now;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.http.domain.HttpConstants.DIGEST;
import static org.trellisldp.http.domain.HttpConstants.PREFER;
import static org.trellisldp.http.domain.HttpConstants.WANT_DIGEST;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Trellis;

/**
 * Run LDP-related tests on a Trellis application.
 */
@RunWith(JUnitPlatform.class)
public class LdpTests extends BaseCommonTests {

    private static final String ENG = "eng";
    private static final String BASIC_CONTAINER_LABEL = "Basic Container";
    private static final String DIRECT_CONTAINER = "/directContainer.ttl";
    private static final String INDIRECT_CONTAINER = "/indirectContainer.ttl";
    private static final String INDIRECT_CONTAINER_MEMBER_SUBJECT = "/indirectContainerMemberSubject.ttl";
    private static final String BASIC_CONTAINER = "/basicContainer.ttl";
    private static final String SIMPLE_RESOURCE = "/simpleResource.ttl";
    private static final String MEMBER_RESOURCE1 = "/members1";
    private static final String MEMBER_RESOURCE2 = "/members2";
    private static final String MEMBER_RESOURCE_HASH = "#members";

    /**
     * Tests of LDP-RS (RDF resources).
     */
    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("RDF resource tests")
    public class RDFTests {

        private String container;
        private String resource;
        private EntityTag etag1;
        private EntityTag etag2;
        private final String content = getResourceAsString(SIMPLE_RESOURCE);

        @BeforeAll
        @DisplayName("Initialize RDF tests")
        protected void setUp() {
            final String containerContent = getResourceAsString(BASIC_CONTAINER);

            // POST an LDP-BC
            try (final Response res = target().request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                container = res.getLocation().toString();
            }

            // POST an LDP-RS
            try (final Response res = target(container).request().post(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                resource = res.getLocation().toString();
            }
        }

        /**
         * Fetch the default RDF serialization.
         */
        @Test
        @DisplayName("Fetch the default RDF serialization")
        public void testGetDefault() {
            try (final Response res = target(resource).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }
        }

        /**
         * Fetch the JSON-LD serialization.
         */
        @Test
        @DisplayName("Fetch the JSON-LD serialization")
        public void testGetJsonLd() {
            try (final Response res = target(resource).request().accept("application/ld+json").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
                assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }
        }

        /**
         * Fetch the N-Triples serialization.
         */
        @Test
        @DisplayName("Fetch the N-Triples serialization")
        public void testGetNTriples() {
            try (final Response res = target(resource).request().accept("application/n-triples").get()) {
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
        public void testPostRDF() {

            // POST an LDP-RS
            try (final Response res = target(container).request().post(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                final String location = res.getLocation().toString();
                assertTrue(location.startsWith(container));
                assertTrue(location.length() > container.length());
            }
        }

        /**
         * Test fetching an RDF resource.
         */
        @Test
        @DisplayName("Test fetching an RDF resource")
        public void testGetRDF() {
            // Fetch the new resource
            try (final Response res = target(resource).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertEquals(3L, g.size());
                final IRI identifier = rdf.createIRI(resource);
                assertTrue(g.contains(identifier, type, SKOS.Concept));
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Resource Name", ENG)));
                assertTrue(g.contains(identifier, DC.subject, rdf.createIRI("http://example.org/subject/1")));
                etag1 = res.getEntityTag();
                assertTrue(etag1.isWeak());
                assertNotEquals(etag1, etag2);
            }
        }

        /**
         * Test modifying an RDF document via PATCH.
         */
        @Test
        @DisplayName("Test modifying an RDF document via PATCH")
        public void testPatchRDF() {
            meanwhile();

            // Patch the resource
            try (final Response res = target(resource).request().method(PATCH,
                        entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                            APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }

            meanwhile();

            // Fetch the updated resource
            try (final Response res = target(resource).request().accept("application/n-triples").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(APPLICATION_N_TRIPLES_TYPE));
                assertTrue(APPLICATION_N_TRIPLES_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), NTRIPLES);
                assertEquals(4L, g.size());
                assertTrue(g.contains(rdf.createIRI(resource), DC.title, rdf.createLiteral("Title")));
                etag2 = res.getEntityTag();
                assertTrue(etag2.isWeak());
                assertNotEquals(etag1, etag2);
            }
        }

        /**
         * Verify that the correct containment triples exist.
         */
        @Test
        @DisplayName("Verify that the correct containment triples exist")
        public void testRdfContainment() {
            // Test the root container, verifying that the containment triple exists
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertTrue(g.contains(rdf.createIRI(container), LDP.contains, rdf.createIRI(resource)));
            }
        }

        /**
         * Test creating resource with invalid RDF.
         */
        @Test
        @DisplayName("Test creating resource with invalid RDF")
        public void testWeirdRDF() {
            final String rdf = getResourceAsString(SIMPLE_RESOURCE)
                + "<> a \"skos concept\" .";

            // POST an LDP-RS
            try (final Response res = target(container).request().post(entity(rdf, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidRange)));
            }
        }

        /**
         * Test creating resource with syntactically invalid RDF.
         */
        @Test
        @DisplayName("Test creating resource with syntactically invalid RDF")
        public void testInvalidRDF() {
            final String rdf
                = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
                + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
                + "<> a skos:Concept \n"
                + "   skos:prefLabel \"Resource Name\"@eng \n"
                + "   dc:subject <http://example.org/subject/1> .";

            // POST an LDP-RS
            try (final Response res = target(container).request().post(entity(rdf, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
            }
        }
    }


    /**
     * Tests of LDP-NR (Binary resources).
     */
    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Binary resource tests")
    public class BinaryTests {

        private final String content = "This is a file.";
        private String container;
        private String binary;
        private EntityTag etag1;
        private EntityTag etag2;

        /**
         * Initialize Binary tests.
         */
        @BeforeAll
        @DisplayName("Initialize Binary tests")
        public void init() {
            final String containerContent = getResourceAsString(BASIC_CONTAINER);

            // POST an LDP-BC
            try (final Response res = target().request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                container = res.getLocation().toString();
            }

            // POST an LDP-NR
            try (final Response res = target(container).request().post(entity(content, TEXT_PLAIN))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));

                binary = res.getLocation().toString();
            }
        }

        /**
         * Test fetching a binary resource.
         */
        @Test
        @DisplayName("Test fetching a binary resource")
        public void testGetBinary() {
            // Fetch the new resource
            try (final Response res = target(binary).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
                assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                assertEquals(content, readEntityAsString(res.getEntity()));
                assertFalse(res.getEntityTag().isWeak());
                etag1 = res.getEntityTag();
                assertNotEquals(etag1, etag2);
            }
        }

        /**
         * Test fetching a binary description.
         */
        @Test
        @DisplayName("Test fetching a binary description")
        public void testGetBinaryDescription() {
            // Fetch the description
            try (final Response res = target(binary).request().accept("text/turtle").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertTrue(g.size() >= 0L);
                assertTrue(res.getEntityTag().isWeak());
                etag2 = res.getEntityTag();
                assertNotEquals(etag1, etag2);
            }
        }

        /**
         * Test creating a new binary via POST.
         */
        @Test
        @DisplayName("Test creating a new binary via POST")
        public void testPostBinary() {
            // POST an LDP-NR
            try (final Response res = target(container).request().post(entity(content, TEXT_PLAIN))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                final String location = res.getLocation().toString();
                assertTrue(location.startsWith(container));
                assertTrue(location.length() > container.length());
            }
        }

        /**
         * Test creating a new binary via POST with a digest header.
         */
        @Test
        @DisplayName("Test creating a new binary via POST with a digest header")
        public void testPostBinaryWithDigest() {
            // POST an LDP-NR
            try (final Response res = target(container).request().header(DIGEST, "md5=bUMuG430lSc5B2PWyoNIgA==")
                    .post(entity(content, TEXT_PLAIN))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                final String resource = res.getLocation().toString();
                assertTrue(resource.startsWith(container));
                assertTrue(resource.length() > container.length());
            }
        }

        /**
         * Test modifying a binary's description via PATCH.
         */
        @Test
        @DisplayName("Test modifying a binary's description via PATCH")
        public void testPatchBinaryDescription() {
            final EntityTag etag;

            // Fetch the description
            try (final Response res = target(binary).request().accept("text/turtle").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertEquals(0L, g.size());
                etag = res.getEntityTag();
                assertTrue(etag.isWeak());
                assertNotEquals(etag1, etag);
            }

            meanwhile();

            // Patch the description
            try (final Response res = target(binary).request().method(PATCH,
                        entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                            APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }

            // Fetch the new description
            try (final Response res = target(binary).request().accept("text/turtle").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertEquals(1L, g.size());
                assertTrue(g.contains(rdf.createIRI(binary), DC.title, rdf.createLiteral("Title")));
                assertNotEquals(etag, res.getEntityTag());
            }

            // Verify that the binary is still accessible
            try (final Response res = target(binary).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
                assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertEquals(content, readEntityAsString(res.getEntity()));
                assertEquals(etag1, res.getEntityTag());
            }
        }

        /**
         * Test that the binary appears in the parent container.
         */
        @Test
        @DisplayName("Test that the binary appears in the parent container")
        public void testBinaryIsInContainer() {
            // Test the root container, verifying that the containment triple exists
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertTrue(g.contains(rdf.createIRI(container), LDP.contains, rdf.createIRI(binary)));
            }
        }

        /**
         * Test that the SHA digest is generated.
         */
        @Test
        @DisplayName("Test that the SHA digest is generated")
        public void testBinaryWantDigestSha() {
            // Test the SHA-1 algorithm
            try (final Response res = target(binary).request().header(WANT_DIGEST, "SHA,MD5").get()) {
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
        public void testBinaryWantDigestSha256() {
            // Test the SHA-256 algorithm
            try (final Response res = target(binary).request().header(WANT_DIGEST, "SHA-256").get()) {
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
        public void testBinaryWantDigestUnknown() {
            // Test an unknown digest algorithm
            try (final Response res = target(binary).request().header(WANT_DIGEST, "FOO").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
                assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
                assertNull(res.getHeaderString(DIGEST));
            }
        }
    }


    /**
     * Tests of LDP-BS (Basic Containers).
     */
    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Basic Container Tests")
    public class BasicContainerTests {

        private String container;
        private String child1;
        private String child2;
        private EntityTag etag1;
        private EntityTag etag2;
        private EntityTag etag3;
        private EntityTag etag4;
        private EntityTag etag5;
        private final String containerContent = getResourceAsString(BASIC_CONTAINER);

        /**
         * Initialize Basic Containment tests.
         */
        @BeforeAll
        @DisplayName("Initialize Basic Containment tests")
        public void init() {
            // POST an LDP-BC
            try (final Response res = target().request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                container = res.getLocation().toString();
            }

            // POST an LDP-BC
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                child1 = res.getLocation().toString();
            }

            // POST an LDP-BC
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                child2 = res.getLocation().toString();
            }
        }

        /**
         * Test with ldp:PreferMinimalContainer Prefer header.
         */
        @Test
        @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
        public void testGetEmptyContainer() {
            try (final Response res = target(container).request().header(PREFER,
                        "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
                assertFalse(g.contains(identifier, LDP.contains, null));
            }
        }

        /**
         * Test with ldp:PreferMinimalContainer Prefer header.
         */
        @Test
        @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
        public void testGetInverseEmptyContainer() {
            try (final Response res = target(container).request().header(PREFER,
                        "return=representation; omit=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertFalse(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
                assertTrue(g.contains(identifier, LDP.contains, null));
            }
        }

        /**
         * Test fetching a basic container.
         */
        @Test
        @DisplayName("Test fetching a basic container")
        public void testGetContainer() {
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.size() >= 2);
                etag1 = res.getEntityTag();
                assertTrue(etag1.isWeak());
                assertNotEquals(etag1, etag2);
            }
        }

        /**
         * Test creating a basic container via POST.
         */
        @Test
        @DisplayName("Test creating a basic container via POST")
        public void testCreateContainerViaPost() {
            final String child3;
            meanwhile();

            // POST an LDP-BC
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                child3 = res.getLocation().toString();
                assertTrue(child3.startsWith(container));
                assertTrue(child3.length() > container.length());
            }

            // Now fetch the container
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child3)));
                assertTrue(g.size() >= 3);
                etag2 = res.getEntityTag();
                assertTrue(etag2.isWeak());
                assertNotEquals(etag1, etag2);
                assertNotEquals(etag3, etag2);
                assertNotEquals(etag4, etag2);
            }
        }

        /**
         * Test creating a child resource via PUT.
         */
        @Test
        @DisplayName("Test creating a child resource via PUT")
        public void testCreateContainerViaPut() {
            final String child4 = container + "/child4";
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
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child4)));
                assertTrue(g.size() >= 3);
                etag3 = res.getEntityTag();
                assertTrue(etag3.isWeak());
                assertNotEquals(etag1, etag3);
                assertNotEquals(etag2, etag3);
                assertNotEquals(etag4, etag3);
            }
        }

        /**
         * Test creating a child resource with a Slug header.
         */
        @Test
        @DisplayName("Test creating a child resource with a Slug header")
        public void testCreateContainerWithSlug() {
            final String child5 = container + "/child5";
            // POST an LDP-BC
            try (final Response res = target(container).request().header("Slug", "child5")
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                assertEquals(child5, res.getLocation().toString());
            }

            // Now fetch the resource
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child5)));
                assertTrue(g.size() >= 3);
                etag4 = res.getEntityTag();
                assertTrue(etag4.isWeak());
                assertNotEquals(etag1, etag4);
                assertNotEquals(etag2, etag4);
                assertNotEquals(etag3, etag4);
            }
        }

        /**
         * Test deleting a basic container.
         */
        @Test
        @DisplayName("Test deleting a basic container")
        public void testDeleteContainer() {
            final EntityTag etag;

            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral(BASIC_CONTAINER_LABEL, ENG)));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child1)));
                assertTrue(g.size() >= 3);
                etag = res.getEntityTag();
                assertTrue(etag.isWeak());
            }

            meanwhile();

            // Delete one of the child resources
            try (final Response res = target(child1).request().delete()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }

            // Try fetching the deleted resource
            try (final Response res = target(child1).request().get()) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
            }

            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertFalse(g.contains(rdf.createIRI(container), LDP.contains, rdf.createIRI(child1)));
                assertTrue(res.getEntityTag().isWeak());
                assertNotEquals(etag, res.getEntityTag());
            }
        }
    }


    /**
     * Tests of LDP-DC (Direct Containers).
     */
    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Direct Container Tests")
    public class DirectContainerTests {

        private String base;
        private String container;
        private String member;
        private String other;
        private String container2;
        private String child;

        /**
         * Initialize Direct Container tests.
         */
        @BeforeAll
        @DisplayName("Initialize Direct Container tests")
        public void init() {
            final String containerContent = getResourceAsString(BASIC_CONTAINER);

            // POST an LDP-BC
            try (final Response res = target().request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                base = res.getLocation().toString();
            }

            member = base + MEMBER_RESOURCE1;

            final String content = getResourceAsString(DIRECT_CONTAINER)
                + membershipResource(member);

            // POST an LDP-DC
            try (final Response res = target(base).request()
                    .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));

                container = res.getLocation().toString();
            }

            final String memberContent = getResourceAsString(SIMPLE_RESOURCE);

            // PUT an LDP-RS
            try (final Response res = target(member).request().put(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }

            final String simpleContent = getResourceAsString(DIRECT_CONTAINER)
                + membershipResource(MEMBER_RESOURCE_HASH);

            // POST an LDP-DC
            try (final Response res = target(base).request()
                    .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(simpleContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));

                container2 = res.getLocation().toString();
            }

            // POST an LDP-RS
            try (final Response res = target(container2).request()
                    .post(entity(memberContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                child = res.getLocation().toString();
            }

            other = base + "/other";

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
            }
        }

        /**
         * Test fetch a self-contained direct container.
         */
        @Test
        @DisplayName("Test fetch a self-contained direct container")
        public void testSimpleDirectContainer() {
            // Fetch the member resource
            try (final Response res = target(container2).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertTrue(g.contains(rdf.createIRI(container2), LDP.contains, rdf.createIRI(child)));
                assertTrue(g.contains(rdf.createIRI(container2 + MEMBER_RESOURCE_HASH), LDP.member,
                            rdf.createIRI(child)));
            }
        }

        /**
         * Test adding resources to the direct container.
         */
        @Test
        @DisplayName("Test adding resources to the direct container")
        public void testAddingMemberResources() {
            // TODO -- this can be nested further to break it up into smaller testable sections.
            final String child1;
            final String child2;
            final EntityTag etag1;
            final EntityTag etag2;
            final EntityTag etag3;
            final EntityTag etag4;
            final EntityTag etag5;
            final EntityTag etag6;
            final String childContent = getResourceAsString(SIMPLE_RESOURCE);

            // Fetch the member resource
            try (final Response res = target(member).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertFalse(g.contains(rdf.createIRI(member), LDP.member, null));
                etag1 = res.getEntityTag();
                assertTrue(etag1.isWeak());
            }

            // Fetch the container resource
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertFalse(g.contains(rdf.createIRI(container), LDP.contains, null));
                etag4 = res.getEntityTag();
                assertTrue(etag4.isWeak());
            }

            meanwhile();

            // POST an LDP-RS child
            try (final Response res = target(container).request().post(entity(childContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                child1 = res.getLocation().toString();
                assertTrue(child1.startsWith(container));
                assertTrue(child1.length() > container.length());
            }

            // POST an LDP-RS child
            try (final Response res = target(container).request().post(entity(childContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                child2 = res.getLocation().toString();
                assertTrue(child2.startsWith(container));
                assertTrue(child2.length() > container.length());
            }

            // Fetch the member resource
            try (final Response res = target(member).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child1)));
                assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2)));
                etag2 = res.getEntityTag();
                assertTrue(etag2.isWeak());
                assertNotEquals(etag1, etag2);
            }

            // Fetch the container resource
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child1)));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child2)));
                etag5 = res.getEntityTag();
                assertTrue(etag5.isWeak());
                assertNotEquals(etag4, etag5);
            }

            meanwhile();

            // Delete one of the child resources
            try (final Response res = target(child1).request().delete()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }

            // Try fetching the deleted resource
            try (final Response res = target(child1).request().get()) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
            }

            // Fetch the member resource
            try (final Response res = target(member).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertFalse(g.contains(identifier, LDP.member, rdf.createIRI(child1)));
                assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2)));
                etag3 = res.getEntityTag();
                assertTrue(etag3.isWeak());
                assertNotEquals(etag1, etag3);
                assertNotEquals(etag2, etag3);
            }

            // Fetch the container resource
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertFalse(g.contains(identifier, LDP.contains, rdf.createIRI(child1)));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child2)));
                etag6 = res.getEntityTag();
                assertTrue(etag6.isWeak());
                assertNotEquals(etag5, etag6);
                assertNotEquals(etag4, etag6);
            }

            // Now change the membership property
            final String updateContent
                = "PREFIX dc: <http://purl.org/dc/terms/>\n"
                + "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n\n"
                + "DELETE WHERE { <> ldp:hasMemberRelation ?o };"
                + "INSERT { <> ldp:hasMemberRelation dc:relation } WHERE {}";

            // Patch the direct container
            try (final Response res = target(container).request()
                    .method(PATCH, entity(updateContent, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
            }

            // Fetch the member resource
            try (final Response res = target(member).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertTrue(g.contains(identifier, DC.relation, rdf.createIRI(child2)));
            }
        }

        /**
         * Test creating a direct container via PUT.
         */
        @Test
        @DisplayName("Test creating a direct container via PUT")
        public void testCreateDirectContainerViaPut() {
            final String other2 = base + "/other2";
            final String content = getResourceAsString(DIRECT_CONTAINER)
                + membershipResource(base + MEMBER_RESOURCE2);

            // PUT an LDP-DC
            try (final Response res = target(other2).request()
                    .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
            }
        }

        /**
         * Test updating a direct container via PUT.
         */
        @Test
        @DisplayName("Test updating a direct container via PUT")
        public void testUpdateDirectContainerViaPut() {
            final String content = getResourceAsString("/directContainerIsPartOf.ttl")
                + membershipResource(base + MEMBER_RESOURCE2);

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
            }
        }

        /**
         * Test updating a direct container with too many member-related properties.
         */
        @Test
        @DisplayName("Test updating a direct container with too many member-related properties")
        public void testUpdateDirectContainerTooManyMemberProps() {
            final String content = getResourceAsString(DIRECT_CONTAINER)
                + membershipResource(base + MEMBER_RESOURCE2)
                + "<> ldp:isMemberOfRelation dc:isPartOf .";

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)));
            }
        }

        /**
         * Test updating a direct container with too many membership resources.
         */
        @Test
        @DisplayName("Test updating a direct container with too many membership resources")
        public void testUpdateDirectContainerMultipleMemberResources() {
            final String content = getResourceAsString("/directContainer.ttl")
                + membershipResource(base + MEMBER_RESOURCE2)
                + membershipResource(base + "/member3");

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)));
            }
        }

        /**
         * Test updating a direct container with no member relation property.
         */
        @Test
        @DisplayName("Test updating a direct container with no member relation property")
        public void testUpdateDirectContainerMissingMemberRelation() {
            final String content = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
                + "PREFIX ldp: <http://www.w3.org/ns/ldp#> \n"
                + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
                + "<> skos:prefLabel \"Direct Container\"@eng ; "
                + "   ldp:membershipResource <" + base + MEMBER_RESOURCE2 + "> ; "
                + "   dc:description \"This is a Direct Container for testing.\"@eng .";

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)));
            }
        }

        /**
         * Test updating a direct container with no member resource.
         */
        @Test
        @DisplayName("Test updating a direct container with no member resource")
        public void testUpdateDirectContainerMissingMemberResource() {
            final String content = getResourceAsString("/directContainer.ttl");

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)));
            }
        }

        /**
         * Test with ldp:PreferMinimalContainer Prefer header.
         */
        @Test
        @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
        public void testGetEmptyMember() {
            try (final Response res = target(member).request().header(PREFER,
                        "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertTrue(g.contains(identifier, SKOS.prefLabel, null));
                assertFalse(g.contains(identifier, LDP.member, null));
                assertFalse(g.contains(identifier, DC.relation, null));
            }
        }

        /**
         * Test with ldp:PreferMinimalContainer Prefer header.
         */
        @Test
        @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
        public void testGetInverseEmptyMember() {
            try (final Response res = target(member).request().header(PREFER,
                        "return=representation; omit=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertFalse(g.contains(identifier, SKOS.prefLabel, null));
                assertTrue(g.contains(identifier, LDP.member, null) || g.contains(identifier, DC.relation, null));
            }
        }
    }

    /**
     * Tests of LDP-IC (Indirect Containers).
     */
    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Indirect Container Tests")
    public class IndirectContainerTests {

        private String base;
        private String container;
        private String container2;
        private String member;
        private String other;
        private String child;

        /**
         * Initialize Indirect Container tests.
         */
        @BeforeAll
        @DisplayName("Initialize Indirect Container tests")
        public void init() {
            final String containerContent = getResourceAsString(BASIC_CONTAINER);

            // POST an LDP-BC
            try (final Response res = target().request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                base = res.getLocation().toString();
            }

            member = base + "/member";

            final String content = getResourceAsString(INDIRECT_CONTAINER)
                + membershipResource(member);

            // POST an LDP-IC
            try (final Response res = target(base).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));

                container = res.getLocation().toString();
            }

            final String content2 = getResourceAsString(INDIRECT_CONTAINER_MEMBER_SUBJECT)
                + membershipResource(MEMBER_RESOURCE_HASH);

            // POST an LDP-IC
            try (final Response res = target(base).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .post(entity(content2, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));

                container2 = res.getLocation().toString();
            }

            final String memberContent = getResourceAsString(SIMPLE_RESOURCE)
                + "<> foaf:primaryTopic <#it> .";

            // PUT an LDP-RS
            try (final Response res = target(container2).request().post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                child = res.getLocation().toString();
            }

            // PUT an LDP-RS
            try (final Response res = target(member).request().put(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }

            other = base + "/other";

            // PUT an LDP-IC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));
            }
        }

        /**
         * Test adding resource to the indirect container.
         */
        @Test
        @DisplayName("Test adding resource to the indirect container")
        public void testAddResourceWithMemberSubject() {
            //Fetch the member resource
            try (final Response res = target(container2).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertTrue(g.contains(rdf.createIRI(container2), LDP.contains, rdf.createIRI(child)));
                assertTrue(g.contains(rdf.createIRI(container2 + MEMBER_RESOURCE_HASH), LDP.member,
                            rdf.createIRI(child)));
            }
        }

        /**
         * Test adding resources to the indirect container.
         */
        @Test
        @DisplayName("Test adding resources to the indirect container")
        public void testAddingMemberResources() {
            // TODO -- this can be nested further to break it up into smaller testable sections.
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
            try (final Response res = target(member).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertFalse(g.contains(rdf.createIRI(member), LDP.member, null));
                etag1 = res.getEntityTag();
                assertTrue(etag1.isWeak());
            }

            // Fetch the container resource
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                assertFalse(g.contains(rdf.createIRI(container), LDP.contains, null));
                etag4 = res.getEntityTag();
                assertTrue(etag4.isWeak());
            }

            meanwhile();

            // POST an LDP-RS child
            try (final Response res = target(container).request().post(entity(childContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                child1 = res.getLocation().toString();
                assertTrue(child1.startsWith(container));
                assertTrue(child1.length() > container.length());
            }

            // POST an LDP-RS child
            try (final Response res = target(container).request().post(entity(childContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                child2 = res.getLocation().toString();
                assertTrue(child2.startsWith(container));
                assertTrue(child2.length() > container.length());
            }

            // Fetch the member resource
            try (final Response res = target(member).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child1 + hash)));
                assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2 + hash)));
                etag2 = res.getEntityTag();
                assertTrue(etag2.isWeak());
                assertNotEquals(etag1, etag2);
            }

            // Fetch the container resource
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child1)));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child2)));
                etag5 = res.getEntityTag();
                assertTrue(etag5.isWeak());
                assertNotEquals(etag4, etag5);
            }

            meanwhile();

            // Delete one of the child resources
            try (final Response res = target(child1).request().delete()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }

            // Try fetching the deleted resource
            try (final Response res = target(child1).request().get()) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
            }

            // Fetch the member resource
            try (final Response res = target(member).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertFalse(g.contains(identifier, LDP.member, rdf.createIRI(child1 + hash)));
                assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2 + hash)));
                etag3 = res.getEntityTag();
                assertTrue(etag3.isWeak());
                assertNotEquals(etag1, etag3);
                assertNotEquals(etag2, etag3);
            }

            // Fetch the container resource
            try (final Response res = target(container).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(container);
                assertFalse(g.contains(identifier, LDP.contains, rdf.createIRI(child1)));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child2)));
                etag6 = res.getEntityTag();
                assertTrue(etag6.isWeak());
                assertNotEquals(etag5, etag6);
                assertNotEquals(etag4, etag6);
            }

            // Now change the membership property
            final String updateContent
                = "PREFIX dc: <http://purl.org/dc/terms/>\n"
                + "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n\n"
                + "DELETE WHERE { <> ldp:hasMemberRelation ?o };"
                + "INSERT { <> ldp:hasMemberRelation dc:relation } WHERE {}";

            // Patch the indirect container
            try (final Response res = target(container).request()
                    .method(PATCH, entity(updateContent, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));
            }

            // Fetch the member resource
            try (final Response res = target(member).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertTrue(g.contains(identifier, DC.relation, rdf.createIRI(child2 + hash)));
            }
        }

        /**
         * Test creating an indirect container via PUT.
         */
        @Test
        @DisplayName("Test creating an indirect container via PUT")
        public void testCreateIndirectContainerViaPut() {
            final String other2 = base + "/other2";
            final String content = getResourceAsString(INDIRECT_CONTAINER)
                + membershipResource(base + MEMBER_RESOURCE2);

            // PUT an LDP-IC
            try (final Response res = target(other2).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));
            }
        }

        /**
         * Test updating an indirect container via PUT.
         */
        @Test
        @DisplayName("Test updating an indirect container via PUT")
        public void testUpdateIndirectContainerViaPut() {
            final String content = getResourceAsString("/indirectContainerInverse.ttl")
                + membershipResource(base + MEMBER_RESOURCE2);

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.IndirectContainer)));
            }
        }

        /**
         * Test updating an indirect container with too many member-related properties.
         */
        @Test
        @DisplayName("Test updating an indirect container with too many member-related properties")
        public void testUpdateIndirectContainerTooManyMemberProps() {
            final String content = getResourceAsString(INDIRECT_CONTAINER)
                + membershipResource(base + MEMBER_RESOURCE2)
                + "<> ldp:isMemberOfRelation dc:isPartOf .";

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)));
            }
        }

        /**
         * Test updating an indirect container with no ldp:insertedContentRelation property.
         */
        @Test
        @DisplayName("Test updating an indirect container with no ldp:insertedContentRelation property")
        public void testUpdateIndirectContainerNoICRProp() {
            final String content = getResourceAsString(DIRECT_CONTAINER)
                + membershipResource(base + MEMBER_RESOURCE2);

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)));
            }
        }

        /**
         * Test updating an indirect container with too many membership resources.
         */
        @Test
        @DisplayName("Test updating an indirect container with too many membership resources")
        public void testUpdateIndirectContainerMultipleMemberResources() {
            final String content = getResourceAsString(INDIRECT_CONTAINER)
                + membershipResource(base + MEMBER_RESOURCE2)
                + membershipResource(base + "/member3");

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)));
            }
        }

        /**
         * Test updating an indirect container with no member relation property.
         */
        @Test
        @DisplayName("Test updating an indirect container with no member relation property")
        public void testUpdateIndirectContainerMissingMemberRelation() {
            final String content = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
                + "PREFIX ldp: <http://www.w3.org/ns/ldp#> \n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
                + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
                + "<> skos:prefLabel \"Indirect Container\"@eng ; "
                + "   ldp:membershipResource <" + base + MEMBER_RESOURCE2 + "> ; "
                + "   ldp:insertedContentRelation foaf:primaryTopic ; "
                + "   dc:description \"This is an Indirect Container for testing.\"@eng .";

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)));
            }
        }

        /**
         * Test updating an indirect container with no member resource.
         */
        @Test
        @DisplayName("Test updating an indirect container with no member resource")
        public void testUpdateIndirectContainerMissingMemberResource() {
            final String content = getResourceAsString(INDIRECT_CONTAINER);

            // PUT an LDP-DC
            try (final Response res = target(other).request()
                    .header(LINK, fromUri(LDP.IndirectContainer.getIRIString()).rel(TYPE).build())
                    .put(entity(content, TEXT_TURTLE))) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasConstrainedBy(Trellis.InvalidCardinality)));
            }
        }

        /**
         * Test with ldp:PreferMinimalContainer Prefer header.
         */
        @Test
        @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
        public void testGetEmptyMember() {
            try (final Response res = target(member).request().header(PREFER,
                        "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertTrue(g.contains(identifier, SKOS.prefLabel, null));
                assertFalse(g.contains(identifier, LDP.member, null));
                assertFalse(g.contains(identifier, DC.relation, null));
            }
        }

        /**
         * Test with ldp:PreferMinimalContainer Prefer header.
         */
        @Test
        @DisplayName("Test with ldp:PreferMinimalContainer Prefer header")
        public void testGetInverseEmptyMember() {
            try (final Response res = target(member).request().header(PREFER,
                        "return=representation; omit=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI identifier = rdf.createIRI(member);
                assertFalse(g.contains(identifier, SKOS.prefLabel, null));
                assertTrue(g.contains(identifier, LDP.member, null) || g.contains(identifier, DC.relation, null));
            }
        }
    }

    private Instant meanwhile() {
        final Instant t1 = now();
        await().until(() -> isReallyLaterThan(t1));
        final Instant t2 = now();
        await().until(() -> isReallyLaterThan(t2));
        return t2;
    }

    private Boolean isReallyLaterThan(final Instant time) {
        final Instant t = now();
        return t.isAfter(time) && (t.toEpochMilli() > time.toEpochMilli() || t.getNano() > time.getNano());
    }

    private String membershipResource(final String uri) {
        return "<> ldp:membershipResource <" + uri + ">.\n";
    }
}
