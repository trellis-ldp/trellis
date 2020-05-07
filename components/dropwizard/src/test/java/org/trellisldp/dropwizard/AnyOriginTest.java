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
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.dropwizard.config.TrellisConfiguration;

/**
 * LDP-related tests for Trellis.
 */
class AnyOriginTest extends TrellisApplicationTest {

    private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String MAX_AGE = "Access-Control-Max-Age";
    private static final String REQUEST_HEADERS = "Access-Control-Request-Headers";
    private static final String REQUEST_METHOD = "Access-Control-Request-Method";

    private static final DropwizardTestSupport<TrellisConfiguration> APP;
    private static final Client CLIENT;

    static {
        APP = new DropwizardTestSupport<>(SimpleTrellisApp.class, resourceFilePath("trellis-config.yml"));
        try {
            APP.before();
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error starting application", ex);
        }
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
            assertEquals(origin, res.getHeaderString(ALLOW_ORIGIN), "Incorrect -Allow-Origin header!");
            assertEquals("true", res.getHeaderString(ALLOW_CREDENTIALS), "Incorrect -Allow-Credentials!");
            assertNull(res.getHeaderString(ALLOW_HEADERS), "Unexpected -Allow-Headers!");
            assertNull(res.getHeaderString(ALLOW_METHODS), "Unexpected -Allow-Methods!");
            assertNull(res.getHeaderString(MAX_AGE), "Unexpected -Max-Age header!");
            assertNull(res.getHeaderString(EXPOSE_HEADERS), "Unexpected -Expose-Headers!");
        }
    }

    @Test
    void testOPTIONSWithOrigin() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        final String origin = "https://example.com";
        try (final Response res = CLIENT.target(baseUrl).request().header("Origin", origin)
                .header(REQUEST_METHOD, "PUT").header(REQUEST_HEADERS, "Content-Language, Link").options()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertEquals(origin, res.getHeaderString(ALLOW_ORIGIN), "Incorrect -Allow-Origin header!");
            assertEquals("180", res.getHeaderString(MAX_AGE), "Incorrect -Max-Age header!");
            assertEquals("true", res.getHeaderString(ALLOW_CREDENTIALS), "Incorrect -Allow-Credentials!");

            final List<String> headers = stream(res.getHeaderString(ALLOW_HEADERS).split(",")).collect(toList());
            assertTrue(headers.contains("accept"), "Accept missing from -Allow-Headers!");
            assertTrue(headers.contains("link"), "Link missing from -Allow-Headers!");
            assertTrue(headers.contains("content-type"), "Content-Type missing from -Allow-Headers!");
            assertTrue(headers.contains("accept-datetime"), "Accept-Datetime missing from -Allow-Headers!");

            final List<String> methods = stream(res.getHeaderString(ALLOW_METHODS).split(",")).collect(toList());
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
                .header(REQUEST_METHOD, "FOO").header(REQUEST_HEADERS, "Content-Language, Link").options()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertNull(res.getHeaderString(ALLOW_ORIGIN), "Unexpected -Allow-Origin header!");
            assertNull(res.getHeaderString(MAX_AGE), "Unexpected -Max-Age header!");
            assertNull(res.getHeaderString(ALLOW_HEADERS), "Unexpected -Allow-Headers!");
            assertNull(res.getHeaderString(ALLOW_METHODS), "Unexpected -Allow-Methods!");
            assertNull(res.getHeaderString(ALLOW_CREDENTIALS), "Unexpected -Allow-Credentials!");
        }
    }

    @Test
    void testOPTIONSWithOriginAndNoMatchingHeaders() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        final String origin = "https://example.com";
        try (final Response res = CLIENT.target(baseUrl).request().header("Origin", origin)
                .header(REQUEST_METHOD, "PATCH").header(REQUEST_HEADERS, "X-FakeHeader, X-OtherHeader").options()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertNull(res.getHeaderString(ALLOW_CREDENTIALS), "Unexpected -Allow-Credentials!");
            assertNull(res.getHeaderString(ALLOW_ORIGIN), "Unexpected -Allow-Origin header!");
            assertNull(res.getHeaderString(MAX_AGE), "Unexpected -Max-Age header!");
            assertNull(res.getHeaderString(ALLOW_HEADERS), "Unexpected -Allow-Headers!");
            assertNull(res.getHeaderString(ALLOW_METHODS), "Unexpected -Allow-Methods!");
        }
    }

    @Test
    void testOPTIONSNoOrigin() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        try (final Response res = CLIENT.target(baseUrl).request().options()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertNull(res.getHeaderString(ALLOW_ORIGIN), "Unexpected -Allow-Origin header!");
            assertNull(res.getHeaderString(MAX_AGE), "Unexpected -Max-Age header!");
            assertNull(res.getHeaderString(ALLOW_HEADERS), "Unexpected -Allow-Headers!");
            assertNull(res.getHeaderString(ALLOW_METHODS), "Unexpected -Allow-Methods!");
            assertNull(res.getHeaderString(ALLOW_CREDENTIALS), "Unexpected -Allow-Credentials!");
        }
    }
}
