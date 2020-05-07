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

import static java.time.Instant.from;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.core.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.test.TestUtils.getLinks;

import java.time.Instant;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.function.Executable;

/**
 * Test Memento TimeGate resources.
 */
public interface MementoTimeGateTests extends MementoCommonTests {

    /**
     * Run the tests.
     * @return the tests
     */
    default Stream<Executable> runTests() {
        setUp();
        return Stream.of(this::testAcceptDateTimeHeader,
                this::testTimeGateLinkHeader,
                this::testOriginalLinkHeader,
                this::testTimeGateRedirect,
                this::testTimeGateRedirected,
                this::testBadTimeGateRequest,
                this::testTimeGateRequestPrecedingMemento);
    }

    /**
     * Test the presence of a Vary: Accept-DateTime header.
     */
    default void testAcceptDateTimeHeader() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response");
            assertTrue(res.getHeaderString(VARY).contains(ACCEPT_DATETIME), "Check for a Vary: Accept-Datetime header");
        }
    }

    /**
     * Test the presence of a rel=timegate Link header.
     */
    default void testTimeGateLinkHeader() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response");
            assertTrue(getLinks(res).stream().anyMatch(l -> l.getRels().contains("timegate")
                        && l.getUri().toString().equals(getResourceLocation())),
                    "Check for a rel=timegate Link header");
        }
    }

    /**
     * Test the presence of a rel=original Link header.
     */
    default void testOriginalLinkHeader() {
        try (final Response res = target(getResourceLocation()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a successful response");
            assertTrue(getLinks(res).stream().anyMatch(l -> l.getRels().contains("original")
                        && l.getUri().toString().equals(getResourceLocation())),
                    "Check for a rel=original Link header");
        }
    }

    /**
     * Test redirection of a timegate request.
     */
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
    default void testTimeGateRedirected() {
        final String date = RFC_1123_DATE_TIME.withZone(UTC).format(now());
        final String location;
        try (final Response res = target(getResourceLocation()).request().header(ACCEPT_DATETIME, date).head()) {
            if (REDIRECTION.equals(res.getStatusInfo().getFamily())) {
                location = res.getLocation().toString();
            } else {
                location = getResourceLocation();
            }
        }

        try (final Response res = target(location).request().header(ACCEPT_DATETIME, date).head()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid response");
            assertNotNull(res.getHeaderString(MEMENTO_DATETIME), "Check for a Memento-Datetime header");
        }
    }

    /**
     * Test bad timegate request.
     */
    default void testBadTimeGateRequest() {
        try (final Response res = target(getResourceLocation()).request()
                .header(ACCEPT_DATETIME, "unparseable date string").get()) {
            assertEquals(CLIENT_ERROR, res.getStatusInfo().getFamily(), "Check for an error response");
        }
    }

    /**
     * Test timegate request that predates creation.
     */
    default void testTimeGateRequestPrecedingMemento() {
        final Instant time = now().minusSeconds(1000000);
        final String acceptDatetime = RFC_1123_DATE_TIME.withZone(UTC).format(time);
        final String location;
        try (final Response res = target(getResourceLocation()).request()
                .header(ACCEPT_DATETIME, acceptDatetime).get()) {
            if (REDIRECTION.equals(res.getStatusInfo().getFamily())) {
                location = res.getLocation().toString();
            } else {
                location = getResourceLocation();
            }
        }

        try (final Response res = target(location).request().header(ACCEPT_DATETIME, acceptDatetime).head()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check for a valid response");
            assertNotNull(res.getHeaderString(MEMENTO_DATETIME), "Check for a Memento-Datetime header");
            final Instant mementoTime = from(RFC_1123_DATE_TIME.withZone(UTC)
                    .parse(res.getHeaderString(MEMENTO_DATETIME)));
            assertTrue(mementoTime.isAfter(time));
        }
    }
}
