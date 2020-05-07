/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
 *
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
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.core.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.SKOS;

/**
 * Run Memento-related tests on a Trellis application.
 */
public interface MementoResourceTests extends MementoCommonTests {

    /**
     * Run the tests.
     * @return the tests
     */
    default Stream<Executable> runTests() {
        setUp();
        return Stream.of(this::testMementosWereFound,
                this::testMementoDateTimeHeader,
                this::testMementoAcceptDateTimeHeader,
                this::testMementoAllowedMethods,
                this::testMementoLdpResource,
                this::testMementoContent);
    }

    /**
     * Build a list of all Mementos.
     * @return the resource mementos
     */
    default Map<String, String> getMementos() {
        final Map<String, String> mementos = new HashMap<>();
        try (final Response res = target(getResourceLocation()).request().get()) {
            getLinks(res).stream().filter(link -> link.getRels().contains("memento"))
                .filter(l -> l.getParams().containsKey("datetime"))
                .forEach(link -> mementos.put(link.getUri().toString(), link.getParams().get("datetime")));
        }
        return mementos;
    }

    /**
     * Test the presence of two memento links.
     */
    default void testMementosWereFound() {
        final Map<String, String> mementos = getMementos();
        assertFalse(mementos.isEmpty(), "Check that mementos were found");
        assertEquals(2, mementos.size(), "Check that 2 mementos were found");
    }

    /**
     * Test the presence of a datetime header for each memento.
     */
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
    default void testMementoAcceptDateTimeHeader() {
        getMementos().forEach((memento, date) -> {
            final String location;
            try (final Response res = target(getResourceLocation()).request().header(ACCEPT_DATETIME, date).head()) {
                if (REDIRECTION.equals(res.getStatusInfo().getFamily())) {
                    location = res.getLocation().toString();
                } else {
                    assumeTrue(SUCCESSFUL.equals(res.getStatusInfo().getFamily()));
                    location = getResourceLocation();
                }
            }

            try (final Response res = target(location).request().header(ACCEPT_DATETIME, date).head()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                                "Check for a successful memento request");
                assertNotNull(res.getHeaderString(MEMENTO_DATETIME));
                final ZonedDateTime zdt1 = ZonedDateTime.parse(date, RFC_1123_DATE_TIME);
                final ZonedDateTime zdt2 = ZonedDateTime.parse(res.getHeaderString(MEMENTO_DATETIME),
                        RFC_1123_DATE_TIME);
                assertFalse(zdt2.isBefore(zdt1), "Invalid datetime header. "
                        + " Request header: " + date
                        + " Response header: " + res.getHeaderString(MEMENTO_DATETIME));
            }
        });
    }

    /**
     * Test allowed methods on memento resources.
     */
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
    default void testMementoLdpResource() {
        getMementos().forEach((memento, date) -> {
            try (final Response res = target(memento).request().get()) {
                assertAll("Check LDP headers", checkMementoLdpHeaders(res, LDP.RDFSource));
            }
        });
    }

    /**
     * Test the content of memento resources.
     * @throws Exception if the RDF resources did not exit cleanly
     */
    default void testMementoContent() throws Exception {
        final RDF rdf = RDFFactory.getInstance();
        try (final Dataset dataset = rdf.createDataset()) {
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
            assertEquals(2L, urls.size(), "Check that two mementos were found");
            assertTrue(dataset.getGraph(urls.get(0)).isPresent(), "Check that the first graph is present");
            dataset.getGraph(urls.get(0)).ifPresent(g -> {
                assertTrue(g.contains(subject, type, SKOS.Concept), "Check for a skos:Concept type");
                assertTrue(g.contains(subject, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")),
                        "Check for a skos:prefLabel property");
                assertTrue(g.contains(subject, DC.subject, rdf.createIRI("http://example.org/subject/1")),
                        "Check for a dc:subject property");
                assertEquals(2L, g.stream().map(Triple::getPredicate).filter(isEqual(type).negate()).count(),
                        "Check for two non-type triples");
            });

            assertTrue(dataset.getGraph(urls.get(1)).isPresent(), "Check that the last graph is present");
            dataset.getGraph(urls.get(1)).ifPresent(g -> {
                assertTrue(g.contains(subject, type, SKOS.Concept), "Check for a skos:Concept type");
                assertTrue(g.contains(subject, SKOS.prefLabel, rdf.createLiteral("Resource Name", "eng")),
                        "Check for a skos:prefLabel property");
                assertTrue(g.contains(subject, DC.subject, rdf.createIRI("http://example.org/subject/1")),
                        "Check for a dc:subject property");
                assertTrue(g.contains(subject, DC.title, rdf.createLiteral("Title")),
                        "Check for a dc:title property");
                assertTrue(g.contains(subject, DC.alternative, rdf.createLiteral("Alternative Title")),
                        "Check for a dc:alternative property");
                assertEquals(4L, g.stream().map(Triple::getPredicate).filter(isEqual(type).negate()).count(),
                        "Check for four non-type triples");
            });
        }
    }
}
