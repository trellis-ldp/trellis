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

import static java.lang.String.join;
import static java.time.Instant.ofEpochSecond;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.SecurityContext.BASIC_AUTH;
import static javax.ws.rs.core.SecurityContext.DIGEST_AUTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE_TYPE;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LdpUnauthorizedResourceTest extends JerseyTest {

    private static final IOService ioService = new JenaIOService(null);

    private static final Instant time = ofEpochSecond(1496262729);

    private static final RDF rdf = getInstance();

    private static final IRI identifier = rdf.createIRI("trellis:repo1/resource");

    private static final IRI agent = rdf.createIRI("user:agent");

    private static final BlankNode bnode = rdf.createBlankNode();

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private Resource mockResource;

    @Mock
    private Resource mockVersionedResource;

    @Mock
    private AccessControlService mockAccessControlService;

    @Override
    public Application configure() {

        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);

        // Junit runner doesn't seem to work very well with JerseyTest
        initMocks(this);

        final WebAcFilter webacFilter = new WebAcFilter(mockAccessControlService);
        webacFilter.setChallenges(asList(BASIC_AUTH, DIGEST_AUTH));

        final ResourceConfig config = new ResourceConfig();
        config.register(new LdpResource(mockResourceService, ioService, mockBinaryService, new SimpleAgentService()));
        config.register(new TestAuthenticationFilter("testUser", "group"));
        config.register(webacFilter);
        config.register(new CrossOriginResourceSharingFilter(asList(origin),
                    asList("PATCH", "POST", "PUT"),
                    asList("Link", "Content-Type", "Accept", "Accept-Datetime"),
                    emptyList(), false, 0));
        return config;
    }

    @BeforeAll
    public void before() throws Exception {
        super.setUp();
    }

    @AfterAll
    public void after() throws Exception {
        super.tearDown();
    }

    @BeforeEach
    public void setUpMocks() {
        Mockito.<Optional<? extends Resource>>when(mockResourceService.get(any(IRI.class), any(Instant.class)))
                        .thenReturn(of(mockVersionedResource));
        Mockito.<Optional<? extends Resource>>when(mockResourceService.get(any(IRI.class)))
                        .thenReturn(of(mockResource));
        when(mockResourceService.getMementos(any())).thenReturn(emptyList());

        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(emptySet());

        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockVersionedResource.getModified()).thenReturn(time);
        when(mockVersionedResource.getBinary()).thenReturn(empty());
        when(mockVersionedResource.isMemento()).thenReturn(true);
        when(mockVersionedResource.getIdentifier()).thenReturn(identifier);
        when(mockVersionedResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());

        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getBinary()).thenReturn(empty());
        when(mockResource.isMemento()).thenReturn(false);
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());

        when(mockResourceService.unskolemize(any(IRI.class)))
            .thenAnswer(inv -> {
                final String uri = ((IRI) inv.getArgument(0)).getIRIString();
                if (uri.startsWith(TRELLIS_BNODE_PREFIX)) {
                    return bnode;
                }
                return (IRI) inv.getArgument(0);
            });

        when(mockResourceService.unskolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.create(any(IRI.class), any(Session.class), any(IRI.class), any(IRI.class),
                        any(Dataset.class))).thenReturn(completedFuture(true));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Control)));
    }

    @Test
    public void testGetJson() {
        final Response res = target("/repo1/resource").request().accept("application/ld+json").get();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testDefaultType() {
        final Response res = target("repo1/resource").request().get();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testTrailingSlash() {
        final Response res = target("repo1/resource/").request().get();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testCORS() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);

        final Response res = target("repo1/resource").request().header("Origin", origin).options();
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    public void testOptions1() {
        final Response res = target("repo1/resource").request().options();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testOptions2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        final Response res = target("repo1/resource").request().options();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetJsonCompact() {
        final Response res = target("repo1/resource").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetTimeMapLink() {
        final Response res = target("repo1/resource").queryParam("ext", "timemap").request()
            .accept(APPLICATION_LINK_FORMAT).get();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetTimeMapJson() {
        final Response res = target("repo1/resource").queryParam("ext", "timemap").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetVersionJson() {
        final Response res = target("repo1/resource").queryParam("version", 1496262729).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testGetAclJsonCompact() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPatch1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE_TYPE));

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPatch2() {
        final Response res = target("repo1/resource").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE_TYPE));

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPost1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPost2() {
        final Response res = target("repo1/resource").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPut1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testPut2() {
        final Response res = target("repo1/resource").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" . ", APPLICATION_N_TRIPLES_TYPE));

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testDelete1() {
        final Response res = target("repo1/resource").queryParam("ext", "acl").request().delete();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testDelete2() {
        final Response res = target("repo1/resource").request().delete();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }

    @Test
    public void testDelete3() {
        final Response res = target("repo1/resource/").request().delete();

        assertEquals(UNAUTHORIZED, res.getStatusInfo());
        assertEquals(join(",", BASIC_AUTH, DIGEST_AUTH), res.getHeaderString(WWW_AUTHENTICATE));
        assertEquals(2L, res.getHeaders().get(WWW_AUTHENTICATE).size());
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(DIGEST_AUTH));
        assertTrue(res.getHeaders().get(WWW_AUTHENTICATE).contains(BASIC_AUTH));
    }
}
