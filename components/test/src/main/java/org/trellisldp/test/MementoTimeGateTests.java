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

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.core.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.test.TestUtils.getLinks;

import java.time.Instant;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Test Memento TimeGate resources.
 */
@TestInstance(PER_CLASS)
@DisplayName("Memento timegate tests")
public interface MementoTimeGateTests extends MementoCommonTests {

    /**
     * Test the presence of a Vary: Accept-DateTime header.
     */
    @Test
    @DisplayName("Test the presence of a Vary: Accept-DateTime header")
    default void testAcceptDateTimeHeader() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response");
            assertTrue(res.getHeaderString(VARY).contains(ACCEPT_DATETIME), "Check for a Vary: Accept-Datetime header");
        }
    }

    /**
     * Test the presence of a rel=timegate Link header.
     */
    @Test
    @DisplayName("Test the presence of a rel=timegate Link header")
    default void testTimeGateLinkHeader() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response");
            assertTrue(getLinks(res).stream().filter(l -> l.getRels().contains("timegate")
                        && l.getUri().toString().equals(getResourceLocation())).findFirst().isPresent(),
                    "Check for a rel=timegate Link header");
        }
    }

    /**
     * Test the presence of a rel=original Link header.
     */
    @Test
    @DisplayName("Test the presence of a rel=original Link header")
    default void testOriginalLinkHeader() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response");
            assertTrue(getLinks(res).stream().filter(l -> l.getRels().contains("original")
                        && l.getUri().toString().equals(getResourceLocation())).findFirst().isPresent(),
                    "Check for a rel=original Link header");
        }
    }

    /**
     * Test redirection of a timegate request.
     */
    @Test
    @DisplayName("Test redirection of a timegate request")
    default void testTimeGateRedirect() {
        final Instant time = now();
        try (final Response res = target(getResourceLocation()).request()
                .property("jersey.config.client.followRedirects", Boolean.FALSE)
                .header(ACCEPT_DATETIME, RFC_1123_DATE_TIME.withZone(UTC).format(time)).get()) {
            assertEquals(REDIRECTION, res.getStatusInfo().getFamily(), "Check for a redirect response");
            assertNotNull(res.getLocation(), "Check for a non-null Location header");
        }
    }

    /**
     * Test normal redirection of a timegate request.
     */
    @Test
    @DisplayName("Test normal redirection of a timegate request")
    default void testTimeGateRedirected() {
        final Instant time = now();
        try (final Response res = target(getResourceLocation()).request()
                .header(ACCEPT_DATETIME, RFC_1123_DATE_TIME.withZone(UTC).format(time)).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid response");
            assertNotNull(res.getHeaderString(MEMENTO_DATETIME), "Check for a Memento-Datetime header");
        }
    }

    /**
     * Test bad timegate request.
     */
    @Test
    @DisplayName("Test bad timegate request")
    default void testBadTimeGateRequest() {
        try (final Response res = target(getResourceLocation()).request()
                .header(ACCEPT_DATETIME, "unparseable date string").get()) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Check for an error response");
        }
    }

    /**
     * Test timegate request that predates creation.
     */
    @Test
    @DisplayName("Test timegate request that predates creation")
    default void testTimeGateNotFound() {
        final Instant time = now().minusSeconds(1000000);
        try (final Response res = target(getResourceLocation()).request()
                .header(ACCEPT_DATETIME, RFC_1123_DATE_TIME.withZone(UTC).format(time)).get()) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Check for an error response");
            assertEquals(NOT_ACCEPTABLE, fromStatusCode(res.getStatus()), "Check for a 406 error");
        }
    }
}
