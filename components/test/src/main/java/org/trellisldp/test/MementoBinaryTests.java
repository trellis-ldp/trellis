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

import static java.util.Objects.isNull;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.readEntityAsString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.vocabulary.LDP;

/**
 * Run Memento-related binary resource tests on a Trellis application.
 */
@TestInstance(PER_CLASS)
public interface MementoBinaryTests extends MementoResourceTests {

    /**
     * Build a list of all Mementos.
     * @return the resource mementos
     */
    default Map<String, String> getMementos() {
        final Map<String, String> mementos = new HashMap<>();
        try (final Response res = target(getBinaryLocation()).request().head()) {
            getLinks(res).stream().filter(link -> link.getRel().equals("memento"))
                .filter(l -> l.getParams().containsKey("datetime"))
                .forEach(link -> mementos.put(link.getUri().toString(), link.getParams().get("datetime")));
        }
        return mementos;
    }

    /**
     * Check the link headers on a binary Memento.
     */
    @Test
    @DisplayName("Test the link canonical header")
    default void testCanonicalHeader() {
        getMementos().forEach((memento, date) -> {
            try (final Response res = target(memento).request().head()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid response");
                assertTrue(getLinks(res).stream().filter(link -> link.getRel().equals("canonical"))
                        .anyMatch(link -> link.getUri().toString().equals(memento)),
                        "Check for a rel=canonical Link header");
                assertTrue(getLinks(res).stream().filter(link -> link.getRel().equals("describedby"))
                        .anyMatch(link -> link.getUri().toString().equals(memento + "&ext=description")),
                        "Check for a rel=describedby Link header");
            }
        });
    }

    /**
     * Check the link headers on a binary description Memento.
     */
    @Test
    @DisplayName("Test the link canonical header")
    default void testCanonicalHeaderDescriptions() {
        getMementos().forEach((memento, date) -> {
            final String description = getDescription(memento);
            if (isNull(description)) {
                fail("Could not find description link header!");
            }
            try (final Response res = target(description).request().accept("text/turtle").head()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid response");
                assertTrue(getLinks(res).stream().filter(link -> link.getRel().equals("canonical"))
                        .anyMatch(link -> link.getUri().toString().equals(memento + "&ext=description")),
                        "Check for a rel=canonical Link header");
                assertTrue(getLinks(res).stream().filter(link -> link.getRel().equals("describes"))
                        .anyMatch(link -> link.getUri().toString().equals(memento)),
                        "Check for a rel=describes Link header");
            }
        });
    }

    /**
     * Test the content of a binary memento resource.
     */
    @Test
    @DisplayName("Test the content of memento resources")
    default void testMementoContent() {
        final Map<String, String> mementos = getMementos();
        final Map<String, String> responses = new HashMap<>();
        mementos.forEach((memento, date) -> {
            try (final Response res = target(memento).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid response");
                responses.put(memento, readEntityAsString(res.getEntity()));
            }
        });
        assertEquals(3L, responses.size(), "Check for 3 mementos");
        responses.forEach((response, content) ->
            assertTrue(content.startsWith("This is a text file."), "Check the binary content of the mementos"));
        assertEquals(3L, responses.values().size(), "Check the number of Memento responses");
        final Set<String> values = new HashSet<>(responses.values());
        assertEquals(3L, values.size(), "Check the number of distinct Memento responses");
    }

    /**
     * Test that memento resources are also LDP resources.
     */
    @Test
    @DisplayName("Test that memento resources are also LDP resources")
    default void testMementoLdpResource() {
        getMementos().forEach((memento, date) -> {
            try (final Response res = target(memento).request().head()) {
                assertAll("Check LDP headers", checkMementoLdpHeaders(res, LDP.NonRDFSource));
            }
        });
    }

    /**
     * Test that memento binary descriptions are also LDP resources.
     */
    @Test
    @DisplayName("Test that memento binary descriptions are also LDP resources")
    default void testMementoBinaryDescriptionLdpResource() {
        getMementos().forEach((memento, date) -> {
            final String description = getDescription(memento);
            assertNotNull(description, "No describedby Link header!");

            try (final Response res = target(description).request().accept("text/turtle").head()) {
                assertAll("Check LDP headers", checkMementoLdpHeaders(res, LDP.RDFSource));
            }
        });
    }
}
