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

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.SKOS;

/**
 * Run Memento-related tests on a Trellis application.
 */
@TestInstance(PER_CLASS)
public interface MementoResourceTests extends MementoCommonTests {

    /**
     * Build a list of all Mementos.
     * @return the resource mementos
     */
    default Map<String, String> getMementos() {
        final Map<String, String> mementos = new HashMap<>();
        try (final Response res = target(getResourceLocation()).request().get()) {
            getLinks(res).stream().filter(link -> link.getRel().equals("memento"))
                .filter(l -> l.getParams().containsKey("datetime"))
                .forEach(link -> mementos.put(link.getUri().toString(), link.getParams().get("datetime")));
        }
        return mementos;
    }

    /**
     * Test the presence of three mementos.
     */
    @Test
    @DisplayName("Test the presence of three mementos")
    default void testMementosWereFound() {
        final Map<String, String> mementos = getMementos();
        assertFalse(mementos.isEmpty(), "Check that mementos were found");
        assertEquals(3, mementos.size(), "Check that 3 mementos were found");
    }

    /**
     * Test the presence of a datetime header for each memento.
     */
    @Test
    @DisplayName("Test the presence of a datetime header for each memento")
    default void testMementoDateTimeHeader() {
        getMementos().forEach((memento, date) -> {
            try (final Response res = target(memento).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful memento request");
                final ZonedDateTime zdt = ZonedDateTime.parse(date, RFC_1123_DATE_TIME);
                assertEquals(zdt, ZonedDateTime.parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME),
                        "Check that the memento-datetime header is correct");
            }
        });
    }

    /**
     * Test the presence of a datetime header for each memento.
     */
    @Test
    @DisplayName("Test the presence of a datetime header for each memento")
    default void testMementoAcceptDateTimeHeader() {
        getMementos().forEach((memento, date) -> {
            try (final Response res = target(getResourceLocation()).request().header(ACCEPT_DATETIME, date).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful memento request");
                final ZonedDateTime zdt = ZonedDateTime.parse(date, RFC_1123_DATE_TIME);
                assertEquals(zdt, ZonedDateTime.parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME),
                        "Check that the memento-datetime header is correct");
            }
        });
    }

    /**
     * Test allowed methods on memento resources.
     */
    @Test
    @DisplayName("Test allowed methods on memento resources")
    default void testMementoAllowedMethods() {
        getMementos().forEach((memento, date) -> {
            try (final Response res = target(memento).request().get()) {
                assertAll("Check allowed methods", checkMementoAllowedMethods(res));
            }
        });
    }

    /**
     * Test that memento resources are also LDP resources.
     */
    @Test
    @DisplayName("Test that memento resources are also LDP resources")
    default void testMementoLdpResource() {
        getMementos().forEach((memento, date) -> {
            try (final Response res = target(memento).request().get()) {
                assertAll("Check LDP headers", checkMementoLdpHeaders(res, LDP.RDFSource));
            }
        });
    }

    /**
     * Test the content of memento resources.
     */
    @Test
    @DisplayName("Test the content of memento resources")
    default void testMementoContent() {
        final RDF rdf = getInstance();
        final Dataset dataset = rdf.createDataset();
        final Map<String, String> mementos = getMementos();
        mementos.forEach((memento, date) -> {
            try (final Response res = target(memento).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful request");
                readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE).stream().forEach(triple ->
                        dataset.add(rdf.createIRI(memento), triple.getSubject(), triple.getPredicate(),
                            triple.getObject()));
            }
        });

        final IRI subject = rdf.createIRI(getResourceLocation());
        final List<IRI> urls = mementos.keySet().stream().sorted().map(rdf::createIRI).collect(toList());
        assertEquals(3L, urls.size(), "Check that three mementos were found");
        assertTrue(dataset.getGraph(urls.get(0)).isPresent(), "Check that the first graph is present");
        dataset.getGraph(urls.get(0)).ifPresent(g -> {
            assertTrue(g.contains(subject, type, SKOS.Concept), "Check for a skos:Concept type");
            assertTrue(g.contains(subject, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")),
                    "Check for a skos:prefLabel property");
            assertTrue(g.contains(subject, DC.subject, rdf.createIRI("http://example.org/subject/1")),
                    "Check for a dc:subject property");
            assertEquals(3L, g.size(), "Check for three triples");
        });

        assertTrue(dataset.getGraph(urls.get(1)).isPresent(), "Check that the second graph is present");
        dataset.getGraph(urls.get(1)).ifPresent(g -> {
            assertTrue(g.contains(subject, type, SKOS.Concept), "Check for a skos:Concept type");
            assertTrue(g.contains(subject, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")),
                    "Check for a skos:prefLabel property");
            assertTrue(g.contains(subject, DC.subject, rdf.createIRI("http://example.org/subject/1")),
                    "Check for a dc:subject property");
            assertTrue(g.contains(subject, DC.title, rdf.createLiteral("Title")),
                    "Check for a dc:title property");
            assertEquals(4L, g.size(), "Check for four triples");
        });

        assertTrue(dataset.getGraph(urls.get(2)).isPresent(), "Check that the third graph is present");
        dataset.getGraph(urls.get(2)).ifPresent(g -> {
            assertTrue(g.contains(subject, type, SKOS.Concept), "Check for a skos:Concept type");
            assertTrue(g.contains(subject, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")),
                    "Check for a skos:prefLabel property");
            assertTrue(g.contains(subject, DC.subject, rdf.createIRI("http://example.org/subject/1")),
                    "Check for a dc:subject property");
            assertTrue(g.contains(subject, DC.title, rdf.createLiteral("Title")),
                    "Check for a dc:title property");
            assertTrue(g.contains(subject, DC.alternative, rdf.createLiteral("Alternative Title")),
                    "Check for a dc:alternative property");
            assertEquals(5L, g.size(), "Check for five triples");
        });
    }
}
