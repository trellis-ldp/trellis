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

import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Graph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;

/**
 * Audit tests.
 *
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class AuditTests extends BaseCommonTests {

    private static String container;
    private static String resource;
    private static String JWT_SECRET = "secret";

    protected static void setUp() {
        final String jwt = buildJwt(Trellis.AdministratorAgent.getIRIString(), JWT_SECRET);

        final String user1 = buildJwt("https://people.apache.org/~acoburn/#i", JWT_SECRET);

        final String user2 = buildJwt("https://madison.example.com/profile#me", JWT_SECRET);

        final String containerContent = getResourceAsString("/basicContainer.ttl");

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

    /**
     * Check the absense of audit triples.
     */
    @Test
    @DisplayName("Check the absense of audit triples.")
    public void testNoAuditTriples() {
        try (final Response res = target(resource).request().get()) {
            final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
            assertEquals(2L, g.size());
        }
    }

    /**
     * Check the explicit absense of audit triples.
     */
    @Test
    @DisplayName("Check the explicit absense of audit triples.")
    public void testOmitAuditTriples() {
        try (final Response res = target(resource).request().header("Prefer",
                    "return=representation; omit=\"" + Trellis.PreferAudit.getIRIString() + "\"").get()) {
            final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
            assertEquals(2L, g.size());
        }
    }

    /**
     * Check the presence of audit triples.
     */
    @Test
    @DisplayName("Check the presence of audit triples.")
    public void testAuditTriples() {
        try (final Response res = target(resource).request().header("Prefer",
                    "return=representation; include=\"" + Trellis.PreferAudit.getIRIString() + "\"").get()) {
            final Graph g = readEntityAsGraph(res.getEntity(), TURTLE);
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
}

