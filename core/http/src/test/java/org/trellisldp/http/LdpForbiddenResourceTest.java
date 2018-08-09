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
package org.trellisldp.http;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.client.Entity.entity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.http.domain.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.domain.HttpConstants.CONFIGURATION_BASE_URL;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE_TYPE;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.IRI;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LdpForbiddenResourceTest extends BaseLdpResourceTest {

    protected String BASE_URL = "";

    @Override
    public Application configure() {

        // Junit runner doesn't seem to work very well with JerseyTest
        initMocks(this);

        BASE_URL = getBaseUri().toString();
        System.getProperties().setProperty(CONFIGURATION_BASE_URL, BASE_URL);

        final ResourceConfig config = new ResourceConfig();
        config.register(new TestAuthenticationFilter("testUser", "group"));
        config.register(new AgentAuthorizationFilter(mockAgentService));
        config.register(new WebAcFilter(mockAccessControlService));
        config.register(new LdpResource(mockBundler, null));
        System.getProperties().remove(CONFIGURATION_BASE_URL);

        return config;
    }

    @BeforeEach
    public void setUpMocks() {
        super.setUpMocks();
        when(mockResourceService.get(any(IRI.class))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(emptySet());
    }

    @Test
    public void testGetJson() {
        final Response res = target("/repo1/resource").request().accept("application/ld+json").get();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testForbiddenNoAcl() {
        final Response res = target("/repo1/resource").request().get();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testDefaultType() {
        final Response res = target("repo1/resource").request().get();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testTrailingSlash() {
        final Response res = target("repo1/resource/").request().get();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testOptions1() {
        final Response res = target("repo1/resource").request().options();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testOptions2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        final Response res = target("repo1/resource").request().options();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testGetJsonCompact() {
        final Response res = target("repo1/resource").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testGetTimeMapLink() {
        final Response res = target("repo1/resource").queryParam("ext", "timemap").request()
            .accept(APPLICATION_LINK_FORMAT).get();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testGetTimeMapJson() {
        final Response res = target("repo1/resource").queryParam("ext", "timemap").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testGetVersionJson() {
        final Response res = target("repo1/resource").queryParam("version", 1496262729).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testGetAclJsonCompact() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testPatch1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE_TYPE));

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testPatch2() {
        final Response res = target("repo1/resource").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE_TYPE));

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testPost1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testPost2() {
        final Response res = target("repo1/resource").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testPut1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testPut2() {
        final Response res = target("repo1/resource").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testDelete1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request().delete();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testDelete2() {
        final Response res = target("repo1/resource").request().delete();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testDelete3() {
        final Response res = target("repo1/resource/").request().delete();

        assertEquals(SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void testHasAccess() {
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class)))
            .thenReturn(singleton(ACL.Read));

        final Response res = target("repo1/resource/").request().get();

        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    public void testUnknown() {
        final Response res = target("repo1/resource").request()
            .method("FOO", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus());
    }

}
