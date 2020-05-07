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
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.dropwizard.config.TrellisConfiguration;
import org.trellisldp.vocabulary.LDP;

/**
 * LDP-related tests for Trellis.
 */
class TrellisApplicationTest {

    private static final DropwizardTestSupport<TrellisConfiguration> APP;
    private static final Client CLIENT;

    static {
        APP = new DropwizardTestSupport<>(SimpleTrellisApp.class, resourceFilePath("trellis-config.yml"));
        try {
            APP.before();
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error starting application", ex);
        }
        CLIENT = new JerseyClientBuilder(APP.getEnvironment()).build("test client");
        CLIENT.property(CONNECT_TIMEOUT, 10000);
        CLIENT.property(READ_TIMEOUT, 12000);
    }

    @Test
    void testGetName() {
        final Application<TrellisConfiguration> app = new SimpleTrellisApp();
        assertEquals("Trellis LDP", app.getName(), "Incorrect application name!");
    }

    @Test
    void testGET() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        final String origin = "https://example.com";
        try (final Response res = CLIENT.target(baseUrl).request().header("Origin", origin).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Wrong content-type " + res.getMediaType());
            assertTrue(res.getStringHeaders().get(LINK).stream().map(Link::valueOf).anyMatch(link ->
                        TYPE.equals(link.getRel()) && LDP.Resource.getIRIString().equals(link.getUri().toString())),
                    "No ldp:Resource link header!");
        }
    }

    @Test
    void testPOST() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        try (final Response res = CLIENT.target(baseUrl).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertNotNull(res.getLocation(), "Missing Location header!");
            assertTrue(res.getStringHeaders().get(LINK).stream().map(Link::valueOf).anyMatch(link ->
                        TYPE.equals(link.getRel()) && LDP.Resource.getIRIString().equals(link.getUri().toString())),
                    "Missing ldp:Resource link header!");
            assertTrue(res.getStringHeaders().get(LINK).stream().map(Link::valueOf).anyMatch(link ->
                        TYPE.equals(link.getRel()) && LDP.RDFSource.getIRIString().equals(link.getUri().toString())),
                    "Missing ldp:RDFSource link header!");
        }
    }

    @Test
    void testOPTIONS() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        try (final Response res = CLIENT.target(baseUrl).request().options()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
        }
    }
}
