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
import static java.time.Instant.ofEpochSecond;
import static java.util.Collections.emptySet;
import static java.util.Date.from;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.io.InputStream;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.RDFTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
public class PutHandlerTest {

    private static final Instant time = ofEpochSecond(1496262729);
    private static final Instant binaryTime = ofEpochSecond(1496262750);

    private static final String baseUrl = "http://localhost:8080/repo/";
    private static final RDF rdf = getInstance();
    private static final IRI identifier = rdf.createIRI("trellis:data/resource");
    private static final Set<IRI> allInteractionModels = newHashSet(LDP.Resource, LDP.RDFSource, LDP.NonRDFSource,
            LDP.Container, LDP.BasicContainer, LDP.DirectContainer, LDP.IndirectContainer);

    private final Binary testBinary = new Binary(rdf.createIRI("file:///binary.txt"), binaryTime, "text/plain", null);

    private final AgentService agentService = new SimpleAgentService();

    private final AuditService mockAuditService = none();

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private IOService mockIoService;

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private Resource mockResource;

    @Mock
    private Request mockRequest;

    @Mock
    private LdpRequest mockLdpRequest;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private Future<Boolean> mockFuture;

    @Captor
    private ArgumentCaptor<Dataset> dataset;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getBinary()).thenReturn(empty());
        when(mockResource.getModified()).thenReturn(time);
        when(mockBinaryService.generateIdentifier()).thenReturn("file:///" + randomUUID());

        when(mockResourceService.supportedInteractionModels()).thenReturn(allInteractionModels);
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));
        when(mockResourceService.replace(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class),
                        any(), any())).thenReturn(completedFuture(true));
        when(mockResourceService.create(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class),
                        any(), any())).thenReturn(completedFuture(true));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));

        when(mockLdpRequest.getRequest()).thenReturn(mockRequest);
        when(mockLdpRequest.getPath()).thenReturn("resource");
        when(mockLdpRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockLdpRequest.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = inv.getArgument(0);
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
    public void testPutConflict() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.DirectContainer.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        assertThrows(WebApplicationException.class, () -> putHandler.setResource(mockResource));
    }

    @Test
    public void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);
        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        // will never store audit
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(false));
        final AuditService badAuditService = new DefaultAuditService() {};
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, badAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        assertThrows(BadRequestException.class, () -> putHandler.setResource(mockResource));
    }

    @Test
    public void testPutLdpResourceDefaultType() {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + "resource"));
    }

    @Test
    public void testPutLdpResourceContainer() {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + "resource"));
    }

    @Test
    public void testPutError() {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile() + ".non-existent-file");
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        assertThrows(WebApplicationException.class, () -> putHandler.setResource(mockResource));
    }

    @Test
    public void testPutLdpBinaryResourceWithLdprLink() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_PLAIN);

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService).setContent(any(IRI.class), any(InputStream.class), any());
        verify(mockIoService, never()).read(any(InputStream.class), any(RDFSyntax.class), anyString());
    }

    @Test
    public void testPutLdpBinaryResource() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService).setContent(any(IRI.class), any(InputStream.class), any());
        verify(mockIoService, never()).read(any(InputStream.class), any(RDFSyntax.class), anyString());
    }

    @Test
    public void testPutLdpNRDescription() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.RDFSource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleLiteral.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), any(RDFSyntax.class), anyString());
    }

    @Test
    public void testPutLdpNRDescription2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleLiteral.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), any(RDFSyntax.class), anyString());
    }

    @Test
    public void testPutLdpResourceEmpty() {
        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), any(RDFSyntax.class), anyString());
    }

    @Test
    public void testCache() {
        when(mockRequest.evaluatePreconditions(eq(from(binaryTime)), any(EntityTag.class)))
                .thenReturn(status(PRECONDITION_FAILED));
        when(mockResource.getBinary()).thenReturn(of(testBinary));

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        assertThrows(WebApplicationException.class, () -> putHandler.setResource(mockResource));
    }

    @Test
    public void testRdfToNonRDFSource() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockAuditService, mockIoService, mockBinaryService, agentService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testUnsupportedType() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        final BadRequestException ex = assertThrows(BadRequestException.class, () ->
                putHandler.setResource(mockResource));
        assertTrue(ex.getResponse().getLinks().stream().anyMatch(link ->
                link.getUri().toString().equals(Trellis.UnsupportedInteractionModel.getIRIString()) &&
                link.getRel().equals(LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testError() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResourceService.replace(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")), any(Session.class),
                    any(IRI.class), any(Dataset.class), any(), any())).thenReturn(completedFuture(false));
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        assertThrows(BadRequestException.class, () -> putHandler.setResource(mockResource));
    }

    @Test
    public void testException() throws Exception {
        when(mockFuture.get()).thenThrow(new InterruptedException("Expected"));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResourceService.replace(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")), any(Session.class),
                    any(IRI.class), any(Dataset.class), any(), any())).thenReturn(mockFuture);
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService, mockAuditService,
                        mockIoService, mockBinaryService, agentService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(INTERNAL_SERVER_ERROR, res.getStatusInfo());
    }

    private static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    private static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, "type");
    }
}
