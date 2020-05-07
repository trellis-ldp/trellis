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
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.hasType;

import java.net.URI;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.SKOS;

/**
 * Common test interface.
 */
public interface CommonTests {

    String ENG = "eng";
    String BASIC_CONTAINER_LABEL = "Basic Container";

    /**
     * Get the HTTP client.
     * @return the client
     */
    Client getClient();

    /**
     * Get the base URL.
     * @return the base URL.
     */
    String getBaseURL();

    /**
     * Get a web target pointing to the base URL.
     * @return the web target
     */
    default WebTarget target() {
        return target(getBaseURL());
    }

    /**
     * Get a web target pointing to the provided URL.
     * @param url the URL
     * @return the web target
     */
    default WebTarget target(final String url) {
        return getClient().target(url);
    }

    /**
     * Get the describedby Link value, if one exists.
     * @param url the URL
     * @return the location of a description resource, or null if none is available
     */
    default String getDescription(final String url) {
        try (final Response res = target(url).request().head()) {
            return getLinks(res).stream().filter(link -> "describedby".equals(link.getRel()))
                .map(Link::getUri).map(URI::toString).findFirst().orElse(null);
        }
    }

    /**
     * Check for a successful creation response.
     * @param res the response
     * @param ldpType the expected type
     * @return the location of the new resource
     */
    default String checkCreateResponseAssumptions(final Response res, final IRI ldpType) {
        assumeTrue(SUCCESSFUL.equals(res.getStatusInfo().getFamily()),
                "Creation of " + ldpType + " appears not to be supported");
        assumeTrue(getLinks(res).stream().anyMatch(hasType(ldpType)),
                "New resource was not of the expected " + ldpType + " type");
        return res.getLocation().toString();
    }

    /**
     * Get the EntityTag for a given resource.
     * @param url the URL
     * @return the entity tag
     */
    default EntityTag getETag(final String url) {
        try (final Response res = target(url).request().get()) {
            return res.getEntityTag();
        }
    }

    /**
     * Get a randomized Slug header with an appropriate suffix.
     * @param suffix the suffix
     * @return a randomized header name
     */
    default String generateRandomValue(final String suffix) {
        final RandomStringGenerator generator = new RandomStringGenerator.Builder()
            .withinRange('a', 'z').build();
        return generator.generate(16) + "-" + suffix;
    }

    /**
     * Check an RDF response.
     * @param res the response
     * @param ldpType the expected LDP type
     * @param mediaType the media type, or null if no content-type is expected
     * @return a stream of testable assertions
     */
    default Stream<Executable> checkRdfResponse(final Response res, final IRI ldpType, final MediaType mediaType) {
        return Stream.of(
                () -> assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response"),
                () -> assertTrue(mediaType == null || res.getMediaType().isCompatible(mediaType),
                                 "Check for a compatible mediaType, if one is present"),
                () -> assertTrue(mediaType == null || mediaType.isCompatible(res.getMediaType()),
                                 "Check again for a compatible mediaType, if one is present"),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)),
                                 "Check for the presence of a ldp:Resource Link header"),
                () -> assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)),
                                 "Check that no ldp:NonRDFSource Link header is present"),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasType(ldpType)),
                                 "Check for an appropriate LDP Link header"));
    }

    /**
     * Check a Non-RDF response.
     * @param res the response
     * @param mediaType the content-type of the resource, or null if no content-type is expected
     * @return a stream of testable assertions
     */
    default Stream<Executable> checkNonRdfResponse(final Response res, final MediaType mediaType) {
        return Stream.of(
                () -> assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response"),
                () -> assertTrue(mediaType == null || res.getMediaType().isCompatible(mediaType),
                                 "Check for a compatible mediaType, if one exists"),
                () -> assertTrue(mediaType == null || mediaType.isCompatible(res.getMediaType()),
                                 "Check again for a compatible mediaType, if one exists"),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource)),
                                 "Check for the presence of an ldp:Resource Link header"),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource)),
                                 "Check for the presence of an ldp:NonRDFSource Link header"),
                () -> assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)),
                                 "Check for the absence of an ldp:RDFSource Link header"));
    }

    /**
     * Check an RDF graph.
     * @param graph the graph
     * @param identifier the identifier
     * @return a stream of testable assertions
     */
    default Stream<Executable> checkRdfGraph(final Graph graph, final IRI identifier) {
        return Stream.of(
                () -> assertTrue(graph.contains(identifier, SKOS.prefLabel,
                                                RDFFactory.getInstance().createLiteral(BASIC_CONTAINER_LABEL, ENG)),
                                 "Check for the presence of a skos:prefLabel triple"),
                () -> assertTrue(graph.contains(identifier, DC.description, null),
                                 "Check for the presence fo a dc:description triple"));
    }
}
