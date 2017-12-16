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

import static java.time.Instant.ofEpochSecond;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.api.VersionRange;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.XSD;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(JUnitPlatform.class)
public class CORSResourceTest extends JerseyTest {

    protected final static IOService ioService = new JenaIOService(null);

    private final static int timestamp = 1496262729;

    private final static Instant time = ofEpochSecond(timestamp);

    private final static RDF rdf = getInstance();

    private final static IRI agent = rdf.createIRI("user:agent");

    private final static String UPLOAD_SESSION_ID = "upload-session-id";

    private final static BlankNode bnode = rdf.createBlankNode();

    private final static String BINARY_MIME_TYPE = "text/plain";

    private final static Long BINARY_SIZE = 100L;

    private final static String REPO1 = "repo1";
    private final static String REPO2 = "repo2";
    private final static String REPO3 = "repo3";
    private final static String REPO4 = "repo4";

    private final static String BASE_URL = "http://example.org/";

    private final static String RANDOM_VALUE = "randomValue";

    private final static String RESOURCE_PATH = REPO1 + "/resource";
    private final static String CHILD_PATH = RESOURCE_PATH + "/child";
    private final static String BINARY_PATH = REPO1 + "/binary";
    private final static String NON_EXISTENT_PATH = REPO1 + "/nonexistent";

    private final static IRI identifier = rdf.createIRI(TRELLIS_PREFIX + RESOURCE_PATH);
    private final static IRI root = rdf.createIRI(TRELLIS_PREFIX + REPO1);
    private final static IRI binaryIdentifier = rdf.createIRI(TRELLIS_PREFIX + BINARY_PATH);
    private final static IRI binaryInternalIdentifier = rdf.createIRI("file:some/file");
    private final static IRI nonexistentIdentifier = rdf.createIRI(TRELLIS_PREFIX + NON_EXISTENT_PATH);
    private final static IRI childIdentifier = rdf.createIRI(TRELLIS_PREFIX + CHILD_PATH);

    protected final static Set<IRI> allModes = new HashSet<>();

    static {
        allModes.add(ACL.Append);
        allModes.add(ACL.Read);
        allModes.add(ACL.Write);
        allModes.add(ACL.Control);
    }

    @Mock
    protected ResourceService mockResourceService;

    @Mock
    protected BinaryService mockBinaryService;

    @Mock
    protected AccessControlService mockAccessControlService;

    @Mock
    protected AgentService mockAgentService;

    @Mock
    private Resource mockResource, mockVersionedResource, mockBinaryResource, mockDeletedResource,
            mockUserDeletedResource, mockBinaryVersionedResource;

    @Mock
    private Binary mockBinary;

    @Override
    public Application configure() {

        initMocks(this);

        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);

        final ResourceConfig config = new ResourceConfig();
        config.register(new LdpResource(mockResourceService, ioService, mockBinaryService, BASE_URL));
        config.register(new CrossOriginResourceSharingFilter(asList("*"),
                    asList("PATCH", "POST", "PUT"), asList("Link", "Content-Type", "Accept", "Accept-Datetime"),
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
        when(mockResourceService.get(any(IRI.class), any(Instant.class)))
            .thenReturn(of(mockVersionedResource));
        when(mockResourceService.get(eq(identifier))).thenReturn(of(mockResource));
        when(mockResourceService.get(eq(root))).thenReturn(of(mockResource));
        when(mockResourceService.get(eq(childIdentifier))).thenReturn(empty());
        when(mockResourceService.get(eq(childIdentifier), any(Instant.class))).thenReturn(empty());
        when(mockResourceService.get(eq(binaryIdentifier))).thenReturn(of(mockBinaryResource));
        when(mockResourceService.get(eq(binaryIdentifier), any(Instant.class)))
            .thenReturn(of(mockBinaryVersionedResource));
        when(mockResourceService.get(eq(nonexistentIdentifier))).thenReturn(empty());
        when(mockResourceService.get(eq(nonexistentIdentifier), any(Instant.class))).thenReturn(empty());
        when(mockResourceService.getIdentifierSupplier()).thenReturn(() -> RANDOM_VALUE);

        when(mockAgentService.asAgent(anyString())).thenReturn(agent);

        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(allModes);

        when(mockVersionedResource.getMementos()).thenReturn(asList(
                new VersionRange(ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000)),
                new VersionRange(ofEpochSecond(timestamp - 1000), time),
                new VersionRange(time, ofEpochSecond(timestamp + 1000))));
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockVersionedResource.getModified()).thenReturn(time);
        when(mockVersionedResource.getBinary()).thenReturn(empty());
        when(mockVersionedResource.isMemento()).thenReturn(true);
        when(mockVersionedResource.getIdentifier()).thenReturn(identifier);
        when(mockVersionedResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());

        when(mockResource.getMementos()).thenReturn(emptyList());
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getBinary()).thenReturn(empty());
        when(mockResource.isMemento()).thenReturn(false);
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());

        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            if (term instanceof IRI) {
                final String iri = ((IRI) term).getIRIString();
                if (iri.startsWith(BASE_URL)) {
                    return rdf.createIRI(TRELLIS_PREFIX + iri.substring(BASE_URL.length()));
                }
            }
            return term;
        });
        when(mockResourceService.toExternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            if (term instanceof IRI) {
                final String iri = ((IRI) term).getIRIString();
                if (iri.startsWith(TRELLIS_PREFIX)) {
                    return rdf.createIRI(BASE_URL + iri.substring(TRELLIS_PREFIX.length()));
                }
            }
            return term;
        });

        when(mockResourceService.unskolemize(any(IRI.class)))
            .thenAnswer(inv -> {
                final String uri = ((IRI) inv.getArgument(0)).getIRIString();
                if (uri.startsWith(TRELLIS_BNODE_PREFIX)) {
                    return bnode;
                }
                return (IRI) inv.getArgument(0);
            });

        when(mockResourceService.unskolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.put(any(IRI.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(Trellis.PreferServerManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(Trellis.PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Control)));
    }

    @Test
    public void testGetCORS() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target(RESOURCE_PATH).request().header("Origin", origin)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").get();

        assertEquals(OK, res.getStatusInfo());
        assertEquals(origin, res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

    @Test
    public void testCorsPreflight() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target(RESOURCE_PATH).request().header("Origin", origin)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").options();

        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertEquals(origin, res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));

        final List<String> headers = stream(res.getHeaderString("Access-Control-Allow-Headers").split(","))
            .collect(toList());
        assertEquals(3L, headers.size());
        assertTrue(headers.contains("link"));
        assertTrue(headers.contains("content-type"));
        assertTrue(headers.contains("accept-datetime"));

        final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        assertEquals(2L, methods.size());
        assertTrue(methods.contains("PUT"));
        assertTrue(methods.contains("PATCH"));
    }

}
