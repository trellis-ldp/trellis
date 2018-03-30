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
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.test.TestUtils.meanwhile;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeAll;
import org.trellisldp.vocabulary.LDP;

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
    @BeforeAll
    default void beforeAllTests() {
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
                .post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            container = res.getLocation().toString();
        }

        // POST a LDP-NR
        try (final Response res = target(container).request()
                .header(LINK, fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build())
                .post(entity(binary, TEXT_PLAIN))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            setBinaryLocation(res.getLocation().toString());
        }

        // POST an LDP-RS
        try (final Response res = target(container).request().post(entity(content, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            setResourceLocation(res.getLocation().toString());
        }

        meanwhile();

        // PUT a new LDP-NR
        try (final Response res = target(getBinaryLocation()).request()
                .header(LINK, fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build())
                .put(entity(binary + ".2", TEXT_PLAIN))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        // Patch the resource
        try (final Response res = target(getResourceLocation()).request().method("PATCH",
                    entity("INSERT { <> <http://purl.org/dc/terms/title> \"Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        meanwhile();

        // PUT a new LDP-NR
        try (final Response res = target(getBinaryLocation()).request()
                .header(LINK, fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build())
                .put(entity(binary + ".3", TEXT_PLAIN))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        // Patch the resource
        try (final Response res = target(getResourceLocation()).request().method("PATCH",
                    entity("INSERT { <> <http://purl.org/dc/terms/alternative> \"Alternative Title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }
}
