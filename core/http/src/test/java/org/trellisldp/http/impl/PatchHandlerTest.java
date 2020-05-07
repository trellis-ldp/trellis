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
import static java.util.Collections.singletonList;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.core.HttpConstants.ACL;
import static org.trellisldp.http.core.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.util.Optional;
import java.util.concurrent.CompletionException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.trellisldp.api.Event;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDFS;

/**
 * @author acoburn
 */
class PatchHandlerTest extends BaseTestHandler {

    private static final String ERR_CONTENT_TYPE = "Incorrect content-type header!";
    private static final String RETURN_REPRESENTATION = "return=representation";

    private static final String insert = "INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}";

    @Test
    void testPatchNoSparql() {
        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, null, mockBundler, extensions, false,
                null, null);
        try (final Response res = assertThrows(BadRequestException.class, () ->
                patchHandler.initialize(mockParent, mockResource),
                "No exception thrown with a null input!").getResponse()) {
            assertEquals(BAD_REQUEST, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchMissing() {
        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, null, mockBundler, extensions, false,
                null, null);
        try (final Response res = assertThrows(NotFoundException.class, () ->
                    patchHandler.initialize(mockParent, MISSING_RESOURCE)).getResponse()) {
            assertEquals(NOT_FOUND, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchGone() {
        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, null, mockBundler, extensions, false,
                null, null);
        try (final Response res = assertThrows(ClientErrorException.class, () ->
                    patchHandler.initialize(mockParent, DELETED_RESOURCE)).getResponse()) {
            assertEquals(GONE, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchCreate() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService() {});

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions, true,
                null, baseUrl);
        try (final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, MISSING_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchCreateDeleted() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService() {});

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions, true,
                null, baseUrl);
        try (final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, DELETED_RESOURCE))
                .toCompletableFuture().join().build()) {
            assertEquals(CREATED, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build());
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService() {});
        // will never store audit
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(asyncException());

        final PatchHandler handler = new PatchHandler(mockTrellisRequest, "", mockBundler, extensions, false,
                null, null);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.updateResource(handler.initialize(mockParent, mockResource))),
                "No exception thrown when there is an error in the audit backend!");
    }

    @Test
    void testPatchLdprs() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions,
                false, null, baseUrl);
        try (final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testEntity() {
        final Quad quad = rdf.createQuad(PreferUserManaged, identifier, RDFS.label, rdf.createLiteral("A label"));

        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockResource.stream(eq(PreferUserManaged))).thenAnswer(x -> of(quad));
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getBaseUrl()).thenReturn("http://localhost/");

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions,
                false, null, null);
        try (final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            verify(mockIoService).update(any(Graph.class), eq(insert), eq(SPARQL_UPDATE),
                    eq("http://localhost/resource"));
            verify(mockResourceService).replace(any(Metadata.class), any(Dataset.class));
        }
    }

    @Test
    void testAcl() {
        when(mockTrellisRequest.getExt()).thenReturn(ACL);
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getBaseUrl()).thenReturn("http://localhost/");

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions,
                false, null, null);
        try (final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);

            final ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
            verify(mockEventService).emit(event.capture());
            assertEquals(Optional.of("http://localhost/resource?ext=acl"),
                    event.getValue().getObject().map(IRI::getIRIString));
            verify(mockIoService).update(any(Graph.class), eq(insert), eq(SPARQL_UPDATE),
                    eq("http://localhost/resource?ext=acl"));
            verify(mockResourceService).replace(any(Metadata.class), any(Dataset.class));
        }
    }

    @Test
    void testRootResource() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn("");
        when(mockTrellisRequest.getBaseUrl()).thenReturn("http://localhost/");

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions,
                false, null, null);
        try (final Response res = patchHandler.updateResource(patchHandler.initialize(MISSING_RESOURCE, mockParent))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);

            final ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
            verify(mockEventService).emit(event.capture());
            assertEquals(Optional.of("http://localhost/"),
                    event.getValue().getObject().map(IRI::getIRIString));
            verify(mockIoService).update(any(Graph.class), eq(insert), eq(SPARQL_UPDATE),
                    eq("http://localhost/"));
            verify(mockResourceService).replace(any(Metadata.class), any(Dataset.class));
        }
    }

    @Test
    void testPreferRepresentation() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getPrefer()).thenReturn(Prefer.valueOf(RETURN_REPRESENTATION));

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions,
                false, null, null);
        try (final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(RETURN_REPRESENTATION, res.getHeaderString(PREFERENCE_APPLIED),
                    "Incorrect Preference-Applied header!");
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE);
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE);
            assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
            assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
            assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));
        }
    }

    @Test
    void testPreferHTMLRepresentation() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockTrellisRequest.getPrefer()).thenReturn(Prefer.valueOf(RETURN_REPRESENTATION));
        when(mockTrellisRequest.getAcceptableMediaTypes())
            .thenReturn(singletonList(MediaType.valueOf(RDFA.mediaType())));

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions,
                false, null, null);
        try (final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(RETURN_REPRESENTATION, res.getHeaderString(PREFERENCE_APPLIED),
                    "Incorrect Preference-Applied header!");
            assertTrue(TEXT_HTML_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE);
            assertTrue(res.getMediaType().isCompatible(TEXT_HTML_TYPE), ERR_CONTENT_TYPE);
            assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
            assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
            assertAll("Check LDP type link headers", checkLdpType(res, LDP.RDFSource));
        }
    }

    @Test
    void testError() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        when(mockResourceService.replace(any(Metadata.class), any(Dataset.class))).thenReturn(asyncException());

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions,
                false, null, null);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))),
                "No exception thrown when the backend triggers an exception!");
    }

    @Test
    void testNoLdpRsSupport() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions,
                false, null, null);
        try (final Response res = assertThrows(BadRequestException.class, () ->
                patchHandler.initialize(mockParent, mockResource),
                "No exception for an unsupported IXN model!").getResponse()) {
            assertEquals(BAD_REQUEST, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertTrue(res.getLinks().stream().anyMatch(link ->
                    link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                    link.getRel().equals(LDP.constrainedBy.getIRIString())), "Missing Link header with constraint!");
        }
    }

    @Test
    void testError2() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn(RESOURCE_NAME);
        doThrow(RuntimeTrellisException.class).when(mockIoService)
            .update(any(Graph.class), eq(insert), eq(SPARQL_UPDATE), eq(baseUrl + RESOURCE_NAME));

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, extensions,
                false, null, baseUrl);
        try (final Response res = assertThrows(BadRequestException.class, () ->
                patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
                .toCompletableFuture().join(), "No exception when the update triggers an error!").getResponse()) {
            assertEquals(BAD_REQUEST, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }
}
