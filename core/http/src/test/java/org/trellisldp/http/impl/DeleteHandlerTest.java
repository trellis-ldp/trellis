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
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Date.from;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.AuditService.none;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.api.Session;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class DeleteHandlerTest {

    private static final RDF rdf = getInstance();
    private static final Instant time = ofEpochSecond(1496262729);
    private static final String baseUrl = "http://localhost:8080/repo";

    private final AuditService auditService = none();

    private final AgentService agentService = new SimpleAgentService();

    private final MementoService mementoService = new NoopMementoService();

    @Mock
    private ServiceBundler mockBundler;

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private Resource mockResource;

    @Mock
    private Request mockRequest;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private LdpRequest mockLdpRequest;

    @Mock
    private CompletableFuture<Boolean> mockFuture;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        final IRI iri = rdf.createIRI("trellis:data/");
        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockBundler.getAuditService()).thenReturn(auditService);
        when(mockBundler.getMementoService()).thenReturn(mementoService);
        when(mockBundler.getAgentService()).thenReturn(agentService);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getIdentifier()).thenReturn(iri);
        when(mockResourceService.supportedInteractionModels()).thenReturn(singleton(LDP.Resource));
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());

        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class)))
            .thenReturn(rdf.createIRI(TRELLIS_BNODE_PREFIX + "foo"));
        when(mockResourceService.delete(eq(iri), any(Session.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));
        when(mockResourceService.add(eq(iri), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));

        when(mockLdpRequest.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockLdpRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockLdpRequest.getPath()).thenReturn("/");
        when(mockLdpRequest.getRequest()).thenReturn(mockRequest);

        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            final String base = (String) inv.getArgument(1);
            if (term instanceof IRI) {
                final String iriString = ((IRI) term).getIRIString();
                if (iriString.startsWith(base)) {
                    return rdf.createIRI(TRELLIS_DATA_PREFIX + iriString.substring(base.length()));
                }
            }
            return term;
        });
    }

    @Test
    public void testDelete() {
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, null);

        final Response res = handler.deleteResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
    }

    @Test
    public void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);
        // will never store audit
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(false));
        final AuditService badAuditService = new DefaultAuditService() {};
        when(mockBundler.getAuditService()).thenReturn(badAuditService);
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, null);

        assertThrows(BadRequestException.class, () -> handler.deleteResource(mockResource));
    }

    @Test
    public void testDeleteError() {
        when(mockResourceService.delete(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(completedFuture(false));
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);

        assertThrows(BadRequestException.class, () -> handler.deleteResource(mockResource));
    }

    @Test
    public void testDeleteException() throws Exception {
        when(mockFuture.get()).thenThrow(new InterruptedException("Expected"));
        when(mockResourceService.delete(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(mockFuture);
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);

        final Response res = handler.deleteResource(mockResource).build();
        assertEquals(INTERNAL_SERVER_ERROR, res.getStatusInfo());
    }

    @Test
    public void testDeletePersistenceSupport() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);

        final BadRequestException ex = assertThrows(BadRequestException.class, () ->
                handler.deleteResource(mockResource));
        assertTrue(ex.getResponse().getLinks().stream().anyMatch(link ->
                link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                link.getRel().equals(LDP.constrainedBy.getIRIString())));
        assertEquals(TEXT_PLAIN_TYPE, ex.getResponse().getMediaType());
    }

    @Test
    public void testDeleteACLError() {
        when(mockResourceService.replace(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class), any(),
                        any())).thenReturn(completedFuture(false));
        when(mockLdpRequest.getExt()).thenReturn(ACL);
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);

        assertThrows(BadRequestException.class, () -> handler.deleteResource(mockResource));
    }

    @Test
    public void testDeleteACLAuditError() {
        when(mockResourceService.replace(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class), any(),
                        any())).thenReturn(completedFuture(true));
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(false));
        when(mockLdpRequest.getExt()).thenReturn(ACL);
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);

        assertThrows(BadRequestException.class, () -> handler.deleteResource(mockResource));
    }


    @Test
    public void testCache() {
        when(mockRequest.evaluatePreconditions(eq(from(time)), any(EntityTag.class)))
                .thenReturn(status(PRECONDITION_FAILED));
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);

        assertThrows(WebApplicationException.class, () -> handler.deleteResource(mockResource));
    }

    @Test
    public void testGetDeleted() {
        when(mockResource.isDeleted()).thenReturn(true);

        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockBundler, baseUrl);

        assertThrows(WebApplicationException.class, () -> handler.deleteResource(mockResource));
    }
}
