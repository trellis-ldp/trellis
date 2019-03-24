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
import static java.util.Collections.singletonList;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.core.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.util.concurrent.CompletionException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDFS;

/**
 * @author acoburn
 */
public class PatchHandlerTest extends BaseTestHandler {

    private static final String insert = "INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}";

    @Test
    public void testPatchNoSparql() {
        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, null, mockBundler, null, null);
        final Response res = assertThrows(BadRequestException.class, () ->
                patchHandler.initialize(mockParent, mockResource),
                "No exception thrown with a null input!").getResponse();
        assertEquals(BAD_REQUEST, res.getStatusInfo(), "Incorrect response code!");
    }

    @Test
    public void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService() {});
        // will never store audit
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(asyncException());

        final PatchHandler handler = new PatchHandler(mockTrellisRequest, "", mockBundler, null, null);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.updateResource(handler.initialize(mockParent, mockResource))),
                "No exception thrown when there is an error in the audit backend!");
    }

    @Test
    public void testPatchLdprs() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, null, baseUrl);
        final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
    }

    @Test
    public void testEntity() {
        final Quad quad = rdf.createQuad(PreferUserManaged, identifier, RDFS.label, rdf.createLiteral("A label"));

        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockResource.stream(eq(PreferUserManaged))).thenAnswer(x -> of(quad));
        when(mockTrellisRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, null, null);
        final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");

        verify(mockIoService).update(any(Graph.class), eq(insert), eq(SPARQL_UPDATE), eq(identifier.getIRIString()));
        verify(mockResourceService).replace(any(Metadata.class), any(Dataset.class));
    }

    @Test
    public void testPreferRepresentation() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn("resource");
        when(mockTrellisRequest.getPrefer()).thenReturn(Prefer.valueOf("return=representation"));

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, null, null);
        final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(OK, res.getStatusInfo(), "Incorrect response code!");
        assertEquals("return=representation", res.getHeaderString(PREFERENCE_APPLIED),
                "Incorrect Preference-Applied header!");
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incorrect content-type header!");
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incorrect content-type header!");
        assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));
    }

    @Test
    public void testPreferHTMLRepresentation() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn("resource");
        when(mockTrellisRequest.getPrefer()).thenReturn(Prefer.valueOf("return=representation"));
        when(mockTrellisRequest.getAcceptableMediaTypes())
            .thenReturn(singletonList(MediaType.valueOf(RDFA.mediaType())));

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, null, null);
        final Response res = patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
            .toCompletableFuture().join().build();

        assertEquals(OK, res.getStatusInfo(), "Incorrect response code!");
        assertEquals("return=representation", res.getHeaderString(PREFERENCE_APPLIED),
                "Incorrect Preference-Applied header!");
        assertTrue(TEXT_HTML_TYPE.isCompatible(res.getMediaType()), "Incorrect content-type header!");
        assertTrue(res.getMediaType().isCompatible(TEXT_HTML_TYPE), "Incorrect content-type header!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
        assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
        assertAll("Check LDP type link headers", checkLdpType(res, LDP.RDFSource));
    }

    @Test
    public void testError() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn("resource");
        when(mockResourceService.replace(any(Metadata.class), any(Dataset.class))).thenReturn(asyncException());

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, null, null);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))),
                "No exception thrown when the backend triggers an exception!");
    }

    @Test
    public void testNoLdpRsSupport() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, null, null);
        final Response res = assertThrows(BadRequestException.class, () ->
                patchHandler.initialize(mockParent, mockResource),
                "No exception for an unsupported IXN model!").getResponse();
        assertEquals(BAD_REQUEST, res.getStatusInfo(), "Incorrect response code!");
        assertTrue(res.getLinks().stream().anyMatch(link ->
                link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                link.getRel().equals(LDP.constrainedBy.getIRIString())), "Missing Link header with constraint!");
    }

    @Test
    public void testError2() {
        when(mockTrellisRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockTrellisRequest.getPath()).thenReturn("resource");
        doThrow(RuntimeTrellisException.class).when(mockIoService)
            .update(any(Graph.class), eq(insert), eq(SPARQL_UPDATE), eq(identifier.getIRIString()));

        final PatchHandler patchHandler = new PatchHandler(mockTrellisRequest, insert, mockBundler, null, baseUrl);
        final Response res = assertThrows(BadRequestException.class, () ->
                patchHandler.updateResource(patchHandler.initialize(mockParent, mockResource))
                .toCompletableFuture().join(), "No exception when the update triggers an error!").getResponse();
        assertEquals(BAD_REQUEST, res.getStatusInfo(), "Incorrect response type!");
    }
}
