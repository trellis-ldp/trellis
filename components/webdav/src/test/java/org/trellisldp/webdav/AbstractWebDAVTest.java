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
package org.trellisldp.webdav;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.ofEpochSecond;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_GONE;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.client.Entity.xml;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.apache.commons.io.IOUtils.readLines;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.http.core.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.core.Link;
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
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.XSD;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractWebDAVTest extends JerseyTest {

    private static final Logger LOGGER = getLogger(WebDAVTest.class);
    private static final int SC_MULTI_STATUS = 207;

    private static final IOService ioService = new JenaIOService();

    private static final int timestamp = 1496262729;

    private static final Instant time = ofEpochSecond(timestamp);

    private static final RDF rdf = RDFFactory.getInstance();

    private static final String RANDOM_VALUE = "randomValue";

    private static final String RESOURCE_PATH = "resource";
    private static final String CHILD_PATH = RESOURCE_PATH + "/child";
    private static final String OTHER_PATH = RESOURCE_PATH + "/other";
    private static final String NON_EXISTENT_PATH = "nonexistent";
    private static final String DELETED_PATH = "deleted";
    private static final String BINARY_PATH = "binary";

    private static final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH);
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final IRI binaryIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + BINARY_PATH);
    private static final IRI nonexistentIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + NON_EXISTENT_PATH);
    private static final IRI childIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + CHILD_PATH);
    private static final IRI otherIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + OTHER_PATH);
    private static final IRI deletedIdentifier = rdf.createIRI(TRELLIS_DATA_PREFIX + DELETED_PATH);
    private static final IRI binaryInternalIdentifier = rdf.createIRI("file:///some/file");
    private static final Set<IRI> allInteractionModels = new HashSet<>(asList(LDP.Resource, LDP.RDFSource,
                LDP.NonRDFSource, LDP.Container, LDP.BasicContainer, LDP.DirectContainer, LDP.IndirectContainer));

    private static final BinaryMetadata testBinary = BinaryMetadata.builder(binaryInternalIdentifier)
        .mimeType(TEXT_PLAIN).build();

    @Mock
    protected ServiceBundler mockBundler;

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private Resource mockResource, mockRootResource, mockBinaryResource, mockOtherResource;

    @BeforeAll
    void before() throws Exception {
        super.setUp();
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
    }

    @AfterAll
    void after() throws Exception {
        super.tearDown();
    }

    @BeforeEach
    void setUpMocks() {
        setUpBundler();
        setUpResourceService();
        setUpBinaryService();
        setUpResources();
    }

    /* ****************************** *
     *           MKCOL Tests
     * ****************************** */
    @Test
    void testMkcol() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(CHILD_PATH).request().method("MKCOL");

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUri() + CHILD_PATH + "/", res.getLocation().toString(), "Incorrect Location header!");
    }

    @Test
    void testMkcolRoot() {
        final Response res = target().request().method("MKCOL");
        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMkcolExisting() {
        final Response res = target(RESOURCE_PATH).request().method("MKCOL");
        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMkcolNotContainer() {
        final Response res = target(CHILD_PATH).request().method("MKCOL");
        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }


    /* ****************************** *
     *           PROPFIND Tests
     * ****************************** */
    @Test
    void testPropFindAll() throws IOException {
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
    void testPropFindNamesOnly() throws IOException {
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
    void testPropFind() throws IOException {
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
    void testPropFindContainer() throws IOException {
        when(mockRootResource.stream(eq(LDP.PreferContainment))).thenAnswer(inf -> Stream.of(
                rdf.createQuad(LDP.PreferContainment, root, LDP.contains, identifier),
                rdf.createQuad(LDP.PreferContainment, root, LDP.contains, binaryIdentifier)));

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
        assertTrue(entity.contains("title"));
        assertTrue(entity.contains("collection"));
        assertTrue(entity.contains("HTTP/1.1 200 OK"));
        assertNull(res.getHeaderString("DAV"));
    }

    @Test
    void testPropFindDeleted() {
        final Response res = target(DELETED_PATH).request()
            .method("PROPFIND", xml("<D:propfind xmlns:D=\"DAV:\">\n"
                        + "  <D:prop xmlns:dc=\"http://purl.org/dc/terms/\">\n"
                        + "    <dc:title/>\n"
                        + "  </D:prop>\n"
                        + "</D:propfind>"));
        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testPropFindNonExistent() {
        final Response res = target(NON_EXISTENT_PATH).request()
            .method("PROPFIND", xml("<D:propfind xmlns:D=\"DAV:\">\n"
                        + "  <D:prop xmlns:dc=\"http://purl.org/dc/terms/\">\n"
                        + "    <dc:title/>\n"
                        + "  </D:prop>\n"
                        + "</D:propfind>"));
        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testPropFindBadXMl() {
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
    void testPropPatchAddRemove() throws IOException {
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
    void testPropPatchAdd() throws IOException {
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
    void testPropPatchRemove() throws IOException {
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

    @Test
    void testPropPatchRemoveWithEmptySet() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .method("PROPPATCH", xml("<D:propertyupdate xmlns:D=\"DAV:\">\n"
                        + "  <D:set>"
                        + "  </D:set>"
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
     *           PUT Tests
     * ****************************** */
    @Test
    void testPut() {
        final Response res = target(BINARY_PATH).request().put(entity("a text document.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().map(l ->
                    l.getUri().toString()).anyMatch(isEqual(LDP.NonRDFSource.getIRIString())));
        assertTrue(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    void testPutNew() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        final Response res = target(CHILD_PATH).request().put(entity("a text document.", TEXT_PLAIN_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().map(l ->
                    l.getUri().toString()).anyMatch(isEqual(LDP.NonRDFSource.getIRIString())));
        assertTrue(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
    }

    @Test
    void testPutPreviouslyDeleted() {
        when(mockResourceService.get(eq(childIdentifier))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        final Response res = target(CHILD_PATH).request().put(entity("another text document.", TEXT_PLAIN_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().map(l ->
                    l.getUri().toString()).anyMatch(isEqual(LDP.NonRDFSource.getIRIString())));
        assertTrue(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
    }

    @Test
    void testPutRoot() {
        when(mockResourceService.get(eq(root))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        final Response res = target().request().put(entity("another text document.", TEXT_PLAIN_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream()
                .map(link -> link.getUri().toString()).anyMatch(isEqual(LDP.NonRDFSource.getIRIString())));
        assertTrue(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
    }

    /* ****************************** *
     *           DELETE Tests
     * ****************************** */
    @Test
    void testDeleteExisting() {
        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testDeleteRecursive() {

        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResource.stream(eq(LDP.PreferContainment))).thenAnswer(inf -> Stream.of(
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains, otherIdentifier)));

        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    /* ****************************** *
     *           MOVE Tests
     * ****************************** */
    @Test
    void testMove() {
        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri() + NON_EXISTENT_PATH)
            .method("MOVE");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMoveNoDestination() {
        final Response res = target(RESOURCE_PATH).request().method("MOVE");

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMoveToExisting() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Destination", getBaseUri() + BINARY_PATH).method("MOVE");

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMoveToDeleted() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Destination", getBaseUri() + DELETED_PATH).method("MOVE");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMoveFromDeleted() {
        final Response res = target(DELETED_PATH).request()
            .header("Destination", getBaseUri() + DELETED_PATH).method("MOVE");

        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMoveOutOfDomain() {
        final Response res = target(RESOURCE_PATH).request().header("Destination", "http://example.com/location")
            .method("MOVE");

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMoveError() {
        when(mockResourceService.get(root)).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException("Expected");
        }));
        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri() + NON_EXISTENT_PATH)
            .method("MOVE");

        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMoveToRoot() {

        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri())
            .method("MOVE");

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMoveToDeletedParent() {

        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));

        final Response res = target(OTHER_PATH).request().header("Destination", getBaseUri() + RESOURCE_PATH + "/other")
            .method("MOVE");

        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testMoveToMissingParent() {

        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(OTHER_PATH).request().header("Destination", getBaseUri() + RESOURCE_PATH + "/other")
            .method("MOVE");

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    /* ****************************** *
     *           COPY Tests
     * ****************************** */
    @Test
    void testCopy() {
        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri() + NON_EXISTENT_PATH)
            .method("COPY");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyDepth0() {
        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri() + NON_EXISTENT_PATH)
            .header("Depth", "0").method("COPY");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyDepth1() {
        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri() + NON_EXISTENT_PATH)
            .header("Depth", "1").method("COPY");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyDepthInfinity() {
        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri() + NON_EXISTENT_PATH)
            .header("Depth", "infinity").method("COPY");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyFromDeleted() {
        final Response res = target(DELETED_PATH).request()
            .header("Destination", getBaseUri() + DELETED_PATH).method("COPY");

        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyFromRoot() {
        final Response res = target().request()
            .header("Destination", getBaseUri() + NON_EXISTENT_PATH).method("COPY");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyNoDestination() {
        final Response res = target(RESOURCE_PATH).request().method("COPY");

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyToExisting() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Destination", getBaseUri() + BINARY_PATH).method("COPY");

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyToDeleted() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Destination", getBaseUri() + DELETED_PATH).method("COPY");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyOutOfDomain() {
        final Response res = target(RESOURCE_PATH).request().header("Destination", "http://example.com/location")
            .method("COPY");

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyMissingResource() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri() + NON_EXISTENT_PATH)
            .method("COPY");

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyRecursive() {

        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResource.stream(eq(LDP.PreferContainment))).thenAnswer(inf -> Stream.of(
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains, otherIdentifier)));

        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri() + NON_EXISTENT_PATH)
            .method("COPY");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyRecursiveDepth0() {

        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResource.stream(eq(LDP.PreferContainment))).thenAnswer(inf -> Stream.of(
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains, otherIdentifier)));

        final Response res = target(RESOURCE_PATH).request().header("Depth", "0")
            .header("Destination", getBaseUri() + NON_EXISTENT_PATH).method("COPY");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyRecursiveDepth1() {

        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockResource.stream(eq(LDP.PreferContainment))).thenAnswer(inf -> Stream.of(
                rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains, otherIdentifier)));

        final Response res = target(RESOURCE_PATH).request().header("Depth", "1")
            .header("Destination", getBaseUri() + NON_EXISTENT_PATH).method("COPY");

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyToRoot() {

        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().header("Destination", getBaseUri())
            .method("COPY");

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyToDeletedParent() {

        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));

        final Response res = target(OTHER_PATH).request().header("Destination", getBaseUri() + RESOURCE_PATH + "/other")
            .method("COPY");

        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    void testCopyToMissingParent() {

        when(mockResourceService.get(eq(otherIdentifier))).thenAnswer(inv -> completedFuture(mockOtherResource));
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(OTHER_PATH).request().header("Destination", getBaseUri() + RESOURCE_PATH + "/other")
            .method("COPY");

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    /* ****************************** *
     *           OPTIONS Tests
     * ****************************** */
    @Test
    void testOptions() {
        final Response res = target(RESOURCE_PATH).request().options();
        assertEquals("1,3", res.getHeaderString("DAV"));
    }

    /* ***************************** *
     *      Other tests
     * ***************************** */
    @Test
    void testNoargCtor() {
        assertDoesNotThrow(() -> new TrellisWebDAV());
    }

    private static List<Link> getLinks(final Response res) {
        // Jersey's client doesn't parse complex link headers correctly
        final List<String> links = res.getStringHeaders().get(LINK);
        if (links != null) {
            return links.stream().map(Link::valueOf).collect(toList());
        }
        return emptyList();
    }

    private void setUpBundler() {
        when(mockBundler.getIOService()).thenReturn(ioService);
        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService());
        when(mockBundler.getBinaryService()).thenReturn(mockBinaryService);
        when(mockBundler.getEventService()).thenReturn(new NoopEventService());
        when(mockBundler.getMementoService()).thenReturn(new NoopMementoService());
    }

    private void setUpBinaryService() {
        when(mockBinaryService.generateIdentifier()).thenReturn("file://some/binary/location");
        when(mockBinaryService.setContent(any(BinaryMetadata.class), any(InputStream.class)))
            .thenAnswer(inv -> {
                readLines((InputStream) inv.getArguments()[1], UTF_8);
                return completedFuture(null);
            });
    }

    private void setUpResourceService() {
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.create(any(Metadata.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.delete(any(Metadata.class))).thenReturn(completedFuture(null));
        when(mockResourceService.generateIdentifier()).thenReturn(RANDOM_VALUE);
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource"))))
            .thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.get(eq(binaryIdentifier))).thenAnswer(inv -> completedFuture(mockBinaryResource));
        when(mockResourceService.get(eq(childIdentifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockResourceService.get(eq(deletedIdentifier))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        when(mockResourceService.get(eq(nonexistentIdentifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockResourceService.get(eq(root))).thenAnswer(inv -> completedFuture(mockRootResource));
        when(mockResourceService.replace(any(Metadata.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.supportedInteractionModels()).thenReturn(allInteractionModels);
        when(mockResourceService.toExternal(any(RDFTerm.class), any())).thenCallRealMethod();
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenCallRealMethod();
        when(mockResourceService.touch(any(IRI.class))).thenReturn(completedFuture(null));
        when(mockResourceService.unskolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.unskolemize(any(IRI.class))).thenCallRealMethod();
    }

    private void setUpResources() {
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getBinaryMetadata()).thenReturn(empty());
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());
        when(mockResource.stream(eq(PreferUserManaged))).thenAnswer(inf -> Stream.of(
                rdf.createQuad(PreferUserManaged, identifier, DC.relation, rdf.createBlankNode()),
                rdf.createQuad(PreferUserManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(PreferUserManaged, identifier, DC.subject, rdf.createIRI("http://example.com/Subject")),
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title"))));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                rdf.createQuad(PreferUserManaged, identifier, DC.relation, rdf.createBlankNode()),
                rdf.createQuad(PreferUserManaged, identifier, DC.subject, rdf.createIRI("http://example.com/Subject")),
                rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
                rdf.createQuad(PreferServerManaged, identifier, DC.created,
                    rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
                rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
                rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control)));
        doCallRealMethod().when(mockResource).getRevision();

        when(mockRootResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockRootResource.getModified()).thenReturn(time);
        when(mockRootResource.getBinaryMetadata()).thenReturn(empty());
        when(mockRootResource.getIdentifier()).thenReturn(root);
        when(mockRootResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());
        when(mockRootResource.getMetadataGraphNames()).thenReturn(singleton(PreferAccessControl));
        doCallRealMethod().when(mockRootResource).getRevision();
        doCallRealMethod().when(mockRootResource).hasMetadata(any());

        when(mockBinaryResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockBinaryResource.getModified()).thenReturn(time);
        when(mockBinaryResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockBinaryResource.getIdentifier()).thenReturn(binaryIdentifier);
        when(mockBinaryResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());
        doCallRealMethod().when(mockBinaryResource).getRevision();

        when(mockOtherResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockOtherResource.getModified()).thenReturn(time);
        when(mockOtherResource.getBinaryMetadata()).thenReturn(empty());
        when(mockOtherResource.getContainer()).thenReturn(of(identifier));
        when(mockOtherResource.getIdentifier()).thenReturn(otherIdentifier);
        when(mockOtherResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());
        doCallRealMethod().when(mockOtherResource).hasMetadata(any());
        doCallRealMethod().when(mockOtherResource).getMetadataGraphNames();
        doCallRealMethod().when(mockOtherResource).getRevision();
    }
}

