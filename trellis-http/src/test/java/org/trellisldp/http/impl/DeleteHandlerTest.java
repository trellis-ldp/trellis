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
import static java.util.Collections.emptyList;
import static java.util.Date.from;
import static java.util.Optional.empty;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;

import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.vocabulary.XSD;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class DeleteHandlerTest {

    private static final RDF rdf = getInstance();
    private static final Instant time = ofEpochSecond(1496262729);
    private static final String baseUrl = "http://localhost:8080/repo";
    private static final Literal date = rdf.createLiteral(time.toString(), XSD.dateTime);

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private Resource mockResource;

    @Mock
    private Request mockRequest;

    @Mock
    private Session mockSession;

    @Mock
    private LdpRequest mockLdpRequest;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        final IRI iri = rdf.createIRI("trellis:repo");
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getIdentifier()).thenReturn(iri);
        when(mockResource.getMementos()).thenReturn(emptyList());
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());

        when(mockResourceService.skolemize(any(BlankNode.class)))
            .thenReturn(rdf.createIRI(TRELLIS_BNODE_PREFIX + "foo"));
        when(mockResourceService.skolemize(eq(iri))).thenReturn(iri);
        when(mockResourceService.skolemize(eq(AS.Delete))).thenReturn(AS.Delete);
        when(mockResourceService.skolemize(eq(PROV.Activity))).thenReturn(PROV.Activity);
        when(mockResourceService.skolemize(eq(Trellis.AnonymousAgent))).thenReturn(Trellis.AnonymousAgent);
        when(mockResourceService.skolemize(eq(date))).thenReturn(date);
        when(mockResourceService.put(eq(iri), any(IRI.class), any(Dataset.class))).thenReturn(completedFuture(true));

        when(mockLdpRequest.getSession()).thenReturn(mockSession);
        when(mockLdpRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockLdpRequest.getPath()).thenReturn("/");
        when(mockLdpRequest.getRequest()).thenReturn(mockRequest);

        when(mockSession.getCreated()).thenReturn(time);
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        when(mockSession.getDelegatedBy()).thenReturn(empty());
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            final String base = (String) inv.getArgument(1);
            if (term instanceof IRI) {
                final String iriString = ((IRI) term).getIRIString();
                if (iriString.startsWith(base)) {
                    return rdf.createIRI(TRELLIS_PREFIX + iriString.substring(base.length()));
                }
            }
            return term;
        });
    }

    @Test
    public void testDelete() {
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockResourceService, null);

        final Response res = handler.deleteResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
    }

    @Test
    public void testDeleteError() {
        when(mockResourceService.put(any(IRI.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(completedFuture(false));
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockResourceService, baseUrl);

        final Response res = handler.deleteResource(mockResource).build();
        assertEquals(INTERNAL_SERVER_ERROR, res.getStatusInfo());
    }

    @Test
    public void testCache() {
        when(mockRequest.evaluatePreconditions(eq(from(time)), any(EntityTag.class)))
                .thenReturn(status(PRECONDITION_FAILED));
        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockResourceService, baseUrl);

        assertThrows(WebApplicationException.class, () -> handler.deleteResource(mockResource));
    }

    @Test
    public void testGetDeleted() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Resource);
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv ->
                Stream.of(new SimpleEntry<>(Trellis.DeletedResource.getIRIString(), "type")));

        final DeleteHandler handler = new DeleteHandler(mockLdpRequest, mockResourceService, baseUrl);

        assertThrows(WebApplicationException.class, () -> handler.deleteResource(mockResource));
    }
}
