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

import static java.time.Instant.ofEpochSecond;
import static java.util.Date.from;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CONFLICT;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;

import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.function.Predicate;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.RDFTerm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class PutHandlerTest {

    private final static Instant time = ofEpochSecond(1496262729);
    private final static Instant binaryTime = ofEpochSecond(1496262750);

    private final static String baseUrl = "http://localhost:8080/repo/";
    private final static RDF rdf = getInstance();
    private final Binary testBinary = new Binary(rdf.createIRI("file:binary.txt"), binaryTime, "text/plain", null);

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

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getBinary()).thenReturn(empty());
        when(mockResource.getModified()).thenReturn(time);
        when(mockBinaryService.getIdentifierSupplier()).thenReturn(() -> "file:" + randomUUID());
        when(mockResourceService.put(any(IRI.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));

        when(mockLdpRequest.getRequest()).thenReturn(mockRequest);
        when(mockLdpRequest.getPath()).thenReturn("resource");
        when(mockLdpRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockLdpRequest.getSession()).thenReturn(new HttpSession());
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
    public void testPutConflict() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.DirectContainer.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(CONFLICT, res.getStatusInfo());
    }

    @Test
    public void testPutLdpResourceDefaultType() {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), eq(baseUrl + "resource"), eq(TURTLE));
    }

    @Test
    public void testPutLdpResourceContainer() {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), eq(baseUrl + "resource"), eq(TURTLE));
    }

    @Test
    public void testPutError() {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile() + ".non-existent-file");
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        assertThrows(WebApplicationException.class, () -> putHandler.setResource(mockResource));
    }

    @Test
    public void testPutLdpBinaryResourceWithLdprLink() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_PLAIN);

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService).setContent(any(IRI.class), any(InputStream.class), any());
        verify(mockIoService, never()).read(any(InputStream.class), anyString(), any(RDFSyntax.class));
    }

    @Test
    public void testPutLdpBinaryResource() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService).setContent(any(IRI.class), any(InputStream.class), any());
        verify(mockIoService, never()).read(any(InputStream.class), anyString(), any(RDFSyntax.class));
    }

    @Test
    public void testPutLdpNRDescription() {
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.RDFSource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleLiteral.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), anyString(), any(RDFSyntax.class));
    }

    @Test
    public void testPutLdpNRDescription2() {
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final File entity = new File(getClass().getResource("/simpleLiteral.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), anyString(), any(RDFSyntax.class));
    }

    @Test
    public void testPutLdpResourceEmpty() {
        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService, never()).read(any(InputStream.class), anyString(), any(RDFSyntax.class));
    }

    @Test
    public void testCache() {
        when(mockRequest.evaluatePreconditions(eq(from(binaryTime)), any(EntityTag.class)))
                .thenReturn(status(PRECONDITION_FAILED));
        when(mockResource.getBinary()).thenReturn(of(testBinary));

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        assertThrows(WebApplicationException.class, () -> putHandler.setResource(mockResource));
    }

    @Test
    public void testRdfToNonRDFSource() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

        final Response res = putHandler.setResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
    }

    @Test
    public void testError() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResourceService.put(eq(rdf.createIRI(TRELLIS_PREFIX + "resource")), any(IRI.class),
                    any(Dataset.class))).thenReturn(completedFuture(false));
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PutHandler putHandler = new PutHandler(mockLdpRequest, entity, mockResourceService,
                mockIoService, mockBinaryService, null);

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
