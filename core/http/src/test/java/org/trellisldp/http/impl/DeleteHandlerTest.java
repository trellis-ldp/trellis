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
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.trellisldp.http.core.HttpConstants.ACL;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.util.concurrent.CompletionException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Metadata;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
class DeleteHandlerTest extends BaseTestHandler {

    @Test
    void testDelete() {
        final DeleteHandler handler = new DeleteHandler(mockTrellisRequest, mockBundler, extensions, null);
        try (final Response res = handler.deleteResource(handler.initialize(mockParent, mockResource))
                .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
        // will never store audit
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(asyncException());
        final AuditService badAuditService = new DefaultAuditService() {};
        when(mockBundler.getAuditService()).thenReturn(badAuditService);
        final DeleteHandler handler = new DeleteHandler(mockTrellisRequest, mockBundler, extensions, null);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.deleteResource(handler.initialize(mockParent, mockResource))),
                "No exception thrown when the backend reports an exception!");
    }

    @Test
    void testDeleteError() {
        when(mockResourceService.delete(any(Metadata.class))).thenReturn(asyncException());
        final DeleteHandler handler = new DeleteHandler(mockTrellisRequest, mockBundler, extensions, baseUrl);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.deleteResource(handler.initialize(mockParent, mockResource))),
                "No exception thrown when the backend reports an exception!");
    }

    @Test
    void testDeletePersistenceSupport() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        final DeleteHandler handler = new DeleteHandler(mockTrellisRequest, mockBundler, extensions, baseUrl);
        try (final Response res = assertThrows(WebApplicationException.class, () ->
                handler.initialize(mockParent, mockResource)).getResponse()) {
            assertTrue(res.getLinks().stream().anyMatch(link ->
                link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                link.getRel().equals(LDP.constrainedBy.getIRIString())), "Missing Link headers");
            assertEquals(TEXT_PLAIN_TYPE, res.getMediaType(), "Incorrect media type");
        }
    }

    @Test
    void testDeleteACLError() {
        when(mockResourceService.replace(any(Metadata.class), any(Dataset.class))).thenReturn(asyncException());
        when(mockTrellisRequest.getExt()).thenReturn(ACL);
        final DeleteHandler handler = new DeleteHandler(mockTrellisRequest, mockBundler, extensions, baseUrl);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.deleteResource(handler.initialize(mockParent, mockResource))),
                "No exception thrown when an ACL couldn't be deleted!");
    }

    @Test
    void testDeleteACLAuditError() {
        when(mockResourceService.replace(any(Metadata.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(asyncException());
        when(mockTrellisRequest.getExt()).thenReturn(ACL);
        final DeleteHandler handler = new DeleteHandler(mockTrellisRequest, mockBundler, extensions, baseUrl);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.deleteResource(handler.initialize(mockParent, mockResource))),
                "No exception thrown when an ACL audit stream couldn't be written!");
    }
}
