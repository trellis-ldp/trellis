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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;
import static javax.ws.rs.core.SecurityContext.BASIC_AUTH;
import static javax.ws.rs.core.SecurityContext.DIGEST_AUTH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.http.core.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE_TYPE;

import java.util.stream.Stream;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.IRI;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TrellisHttpResourceUnauthorizedTest extends BaseTrellisHttpResourceTest {

    @Override
    public Application configure() {

        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);

        // Junit runner doesn't seem to work very well with JerseyTest
        initMocks(this);

        final WebAcFilter webacFilter = new WebAcFilter(mockAccessControlService, asList(BASIC_AUTH, DIGEST_AUTH),
                "my-realm");

        final ResourceConfig config = new ResourceConfig();
        config.register(new TrellisHttpResource(mockBundler));
        config.register(new TestAuthenticationFilter("testUser", "group"));
        config.register(webacFilter);
        config.register(new CrossOriginResourceSharingFilter(asList(origin),
                    asList("PATCH", "POST", "PUT"),
                    asList("Link", "Content-Type", "Accept", "Accept-Datetime"),
                    emptyList(), false, 0));
        return config;
    }

    @BeforeEach
    public void setUpMocks() throws Exception {
        super.setUpMocks();
        when(mockResourceService.get(any(IRI.class))).thenAnswer(inv -> of(mockResource));
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(emptySet());
    }

    @Test
    public void testGetJson() {
        final Response res = target("/resource").request().accept("application/ld+json").get();
        assertAll("Check response", checkStandardResponse(res));
    }

    @Test
    public void testDefaultType() {
        final Response res = target("resource").request().get();
        assertAll("Check response", checkStandardResponse(res));
    }

    @Test
    public void testTrailingSlash() {
        final Response res = target("resource/").request().get();
        assertAll("Check response", checkStandardResponse(res));
    }

    @Test
    public void testCORS() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target("resource").request().header("Origin", origin).options();
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"), "Unexpected CORS header!");
    }

    @Test
    public void testOptions1() {
        final Response res = target("resource").request().options();
        assertAll("Check response", checkStandardResponse(res));
    }

    @Test
    public void testOptions2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        final Response res = target("resource").request().options();
        assertAll("Check response", checkStandardResponse(res));
    }

    @Test
    public void testGetJsonCompact() {
        final Response res = target("resource").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testGetTimeMapLink() {
        final Response res = target("resource").queryParam("ext", "timemap").request()
            .accept(APPLICATION_LINK_FORMAT).get();
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testGetTimeMapJson() {
        final Response res = target("resource").queryParam("ext", "timemap").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testGetVersionJson() {
        final Response res = target("resource").queryParam("version", 1496262729).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testGetAclJsonCompact() {
        final Response res = target("resource").queryParam("ext", "acl").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testPatch1() {
        final Response res = target("resource").queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE_TYPE));
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testPatch2() {
        final Response res = target("resource").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE_TYPE));
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testPost1() {
        final Response res = target("resource").queryParam("ext", "acl").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testPost2() {
        final Response res = target("resource").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testPut1() {
        final Response res = target("resource").queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testPut2() {
        final Response res = target("resource").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testDelete1() {
        final Response res = target("resource").queryParam("ext", "acl").request().delete();
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testDelete2() {
        final Response res = target("resource").request().delete();
        assertAll("Check standard response", checkStandardResponse(res));
    }

    @Test
    public void testDelete3() {
        final Response res = target("resource/").request().delete();
        assertAll("Check standard response", checkStandardResponse(res));
    }

    private Stream<Executable> checkStandardResponse(final Response res) {
        final String realm = " realm=\"my-realm\"";
        return Stream.of(
                () -> assertEquals(SC_UNAUTHORIZED, res.getStatus(), "Incorrect response code!"),
                () -> assertNotNull(res.getHeaderString(WWW_AUTHENTICATE), "Missing WWW-Authenticate header!"),
                () -> assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size(), "Incorrect auth header size!"),
                () -> assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH + realm),
                                 "Digest not in header!"),
                () -> assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH + realm),
                                 "Basic not in header!"));
    }
}
