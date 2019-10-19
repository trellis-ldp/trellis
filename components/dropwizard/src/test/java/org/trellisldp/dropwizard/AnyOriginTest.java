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
package org.trellisldp.dropwizard;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;
import static org.junit.jupiter.api.Assertions.*;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.trellisldp.dropwizard.config.TrellisConfiguration;

/**
 * LDP-related tests for Trellis.
 */
class AnyOriginTest extends TrellisApplicationTest {

    private static final DropwizardTestSupport<TrellisConfiguration> APP
        = new DropwizardTestSupport<>(SimpleTrellisApp.class,
                    resourceFilePath("trellis-config.yml"));

    private static final Client CLIENT;

    static {
        APP.before();
        CLIENT = new JerseyClientBuilder(APP.getEnvironment()).build("test client 2");
        CLIENT.property(CONNECT_TIMEOUT, 10000);
        CLIENT.property(READ_TIMEOUT, 12000);
    }

    @Test
    void testGETWithOrigin() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        final String origin = "https://example.com";
        try (final Response res = CLIENT.target(baseUrl).request().header("Origin", origin).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertEquals(origin, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        }
    }

    @Test
    void testOPTIONSWithOrigin() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        final String origin = "https://example.com";
        try (final Response res = CLIENT.target(baseUrl).request().header("Origin", origin)
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "Content-Language, Link").options()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertEquals(origin, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");

            final List<String> headers = stream(res.getHeaderString("Access-Control-Allow-Headers").split(","))
                .collect(toList());
            assertTrue(headers.contains("accept"), "Accept missing from -Allow-Headers!");
            assertTrue(headers.contains("link"), "Link missing from -Allow-Headers!");
            assertTrue(headers.contains("content-type"), "Content-Type missing from -Allow-Headers!");
            assertTrue(headers.contains("accept-datetime"), "Accept-Datetime missing from -Allow-Headers!");

            final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
                .collect(toList());
            assertTrue(methods.contains("PUT"), "Missing PUT method in CORS header!");
            assertTrue(methods.contains("PATCH"), "Missing PATCH method in CORS header!");
            assertTrue(methods.contains("GET"), "Missing GET method in CORS header!");
            assertTrue(methods.contains("HEAD"), "Missing HEAD method in CORS header!");
        }
    }

    @Test
    void testOPTIONSWithOriginAndNoMatchingMethods() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        final String origin = "https://example.com";
        try (final Response res = CLIENT.target(baseUrl).request().header("Origin", origin)
                .header("Access-Control-Request-Method", "FOO")
                .header("Access-Control-Request-Headers", "Content-Language, Link").options()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertNull(res.getHeaderString("Access-Control-Allow-Origin"), "Unexpected -Allow-Origin header!");
        }
    }

    @Test
    void testOPTIONSWithOriginAndNoMatchingHeaders() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        final String origin = "https://example.com";
        try (final Response res = CLIENT.target(baseUrl).request().header("Origin", origin)
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "X-FakeHeader, X-OtherHeader").options()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertNull(res.getHeaderString("Access-Control-Allow-Origin"), "Unexpected -Allow-Origin header!");
        }
    }

    @Test
    void testOPTIONSNoOrigin() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        try (final Response res = CLIENT.target(baseUrl).request().options()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertNull(res.getHeaderString("Access-Control-Allow-Origin"), "Unexpected -Allow-Origin header!");
        }
    }
}
