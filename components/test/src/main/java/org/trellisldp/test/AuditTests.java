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

import static java.util.function.Predicate.isEqual;
import static java.util.stream.Stream.of;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.test.TestUtils.buildJwt;
import static org.trellisldp.test.TestUtils.getResourceAsString;
import static org.trellisldp.test.TestUtils.readEntityAsGraph;
import static org.trellisldp.vocabulary.RDF.type;

import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;

/**
 * Audit tests.
 *
 * @author acoburn
 */
public interface AuditTests extends CommonTests {

    /**
     * Get the JWT secret.
     * @return the JWT secret
     */
    String getJwtSecret();

    /**
     * Get the location of the test resource.
     * @return the resource URL
     */
    String getResourceLocation();

    /**
     * Set the location of the test resource.
     * @param location the URL
     */
    void setResourceLocation(String location);

    /**
     * Set up the test infrastructure.
     */
    default void setUp() {
        final String jwt = buildJwt(Trellis.AdministratorAgent.getIRIString(), getJwtSecret());

        final String user1 = buildJwt("https://people.apache.org/~acoburn/#i", getJwtSecret());

        final String user2 = buildJwt("https://madison.example.com/profile#me", getJwtSecret());

        final String container;
        final String containerContent = getResourceAsString("/basicContainer.ttl");

        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel("type").build())
                .header(SLUG, generateRandomValue(getClass().getSimpleName()))
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Confirm a successful POST response");
            container = res.getLocation().toString();
        }

        // POST an LDP-RS
        try (final Response res = target(container).request().header(AUTHORIZATION, jwt)
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Confirm a successful POST response");
            setResourceLocation(res.getLocation().toString());
        }

        // PATCH the LDP-RS
        try (final Response res = target(getResourceLocation()).request().header(AUTHORIZATION, user1)
                .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Confirm a successful PATCH response");
        }

        // PATCH the LDP-RS
        try (final Response res = target(getResourceLocation()).request().header(AUTHORIZATION, user2).method("PATCH",
                    entity("INSERT { <> <http://www.w3.org/2004/02/skos/core#prefLabel> \"Label\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Confirm a successful PATCH response");
        }
    }

    default Stream<Executable> runTests() throws Exception {
        setUp();
        return of(this::testNoAuditTriples, this::testOmitAuditTriples, this::testAuditTriples);
    }

    /**
     * Check the absense of audit triples.
     * @throws Exception if the RDF resource didn't close cleanly
     */
    default void testNoAuditTriples() throws Exception {
        try (final Response res = target(getResourceLocation()).request().get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertEquals(2L, g.stream().map(Triple::getPredicate).filter(isEqual(type).negate()).count(),
                    "Check that the graph has 2 triples");
        }
    }

    /**
     * Check the explicit absense of audit triples.
     * @throws Exception if the RDF resource didn't close cleanly
     */
    default void testOmitAuditTriples() throws Exception {
        try (final Response res = target(getResourceLocation()).request().header("Prefer",
                    "return=representation; omit=\"" + Trellis.PreferAudit.getIRIString() + "\"").get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertEquals(2L, g.stream().map(Triple::getPredicate).filter(isEqual(type).negate()).count(),
                    "Check that the graph has only 2 triples");
        }
    }

    /**
     * Check the presence of audit triples.
     * @throws Exception if the RDF resource didn't close cleanly
     */
    default void testAuditTriples() throws Exception {
        final RDF rdf = RDFFactory.getInstance();
        try (final Response res = target(getResourceLocation()).request().header("Prefer",
                    "return=representation; include=\"" + Trellis.PreferAudit.getIRIString() + "\"").get();
             final Graph g = readEntityAsGraph(res.getEntity(), getBaseURL(), TURTLE)) {
            assertEquals(3L, g.stream(rdf.createIRI(getResourceLocation()), PROV.wasGeneratedBy, null).count(),
                    "Check for the presence of audit triples in the graph");
            assertAll("Check the graph triples",
                    g.stream(rdf.createIRI(getResourceLocation()), PROV.wasGeneratedBy, null).flatMap(triple -> of(
                        () -> assertTrue(g.contains((BlankNodeOrIRI) triple.getObject(), type, PROV.Activity),
                                         "Verify that the prov:activity type is present for the resource"),
                        () -> assertTrue(g.contains((BlankNodeOrIRI) triple.getObject(), PROV.atTime, null),
                                         "Verify that the prov:atTime property is present for the resource"),
                        () -> assertEquals(4L, g.stream((BlankNodeOrIRI) triple.getObject(), null, null).count(),
                                         "Verify that we have the right number of triples for the resource"))));
            assertTrue(g.contains(null, PROV.wasAssociatedWith, Trellis.AdministratorAgent), "Verify agent 1");
            assertTrue(g.contains(null, PROV.wasAssociatedWith,
                        rdf.createIRI("https://madison.example.com/profile#me")), "Verify agent 2");
            assertTrue(g.contains(null, PROV.wasAssociatedWith,
                        rdf.createIRI("https://people.apache.org/~acoburn/#i")), "Verify agent 3");
            assertEquals(2L, g.stream(null, type, AS.Update).count(), "Count the number of update events");
            assertEquals(1L, g.stream(null, type, AS.Create).count(), "Count the number of create events");
            assertEquals(11L, g.stream().map(Triple::getPredicate).filter(isEqual(type).negate()).count(),
                        "Get the total graph size, absent any type triples");
        }
    }
}

