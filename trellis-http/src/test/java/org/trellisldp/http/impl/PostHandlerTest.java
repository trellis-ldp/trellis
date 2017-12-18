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
package org.trellisldp.http.impl;

import static java.net.URI.create;
import static java.time.Instant.ofEpochSecond;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class PostHandlerTest {

    private static final Instant time = ofEpochSecond(1496262729);
    private static final String baseUrl = "http://example.org/repo/";
    private static final RDF rdf = getInstance();
    private File entity;

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private IOService mockIoService;

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private Resource mockResource;

    @Mock
    private LdpRequest mockRequest;

    @Captor
    private ArgumentCaptor<IRI> iriArgument;

    @Captor
    private ArgumentCaptor<Map<String, String>> metadataArgument;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockBinaryService.getIdentifierSupplier()).thenReturn(() -> "file:" + randomUUID());
        when(mockResourceService.put(any(IRI.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));

        when(mockRequest.getSession()).thenReturn(new HttpSession());
        when(mockRequest.getPath()).thenReturn("");
        when(mockRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            if (term instanceof IRI) {
                final String iri = ((IRI) term).getIRIString();
                if (iri.startsWith(baseUrl)) {
                    return rdf.createIRI(TRELLIS_PREFIX + iri.substring(baseUrl.length()));
                }
            }
            return term;
        });
    }

    @Test
    public void testPostLdprs() throws IOException {
        when(mockRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testDefaultType1() throws IOException {
        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testDefaultType2() throws IOException {
        when(mockRequest.getContentType()).thenReturn("text/plain");

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testDefaultType3() throws IOException {
        when(mockRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testDefaultType4() throws IOException {
        when(mockRequest.getContentType()).thenReturn("text/plain");
        when(mockRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testDefaultType5() throws IOException {
        when(mockRequest.getContentType()).thenReturn("text/turtle");
        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testEntity() throws IOException {
        final String path = "newresource";
        final IRI identifier = rdf.createIRI("trellis:" + path);
        final Triple triple = rdf.createTriple(rdf.createIRI(baseUrl + path), DC.title,
                        rdf.createLiteral("A title"));
        when(mockIoService.read(any(), any(), eq(TURTLE))).thenAnswer(x -> Stream.of(triple));
        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());

        when(mockRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + path), res.getLocation());

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));

        verify(mockIoService).read(any(InputStream.class), eq(baseUrl + path), eq(TURTLE));

        verify(mockResourceService).put(eq(identifier), eq(LDP.RDFSource), any(Dataset.class));
    }

    @Test
    public void testEntity2() throws IOException {
        final IRI identifier = rdf.createIRI("trellis:newresource");
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockRequest.getContentType()).thenReturn("text/plain");

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());

        verify(mockIoService, never()).read(any(), any(), any());

        verify(mockBinaryService).setContent(iriArgument.capture(), any(InputStream.class),
                metadataArgument.capture());
        assertTrue(iriArgument.getValue().getIRIString().startsWith("file:"));
        assertEquals("text/plain", metadataArgument.getValue().get(CONTENT_TYPE));

        verify(mockResourceService).put(eq(identifier), eq(LDP.NonRDFSource), any(Dataset.class));
    }

    @Test
    public void testEntity3() throws IOException {
        final IRI identifier = rdf.createIRI("trellis:newresource");
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockRequest.getContentType()).thenReturn("text/plain");
        when(mockRequest.getDigest()).thenReturn(new Digest("md5", "1VOyRwUXW1CPdC5nelt7GQ=="));

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());

        verify(mockIoService, never()).read(any(), any(), any());

        verify(mockBinaryService).setContent(iriArgument.capture(), any(InputStream.class),
                metadataArgument.capture());
        assertTrue(iriArgument.getValue().getIRIString().startsWith("file:"));
        assertEquals("text/plain", metadataArgument.getValue().get(CONTENT_TYPE));

        verify(mockResourceService).put(eq(identifier), eq(LDP.NonRDFSource), any(Dataset.class));
    }

    @Test
    public void testEntityBadDigest() {
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockRequest.getContentType()).thenReturn("text/plain");
        when(mockRequest.getDigest()).thenReturn(new Digest("md5", "blahblah"));

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        assertEquals(BAD_REQUEST, postHandler.createResource().build().getStatusInfo());
    }

    @Test
    public void testBadDigest2() {
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockRequest.getContentType()).thenReturn("text/plain");
        when(mockRequest.getDigest()).thenReturn(new Digest("foo", "blahblah"));

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        assertThrows(BadRequestException.class, postHandler::createResource);
    }

    @Test
    public void testBadEntityDigest() {
        when(mockRequest.getContentType()).thenReturn("text/plain");
        when(mockRequest.getDigest()).thenReturn(new Digest("md5", "blahblah"));
        final File entity = new File(new File(getClass().getResource("/simpleData.txt").getFile()).getParent());

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, null);

        assertThrows(WebApplicationException.class, postHandler::createResource);
    }

    @Test
    public void testEntityError() {
        final IRI identifier = rdf.createIRI("trellis:newresource");
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile() + ".nonexistent-suffix");
        when(mockRequest.getContentType()).thenReturn("text/plain");

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, baseUrl);

        assertThrows(WebApplicationException.class, postHandler::createResource);
    }

    @Test
    public void testError() throws IOException {
        when(mockResourceService.put(eq(rdf.createIRI(TRELLIS_PREFIX + "newresource")), any(IRI.class),
                    any(Dataset.class))).thenReturn(completedFuture(false));
        when(mockRequest.getContentType()).thenReturn("text/turtle");

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity,
                mockResourceService, mockIoService, mockBinaryService, baseUrl);

        final Response res = postHandler.createResource().build();
        assertEquals(INTERNAL_SERVER_ERROR, res.getStatusInfo());
    }

    private static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    private static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, "type");
    }
}
