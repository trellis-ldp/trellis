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
package org.trellisldp.app;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.DropwizardTestSupport;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.vocabulary.LDP;

/**
 * LDP-related tests for Trellis.
 */
public class TrellisApplicationTest {

    private static final DropwizardTestSupport<TrellisConfiguration> APP
        = new DropwizardTestSupport<TrellisConfiguration>(SimpleTrellisApp.class,
                resourceFilePath("trellis-config.yml"));

    private static final Client CLIENT;

    static {
        APP.before();
        CLIENT = new JerseyClientBuilder(APP.getEnvironment()).build("test client");
        CLIENT.property(CONNECT_TIMEOUT, 5000);
        CLIENT.property(READ_TIMEOUT, 5000);
    }

    @Test
    public void testGetName() {
        final Application<TrellisConfiguration> app = new SimpleTrellisApp();
        assertEquals("Trellis LDP", app.getName(), "Incorrect application name!");
    }

    @Test
    public void testGET() {
        final String baseUrl = "http://localhost:" + APP.getLocalPort();
        try (final Response res = CLIENT.target(baseUrl).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Incorrect response family!");
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Wrong content-type " + res.getMediaType());
            assertTrue(res.getStringHeaders().get(LINK).stream().map(Link::valueOf).anyMatch(link ->
                        TYPE.equals(link.getRel()) && LDP.Resource.getIRIString().equals(link.getUri().toString())),
                    "No ldp:Resource link header!");
        }
    }

    @Test
    public void testPOST() {
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
}
