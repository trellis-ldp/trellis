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

import static java.net.URI.create;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
class PostHandlerTest extends BaseTestHandler {

    private static final String ERR_LOCATION = "Incorrect Location header!";
    private static final String NEW_RESOURCE = "newresource";

    @Test
    void testPostLdprs() throws IOException {
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.RDFSource.getIRIString()).rel(TYPE).build());

        final PostHandler handler = buildPostHandler(RESOURCE_EMPTY, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostLdpc() throws IOException {
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel(TYPE).build());

        final PostHandler handler = buildPostHandler(RESOURCE_EMPTY, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE + "/"), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.Container));
        }
    }

    @Test
    void testBadAudit() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build());
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService() {});
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(asyncException());

        final PostHandler handler = buildPostHandler(RESOURCE_TURTLE, null, null);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))),
                "No exception when the backend audit service throws an error!");
    }

    @Test
    void testDefaultType1() throws IOException {
        final PostHandler handler = buildPostHandler(RESOURCE_EMPTY, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource));
        }
    }

    @Test
    void testDefaultType2() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);

        final PostHandler handler = buildPostHandler(RESOURCE_SIMPLE, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, DELETED_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.NonRDFSource));
        }
    }

    @Test
    void testDefaultType3() throws IOException {
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel(TYPE).build());

        final PostHandler handler = buildPostHandler(RESOURCE_EMPTY, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource));
        }
    }

    @Test
    void testDefaultType4() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel(TYPE).build());

        final PostHandler handler = buildPostHandler(RESOURCE_SIMPLE, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.NonRDFSource));
        }
    }

    @Test
    void testDefaultType5() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = buildPostHandler(RESOURCE_EMPTY, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource));
        }
    }

    @Test
    void testUnsupportedType() throws IOException {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel(TYPE).build());

        final PostHandler handler = buildPostHandler(RESOURCE_EMPTY, NEW_RESOURCE, null);
        try (final Response res = assertThrows(BadRequestException.class, () ->
                handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).toCompletableFuture().join(),
                "No exception thrown when the IXN model isn't supported!").getResponse()) {
            assertEquals(BAD_REQUEST, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertTrue(res.getLinks().stream().anyMatch(link ->
                    link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                    link.getRel().equals(LDP.constrainedBy.getIRIString())), "Missing constraint Link header!");
        }
    }

    @Test
    void testRdfEntity() throws IOException {
        final Triple triple = rdf.createTriple(rdf.createIRI(baseUrl + NEW_RESOURCE), DC.title,
                        rdf.createLiteral("A title"));

        when(mockIoService.supportedWriteSyntaxes()).thenReturn(asList(TURTLE, JSONLD, NTRIPLES));
        when(mockIoService.read(any(), eq(TURTLE), any())).thenAnswer(x -> Stream.of(triple));
        when(mockTrellisRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = buildPostHandler(RESOURCE_TURTLE, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource));

            verify(mockBinaryService, never()).setContent(any(BinaryMetadata.class), any(InputStream.class));
            verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + NEW_RESOURCE));
            verify(mockResourceService).create(any(Metadata.class), any(Dataset.class));
        }
    }

    @Test
    void testBinaryEntity() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_PLAIN);

        final PostHandler handler = buildPostHandler(RESOURCE_SIMPLE, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.NonRDFSource));
            assertAll("Check Binary response", checkBinaryEntityResponse(TEXT_PLAIN));
        }
    }

    @Test
    void testBinaryEntityNoContentType() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn(null);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.NonRDFSource.getIRIString()).rel(TYPE).build());

        final PostHandler handler = buildPostHandler(RESOURCE_SIMPLE, NEW_RESOURCE, null);
        try (final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(create(baseUrl + NEW_RESOURCE), res.getLocation(), ERR_LOCATION);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.NonRDFSource));
            assertAll("Check Binary response", checkBinaryEntityResponse(APPLICATION_OCTET_STREAM));
        }
    }

    @Test
    void testRdfIOError() throws IOException {
        final Triple triple = rdf.createTriple(rdf.createIRI(baseUrl + NEW_RESOURCE), DC.title,
                        rdf.createLiteral("A title"));

        when(mockIoService.supportedWriteSyntaxes()).thenReturn(asList(TURTLE, JSONLD, NTRIPLES));
        when(mockIoService.read(any(), eq(TURTLE), any())).thenAnswer(x -> Stream.of(triple));
        when(mockTrellisRequest.getContentType()).thenReturn("text/turtle");

        final InputStream ioStream = getClass().getResource(RESOURCE_TURTLE).openStream();
        ioStream.close();
        final PostHandler handler = new PostHandler(mockTrellisRequest, root, NEW_RESOURCE, ioStream, mockBundler,
                extensions, null);
        assertThrows(BadRequestException.class, () ->
                unwrapAsyncError(handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))));
    }


    @Test
    void testError() throws IOException {
        when(mockResourceService.create(any(Metadata.class), any(Dataset.class))).thenReturn(asyncException());
        when(mockTrellisRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = buildPostHandler(RESOURCE_EMPTY, NEW_RESOURCE, baseUrl);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))),
                "No exception thrown when the backend errors!");
    }

    private PostHandler buildPostHandler(final String resourceName, final String id, final String baseUrl)
                    throws IOException {
        return new PostHandler(mockTrellisRequest, root, id, getClass().getResource(resourceName).openStream(),
                mockBundler, extensions, baseUrl);
    }

    private Stream<Executable> checkBinaryEntityResponse(final String contentType) {
        return Stream.of(
                () -> verify(mockResourceService, description("ResourceService::create not called!"))
                            .create(any(Metadata.class), any(Dataset.class)),
                () -> verify(mockIoService, never().description("entity shouldn't be read!")).read(any(), any(), any()),
                () -> verify(mockBinaryService, description("content not set on binary service!"))
                            .setContent(metadataArgument.capture(), any(InputStream.class)),
                () -> assertEquals(of(contentType), metadataArgument.getValue().getMimeType(), "Invalid content-type"),
                () -> assertTrue(metadataArgument.getValue().getIdentifier().getIRIString().startsWith("file:///"),
                                 "Invalid binary ID!"));
    }
}
