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
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.domain.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.util.Date;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.Session;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDFS;

/**
 * @author acoburn
 */
public class PatchHandlerTest extends HandlerBaseTest {

    private static final String insert = "INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}";

    @Test
    public void testPatchNoSparql() {
        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, null, mockBundler, null);
        assertEquals(BAD_REQUEST, patchHandler.initialize(mockResource).build().getStatusInfo());
    }

    @Test
    public void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService() {});
        // will never store audit
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(false));

        final PatchHandler handler = new PatchHandler(mockLdpRequest, "", mockBundler, null);

        assertEquals(INTERNAL_SERVER_ERROR, handler.updateResource(handler.initialize(mockResource)).join().build()
                .getStatusInfo());
    }

    @Test
    public void testPatchLdprs() {
        when(mockLdpRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockLdpRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockBundler, baseUrl);
        final Response res = patchHandler.updateResource(patchHandler.initialize(mockResource)).join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo());
    }

    @Test
    public void testEntity() {
        final Triple triple = rdf.createTriple(identifier, RDFS.label, rdf.createLiteral("A label"));

        when(mockLdpRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockResource.stream(eq(PreferUserManaged))).thenAnswer(x -> of(triple));
        when(mockLdpRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockBundler, null);
        final Response res = patchHandler.updateResource(patchHandler.initialize(mockResource)).join().build();

        assertEquals(NO_CONTENT, res.getStatusInfo());

        verify(mockIoService).update(any(Graph.class), eq(insert), eq(SPARQL_UPDATE), eq(identifier.getIRIString()));
        verify(mockResourceService).replace(eq(identifier), any(Session.class), eq(LDP.RDFSource), any(Dataset.class),
                        any(), any());
    }

    @Test
    public void testPreferRepresentation() {
        when(mockLdpRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockLdpRequest.getPath()).thenReturn("resource");
        when(mockLdpRequest.getPrefer()).thenReturn(Prefer.valueOf("return=representation"));

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockBundler, null);
        final Response res = patchHandler.updateResource(patchHandler.initialize(mockResource)).join().build();

        assertEquals(OK, res.getStatusInfo());
        assertEquals("return=representation", res.getHeaderString(PREFERENCE_APPLIED));
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertType(res, LDP.RDFSource);
    }

    @Test
    public void testPreferHTMLRepresentation() {
        when(mockLdpRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockLdpRequest.getPath()).thenReturn("resource");
        when(mockLdpRequest.getPrefer()).thenReturn(Prefer.valueOf("return=representation"));
        when(mockLdpRequest.getHeaders().getAcceptableMediaTypes())
            .thenReturn(singletonList(MediaType.valueOf(RDFA.mediaType())));

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockBundler, null);
        final Response res = patchHandler.updateResource(patchHandler.initialize(mockResource)).join().build();

        assertEquals(OK, res.getStatusInfo());
        assertEquals("return=representation", res.getHeaderString(PREFERENCE_APPLIED));
        assertTrue(TEXT_HTML_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_HTML_TYPE));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertType(res, LDP.RDFSource);
    }

    @Test
    public void testConflict() {
        when(mockLdpRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockRequest.evaluatePreconditions(any(Date.class), any(EntityTag.class))).thenReturn(status(CONFLICT));
        when(mockLdpRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockBundler, null);

        assertEquals(CONFLICT, patchHandler.initialize(mockResource).build().getStatusInfo());
    }

    @Test
    public void testError() {
        when(mockLdpRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockLdpRequest.getPath()).thenReturn("resource");
        when(mockResourceService.replace(eq(identifier), any(Session.class),
                    any(IRI.class), any(Dataset.class), any(), any())).thenReturn(completedFuture(false));

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockBundler, null);

        assertEquals(INTERNAL_SERVER_ERROR, patchHandler.updateResource(patchHandler.initialize(mockResource)).join()
                .build().getStatusInfo());
    }

    @Test
    public void testNoLdpRsSupport() {
        when(mockLdpRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockBundler, null);
        final Response res = patchHandler.initialize(mockResource).build();

        assertEquals(BAD_REQUEST, res.getStatusInfo());
        assertEquals(TEXT_PLAIN_TYPE, res.getMediaType());
        assertTrue(res.getLinks().stream().anyMatch(link ->
                link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                link.getRel().equals(LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testError2() {
        when(mockLdpRequest.getContentType()).thenReturn(APPLICATION_SPARQL_UPDATE);
        when(mockLdpRequest.getPath()).thenReturn("resource");
        doThrow(RuntimeTrellisException.class).when(mockIoService)
            .update(any(Graph.class), eq(insert), eq(SPARQL_UPDATE), eq(identifier.getIRIString()));

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockBundler, baseUrl);

        assertEquals(BAD_REQUEST, patchHandler.updateResource(patchHandler.initialize(mockResource)).join().build()
                .getStatusInfo());
    }
}
