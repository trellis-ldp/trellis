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
import static java.util.Date.from;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.util.concurrent.CompletionException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Session;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class DeleteHandlerTest extends HandlerBaseTest {

    @Test
    public void testDelete() {
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, null);
        final Response res = handler.deleteResource(handler.initialize(mockResource)).join().build();
        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect delete response!");
    }

    @Test
    public void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);
        // will never store audit
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(asyncException());
        final AuditService badAuditService = new DefaultAuditService() {};
        when(mockBundler.getAuditService()).thenReturn(badAuditService);
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, null);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.deleteResource(handler.initialize(mockResource))),
                "No exception thrown when the backend reports an exception!");
    }

    @Test
    public void testDeleteError() {
        when(mockResourceService.delete(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(asyncException());
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.deleteResource(handler.initialize(mockResource))),
                "No exception thrown when the backend reports an exception!");
    }

    @Test
    public void testDeletePersistenceSupport() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);
        final Response res = assertThrows(WebApplicationException.class, () ->
                handler.initialize(mockResource)).getResponse();
        assertTrue(res.getLinks().stream().anyMatch(link ->
            link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
            link.getRel().equals(LDP.constrainedBy.getIRIString())), "Missing Link headers");
        assertEquals(TEXT_PLAIN_TYPE, res.getMediaType(), "Incorrect media type");
    }

    @Test
    public void testDeleteACLError() {
        when(mockResourceService.replace(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class), any(),
                        any())).thenReturn(asyncException());
        when(mockLdpRequest.getExt()).thenReturn(ACL);
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.deleteResource(handler.initialize(mockResource))),
                "No exception thrown when an ACL couldn't be deleted!");
    }

    @Test
    public void testDeleteACLAuditError() {
        when(mockResourceService.replace(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class), any(),
                        any())).thenReturn(completedFuture(null));
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(asyncException());
        when(mockLdpRequest.getExt()).thenReturn(ACL);
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);
        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.deleteResource(handler.initialize(mockResource))),
                "No exception thrown when an ACL audit stream couldn't be written!");
    }

    @Test
    public void testCache() {
        when(mockRequest.evaluatePreconditions(eq(from(time)), any(EntityTag.class)))
                .thenReturn(status(PRECONDITION_FAILED));
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);

        final Response res = assertThrows(WebApplicationException.class, () ->
                handler.initialize(mockResource), "Unexpected response type!").getResponse();
        assertEquals(PRECONDITION_FAILED, res.getStatusInfo(), "Incorrect response type!");
    }
}
