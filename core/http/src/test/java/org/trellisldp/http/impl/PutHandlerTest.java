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

import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.runAsync;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
class PutHandlerTest extends BaseTestHandler {

    private final BinaryMetadata testBinary = BinaryMetadata.builder(rdf.createIRI("file:///binary.txt"))
        .mimeType("text/plain").build();

    @Test
    void testPutConflict() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.DirectContainer.getIRIString()).rel("type").build());
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final PutHandler handler = buildPutHandler("/simpleTriple.ttl", "http://example.com/");

        final Response res = assertThrows(WebApplicationException.class, () ->
                handler.setResource(handler.initialize(mockParent, mockResource)).toCompletableFuture().join(),
                "No exception when trying to invalidly change IXN models!").getResponse();
        assertEquals(CONFLICT, res.getStatusInfo(), "Incorrect response code!");
    }

    @Test
    void testBadAudit() {
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService() {});
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(asyncException());

        final PutHandler handler = buildPutHandler("/simpleTriple.ttl", null);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.setResource(handler.initialize(mockParent, mockResource))),
                "No exception when the audit backend completes exceptionally!");
    }

    @Test
    void testPutLdpResourceDefaultType() {
        when(mockTrellisRequest.getPath()).thenReturn("resource");
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final PutHandler handler = buildPutHandler("/simpleTriple.ttl", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response type");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));

        verify(mockBinaryService, never()).setContent(any(BinaryMetadata.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + "resource"));
    }

    @Test
    void testPutLdpResourceContainer() {
        when(mockTrellisRequest.getPath()).thenReturn("resource");
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final PutHandler handler = buildPutHandler("/simpleTriple.ttl", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.Container));

        verify(mockBinaryService, never()).setContent(any(BinaryMetadata.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + "resource"));
    }

    @Test
    void testPutLdpResourceContainerContained() throws IOException {
        when(mockTrellisRequest.getPath()).thenReturn("resource");
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final PutHandler handler = new PutHandler(mockTrellisRequest,
                getClass().getResource("/simpleTriple.ttl").openStream(), mockBundler, false, false, null);
        final Response res = handler.setResource(handler.initialize(mockParent, MISSING_RESOURCE))
            .toCompletableFuture().join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.Container));

        verify(mockBinaryService, never()).setContent(any(BinaryMetadata.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + "resource"));
    }

    @Test
    void testPutLdpBinaryResourceWithLdprLink() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final PutHandler handler = buildPutHandler("/simpleData.txt", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertAll("Check Binary PUT interactions", checkBinaryPut(res));
    }

    @Test
    void testPutLdpBinaryResource() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());

        final PutHandler handler = buildPutHandler("/simpleData.txt", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertAll("Check Binary PUT interactions", checkBinaryPut(res));
    }

    @Test
    void testPutLdpBinaryResourceNoContentType() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());
        when(mockTrellisRequest.getContentType()).thenReturn(null);

        final PutHandler handler = buildPutHandler("/simpleData.txt", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertAll("Check Binary PUT interactions w/o contentType", checkBinaryPut(res));
    }

    @Test
    void testPutLdpNRDescription() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.RDFSource.getIRIString()).rel("type").build());

        final PutHandler handler = buildPutHandler("/simpleLiteral.ttl", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertAll("Check RDF PUT interactions", checkRdfPut(res));
    }

    @Test
    void testPutLdpNRDescription2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final PutHandler handler = buildPutHandler("/simpleLiteral.ttl", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertAll("Check RDF PUT interactions", checkRdfPut(res));
    }

    @Test
    void testPutLdpResourceEmpty() {
        final PutHandler handler = buildPutHandler("/emptyData.txt", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertAll("Check RDF PUT interactions", checkRdfPut(res));
    }

    @Test
    void testRdfToNonRDFSource() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());

        final PutHandler handler = buildPutHandler("/simpleTriple.ttl", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertAll("Check RDF PUT interactions", checkRdfPut(res));
    }

    @Test
    void testUnsupportedType() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final PutHandler handler = buildPutHandler("/simpleTriple.ttl", null);
        final Response res = assertThrows(BadRequestException.class, () ->
                handler.setResource(handler.initialize(mockParent, mockResource)).toCompletableFuture().join(),
                "No exception when the interaction model isn't supported!").getResponse();

        assertEquals(BAD_REQUEST, res.getStatusInfo(), "Incorrect response code!");
        assertTrue(res.getLinks().stream().anyMatch(link ->
                link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                link.getRel().equals(LDP.constrainedBy.getIRIString())), "Missing constraint header!");
    }

    @Test
    void testError() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getPath()).thenReturn("resource");
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());
        when(mockResourceService.replace(any(Metadata.class), any(Dataset.class))).thenReturn(asyncException());

        final PutHandler handler = buildPutHandler("/simpleData.txt", null);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.setResource(handler.initialize(mockParent, mockResource))),
                "No exception when there's a problem with the backend!");
    }

    @Test
    void testMementoError() {
        final MementoService mockMementoService = mock(MementoService.class);
        when(mockBundler.getMementoService()).thenReturn(mockMementoService);
        doCallRealMethod().when(mockMementoService).put(any(ResourceService.class), any(IRI.class));
        when(mockMementoService.put(any())).thenAnswer(inv -> runAsync(() -> {
            throw new RuntimeTrellisException("Expected error");
        }));

        final PutHandler handler = buildPutHandler("/simpleData.txt", null);
        final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
            .thenCompose(handler::updateMemento).toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
    }

    @Test
    void testBinaryError() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel("type").build());
        when(mockBinaryService.setContent(any(BinaryMetadata.class), any(InputStream.class)))
            .thenReturn(asyncException());

        try (final InputStream entity = getClass().getResource("/simpleData.txt").openStream()) {
            final PutHandler handler = new PutHandler(mockTrellisRequest, entity, mockBundler, false, true, null);

            assertThrows(CompletionException.class,
                            () -> unwrapAsyncError(handler.setResource(handler.initialize(mockParent, mockResource))),
                            "No exception when there's a problem with the backend binary service!");
        }
    }

    private PutHandler buildPutHandler(final String resourceName, final String baseUrl) {
        try {
            return new PutHandler(mockTrellisRequest, getClass().getResource(resourceName).openStream(), mockBundler,
                            false, true, baseUrl);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Stream<Executable> checkRdfPut(final Response res) {
        return Stream.of(
                () -> assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource)),
                () -> verify(mockBinaryService, never().description("Binary service shouldn't have been called!"))
                             .setContent(any(BinaryMetadata.class), any(InputStream.class)),
                () -> verify(mockIoService, description("IOService should have been called with an RDF resource"))
                             .read(any(InputStream.class), any(RDFSyntax.class), anyString()));
    }

    private Stream<Executable> checkBinaryPut(final Response res) {
        return Stream.of(
                () -> assertAll("Check LDP type Link headers", checkLdpType(res, LDP.NonRDFSource)),
                () -> verify(mockBinaryService, description("BinaryService should have been called to set content!"))
                            .setContent(any(BinaryMetadata.class), any(InputStream.class)),
                () -> verify(mockIoService, never().description("IOService shouldn't have been called with a Binary!"))
                            .read(any(InputStream.class), any(RDFSyntax.class), anyString()));
    }
}
