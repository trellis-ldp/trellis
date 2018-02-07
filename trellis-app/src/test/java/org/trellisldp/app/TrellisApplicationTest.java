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
package org.trellisldp.app;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
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
import static org.trellisldp.http.domain.HttpConstants.WANT_DIGEST;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.RDF.type;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.SKOS;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class TrellisApplicationTest {

    private static final DropwizardTestSupport<TrellisConfiguration> APP
        = new DropwizardTestSupport<TrellisConfiguration>(TrellisApplication.class,
                resourceFilePath("trellis-config.yml"),
                config("server.applicationConnectors[0].port", "0"),
                config("binaries", resourceFilePath("data") + "/binaries"),
                config("mementos", resourceFilePath("data") + "/mementos"),
                config("namespaces", resourceFilePath("data/namespaces.json")));

    private static final NamespaceService nsSvc = new NamespacesJsonContext(resourceFilePath("data/namespaces.json"));
    private static final IOService ioSvc = new JenaIOService(nsSvc);
    private static final RDF rdf = new JenaRDF();

    private static Client client;
    private static String baseURL;

    @BeforeAll
    public static void setUp() {
        APP.before();
        client = new JerseyClientBuilder(APP.getEnvironment()).build("test client");
        client.property("jersey.config.client.connectTimeout", 2000);
        client.property("jersey.config.client.readTimeout", 2000);
        baseURL = "http://localhost:" + APP.getLocalPort() + "/";
    }

    @AfterAll
    public static void tearDown() {
        APP.after();
    }

    /**
     * Tests of LDP-RS (RDF resources).
     */
    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("RDF resource tests")
    public class RDFTests {

        private String container, resource;
        private EntityTag etag1, etag2;
        private static final String content
            = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> a skos:Concept ;\n"
            + "   skos:prefLabel \"Resource Name\"@eng ;\n"
            + "   dc:subject <http://example.org/subject/1> .";

        @BeforeAll
        public void init() {
            final String containerContent
                = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
                + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
                + "<> skos:prefLabel \"Basic Container\"@eng ; "
                + "   dc:description \"This is a simple Basic Container for testing.\"@eng .";

            // POST an LDP-BC
            try (final Response res = target().request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                container = res.getLocation().toString();
            }

            // POST an LDP-RS
            try (final Response res = target(container).request().post(entity(content, TEXT_TURTLE))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                resource = res.getLocation().toString();
            }
        }

        @Test
        @DisplayName("Fetch the default RDF serialization")
        public void testGetDefault() {
            try (final Response res = target(resource).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }
        }

        @Test
        @DisplayName("Fetch the JSON-LD serialization")
        public void testGetJsonLd() {
            try (final Response res = target(resource).request().accept("application/ld+json").get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
                assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }
        }

        @Test
        @DisplayName("Fetch the N-Triples serialization")
        public void testGetNTriples() {
            try (final Response res = target(resource).request().accept("application/n-triples").get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(APPLICATION_N_TRIPLES_TYPE));
                assertTrue(APPLICATION_N_TRIPLES_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }
        }

        @Test
        @DisplayName("Test POSTing an RDF resource")
        public void testPostRDF() {

            // POST an LDP-RS
            try (final Response res = target(container).request().post(entity(content, TEXT_TURTLE))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                final String location = res.getLocation().toString();
                assertTrue(location.startsWith(container));
                assertTrue(location.length() > container.length());
            }
        }

        @Test
        @DisplayName("Test fetching an RDF resource")
        public void testGetRDF() {
            // Fetch the new resource
            try (final Response res = target(resource).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
                assertEquals(3L, g.size());
                final IRI identifier = rdf.createIRI(resource);
                assertTrue(g.contains(identifier, type, SKOS.Concept));
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")));
                assertTrue(g.contains(identifier, DC.subject, rdf.createIRI("http://example.org/subject/1")));
                etag1 = res.getEntityTag();
                assertTrue(etag1.isWeak());
                assertNotEquals(etag1, etag2);
            }
        }

        @Test
        @DisplayName("Test modifying an RDF document via PATCH")
        public void testPatchRDF() {
            meanwhile();

            // Patch the resource
            try (final Response res = target(resource).request().method("PATCH",
                        entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                            APPLICATION_SPARQL_UPDATE))) {
                assertEquals(204, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }

            meanwhile();

            // Fetch the updated resource
            try (final Response res = target(resource).request().accept("application/n-triples").get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(APPLICATION_N_TRIPLES_TYPE));
                assertTrue(APPLICATION_N_TRIPLES_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), baseURL, NTRIPLES).forEach(g::add);
                assertEquals(4L, g.size());
                assertTrue(g.contains(rdf.createIRI(resource), DC.title, rdf.createLiteral("Title")));
                etag2 = res.getEntityTag();
                assertTrue(etag2.isWeak());
                assertNotEquals(etag1, etag2);
            }
        }

        @Test
        @DisplayName("Verify that the correct containment triples exist")
        public void testRdfContainment() {
            // Test the root container, verifying that the containment triple exists
            try (final Response res = target(container).request().get()) {
                final Graph g = rdf.createGraph();
                assertEquals(200, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
                assertTrue(g.contains(rdf.createIRI(container), LDP.contains, rdf.createIRI(resource)));
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

        private String container, binary;
        private final String content = "This is a file.";
        private EntityTag etag1, etag2;

        @BeforeAll
        public void init() {
            final String containerContent
                = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
                + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
                + "<> skos:prefLabel \"Basic Container\"@eng ; "
                + "   dc:description \"This is a simple Basic Container for testing.\"@eng .";

            // POST an LDP-BC
            try (final Response res = target().request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                container = res.getLocation().toString();
            }

            // POST an LDP-NR
            try (final Response res = target(container).request().post(entity(content, TEXT_PLAIN))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));

                binary = res.getLocation().toString();
            }
        }

        @Test
        @DisplayName("Test fetching a binary resource")
        public void testGetBinary() throws IOException {
            // Fetch the new resource
            try (final Response res = target(binary).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
                assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                assertEquals(content, IOUtils.toString((InputStream) res.getEntity(), UTF_8));
                assertFalse(res.getEntityTag().isWeak());
                etag1 = res.getEntityTag();
                assertNotEquals(etag1, etag2);
            }
        }

        @Test
        @DisplayName("Test fetching a binary description")
        public void testGetBinaryDescription() {
            // Fetch the description
            try (final Response res = target(binary).request().accept("text/turtle").get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
                assertTrue(g.size() >= 0L);
                assertTrue(res.getEntityTag().isWeak());
                etag2 = res.getEntityTag();
                assertNotEquals(etag1, etag2);
            }
        }

        @Test
        @DisplayName("Test creating a new binary via POST")
        public void testPostBinary() {
            // POST an LDP-NR
            try (final Response res = target(container).request().post(entity(content, TEXT_PLAIN))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                final String location = res.getLocation().toString();
                assertTrue(location.startsWith(container));
                assertTrue(location.length() > container.length());
            }
        }

        @Test
        @DisplayName("Test creating a new binary via POST with a digest header")
        public void testPostBinaryWithDigest() {
            // POST an LDP-NR
            try (final Response res = target(container).request().header(DIGEST, "md5=bUMuG430lSc5B2PWyoNIgA==")
                    .post(entity(content, TEXT_PLAIN))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

                final String resource = res.getLocation().toString();
                assertTrue(resource.startsWith(container));
                assertTrue(resource.length() > container.length());
            }
        }

        @Test
        @DisplayName("Test modifying a binary's description via PATCH")
        public void testPatchBinaryDescription() throws IOException {
            final EntityTag etag;

            // Fetch the description
            try (final Response res = target(binary).request().accept("text/turtle").get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
                assertEquals(0L, g.size());
                etag = res.getEntityTag();
                assertTrue(etag.isWeak());
                assertNotEquals(etag1, etag);
            }

            meanwhile();

            // Patch the description
            try (final Response res = target(binary).request().method("PATCH",
                        entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                            APPLICATION_SPARQL_UPDATE))) {
                assertEquals(204, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }

            // Fetch the new description
            try (final Response res = target(binary).request().accept("text/turtle").get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
                assertEquals(1L, g.size());
                assertTrue(g.contains(rdf.createIRI(binary), DC.title, rdf.createLiteral("Title")));
                assertNotEquals(etag, res.getEntityTag());
            }

            // Verify that the binary is still accessible
            try (final Response res = target(binary).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
                assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertEquals(content, IOUtils.toString((InputStream) res.getEntity(), UTF_8));
                assertEquals(etag1, res.getEntityTag());
            }
        }

        @Test
        @DisplayName("Test that the binary appears in the parent container")
        public void testBinaryIsInContainer() {
            // Test the root container, verifying that the containment triple exists
            try (final Response res = target(container).request().get()) {
                final Graph g = rdf.createGraph();
                assertEquals(200, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
                assertTrue(g.contains(rdf.createIRI(container), LDP.contains, rdf.createIRI(binary)));
            }
        }

        @Test
        @DisplayName("Test that the SHA digest is generated")
        public void testBinaryWantDigestSha() {
            // Test the SHA-1 algorithm
            try (final Response res = target(binary).request().header(WANT_DIGEST, "SHA,MD5").get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
                assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
                assertEquals("sha=Z5pg2cWB1IqkKKMjh57cQKAeKp0=", res.getHeaderString(DIGEST));
            }
        }

        @Test
        @DisplayName("Test that the SHA-256 digest is generated")
        public void testBinaryWantDigestSha256() {
            // Test the SHA-256 algorithm
            try (final Response res = target(binary).request().header(WANT_DIGEST, "SHA-256").get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
                assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
                assertEquals("sha-256=wZXqBpAjgZLSoADF419CRpJCurDcagOwnb/8VAiiQXA=", res.getHeaderString(DIGEST));
            }
        }

        @Test
        @DisplayName("Test that an unknown digest is ignored")
        public void testBinaryWantDigestUnknown() {
            // Test an unknown digest algorithm
            try (final Response res = target(binary).request().header(WANT_DIGEST, "FOO").get()) {
                assertEquals(200, res.getStatus());
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

        private String container, child;
        private EntityTag etag1, etag2, etag3, etag4, etag5;
        private final String containerContent
                = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
                + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
                + "<> skos:prefLabel \"Basic Container\"@eng ; "
                + "   dc:description \"This is a simple Basic Container for testing.\"@eng .";

        @BeforeAll
        public void init() {
            // POST an LDP-BC
            try (final Response res = target().request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                container = res.getLocation().toString();
            }

            // POST an LDP-BC
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                child = res.getLocation().toString();
            }
        }

        @Test
        @DisplayName("Test fetching a basic container")
        public void testGetContainer() {
            try (final Response res = target(container).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), container, TURTLE).forEach(g::add);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Basic Container", "eng")));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.size() >= 2);
                etag1 = res.getEntityTag();
                assertTrue(etag1.isWeak());
                assertNotEquals(etag1, etag2);
            }
        }

        @Test
        @DisplayName("Test creating a basic container via POST")
        public void testCreateContainerViaPost() {
            final String child1;
            meanwhile();

            // POST an LDP-BC
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                child1 = res.getLocation().toString();
                assertTrue(child1.startsWith(container));
                assertTrue(child1.length() > container.length());
            }

            // Now fetch the container
            try (final Response res = target(container).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), container, TURTLE).forEach(g::add);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Basic Container", "eng")));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child1)));
                assertTrue(g.size() >= 3);
                etag2 = res.getEntityTag();
                assertTrue(etag2.isWeak());
                assertNotEquals(etag1, etag2);
                assertNotEquals(etag3, etag2);
                assertNotEquals(etag4, etag2);
            }
        }

        @Test
        @DisplayName("Test creating a child resource via PUT")
        public void testCreateContainerViaPut() {
            final String child2 = container + "/child2";
            meanwhile();

            try (final Response res = target(child2).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                    .put(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(204, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
            }

            // Now fetch the resource
            try (final Response res = target(container).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), container, TURTLE).forEach(g::add);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Basic Container", "eng")));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child2)));
                assertTrue(g.size() >= 3);
                etag3 = res.getEntityTag();
                assertTrue(etag3.isWeak());
                assertNotEquals(etag1, etag3);
                assertNotEquals(etag2, etag3);
                assertNotEquals(etag4, etag3);
            }
        }

        @Test
        @DisplayName("Test creating a child resource with a Slug header")
        public void testCreateContainerWithSlug() {
            final String child3 = container + "/child3";
            // POST an LDP-BC
            try (final Response res = target(container).request().header("Slug", "child3")
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                    .post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(201, res.getStatus());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));

                assertEquals(child3, res.getLocation().toString());
            }

            // Now fetch the resource
            try (final Response res = target(container).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), container, TURTLE).forEach(g::add);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Basic Container", "eng")));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child3)));
                assertTrue(g.size() >= 3);
                etag4 = res.getEntityTag();
                assertTrue(etag4.isWeak());
                assertNotEquals(etag1, etag4);
                assertNotEquals(etag2, etag4);
                assertNotEquals(etag3, etag4);
            }
        }

        @Test
        @DisplayName("Test deleting a basic container")
        public void testDeleteContainer() {
            final EntityTag etag;

            try (final Response res = target(container).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), container, TURTLE).forEach(g::add);
                final IRI identifier = rdf.createIRI(container);
                assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Basic Container", "eng")));
                assertTrue(g.contains(identifier, DC.description, null));
                assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child)));
                assertTrue(g.size() >= 3);
                etag = res.getEntityTag();
                assertTrue(etag.isWeak());
            }

            meanwhile();

            // Delete one of the child resources
            try (final Response res = target(child).request().delete()) {
                assertEquals(204, res.getStatus());
            }

            // Try fetching the deleted resource
            try (final Response res = target(child).request().get()) {
                assertEquals(410, res.getStatus());
            }

            try (final Response res = target(container).request().get()) {
                assertEquals(200, res.getStatus());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
                final Graph g = rdf.createGraph();
                ioSvc.read((InputStream) res.getEntity(), container, TURTLE).forEach(g::add);
                assertFalse(g.contains(rdf.createIRI(container), LDP.contains, rdf.createIRI(child)));
                assertTrue(res.getEntityTag().isWeak());
                assertNotEquals(etag, res.getEntityTag());
            }
        }
    }

    @Test
    public void testPostDirectContainer() {
        final String location, member, child1, child2, child3;
        final EntityTag etag1, etag2, etag3;

        final String memberContent = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> skos:prefLabel \"Member Resource\"@eng ; "
            + "   dc:description \"This is a simple member resource for testing.\"@eng .";

        // POST a member resource
        try (final Response res = target().request().post(entity(memberContent, TEXT_TURTLE))) {
            assertEquals(201, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            member = res.getLocation().toString();
            assertTrue(member.startsWith(baseURL));
            assertTrue(member.length() > baseURL.length());
        }

        // Fetch the new resource
        try (final Response res = target(member).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(2L, g.size());
            final IRI identifier = rdf.createIRI(member);
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Member Resource", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            etag1 = res.getEntityTag();
            assertTrue(etag1.isWeak());
        }

        final String content = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX ldp: <http://www.w3.org/ns/ldp#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> a skos:Concept; "
            + "    skos:prefLabel \"Direct Container\"@eng ; "
            + "    ldp:membershipResource <" + member + "> ; "
            + "    ldp:hasMemberRelation ldp:member ; "
            + "    dc:description \"This is a simple Basic Container for testing.\"@eng .";


        // POST an LDP-DC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel("type").build())
                .post(entity(content, TEXT_TURTLE))) {
            assertEquals(201, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));

            location = res.getLocation().toString();
            assertTrue(location.startsWith(baseURL));
            assertTrue(location.length() > baseURL.length());
        }

        // Fetch the new resource
        try (final Response res = target(location).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(5L, g.size());
            final IRI identifier = rdf.createIRI(location);
            assertTrue(g.contains(identifier, type, SKOS.Concept));
            assertTrue(g.contains(identifier, LDP.hasMemberRelation, LDP.member));
            assertTrue(g.contains(identifier, LDP.membershipResource, rdf.createIRI(member)));
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Direct Container", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(etag1.isWeak());
        }

        meanwhile();

        final String childContent = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<>  skos:prefLabel \"Child Resource\"@eng ; "
            + "    dc:description \"This is a simple child resource for testing.\"@eng .";

        // POST an LDP-RS child
        try (final Response res = target(location).request().post(entity(childContent, TEXT_TURTLE))) {
            assertEquals(201, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            child1 = res.getLocation().toString();
            assertTrue(child1.startsWith(location));
            assertTrue(child1.length() > location.length());
        }

        // POST an LDP-RS
        try (final Response res = target(location).request().post(entity(childContent, TEXT_TURTLE))) {
            assertEquals(201, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            child2 = res.getLocation().toString();
            assertTrue(child2.startsWith(location));
            assertTrue(child2.length() > location.length());
        }

        // POST an LDP-RS
        try (final Response res = target(location).request().post(entity(childContent, TEXT_TURTLE))) {
            assertEquals(201, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            child3 = res.getLocation().toString();
            assertTrue(child3.startsWith(location));
            assertTrue(child3.length() > location.length());
        }

        meanwhile();

        // Fetch the container
        try (final Response res = target(location).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(8L, g.size());
            final IRI identifier = rdf.createIRI(location);
            assertTrue(g.contains(identifier, type, SKOS.Concept));
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Direct Container", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.membershipResource, rdf.createIRI(member)));
            assertTrue(g.contains(identifier, LDP.hasMemberRelation, LDP.member));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child1)));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child2)));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child3)));
        }

        // Fetch the member resource
        try (final Response res = target(member).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(5L, g.size());
            etag2 = res.getEntityTag();
            final IRI identifier = rdf.createIRI(member);
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Member Resource", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child1)));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2)));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child3)));
            assertTrue(etag2.isWeak());
            assertNotEquals(etag1, etag2);
        }


        // Delete one of the child resources
        try (final Response res = target(child3).request().delete()) {
            assertEquals(204, res.getStatus());
        }

        // Try fetching the deleted resource
        try (final Response res = target(child3).request().get()) {
            assertEquals(410, res.getStatus());
        }

        meanwhile();

        // Fetch the member resource
        try (final Response res = target(member).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(4L, g.size());
            final IRI identifier = rdf.createIRI(member);
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Member Resource", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child1)));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2)));
            etag3 = res.getEntityTag();
            assertTrue(etag3.isWeak());
            assertNotEquals(etag1, etag3);
            assertNotEquals(etag2, etag3);
        }

        final String updateContent
            = "PREFIX dc: <http://purl.org/dc/terms/>\n"
            + "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n\n"
            + "DELETE WHERE { <> ldp:hasMemberRelation ?o };"
            + "INSERT { <> ldp:hasMemberRelation dc:relation } WHERE {}";

        // Patch the direct container
        try (final Response res = target(location).request()
                .method("PATCH", entity(updateContent, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(204, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
        }

        // Fetch the member resource
        try (final Response res = target(member).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            final IRI identifier = rdf.createIRI(member);
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Member Resource", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, DC.relation, rdf.createIRI(child1)));
            assertTrue(g.contains(identifier, DC.relation, rdf.createIRI(child2)));
            assertEquals(4L, g.size());
        }
    }

    @Test
    public void testPutDirectContainer() {
        final String location;
        final EntityTag etag1, etag2, etag3;

        final String containerContent = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n\n"
            + "<> skos:prefLabel \"Test Container for direct containment tests\"@eng .";

        // POST a container in which to run these tests
        try (final Response res = target().request().post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(201, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            location = res.getLocation().toString();
            assertTrue(location.startsWith(baseURL));
            assertTrue(location.length() > baseURL.length());
        }

        final String memberContent = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> skos:prefLabel \"Member Resource\"@eng ; "
            + "   dc:description \"This is a simple member resource for testing.\"@eng .";

        final String member = location + "/member";
        // PUT a member resource
        try (final Response res = target(member).request().put(entity(memberContent, TEXT_TURTLE))) {
            assertEquals(204, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        }

        // Fetch the new resource
        try (final Response res = target(member).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            final IRI identifier = rdf.createIRI(member);
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Member Resource", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertEquals(2L, g.size());
            etag1 = res.getEntityTag();
            assertTrue(etag1.isWeak());
        }

        final String content = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX ldp: <http://www.w3.org/ns/ldp#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> a skos:Concept; "
            + "    skos:prefLabel \"Direct Container\"@eng ; "
            + "    ldp:membershipResource <" + member + "> ; "
            + "    ldp:hasMemberRelation ldp:member ; "
            + "    dc:description \"This is a simple Basic Container for testing.\"@eng .";

        final String container = location + "/container";

        // PUT an LDP-DC
        try (final Response res = target(container).request()
                .header(LINK, fromUri(LDP.DirectContainer.getIRIString()).rel("type").build())
                .put(entity(content, TEXT_TURTLE))) {
            assertEquals(204, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
        }

        // Fetch the new resource
        try (final Response res = target(container).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            final IRI identifier = rdf.createIRI(container);
            assertTrue(g.contains(identifier, type, SKOS.Concept));
            assertTrue(g.contains(identifier, LDP.hasMemberRelation, LDP.member));
            assertTrue(g.contains(identifier, LDP.membershipResource, rdf.createIRI(member)));
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Direct Container", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertEquals(5L, g.size());
            assertTrue(etag1.isWeak());
        }

        meanwhile();

        final String childContent = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<>  skos:prefLabel \"Child Resource\"@eng ; "
            + "    dc:description \"This is a simple child resource for testing.\"@eng .";

        final String child1 = container + "/child1";
        final String child2 = container + "/child2";
        final String child3 = container + "/child3";

        // PUT an LDP-RS child
        try (final Response res = target(child1).request().put(entity(childContent, TEXT_TURTLE))) {
            assertEquals(204, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        }

        // PUT an LDP-RS
        try (final Response res = target(child2).request().put(entity(childContent, TEXT_TURTLE))) {
            assertEquals(204, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        }

        // PUT an LDP-RS
        try (final Response res = target(child3).request().put(entity(childContent, TEXT_TURTLE))) {
            assertEquals(204, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        }

        meanwhile();

        // Fetch the container
        try (final Response res = target(container).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            final IRI identifier = rdf.createIRI(container);
            assertTrue(g.contains(identifier, type, SKOS.Concept));
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Direct Container", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.membershipResource, rdf.createIRI(member)));
            assertTrue(g.contains(identifier, LDP.hasMemberRelation, LDP.member));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child1)));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child2)));
            assertTrue(g.contains(identifier, LDP.contains, rdf.createIRI(child3)));
            assertEquals(8L, g.size());
        }

        // Fetch the member resource
        try (final Response res = target(member).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            etag2 = res.getEntityTag();
            final IRI identifier = rdf.createIRI(member);
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Member Resource", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child1)));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2)));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child3)));
            assertEquals(5L, g.size());
            assertTrue(etag2.isWeak());
            assertNotEquals(etag1, etag2);
        }


        // Delete one of the child resources
        try (final Response res = target(child3).request().delete()) {
            assertEquals(204, res.getStatus());
        }

        // Try fetching the deleted resource
        try (final Response res = target(child3).request().get()) {
            assertEquals(410, res.getStatus());
        }

        meanwhile();

        // Fetch the member resource
        try (final Response res = target(member).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            final IRI identifier = rdf.createIRI(member);
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Member Resource", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child1)));
            assertTrue(g.contains(identifier, LDP.member, rdf.createIRI(child2)));
            assertEquals(4L, g.size());
            etag3 = res.getEntityTag();
            assertTrue(etag3.isWeak());
            assertNotEquals(etag1, etag3);
            assertNotEquals(etag2, etag3);
        }

        final String updateContent
            = "PREFIX dc: <http://purl.org/dc/terms/>\n"
            + "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n\n"
            + "DELETE WHERE { <> ldp:hasMemberRelation ?o };"
            + "INSERT { <> ldp:hasMemberRelation dc:relation } WHERE {}";

        // Patch the direct container
        try (final Response res = target(container).request()
                .method("PATCH", entity(updateContent, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(204, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.DirectContainer)));
        }

        // Fetch the member resource
        try (final Response res = target(member).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            final IRI identifier = rdf.createIRI(member);
            assertTrue(g.contains(identifier, SKOS.prefLabel, rdf.createLiteral("Member Resource", "eng")));
            assertTrue(g.contains(identifier, DC.description, null));
            assertTrue(g.contains(identifier, DC.relation, rdf.createIRI(child1)));
            assertTrue(g.contains(identifier, DC.relation, rdf.createIRI(child2)));
            assertEquals(4L, g.size());
        }
    }


    @Test
    public void testGetName() {
        final Application<TrellisConfiguration> app = new TrellisApplication();
        assertEquals("Trellis LDP", app.getName());
    }

    private static Instant meanwhile() {
        final Instant t1 = now();
        await().until(() -> isReallyLaterThan(t1));
        final Instant t2 = now();
        await().until(() -> isReallyLaterThan(t2));
        return t2;
    }

    private static Boolean isReallyLaterThan(final Instant time) {
        final Instant t = now();
        return t.isAfter(time) && (t.toEpochMilli() > time.toEpochMilli() || t.getNano() > time.getNano());
    }

    private static WebTarget target() {
        return target(baseURL);
    }

    private static WebTarget target(final String url) {
        return client.target(url);
    }

    private static List<Link> getLinks(final Response res) {
        // Jersey's client doesn't parse complex link headers correctly
        return res.getStringHeaders().get(LINK).stream().map(Link::valueOf).collect(toList());
    }

    private static Predicate<Link> hasType(final IRI iri) {
        return link -> "type".equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }
}
