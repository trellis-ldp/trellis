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
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.domain.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Memento;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Time;

/**
 * Run Memento-related tests on a Trellis application.
 */
@RunWith(JUnitPlatform.class)
public class MementoTests extends BaseCommonTests {

    private static final String TIMEMAP_QUERY_ARG = "?ext=timemap";
    private static final String PATCH = "PATCH";

    private static String container;
    private static String resource;
    private static final String content
            = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> a skos:Concept ;\n"
            + "   skos:prefLabel \"Resource Name\"@eng ;\n"
            + "   dc:subject <http://example.org/subject/1> .";
    private static final String containerContent
            = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> skos:prefLabel \"Basic Container\"@eng ; "
            + "   dc:description \"This is a simple Basic Container for testing.\"@eng .";

    protected static void setUp() {
        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            container = res.getLocation().toString();
        }

        // POST an LDP-RS
        try (final Response res = target(container).request().post(entity(content, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            resource = res.getLocation().toString();
        }

        meanwhile();

        // Patch the resource
        try (final Response res = target(resource).request().method(PATCH,
                    entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        meanwhile();

        // Patch the resource
        try (final Response res = target(resource).request().method(PATCH,
                    entity("INSERT { <> <http://purl.org/dc/terms/alternative> \"Alternative Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Clean up after running the tests.
     */
    protected static void tearDown() {
        System.getProperties().remove(NamespacesJsonContext.NAMESPACES_PATH);
    }

    /**
     * Test Memento TimeGate resources.
     */
    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Memento timegate tests")
    public class TimeGateTests {

        /**
         * Test the presence of a Vary: Accept-DateTime header.
         */
        @Test
        @DisplayName("Test the presence of a Vary: Accept-DateTime header")
        public void testAcceptDateTimeHeader() {
            try (final Response res = target(resource).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getHeaderString(VARY).contains(ACCEPT_DATETIME));
            }
        }

        /**
         * Test the presence of a rel=timegate Link header.
         */
        @Test
        @DisplayName("Test the presence of a rel=timegate Link header")
        public void testTimeGateLinkHeader() {
            try (final Response res = target(resource).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().filter(l -> l.getRels().contains("timegate")
                            && l.getUri().toString().equals(resource)).findFirst().isPresent());
            }
        }

        /**
         * Test the presence of a rel=original Link header.
         */
        @Test
        @DisplayName("Test the presence of a rel=original Link header")
        public void testOriginalLinkHeader() {
            try (final Response res = target(resource).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().filter(l -> l.getRels().contains("original")
                            && l.getUri().toString().equals(resource)).findFirst().isPresent());
            }
        }

        /**
         * Test redirection of a timeget request.
         */
        @Test
        @DisplayName("Test redirection of a timeget request")
        public void testTimeGateRedirect() {
            final Instant time = now();
            try (final Response res = target(resource).request()
                    .property("jersey.config.client.followRedirects", Boolean.FALSE)
                    .header(ACCEPT_DATETIME, RFC_1123_DATE_TIME.withZone(UTC).format(time)).get()) {
                assertEquals(REDIRECTION, res.getStatusInfo().getFamily());
                assertNotNull(res.getLocation());
            }
        }

        /**
         * Test normal redirection of a timeget request.
         */
        @Test
        @DisplayName("Test normal redirection of a timeget request")
        public void testTimeGateRedirected() {
            final Instant time = now();
            try (final Response res = target(resource).request()
                    .header(ACCEPT_DATETIME, RFC_1123_DATE_TIME.withZone(UTC).format(time)).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertNotNull(res.getHeaderString(MEMENTO_DATETIME));
            }
        }

        /**
         * Test bad timegate request.
         */
        @Test
        @DisplayName("Test bad timegate request")
        public void testBadTimeGateRequest() {
            final Instant time = now();
            try (final Response res = target(resource).request().header(ACCEPT_DATETIME, "unparseable date string")
                    .get()) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Test timegate request that predates creation.
         */
        @Test
        @DisplayName("Test timegate request that predates creation")
        public void testTimeGateNotFound() {
            final Instant time = now().minusSeconds(1000000);
            try (final Response res = target(resource).request()
                    .header(ACCEPT_DATETIME, RFC_1123_DATE_TIME.withZone(UTC).format(time)).get()) {
                assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily());
                assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
            }
        }
    }

    /**
     * Memento TimeMap tests.
     */
    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Memento TimeMap tests")
    public class TimeMapTests {

        /**
         * Test the presence of a rel=timemap Link header.
         */
        @Test
        @DisplayName("Test the presence of a rel=timemap Link header")
        public void testTimeMapLinkHeader() {
            try (final Response res = target(resource).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().filter(l -> l.getRels().contains("timemap")
                            && l.getUri().toString().equals(resource + TIMEMAP_QUERY_ARG)).findFirst().isPresent());
            }
        }

        /**
         * Test the timemap response for a rel=timemap.
         */
        @Test
        @DisplayName("Test the timemap response for a rel=timemap")
        public void testTimeMapResponseHasTimeMapLink() {
            try (final Response res = target(resource + TIMEMAP_QUERY_ARG).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().filter(l -> l.getRels().contains("timemap")
                            && l.getUri().toString().equals(resource + TIMEMAP_QUERY_ARG)).findFirst().isPresent());
            }
        }

        /**
         * Test that the timemap resource is an LDP resource.
         */
        @Test
        @DisplayName("Test that the timemap resource is an LDP resource")
        public void testTimeMapIsLDPResource() {
            try (final Response res = target(resource + TIMEMAP_QUERY_ARG).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
            }
        }

        /**
         * Test that the timemap response is application/link-format.
         */
        @Test
        @DisplayName("Test that the timemap response is application/link-format")
        public void testTimeMapMediaType() {
            try (final Response res = target(resource + TIMEMAP_QUERY_ARG).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(MediaType.valueOf(APPLICATION_LINK_FORMAT)));
                assertTrue(MediaType.valueOf(APPLICATION_LINK_FORMAT).isCompatible(res.getMediaType()));
            }
        }

        /**
         * Test content negotiation on timemap resource: turtle.
         */
        @Test
        @DisplayName("Test content negotiation on timemap resource: turtle")
        public void testTimeMapConnegTurtle() {
            try (final Response res = target(resource + TIMEMAP_QUERY_ARG).request().accept("text/turtle").get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
                assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));

                final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
                final IRI original = rdf.createIRI(resource);
                final IRI timemap = rdf.createIRI(resource + TIMEMAP_QUERY_ARG);
                assertTrue(g.contains(original, type, Memento.OriginalResource));
                assertTrue(g.contains(original, type, Memento.TimeGate));
                assertTrue(g.contains(original, Memento.timegate, original));
                assertTrue(g.contains(original, Memento.timemap, timemap));
                assertTrue(g.contains(original, Memento.memento, null));

                assertTrue(g.contains(timemap, type, Memento.TimeMap));
                assertTrue(g.contains(timemap, Time.hasBeginning, null));
                assertTrue(g.contains(timemap, Time.hasEnd, null));

                assertTrue(g.contains(null, type, Memento.Memento));
                g.stream(original, Memento.memento, null).map(x -> (IRI) x.getObject()).forEach(memento -> {
                    assertTrue(g.contains(memento, type, Memento.Memento));
                    assertTrue(g.contains(memento, Memento.original, original));
                    assertTrue(g.contains(memento, Memento.timegate, original));
                    assertTrue(g.contains(memento, Memento.timemap, timemap));
                    assertTrue(g.contains(memento, Memento.mementoDatetime, null));
                    assertTrue(g.contains(memento, Time.hasTime, null));
                });
            }
        }

