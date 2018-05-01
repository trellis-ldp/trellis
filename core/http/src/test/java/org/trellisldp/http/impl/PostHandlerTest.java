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

import static com.google.common.collect.Sets.newHashSet;
import static java.net.URI.create;
import static java.time.Instant.ofEpochSecond;
import static java.util.Collections.emptySet;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Link.fromUri;
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
import static org.trellisldp.api.AuditService.none;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
public class PostHandlerTest {

    private static final Instant time = ofEpochSecond(1496262729);
    private static final String baseUrl = "http://example.org/repo/";
    private static final RDF rdf = getInstance();
    private static final Set<IRI> allInteractionModels = newHashSet(LDP.Resource, LDP.RDFSource,
            LDP.NonRDFSource, LDP.Container, LDP.BasicContainer, LDP.DirectContainer, LDP.IndirectContainer);

    private File entity;

    @Mock
    private ResourceService mockResourceService;

    private AuditService mockAuditService = none();

    private final AgentService agentService = new SimpleAgentService();

    @Mock
    private IOService mockIoService;

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private Resource mockResource;

    @Mock
    private LdpRequest mockRequest;

    @Mock
    private Future<Boolean> mockFuture;

    @Mock
    private SecurityContext mockSecurityContext;

    @Captor
    private ArgumentCaptor<IRI> iriArgument;

    @Captor
    private ArgumentCaptor<Map<String, String>> metadataArgument;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockBinaryService.generateIdentifier()).thenReturn("file:" + randomUUID());
        when(mockResourceService.supportedInteractionModels()).thenReturn(allInteractionModels);
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));
        when(mockResourceService.create(any(IRI.class), any(Session.class), any(IRI.class), any(),
                        any(Dataset.class))).thenReturn(completedFuture(true));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));

        when(mockRequest.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockRequest.getPath()).thenReturn("");
        when(mockRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            if (term instanceof IRI) {
                final String iri = ((IRI) term).getIRIString();
                if (iri.startsWith(baseUrl)) {
                    return rdf.createIRI(TRELLIS_DATA_PREFIX + iri.substring(baseUrl.length()));
                }
            }
            return term;
        });
    }

    @Test
    public void testPostLdprs() throws IOException {
        when(mockRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockRequest.getContentType()).thenReturn(TEXT_TURTLE);
        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        // will never store audit
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(false));
        final AuditService badAuditService = new DefaultAuditService() {};
        final PostHandler handler = new PostHandler(mockRequest, null, entity, mockResourceService,
                        badAuditService, mockIoService, mockBinaryService, agentService, null);

        assertThrows(BadRequestException.class, handler::createResource);
    }

    @Test
    public void testDefaultType1() throws IOException {
        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

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
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

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
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

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
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

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

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testUnsupportedType() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        when(mockRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

        final BadRequestException ex = assertThrows(BadRequestException.class, postHandler::createResource);
        assertTrue(ex.getResponse().getLinks().stream().anyMatch(link ->
                link.getUri().toString().equals(Trellis.UnsupportedInteractionModel.getIRIString()) &&
                link.getRel().equals(LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testEntity() throws IOException {
        final String path = "newresource";
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + path);
        final Triple triple = rdf.createTriple(rdf.createIRI(baseUrl + path), DC.title,
                        rdf.createLiteral("A title"));
        when(mockIoService.read(any(), any(), eq(TURTLE))).thenAnswer(x -> Stream.of(triple));
        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());

        when(mockRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

        final Response res = postHandler.createResource().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + path), res.getLocation());

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));

        verify(mockIoService).read(any(InputStream.class), eq(baseUrl + path), eq(TURTLE));

        verify(mockResourceService).create(eq(identifier), any(Session.class), eq(LDP.RDFSource), any(),
                        any(Dataset.class));
    }

    @Test
    public void testEntity2() throws IOException {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "newresource");
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockRequest.getContentType()).thenReturn("text/plain");

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

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

        verify(mockResourceService).create(eq(identifier), any(Session.class), eq(LDP.NonRDFSource), any(),
                        any(Dataset.class));
    }

    @Test
    public void testEntity3() throws IOException {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "newresource");
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockRequest.getContentType()).thenReturn("text/plain");
        when(mockRequest.getDigest()).thenReturn(new Digest("md5", "1VOyRwUXW1CPdC5nelt7GQ=="));

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

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

        verify(mockResourceService).create(eq(identifier), any(Session.class), eq(LDP.NonRDFSource), any(),
                        any(Dataset.class));
    }

    @Test
    public void testEntityBadDigest() {
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockRequest.getContentType()).thenReturn("text/plain");
        when(mockRequest.getDigest()).thenReturn(new Digest("md5", "blahblah"));

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

        assertThrows(BadRequestException.class, postHandler::createResource);
    }

    @Test
    public void testBadDigest2() {
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockRequest.getContentType()).thenReturn("text/plain");
        when(mockRequest.getDigest()).thenReturn(new Digest("foo", "blahblah"));

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

        assertThrows(BadRequestException.class, postHandler::createResource);
    }

    @Test
    public void testBadEntityDigest() {
        when(mockRequest.getContentType()).thenReturn("text/plain");
        when(mockRequest.getDigest()).thenReturn(new Digest("md5", "blahblah"));
        final File entity = new File(new File(getClass().getResource("/simpleData.txt").getFile()).getParent());

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, null);

        assertThrows(WebApplicationException.class, postHandler::createResource);
    }

    @Test
    public void testEntityError() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "newresource");
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile() + ".nonexistent-suffix");
        when(mockRequest.getContentType()).thenReturn("text/plain");

        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, baseUrl);

        assertThrows(WebApplicationException.class, postHandler::createResource);
    }

    @Test
    public void testError() throws IOException {
        when(mockResourceService.create(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + "newresource")), any(Session.class),
                    any(IRI.class), any(), any(Dataset.class))).thenReturn(completedFuture(false));
        when(mockRequest.getContentType()).thenReturn("text/turtle");

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, baseUrl);

        assertThrows(BadRequestException.class, postHandler::createResource);
    }

    @Test
    public void testException() throws Exception {
        when(mockFuture.get()).thenThrow(new InterruptedException("Expected"));
        when(mockResourceService.create(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + "newresource")), any(Session.class),
                    any(IRI.class), any(), any(Dataset.class))).thenReturn(mockFuture);
        when(mockRequest.getContentType()).thenReturn("text/turtle");

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler postHandler = new PostHandler(mockRequest, "newresource", entity, mockResourceService,
                        mockAuditService, mockIoService, mockBinaryService, agentService, baseUrl);

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
