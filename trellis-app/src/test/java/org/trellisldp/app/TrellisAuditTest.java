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
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.RDF.type;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;

/**
 * Audit tests
 *
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class TrellisAuditTest {

    private static final DropwizardTestSupport<TrellisConfiguration> APP
        = new DropwizardTestSupport<TrellisConfiguration>(TrellisApplication.class,
                resourceFilePath("trellis-config.yml"),
                config("binaries", resourceFilePath("data") + "/binaries"),
                config("mementos", resourceFilePath("data") + "/mementos"),
                config("namespaces", resourceFilePath("data/namespaces.json")));

    private static Client client;
    private static String baseURL;
    private static String container, resource;
    private static String JWT_SECRET = "secret";

    private static final NamespaceService nsSvc = new NamespacesJsonContext(resourceFilePath("data/namespaces.json"));
    private static final IOService ioSvc = new JenaIOService(nsSvc);
    private static final RDF rdf = new JenaRDF();


    @BeforeAll
    public static void setUp() {
        APP.before();
        client = new JerseyClientBuilder(APP.getEnvironment()).build("test client");
        client.property("jersey.config.client.connectTimeout", 5000);
        client.property("jersey.config.client.readTimeout", 5000);
        baseURL = "http://localhost:" + APP.getLocalPort() + "/";

        final String jwt = "Bearer " + Jwts.builder().claim("webid", Trellis.AdministratorAgent.getIRIString())
            .signWith(SignatureAlgorithm.HS512, JWT_SECRET.getBytes(UTF_8)).compact();

        final String user1 = "Bearer " + Jwts.builder().claim("webid", "https://people.apache.org/~acoburn/#i")
            .signWith(SignatureAlgorithm.HS512, JWT_SECRET.getBytes(UTF_8)).compact();

        final String user2 = "Bearer " + Jwts.builder().claim("webid", "https://madison.example.com/profile#me")
            .signWith(SignatureAlgorithm.HS512, JWT_SECRET.getBytes(UTF_8)).compact();


        final String containerContent
            = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> skos:prefLabel \"Basic Container\"@eng ; "
            + "   dc:description \"This is a simple Basic Container for testing.\"@eng .";

        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            container = res.getLocation().toString();
        }

        // POST an LDP-RS
        try (final Response res = target(container).request().header(AUTHORIZATION, jwt)
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }

        // PATCH the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, user1)
                .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        // PATCH the LDP-RS
        try (final Response res = target(resource).request().header(AUTHORIZATION, user2).method("PATCH",
                    entity("INSERT { <> <http://www.w3.org/2004/02/skos/core#prefLabel> \"Label\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    @AfterAll
    public static void tearDown() {
        APP.after();
    }

    @Test
    @DisplayName("Check the absense of audit triples.")
    public void testNoAuditTriples() {
        try (final Response res = target(resource).request().get()) {
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(2L, g.size());
        }
    }

    @Test
    @DisplayName("Check the explicit absense of audit triples.")
    public void testOmitAuditTriples() {
        try (final Response res = target(resource).request().header("Prefer",
                    "return=representation; omit=\"" + Trellis.PreferAudit.getIRIString() + "\"").get()) {
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(2L, g.size());
        }
    }

    @Test
    @DisplayName("Check the presence of audit triples.")
    public void testAuditTriples() {
        try (final Response res = target(resource).request().header("Prefer",
                    "return=representation; include=\"" + Trellis.PreferAudit.getIRIString() + "\"").get()) {
            final Graph g = rdf.createGraph();
            ioSvc.read((InputStream) res.getEntity(), baseURL, TURTLE).forEach(g::add);
            assertEquals(3L, g.stream(rdf.createIRI(resource), PROV.wasGeneratedBy, null).count());
            g.stream(rdf.createIRI(resource), PROV.wasGeneratedBy, null).forEach(triple -> {
                assertTrue(g.contains((BlankNodeOrIRI) triple.getObject(), type, PROV.Activity));
                assertTrue(g.contains((BlankNodeOrIRI) triple.getObject(), PROV.atTime, null));
                assertEquals(4L, g.stream((BlankNodeOrIRI) triple.getObject(), null, null).count());
            });
            assertTrue(g.contains(null, PROV.wasAssociatedWith, Trellis.AdministratorAgent));
            assertTrue(g.contains(null, PROV.wasAssociatedWith,
                        rdf.createIRI("https://madison.example.com/profile#me")));
            assertTrue(g.contains(null, PROV.wasAssociatedWith,
                        rdf.createIRI("https://people.apache.org/~acoburn/#i")));
            assertEquals(2L, g.stream(null, type, AS.Update).count());
            assertEquals(1L, g.stream(null, type, AS.Create).count());
            assertEquals(17L, g.size());
        }
    }

    private static WebTarget target() {
        return target(baseURL);
    }

    private static WebTarget target(final String url) {
        return client.target(url);
    }
}

