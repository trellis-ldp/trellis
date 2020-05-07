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
package org.trellisldp.http.impl;

import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.runAsync;
import static javax.ws.rs.core.Link.TYPE;
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
import org.trellisldp.http.core.HttpConstants;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
class PutHandlerTest extends BaseTestHandler {

    private static final String CHECK_RDF_PUT_INTERACTION = "Check RDF PUT interactions";
    private static final String CHECK_BINARY_PUT_INTERACTION = "Check binary PUT interactions";

    private final BinaryMetadata testBinary = BinaryMetadata.builder(rdf.createIRI("file:///binary.txt"))
        .mimeType("text/plain").build();

    @Test
    void testPutConflict() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE).build());
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, "http://example.com/");

        try (final Response res = assertThrows(WebApplicationException.class, () ->
                handler.setResource(handler.initialize(mockParent, mockResource)).toCompletableFuture().join(),
                "No exception when trying to invalidly change IXN models!").getResponse()) {
            assertEquals(CONFLICT, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutNoConflict() {
        try {
            System.setProperty(HttpConstants.CONFIG_HTTP_LDP_MODEL_MODIFICATIONS, "false");
            when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
            when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.DirectContainer.getIRIString()).rel(TYPE)
                    .build());
            when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);

            final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, "http://example.com/");

            // Update an existing resource
            try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                    .toCompletableFuture().join().build()) {
                assertEquals(NO_CONTENT, res.getStatusInfo());
            }
        } finally {
            System.clearProperty(HttpConstants.CONFIG_HTTP_LDP_MODEL_MODIFICATIONS);
        }
    }

    @Test
    void testBadAudit() {
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService() {});
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build());
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(asyncException());

        final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, null);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.setResource(handler.initialize(mockParent, mockResource))),
                "No exception when the audit backend completes exceptionally!");
    }

    @Test
    void testNoVersioning() {
        final MementoService mockMementoService = mock(MementoService.class);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockBundler.getMementoService()).thenReturn(mockMementoService);

        try {
            System.setProperty(HttpConstants.CONFIG_HTTP_VERSIONING, "false");
            System.setProperty(HttpConstants.CONFIG_HTTP_LDP_MODEL_MODIFICATIONS, "false");
            final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, null, false);
            try (final Response res = handler.setResource(handler.initialize(mockParent, MISSING_RESOURCE))
                    .thenCompose(handler::updateMemento).toCompletableFuture().join().build()) {
                assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);

                verify(mockMementoService, never()).put(any(ResourceService.class), any(IRI.class));
            }

        } finally {
            System.clearProperty(HttpConstants.CONFIG_HTTP_VERSIONING);
            System.clearProperty(HttpConstants.CONFIG_HTTP_LDP_MODEL_MODIFICATIONS);
        }
    }

    @Test
    void testPutLdpResourceDefaultType() {
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel(TYPE).build());

        final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, null, false);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource));

            verify(mockBinaryService, never()).setContent(any(BinaryMetadata.class), any(InputStream.class));
            verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + RESOURCE_NAME));
        }
    }

    @Test
    void testPutLdpResourceContainer() {
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel(TYPE).build());
        when(mockResource.getContainer()).thenReturn(empty());

        final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.Container));

            verify(mockBinaryService, never()).setContent(any(BinaryMetadata.class), any(InputStream.class));
            verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + RESOURCE_NAME + "/"));
        }
    }

    @Test
    void testPutLdpResourceContainerContained() throws IOException {
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel(TYPE).build());

        final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, null, false);
        try (final Response res = handler.setResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.Container));

            verify(mockBinaryService, never()).setContent(any(BinaryMetadata.class), any(InputStream.class));
            verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + RESOURCE_NAME + "/"));
        }
    }

    @Test
    void testPutLdpResourceContainerUncontained() throws IOException {
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel(TYPE).build());

        final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.Container));

            verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + RESOURCE_NAME + "/"));
            verify(mockBinaryService, never()).setContent(any(BinaryMetadata.class), any(InputStream.class));
        }
    }

    @Test
    void testPutLdpBinaryResourceWithLdprLink() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel(TYPE).build());

        final PutHandler handler = buildPutHandler(RESOURCE_SIMPLE, null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_BINARY_PUT_INTERACTION, checkBinaryPut(res));
        }
    }

    @Test
    void testPutLdpBinaryResource() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build());

        final PutHandler handler = buildPutHandler(RESOURCE_SIMPLE, null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_BINARY_PUT_INTERACTION, checkBinaryPut(res));
        }
    }

    @Test
    void testPutLdpBinaryResourceNoContentType() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build());
        when(mockTrellisRequest.getContentType()).thenReturn(null);

        final PutHandler handler = buildPutHandler(RESOURCE_SIMPLE, null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll("Check Binary PUT interactions w/o contentType", checkBinaryPut(res));
        }
    }

    @Test
    void testPutLdpNRDescription() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.RDFSource.getIRIString()).rel(TYPE).build());

        final PutHandler handler = buildPutHandler("/simpleLiteral.ttl", null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_RDF_PUT_INTERACTION, checkRdfPut(res));
        }
    }

    @Test
    void testPutLdpNRDescription2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);

        final PutHandler handler = buildPutHandler("/simpleLiteral.ttl", null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_RDF_PUT_INTERACTION, checkRdfPut(res));
        }
    }

    @Test
    void testPutLdpResourceEmpty() {
        final PutHandler handler = buildPutHandler(RESOURCE_EMPTY, null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_RDF_PUT_INTERACTION, checkRdfPut(res));
        }
    }

    @Test
    void testRdfToNonRDFSource() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build());

        final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_RDF_PUT_INTERACTION, checkRdfPut(res));
        }
    }

    @Test
    void testUnsupportedType() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel(TYPE).build());

        final PutHandler handler = buildPutHandler(RESOURCE_TURTLE, null);
        try (final Response res = assertThrows(BadRequestException.class, () ->
                handler.setResource(handler.initialize(mockParent, mockResource)).toCompletableFuture().join(),
                "No exception when the interaction model isn't supported!").getResponse()) {
            assertEquals(BAD_REQUEST, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertTrue(res.getLinks().stream().anyMatch(link ->
                    link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                    link.getRel().equals(LDP.constrainedBy.getIRIString())), "Missing constraint header!");
        }
    }

    @Test
    void testError() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build());
        when(mockResourceService.replace(any(Metadata.class), any(Dataset.class))).thenReturn(asyncException());

        final PutHandler handler = buildPutHandler(RESOURCE_SIMPLE, null);

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

        final PutHandler handler = buildPutHandler(RESOURCE_SIMPLE, null);
        try (final Response res = handler.setResource(handler.initialize(mockParent, mockResource))
                .thenCompose(handler::updateMemento).toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testBinaryError() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build());
        when(mockBinaryService.setContent(any(BinaryMetadata.class), any(InputStream.class)))
            .thenReturn(asyncException());

        try (final InputStream entity = getClass().getResource(RESOURCE_SIMPLE).openStream()) {
            final PutHandler handler = new PutHandler(mockTrellisRequest, entity, mockBundler, extensions,
                    false, true, null);
            assertThrows(CompletionException.class,
                            () -> unwrapAsyncError(handler.setResource(handler.initialize(mockParent, mockResource))),
                            "No exception when there's a problem with the backend binary service!");
        }
    }

    private PutHandler buildPutHandler(final String resourceName, final String baseUrl) {
        return buildPutHandler(resourceName, baseUrl, true);
    }

    private PutHandler buildPutHandler(final String resourceName, final String baseUrl, final boolean uncontained) {
        try {
            return new PutHandler(mockTrellisRequest, getClass().getResource(resourceName).openStream(), mockBundler,
                            extensions, false, uncontained, baseUrl);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Stream<Executable> checkRdfPut(final Response res) {
        return Stream.of(
                () -> assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource)),
                () -> verify(mockBinaryService, never().description("Binary service shouldn't have been called!"))
                             .setContent(any(BinaryMetadata.class), any(InputStream.class)),
                () -> verify(mockIoService, description("IOService should have been called with an RDF resource"))
                             .read(any(InputStream.class), any(RDFSyntax.class), anyString()));
    }

    private Stream<Executable> checkBinaryPut(final Response res) {
        return Stream.of(
                () -> assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.NonRDFSource)),
                () -> verify(mockBinaryService, description("BinaryService should have been called to set content!"))
                            .setContent(any(BinaryMetadata.class), any(InputStream.class)),
                () -> verify(mockIoService, never().description("IOService shouldn't have been called with a Binary!"))
                            .read(any(InputStream.class), any(RDFSyntax.class), anyString()));
    }
}
