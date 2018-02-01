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
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class TrellisApplicationTest {

    private static final DropwizardTestSupport<TrellisConfiguration> APP
        = new DropwizardTestSupport<TrellisConfiguration>(TrellisApplication.class,
                resourceFilePath("trellis-config.yml"),
                config("server.applicationConnectors[0].port", "0"),
                config("binaries.path", resourceFilePath("data") + "/binaries"),
                config("mementos.path", resourceFilePath("data") + "/mementos"),
                config("namespaces.file", resourceFilePath("data/namespaces.json")));

    private static final NamespaceService nsSvc = new NamespacesJsonContext(resourceFilePath("data/namespaces.json"));
    private static final IOService ioSvc = new JenaIOService(nsSvc);
    private static final RDF rdf = new JenaRDF();

    private static Client client;
    private static String baseURL;

    @BeforeAll
    public static void setUp() {
        APP.before();
        client = new JerseyClientBuilder(APP.getEnvironment()).build("test client");
        client.property("jersey.config.client.connectTimeout", 1000);
        client.property("jersey.config.client.readTimeout", 1000);
        baseURL = "http://localhost:" + APP.getLocalPort() + "/";
    }

    @AfterAll
    public static void tearDown() {
        APP.after();
    }

    @Test
    public void testGetDefault() {
        try (final Response res = target().request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
        }
    }

    @Test
    public void testGetJsonLd() {
        try (final Response res = target().request().accept("application/ld+json").get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
            assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
        }
    }

    @Test
    public void testGetNTriples() {
        try (final Response res = target().request().accept("application/n-triples").get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_N_TRIPLES_TYPE));
            assertTrue(APPLICATION_N_TRIPLES_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
        }
    }

    @Test
    public void testPostBinary() throws IOException {
        final String location;
        final String content = "This is a file.";
        try (final Response res = target().request().post(entity(content, TEXT_PLAIN))) {
            assertEquals(201, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));

            location = res.getLocation().toString();
            assertTrue(location.startsWith(baseURL));
            assertTrue(location.length() > baseURL.length());
        }

        try (final Response res = target(location.substring(baseURL.length())).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
            assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            assertEquals(content, IOUtils.toString((InputStream) res.getEntity(), UTF_8));
        }

        try (final Response res = target(location.substring(baseURL.length())).request().accept("text/turtle").get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(0L, g.size());
        }

        try (final Response res = target(location.substring(baseURL.length())).request()
                .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(204, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
        }

        try (final Response res = target(location.substring(baseURL.length())).request().accept("text/turtle").get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(1L, g.size());
            assertTrue(g.contains(rdf.createIRI(location), DC.title, rdf.createLiteral("Title")));
        }

        try (final Response res = target(location.substring(baseURL.length())).request().get()) {
            assertEquals(200, res.getStatus());
            assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
            assertTrue(TEXT_PLAIN_TYPE.isCompatible(res.getMediaType()));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)));
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            assertEquals(content, IOUtils.toString((InputStream) res.getEntity(), UTF_8));
        }

        try (final Response res = target().request().get()) {
            final Graph g = rdf.createGraph();
            assertEquals(200, res.getStatus());
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
            assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.BasicContainer)));
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertTrue(g.contains(rdf.createIRI(baseURL), LDP.contains, rdf.createIRI(location)));
        }
    }

    @Test
    public void testGetName() {
        final Application<TrellisConfiguration> app = new TrellisApplication();
        assertEquals("Trellis LDP", app.getName());
    }

    private static WebTarget target() {
        return target("");
    }

    private static WebTarget target(final String path) {
        return client.target(String.format("http://localhost:%d/%s", APP.getLocalPort(), path));
    }

    private static List<Link> getLinks(final Response res) {
        // Jersey's client doesn't parse complex link headers correctly
        return res.getStringHeaders().get(LINK).stream().map(Link::valueOf).collect(toList());
    }

    private static Predicate<Link> hasType(final IRI iri) {
        return link -> "type".equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }
}
