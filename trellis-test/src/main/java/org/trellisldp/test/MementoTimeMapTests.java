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

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.hasType;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;
import static org.trellisldp.vocabulary.RDF.type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Memento;
import org.trellisldp.vocabulary.Time;

/**
 * Run Memento TimeMap tests on a Trellis application.
 */
@TestInstance(PER_CLASS)
public interface MementoTimeMapTests extends MementoCommonTests {

    String TIMEMAP_QUERY_ARG = "?ext=timemap";

    /**
     * Test the presence of a rel=timemap Link header.
     */
    @Test
    @DisplayName("Test the presence of a rel=timemap Link header")
    default void testTimeMapLinkHeader() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().filter(l -> l.getRels().contains("timemap")
                        && l.getUri().toString().equals(getResourceLocation() + TIMEMAP_QUERY_ARG)).findFirst()
                    .isPresent());
        }
    }

    /**
     * Test the timemap response for a rel=timemap.
     */
    @Test
    @DisplayName("Test the timemap response for a rel=timemap")
    default void testTimeMapResponseHasTimeMapLink() {
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(getLinks(res).stream().filter(l -> l.getRels().contains("timemap")
                        && l.getUri().toString().equals(getResourceLocation() + TIMEMAP_QUERY_ARG)).findFirst()
                    .isPresent());
        }
    }

    /**
     * Test that the timemap resource is an LDP resource.
     */
    @Test
    @DisplayName("Test that the timemap resource is an LDP resource")
    default void testTimeMapIsLDPResource() {
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().get()) {
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
    default void testTimeMapMediaType() {
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().get()) {
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
    default void testTimeMapConnegTurtle() {
        final RDF rdf = getInstance();
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().accept("text/turtle")
                .get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));

            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE);
            final IRI original = rdf.createIRI(getResourceLocation());
            final IRI timemap = rdf.createIRI(getResourceLocation() + TIMEMAP_QUERY_ARG);
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
    default void testTimeMapConnegJsonLd() {
        final RDF rdf = getInstance();
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request()
                .accept("application/ld+json").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
            assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));

            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), JSONLD);
            final IRI original = rdf.createIRI(getResourceLocation());
            final IRI timemap = rdf.createIRI(getResourceLocation() + TIMEMAP_QUERY_ARG);
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
    default void testTimeMapConnegNTriples() {
        final RDF rdf = getInstance();
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request()
                .accept("application/n-triples").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getMediaType().isCompatible(APPLICATION_N_TRIPLES_TYPE));
            assertTrue(APPLICATION_N_TRIPLES_TYPE.isCompatible(res.getMediaType()));

            final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), NTRIPLES);
            final IRI original = rdf.createIRI(getResourceLocation());
            final IRI timemap = rdf.createIRI(getResourceLocation() + TIMEMAP_QUERY_ARG);
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
    default void testTimeMapAllowedMethods() {
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            assertTrue(res.getAllowedMethods().contains("GET"));
            assertTrue(res.getAllowedMethods().contains("HEAD"));
            assertTrue(res.getAllowedMethods().contains("OPTIONS"));
            assertFalse(res.getAllowedMethods().contains("POST"));
            assertFalse(res.getAllowedMethods().contains("PUT"));
            assertFalse(res.getAllowedMethods().contains("PATCH"));
            assertFalse(res.getAllowedMethods().contains("DELETE"));
        }
    }
}
