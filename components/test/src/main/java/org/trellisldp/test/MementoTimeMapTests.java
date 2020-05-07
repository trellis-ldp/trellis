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

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.http.core.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;

import java.util.stream.Stream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.LDP;

/**
 * Run Memento TimeMap tests on a Trellis application.
 */
public interface MementoTimeMapTests extends MementoCommonTests {

    String TIMEMAP_QUERY_ARG = "?ext=timemap";

    /**
     * Run the tests.
     * @return the tests
     */
    default Stream<Executable> runTests() {
        setUp();
        return Stream.of(this::testTimeMapLinkHeader,
                this::testTimeMapResponseHasTimeMapLink,
                this::testTimeMapIsLDPResource,
                this::testTimeMapMediaType,
                this::testTimeMapConnegTurtle,
                this::testTimeMapConnegJsonLd,
                this::testTimeMapConnegNTriples,
                this::testTimeMapAllowedMethods);
    }

    /**
     * Test the presence of a rel=timemap Link header.
     */
    default void testTimeMapLinkHeader() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful timemap response");
            assertTrue(getLinks(res).stream().anyMatch(l -> l.getRels().contains("timemap")
                        && l.getUri().toString().equals(getResourceLocation() + TIMEMAP_QUERY_ARG)),
                    "Check for a rel=timemap Link header");
        }
    }

    /**
     * Test the timemap response for a rel=timemap.
     */
    default void testTimeMapResponseHasTimeMapLink() {
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response");
            assertTrue(getLinks(res).stream().anyMatch(l -> l.getRels().contains("timemap")
                        && l.getUri().toString().equals(getResourceLocation() + TIMEMAP_QUERY_ARG)),
                    "Check for a rel=timemap Link header");
        }
    }

    /**
     * Test that the timemap resource is an LDP resource.
     */
    default void testTimeMapIsLDPResource() {
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().get()) {
            assertAll("Check for LDP headers", checkMementoLdpHeaders(res, LDP.RDFSource));
        }
    }

    /**
     * Test that the timemap response is application/link-format.
     */
    default void testTimeMapMediaType() {
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid link-format timemap");
            assertTrue(res.getMediaType().isCompatible(MediaType.valueOf(APPLICATION_LINK_FORMAT)),
                    "Check for a response with a application/link-format content-type");
            assertTrue(MediaType.valueOf(APPLICATION_LINK_FORMAT).isCompatible(res.getMediaType()),
                    "Check for an application/link-format content-type");
        }
    }

    /**
     * Test content negotiation on timemap resource: turtle.
     */
    default void testTimeMapConnegTurtle() {
        final RDF rdf = RDFFactory.getInstance();
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().accept("text/turtle")
                .get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid turtle response");
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Check for a TURTLE response");
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Check for a TURTLE-compatible response");
            assertAll("Check memento timemap graph", checkMementoTimeMapGraph(
                        readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE),
                        rdf.createIRI(getResourceLocation()),
                        rdf.createIRI(getResourceLocation() + TIMEMAP_QUERY_ARG)));
        }
    }

    /**
     * Test content negotiation on timemap resource: json-ld.
     */
    default void testTimeMapConnegJsonLd() {
        final RDF rdf = RDFFactory.getInstance();
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request()
                .accept("application/ld+json").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid jsonld response");
            assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE), "Check for a JSONLD response");
            assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()), "Check for a JSONLD-compat response");
            assertAll("Check memento timemap graph", checkMementoTimeMapGraph(
                        readEntityAsGraph(res.getEntity(), getBaseURL(), JSONLD),
                        rdf.createIRI(getResourceLocation()),
                        rdf.createIRI(getResourceLocation() + TIMEMAP_QUERY_ARG)));
        }
    }

    /**
     * Test content negotiation on timemap resource: n-triples.
     */
    default void testTimeMapConnegNTriples() {
        final RDF rdf = RDFFactory.getInstance();
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request()
                .accept("application/n-triples").get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid ntriples response");
            assertTrue(res.getMediaType().isCompatible(APPLICATION_N_TRIPLES_TYPE), "Check for an n-triples response1");
            assertTrue(APPLICATION_N_TRIPLES_TYPE.isCompatible(res.getMediaType()), "Check for an n-triples response2");
            assertAll("Check memento timemap graph", checkMementoTimeMapGraph(
                        readEntityAsGraph(res.getEntity(), getBaseURL(), NTRIPLES),
                        rdf.createIRI(getResourceLocation()),
                        rdf.createIRI(getResourceLocation() + TIMEMAP_QUERY_ARG)));
        }
    }

    /**
     * Test allowed methods on timemap resource.
     */
    default void testTimeMapAllowedMethods() {
        try (final Response res = target(getResourceLocation() + TIMEMAP_QUERY_ARG).request().get()) {
            assertAll("Check allowed methods", checkMementoAllowedMethods(res));
        }
    }
}
