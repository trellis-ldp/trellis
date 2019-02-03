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
package org.trellisldp.webdav;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.ofEpochSecond;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_GONE;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.ws.rs.client.Entity.xml;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.NoopAuditService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.api.Session;
import org.trellisldp.http.TrellisHttpResource;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.XSD;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebDAVTest extends JerseyTest {

    private static final Logger LOGGER = getLogger(WebDAVTest.class);
    private static final int SC_MULTI_STATUS = 207;

    private static final IOService ioService = new JenaIOService();

    private static final int timestamp = 1496262729;

    private static final Instant time = ofEpochSecond(timestamp);

    private static final RDF rdf = getInstance();

    private static final String RANDOM_VALUE = "randomValue";

    private static final String RESOURCE_PATH = "resource";
    private static final String CHILD_PATH = RESOURCE_PATH + "/child";
    private static final String NON_EXISTENT_PATH = "nonexistent";
    private static final String DELETED_PATH = "deleted";

    private static final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH);
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final IRI nonexistentIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + NON_EXISTENT_PATH);
    private static final IRI childIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + CHILD_PATH);
    private static final IRI deletedIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + DELETED_PATH);
    private static final Set<IRI> allInteractionModels = new HashSet<>(asList(LDP.Resource, LDP.RDFSource,
                LDP.NonRDFSource, LDP.Container, LDP.BasicContainer, LDP.DirectContainer, LDP.IndirectContainer));

    private static final Set<IRI> allModes = new HashSet<>(asList(ACL.Append, ACL.Control, ACL.Read, ACL.Write));

    @Mock
    private ServiceBundler mockBundler;

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private AccessControlService mockAccessControlService;

    @Mock
    private AgentService mockAgentService;

    @Mock
    private Resource mockResource, mockRootResource;

    @BeforeAll
    public void before() throws Exception {
        super.setUp();
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
    }

    @AfterAll
    public void after() throws Exception {
        super.tearDown();
    }

    @BeforeEach
    public void setUpMocks() throws Exception {
        setUpBundler();
        setUpResourceService();
        setUpResources();
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(allModes);
    }

    @Override
    public Application configure() {

        initMocks(this);

        final String baseUri = getBaseUri().toString();

        final ResourceConfig config = new ResourceConfig();

        config.register(new DebugExceptionMapper());
        config.register(new TrellisWebDAVRequestFilter(mockBundler));
        config.register(new TrellisWebDAVResponseFilter());
        config.register(new TrellisWebDAV(mockBundler, baseUri));
        config.register(new TrellisWebDAVAuthzFilter(mockAccessControlService));
        config.register(new TrellisHttpResource(mockBundler, baseUri));
        return config;
    }

    /* ****************************** *
     *           MKCOL Tests
     * ****************************** */
    @Test
    public void testMkcol() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(CHILD_PATH).request().method("MKCOL");

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUri() + CHILD_PATH, res.getLocation().toString(), "Incorrect Location header!");
    }

    @Test
    public void testMkcolRoot() {
        final Response res = target().request().method("MKCOL");
        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testMkcolExisting() {
        final Response res = target(RESOURCE_PATH).request().method("MKCOL");
        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testMkcolNotContainer() {
        final Response res = target(CHILD_PATH).request().method("MKCOL");
        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }


    /* ****************************** *
     *           PROPFIND Tests
     * ****************************** */
    @Test
    public void testPropFindAll() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .method("PROPFIND", xml("<D:propfind xmlns:D=\"DAV:\">"
                        + "  <D:allprop/>"
                        + "  <D:include>"
                        + "    <D:supported-live-property-set/>"
                        + "    <D:supported-report-set/>"
                        + "  </D:include>"
                        + "</D:propfind>"));

        assertEquals(SC_MULTI_STATUS, res.getStatus(), "Unexpected response code!");
        assertTrue(APPLICATION_XML_TYPE.isCompatible(res.getMediaType()),
                "Incorrect content-type: " + res.getMediaType());
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        LOGGER.info("Find All: {}", entity);
        assertTrue(entity.contains("A title"));
        assertTrue(entity.contains("2017-04-01T10:15:00Z"));
        assertTrue(entity.contains("HTTP/1.1 200 OK"));
        assertNull(res.getHeaderString("DAV"));
    }

    @Test
    public void testPropFindNamesOnly() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .method("PROPFIND", xml("<D:propfind xmlns:D=\"DAV:\">"
                        + "  <D:propname/>"
                        + "  <D:prop xmlns:dc=\"http://purl.org/dc/terms/\">"
                        + "    <dc:title/>"
                        + "    <dc:subject/>"
                        + "    Text node"
                        + "    <!-- comment -->"
                        + "    <foo/>"
                        + "  <!-- comment -->"
                        + "  </D:prop>"
                        + "</D:propfind>"));

        assertEquals(SC_MULTI_STATUS, res.getStatus(), "Unexpected response code!");
        assertTrue(APPLICATION_XML_TYPE.isCompatible(res.getMediaType()),
                "Incorrect content-type: " + res.getMediaType());
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        LOGGER.info("Find without values {}", entity);
        assertTrue(entity.contains("title"));
        assertFalse(entity.contains("A title"));
        assertTrue(entity.contains("subject"));
        assertFalse(entity.contains("http://example.com/Subject"));
        assertFalse(entity.contains("2017-04-01T10:15:00Z"));
        assertTrue(entity.contains("HTTP/1.1 200 OK"));
        assertNull(res.getHeaderString("DAV"));
    }

    @Test
    public void testPropFind() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .method("PROPFIND", xml("<D:propfind xmlns:D=\"DAV:\">\n"
                        + "  <D:prop xmlns:dc=\"http://purl.org/dc/terms/\">\n"
                        + "    <dc:title/>\n"
                        + "    <dc:subject/>\n"
                        + "  </D:prop>\n"
                        + "</D:propfind>"));

        assertEquals(SC_MULTI_STATUS, res.getStatus(), "Unexpected response code!");
        assertTrue(APPLICATION_XML_TYPE.isCompatible(res.getMediaType()),
                "Incorrect content-type: " + res.getMediaType());
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        LOGGER.info("Find specific {}", entity);
        assertTrue(entity.contains("A title"));
        assertTrue(entity.contains("http://example.com/Subject"));
        assertFalse(entity.contains("2017-04-01T10:15:00Z"));
        assertTrue(entity.contains("HTTP/1.1 200 OK"));
        assertNull(res.getHeaderString("DAV"));
    }

    @Test
    public void testPropFindContainer() throws IOException {
        final Response res = target().request()
            .method("PROPFIND", xml("<D:propfind xmlns:D=\"DAV:\">\n"
                        + "  <D:prop xmlns:dc=\"http://purl.org/dc/terms/\">\n"
                        + "    <dc:title/>\n"
                        + "  </D:prop>\n"
                        + "</D:propfind>"));

        assertEquals(SC_MULTI_STATUS, res.getStatus(), "Unexpected response code!");
        assertTrue(APPLICATION_XML_TYPE.isCompatible(res.getMediaType()),
                "Incorrect content-type: " + res.getMediaType());
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        LOGGER.info("Find root {}", entity);
        assertFalse(entity.contains("title"));
        assertTrue(entity.contains("collection"));
        assertTrue(entity.contains("HTTP/1.1 200 OK"));
        assertNull(res.getHeaderString("DAV"));
    }

    @Test
    public void testPropFindDeleted() {
        final Response res = target(DELETED_PATH).request()
            .method("PROPFIND", xml("<D:propfind xmlns:D=\"DAV:\">\n"
                        + "  <D:prop xmlns:dc=\"http://purl.org/dc/terms/\">\n"
                        + "    <dc:title/>\n"
                        + "  </D:prop>\n"
                        + "</D:propfind>"));
        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPropFindNonExistent() {
        final Response res = target(NON_EXISTENT_PATH).request()
            .method("PROPFIND", xml("<D:propfind xmlns:D=\"DAV:\">\n"
                        + "  <D:prop xmlns:dc=\"http://purl.org/dc/terms/\">\n"
                        + "    <dc:title/>\n"
                        + "  </D:prop>\n"
                        + "</D:propfind>"));
        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPropFindBadXMl() {
        final Response res = target(RESOURCE_PATH).request()
            .method("PROPFIND", xml("<D:propfind xmlns:D=\"DAV:\">\n"
                        + "  <D:prop xmlns:dc=\"http://purl.org/dc/terms/\">\n"
                        + "    <dc:title/>"
                        + "  </D:prop>\n"
                        + "</D:badtag>"));

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    /* ****************************** *
     *           PROPPATCH Tests
     * ****************************** */
    @Test
    public void testPropPatchAddRemove() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .method("PROPPATCH", xml("<D:propertyupdate xmlns:D=\"DAV:\">\n"
                        + "  <D:set>"
                        + "    <D:prop>"
                        + "      <title xmlns=\"http://purl.org/dc/terms/\">A different title</title>"
                        + "      <relation xmlns=\"http://purl.org/dc/terms/\" foo=\"bar\">"
                        + "        <ex:other xmlns:ex=\"http://example.org/\"/>"
                        + "        <!-- comment -->"
                        + "      </relation>"
                        + "      <!-- comment -->"
                        + "      SOME TEXT"
                        + "      <B:foo xmlns:B=\"http://example.com/\"><bar/></B:foo>"
                        + "      <baz/>"
                        + "    </D:prop>"
                        + "  </D:set>"
                        + "  <D:remove>"
                        + "    <D:prop>"
                        + "      <subject xmlns=\"http://purl.org/dc/terms/\"/>"
                        + "      <!-- comment -->"
                        + "      SOME TEXT"
                        + "      <B:foo xmlns:B=\"http://example.com/\"><bar/></B:foo>"
                        + "      <baz/>"
                        + "    </D:prop>"
                        + "  </D:remove>"
                        + "</D:propertyupdate>"));

        assertEquals(SC_MULTI_STATUS, res.getStatus(), "Unexpected response code!");
        assertTrue(APPLICATION_XML_TYPE.isCompatible(res.getMediaType()),
                "Incorrect content-type: " + res.getMediaType());
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        LOGGER.info("Update: {}", entity);
        assertTrue(entity.contains("title"));
        assertTrue(entity.contains("subject"));
        assertTrue(entity.contains("relation"));
        assertTrue(entity.contains("HTTP/1.1 200 OK"));
        assertNull(res.getHeaderString("DAV"));
    }

    @Test
    public void testPropPatchAdd() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .method("PROPPATCH", xml("<D:propertyupdate xmlns:D=\"DAV:\">\n"
                        + "  <D:set>"
                        + "    <D:prop>"
                        + "      <title xmlns=\"http://purl.org/dc/terms/\">A different title</title>"
                        + "      <relation xmlns=\"http://purl.org/dc/terms/\">"
                        + "        <ex:other xmlns:ex=\"http://example.org/\"/>"
                        + "      </relation>"
                        + "    </D:prop>"
                        + "  </D:set>"
                        + "</D:propertyupdate>"));

        assertEquals(SC_MULTI_STATUS, res.getStatus(), "Unexpected response code!");
        assertTrue(APPLICATION_XML_TYPE.isCompatible(res.getMediaType()),
                "Incorrect content-type: " + res.getMediaType());
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        LOGGER.info("Update set: {}", entity);
        assertTrue(entity.contains("title"));
        assertTrue(entity.contains("relation"));
        assertFalse(entity.contains("subject"));
        assertTrue(entity.contains("HTTP/1.1 200 OK"));
        assertNull(res.getHeaderString("DAV"));
    }

    @Test
    public void testPropPatchRemove() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .method("PROPPATCH", xml("<D:propertyupdate xmlns:D=\"DAV:\">\n"
                        + "  <D:remove>"
                        + "    <D:prop>"
                        + "      <subject xmlns=\"http://purl.org/dc/terms/\"/>"
                        + "    </D:prop>"
                        + "  </D:remove>"
                        + "</D:propertyupdate>"));

        assertEquals(SC_MULTI_STATUS, res.getStatus(), "Unexpected response code!");
        assertTrue(APPLICATION_XML_TYPE.isCompatible(res.getMediaType()),
                "Incorrect content-type: " + res.getMediaType());
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        LOGGER.info("Update: {}", entity);
        assertTrue(entity.contains("subject"));
        assertFalse(entity.contains("title"));
        assertFalse(entity.contains("relation"));
        assertTrue(entity.contains("HTTP/1.1 200 OK"));
        assertNull(res.getHeaderString("DAV"));
    }


    /* ****************************** *
     *           OPTIONS Tests
     * ****************************** */
    @Test
    public void testOptions() {
        final Response res = target(RESOURCE_PATH).request().options();
        assertEquals("1,3", res.getHeaderString("DAV"));
    }

    private void setUpBundler() {
        when(mockBundler.getIOService()).thenReturn(ioService);
        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockBundler.getAgentService()).thenReturn(mockAgentService);
        when(mockBundler.getAuditService()).thenReturn(new NoopAuditService());
        when(mockBundler.getBinaryService()).thenReturn(mockBinaryService);
        when(mockBundler.getEventService()).thenReturn(new NoopEventService());
        when(mockBundler.getMementoService()).thenReturn(new NoopMementoService());
    }

    private void setUpResourceService() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource"))))
            .thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.get(eq(root))).thenAnswer(inv -> completedFuture(mockRootResource));
        when(mockResourceService.get(eq(childIdentifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockResourceService.supportedInteractionModels()).thenReturn(allInteractionModels);
        when(mockResourceService.get(eq(nonexistentIdentifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockResourceService.get(eq(deletedIdentifier))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        when(mockResourceService.generateIdentifier()).thenReturn(RANDOM_VALUE);
        when(mockResourceService.unskolemize(any(IRI.class))).thenCallRealMethod();
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenCallRealMethod();
        when(mockResourceService.toExternal(any(RDFTerm.class), any())).thenCallRealMethod();
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.delete(any(Metadata.class))).thenReturn(completedFuture(null));
        when(mockResourceService.replace(any(Metadata.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.create(any(Metadata.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.unskolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockResourceService.touch(any(IRI.class))).thenReturn(completedFuture(null));
        when(mockResource.stream(eq(PreferUserManaged))).thenAnswer(inf -> Stream.of(
                rdf.createTriple(identifier, DC.relation, rdf.createBlankNode()),
                rdf.createTriple(identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createTriple(identifier, DC.subject, rdf.createIRI("http://example.com/Subject")),
                rdf.createTriple(identifier, DC.title, rdf.createLiteral("A title"))));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(PreferUserManaged, identifier, DC.relation, rdf.createBlankNode()),
                rdf.createQuad(PreferUserManaged, identifier, DC.subject, rdf.createIRI("http://example.com/Subject")),
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(PreferServerManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control)));
    }

    private void setUpResources() {
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getBinaryMetadata()).thenReturn(empty());
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());

        when(mockRootResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockRootResource.getModified()).thenReturn(time);
        when(mockRootResource.getBinaryMetadata()).thenReturn(empty());
        when(mockRootResource.getIdentifier()).thenReturn(root);
        when(mockRootResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());
        when(mockRootResource.hasAcl()).thenReturn(true);
    }
}