        /**
         * Test content negotiation on timemap resource: json-ld.
         */
        @Test
        @DisplayName("Test content negotiation on timemap resource: json-ld")
        public void testTimeMapConnegJsonLd() {
            try (final Response res = target(resource + TIMEMAP_QUERY_ARG).request().accept("application/ld+json")
                    .get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
                assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));

                final Graph g = readEntityAsGraph(res.getEntity(), JSONLD);
                final IRI original = rdf.createIRI(resource);
                final IRI timemap = rdf.createIRI(resource + TIMEMAP_QUERY_ARG);
                assertTrue(g.contains(original, type, Memento.OriginalResource));
                assertTrue(g.contains(original, type, Memento.TimeGate));
                assertTrue(g.contains(original, Memento.timegate, original));
                assertTrue(g.contains(original, Memento.timemap, timemap));
                assertTrue(g.contains(original, Memento.memento, null));

                assertTrue(g.contains(timemap, type, Memento.TimeMap));
                assertTrue(g.contains(timemap, Time.hasBeginning, null));
                assertTrue(g.contains(timemap, Time.hasEnd, null));

                assertTrue(g.contains(null, type, Memento.Memento));
                g.stream(original, Memento.memento, null).map(x -> (IRI) x.getObject()).forEach(memento -> {
                    assertTrue(g.contains(memento, type, Memento.Memento));
                    assertTrue(g.contains(memento, Memento.original, original));
                    assertTrue(g.contains(memento, Memento.timegate, original));
                    assertTrue(g.contains(memento, Memento.timemap, timemap));
                    assertTrue(g.contains(memento, Memento.mementoDatetime, null));
                    assertTrue(g.contains(memento, Time.hasTime, null));
                });
            }
        }

        /**
         * Test content negotiation on timemap resource: n-triples.
         */
        @Test
        @DisplayName("Test content negotiation on timemap resource: n-triples")
        public void testTimeMapConnegNTriples() {
            try (final Response res = target(resource + TIMEMAP_QUERY_ARG).request().accept("application/n-triples")
                    .get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getMediaType().isCompatible(APPLICATION_N_TRIPLES_TYPE));
                assertTrue(APPLICATION_N_TRIPLES_TYPE.isCompatible(res.getMediaType()));

                final Graph g = readEntityAsGraph(res.getEntity(), NTRIPLES);
                final IRI original = rdf.createIRI(resource);
                final IRI timemap = rdf.createIRI(resource + TIMEMAP_QUERY_ARG);
                assertTrue(g.contains(original, type, Memento.OriginalResource));
                assertTrue(g.contains(original, type, Memento.TimeGate));
                assertTrue(g.contains(original, Memento.timegate, original));
                assertTrue(g.contains(original, Memento.timemap, timemap));
                assertTrue(g.contains(original, Memento.memento, null));

                assertTrue(g.contains(timemap, type, Memento.TimeMap));
                assertTrue(g.contains(timemap, Time.hasBeginning, null));
                assertTrue(g.contains(timemap, Time.hasEnd, null));

                assertTrue(g.contains(null, type, Memento.Memento));
                g.stream(original, Memento.memento, null).map(x -> (IRI) x.getObject()).forEach(memento -> {
                    assertTrue(g.contains(memento, type, Memento.Memento));
                    assertTrue(g.contains(memento, Memento.original, original));
                    assertTrue(g.contains(memento, Memento.timegate, original));
                    assertTrue(g.contains(memento, Memento.timemap, timemap));
                    assertTrue(g.contains(memento, Memento.mementoDatetime, null));
                    assertTrue(g.contains(memento, Time.hasTime, null));
                });
            }
        }

        /**
         * Test allowed methods on timemap resource.
         */
        @Test
        @DisplayName("Test allowed methods on timemap resource")
        public void testTimeMapAllowedMethods() {
            try (final Response res = target(resource + TIMEMAP_QUERY_ARG).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                assertTrue(res.getAllowedMethods().contains("GET"));
                assertTrue(res.getAllowedMethods().contains("HEAD"));
                assertTrue(res.getAllowedMethods().contains("OPTIONS"));
                assertFalse(res.getAllowedMethods().contains("POST"));
                assertFalse(res.getAllowedMethods().contains("PUT"));
                assertFalse(res.getAllowedMethods().contains(PATCH));
                assertFalse(res.getAllowedMethods().contains("DELETE"));
            }
        }
    }

    /**
     * Memento resource tests.
     */
    @Nested
    @TestInstance(PER_CLASS)
    @DisplayName("Memento resource tests")
    public class MementoResourceTests {

        final Map<String, String> mementos = new HashMap<>();

        /**
         * Build a list of all Mementos.
         */
        @BeforeAll
        @DisplayName("Build a list of all Mementos")
        public void getMementoList() {
            try (final Response res = target(resource).request().get()) {
                getLinks(res).stream().filter(link -> link.getRel().equals("memento"))
                    .filter(l -> l.getParams().containsKey("datetime"))
                    .forEach(link -> mementos.put(link.getUri().toString(), link.getParams().get("datetime")));
            }
        }

        /**
         * Test the presence of three mementos.
         */
        @Test
        @DisplayName("Test the presence of three mementos")
        public void testMementosWereFound() {
            assertFalse(mementos.isEmpty());
            assertEquals(3, mementos.size());
        }

        /**
         * Test the presence of a datetime header for each memento.
         */
        @Test
        @DisplayName("Test the presence of a datetime header for each memento")
        public void testMementoDateTimeHeader() {
            mementos.forEach((memento, date) -> {
                try (final Response res = target(memento).request().get()) {
                    assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                    final ZonedDateTime zdt = ZonedDateTime.parse(date, RFC_1123_DATE_TIME);
                    assertEquals(zdt, ZonedDateTime.parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME));
                }
            });
        }

        /**
         * Test the presence of a datetime header for each memento.
         */
        @Test
        @DisplayName("Test the presence of a datetime header for each memento")
        public void testMementoAcceptDateTimeHeader() {
            mementos.forEach((memento, date) -> {
                try (final Response res = target(resource).request().header(ACCEPT_DATETIME, date).get()) {
                    assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                    final ZonedDateTime zdt = ZonedDateTime.parse(date, RFC_1123_DATE_TIME);
                    assertEquals(zdt, ZonedDateTime.parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME));
                }
            });
        }

        /**
         * Test allowed methods on memento resources.
         */
        @Test
        @DisplayName("Test allowed methods on memento resources")
        public void testMementoAllowedMethods() {
            mementos.forEach((memento, date) -> {
                try (final Response res = target(memento).request().get()) {
                    assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                    assertTrue(res.getAllowedMethods().contains("GET"));
                    assertTrue(res.getAllowedMethods().contains("HEAD"));
                    assertTrue(res.getAllowedMethods().contains("OPTIONS"));
                    assertFalse(res.getAllowedMethods().contains("POST"));
                    assertFalse(res.getAllowedMethods().contains("PUT"));
                    assertFalse(res.getAllowedMethods().contains(PATCH));
                    assertFalse(res.getAllowedMethods().contains("DELETE"));
                }
            });
        }

        /**
         * Test that memento resources are also LDP resources.
         */
        @Test
        @DisplayName("Test that memento resources are also LDP resources")
        public void testMementoLdpResource() {
            mementos.forEach((memento, date) -> {
                try (final Response res = target(memento).request().get()) {
                    assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                    assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)));
                    assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)));
                }
            });
        }

        /**
         * Test the content of memento resources.
         */
        @Test
        @DisplayName("Test the content of memento resources")
        public void testMementoContent() {
            final Dataset dataset = rdf.createDataset();
            mementos.forEach((memento, date) -> {
                try (final Response res = target(memento).request().get()) {
                    assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
                    readEntityAsGraph(res.getEntity(), TURTLE).stream().forEach(triple ->
                            dataset.add(rdf.createIRI(memento), triple.getSubject(), triple.getPredicate(),
                                triple.getObject()));
                }
            });

            final IRI subject = rdf.createIRI(resource);
            final List<IRI> urls = mementos.keySet().stream().sorted().map(rdf::createIRI).collect(toList());
            assertEquals(3L, urls.size());
            assertTrue(dataset.getGraph(urls.get(0)).isPresent());
            dataset.getGraph(urls.get(0)).ifPresent(g -> {
                assertTrue(g.contains(subject, type, SKOS.Concept));
                assertTrue(g.contains(subject, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")));
                assertTrue(g.contains(subject, DC.subject, rdf.createIRI("http://example.org/subject/1")));
                assertEquals(3L, g.size());
            });

            assertTrue(dataset.getGraph(urls.get(1)).isPresent());
            dataset.getGraph(urls.get(1)).ifPresent(g -> {
                assertTrue(g.contains(subject, type, SKOS.Concept));
                assertTrue(g.contains(subject, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")));
                assertTrue(g.contains(subject, DC.subject, rdf.createIRI("http://example.org/subject/1")));
                assertTrue(g.contains(subject, DC.title, rdf.createLiteral("Title")));
                assertEquals(4L, g.size());
            });

            assertTrue(dataset.getGraph(urls.get(2)).isPresent());
            dataset.getGraph(urls.get(2)).ifPresent(g -> {
                assertTrue(g.contains(subject, type, SKOS.Concept));
                assertTrue(g.contains(subject, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")));
                assertTrue(g.contains(subject, DC.subject, rdf.createIRI("http://example.org/subject/1")));
                assertTrue(g.contains(subject, DC.title, rdf.createLiteral("Title")));
                assertTrue(g.contains(subject, DC.alternative, rdf.createLiteral("Alternative Title")));
                assertEquals(5L, g.size());
            });
        }
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
        return t.isAfter(time) && t.getEpochSecond() > time.getEpochSecond();
    }
}
