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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.hasType;

import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.function.Executable;
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
     * Check an RDF response.
     * @param res the response
     * @param ldpType the expected LDP type
     * @param hasEntity whether the response has an RDF entity
     * @return a stream of testable assertions
     */
    default Stream<Executable> checkRdfResponse(final Response res, final IRI ldpType, final Boolean hasEntity) {
        return Stream.of(
                () -> assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily()),
                () -> assertTrue(!hasEntity || res.getMediaType().isCompatible(TEXT_TURTLE_TYPE)),
                () -> assertTrue(!hasEntity || TEXT_TURTLE_TYPE.isCompatible(res.getMediaType())),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasType(LDP.Resource))),
                () -> assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.NonRDFSource))),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasType(ldpType))));
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
                                                getInstance().createLiteral(BASIC_CONTAINER_LABEL, ENG))),
                () -> assertTrue(graph.contains(identifier, DC.description, null)));
    }
}
