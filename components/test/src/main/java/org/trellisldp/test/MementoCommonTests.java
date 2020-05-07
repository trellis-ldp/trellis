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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.hasType;
import static org.trellisldp.test.TestUtils.meanwhile;
import static org.trellisldp.vocabulary.RDF.type;

import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Memento;
import org.trellisldp.vocabulary.Time;

/**
 * Run Memento-related tests on a Trellis application.
 */
public interface MementoCommonTests extends CommonTests {

    /**
     * Get the location of the test resource.
     * @return the resource URL
     */
    String getResourceLocation();

    /**
     * Set the test resource location.
     * @param location the URL of the test resource
     */
    void setResourceLocation(String location);

    /**
     * Get the location of the test binary resource.
     * @return the binary resource URL
     */
    String getBinaryLocation();

    /**
     * Set the test binary resource locaiton.
     * @param location the URL of the test binary resource
     */
    void setBinaryLocation(String location);

    /**
     * Set up the memento resources.
     */
    default void setUp() {
        final String binary = "This is a text file.";

        final String content
            = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> a skos:Concept ;\n"
            + "   skos:prefLabel \"Resource Name\"@eng ;\n"
            + "   dc:subject <http://example.org/subject/1> .";

        final String containerContent
            = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dc: <http://purl.org/dc/terms/> \n\n"
            + "<> skos:prefLabel \"Basic Container\"@eng ; "
            + "   dc:description \"This is a simple Basic Container for testing.\"@eng .";

        // POST an LDP-BC
        final String container;
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                    "Check for a valid response to POSTing an LDP-BC");
            container = res.getLocation().toString();
        }

        // POST a LDP-NR
        try (final Response res = target(container).request()
                .header(LINK, fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build())
                .post(entity(binary, TEXT_PLAIN))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                    "Check for a valid response to POSTing an LDP-NR");
            setBinaryLocation(res.getLocation().toString());
        }

        // POST an LDP-RS
        try (final Response res = target(container).request().post(entity(content, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                    "Check for a valid response to POSTing an LDP-RS");
            setResourceLocation(res.getLocation().toString());
        }

        meanwhile();

        // PUT a new LDP-NR
        try (final Response res = target(getBinaryLocation()).request()
                .header(LINK, fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build())
                .put(entity(binary + ".2", TEXT_PLAIN))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                    "Check for a valid response to PUTting an LDP-NR");
        }

        // Patch the resource
        try (final Response res = target(getResourceLocation()).request().method("PATCH",
                    entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                    "Check for a valid response to PATCHing an LDP-RS");
        }

        meanwhile();

        // PUT a new LDP-NR
        try (final Response res = target(getBinaryLocation()).request()
                .header(LINK, fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build())
                .put(entity(binary + ".3", TEXT_PLAIN))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                    "Check for a valid response to PUTting an LDP-NR");
        }

        // Patch the resource
        try (final Response res = target(getResourceLocation()).request().method("PATCH",
                    entity("INSERT { <> <http://purl.org/dc/terms/alternative> \"Alternative Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(),
                    "Check for a valid response to PATCHing an LDP-RS");
        }
    }

    /**
     * Check a response for expected Allow header values.
     * @param res the Response object
     * @return a Stream of executable assertions
     */
    default Stream<Executable> checkMementoAllowedMethods(final Response res) {
        final Set<String> methods = res.getAllowedMethods().stream()
            .flatMap(item -> stream(item.split(",")).map(String::trim)).collect(toSet());
        return of(
                () -> assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response"),
                () -> assertTrue(methods.contains("GET"), "GET should be allowed!"),
                () -> assertTrue(methods.contains("HEAD"), "HEAD should be allowed!"),
                () -> assertTrue(methods.contains("OPTIONS"), "OPTIONS should be allowed!"),
                () -> assertFalse(methods.contains("POST"), "POST shouldn't be allowed!"),
                () -> assertFalse(methods.contains("PUT"), "PUT shouldn't be allowed!"),
                () -> assertFalse(methods.contains("PATCH"), "PATCH shouldn't be allowed!"),
                () -> assertFalse(methods.contains("DELETE"), "DELETE shouldn't be allowed!"));
    }

    /**
     * Check a response for expected LDP Link headers.
     * @param res the Response object
     * @param ldpType the LDP Resource type
     * @return a Stream of executable assertions
     */
    default Stream<Executable> checkMementoLdpHeaders(final Response res, final IRI ldpType) {
        return of(
                () -> assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response"),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)),
                                 "Check that the response is an LDP Resource"),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasType(ldpType)),
                                 "Check that the response is an " + ldpType));
    }

    /**
     * Check a timemap graph for expected triples.
     * @param graph the resource graph
     * @param original the original resource location
     * @param timemap the timemap location
     * @return a Stream of executable assertions
     */
    default Stream<Executable> checkMementoTimeMapGraph(final Graph graph, final IRI original, final IRI timemap) {
        return of(
            () -> assertTrue(graph.contains(original, type, Memento.OriginalResource),
                "Check for a memento:OriginalResource type"),
            () -> assertTrue(graph.contains(original, type, Memento.TimeGate), "Check for a memento:TimeGate type"),
            () -> assertTrue(graph.contains(original, Memento.timegate, original),
                             "Check for a memento:timegate predicate"),
            () -> assertTrue(graph.contains(original, Memento.timemap, timemap),
                             "Check for a memento:timemap predicate"),
            () -> assertTrue(graph.contains(original, Memento.memento, null), "Check for memento:memento predicates"),

            () -> assertTrue(graph.contains(timemap, type, Memento.TimeMap), "Check for the type memento:TimeMap"),
            () -> assertTrue(graph.contains(timemap, Time.hasBeginning, null),
                "Check for the presence of time::hasBeginning"),
            () -> assertTrue(graph.contains(timemap, Time.hasEnd, null), "Check for the presence of time:hasEnd"),

            () -> assertTrue(graph.contains(null, type, Memento.Memento), "Check that the type is memento:Memento"),
            () -> graph.stream(original, Memento.memento, null).map(x -> (IRI) x.getObject()).forEach(memento ->
                assertAll("Check memento properties", of(
                    () -> assertTrue(graph.contains(memento, type, Memento.Memento),
                                     "Check that the memento has the right type"),
                    () -> assertTrue(graph.contains(memento, Memento.original, original),
                                     "Check the memento:original property"),
                    () -> assertTrue(graph.contains(memento, Memento.timegate, original),
                                     "Check the memento:timegate property"),
                    () -> assertTrue(graph.contains(memento, Memento.timemap, timemap),
                                     "Check the memento:timemap property"),
                    () -> assertTrue(graph.contains(memento, Memento.mementoDatetime, null),
                                     "Check the memento:mementoDatetime property"),
                    () -> assertTrue(graph.contains(memento, Time.hasTime, null),
                                     "Check the memento:hasTime property")))));
    }
}
