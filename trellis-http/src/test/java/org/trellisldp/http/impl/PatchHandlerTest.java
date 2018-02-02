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
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.AuditService.none;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.domain.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;

import java.time.Instant;
import java.util.Date;
import java.util.function.Predicate;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDFS;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class PatchHandlerTest {

    private static final Instant time = ofEpochSecond(1496262729);
    private static final String baseUrl = "http://localhost:8080/repo/";
    private static final RDF rdf = getInstance();
    private static final String insert = "INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}";
    private static final IRI identifier = rdf.createIRI("trellis:data/resource");

    @Mock
    private ResourceService mockResourceService;

    private AuditService mockAuditService = none();

    @Mock
    private IOService mockIoService;

    @Mock
    private Resource mockResource;

    @Mock
    private Request mockRequest;

    @Mock
    private LdpRequest mockLdpRequest;

    @Mock
    private HttpHeaders mockHttpHeaders;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResourceService.put(any(IRI.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockLdpRequest.getRequest()).thenReturn(mockRequest);
        when(mockLdpRequest.getPath()).thenReturn("resource");
        when(mockLdpRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockLdpRequest.getHeaders()).thenReturn(mockHttpHeaders);
        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(TEXT_TURTLE_TYPE));
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            final String base = (String) inv.getArgument(1);
            if (term instanceof IRI) {
                final String iri = ((IRI) term).getIRIString();
                if (iri.startsWith(base)) {
                    return rdf.createIRI(TRELLIS_DATA_PREFIX + iri.substring(base.length()));
                }
            }
            return term;
        });
    }

    @Test
    public void testPatchNoSparql() {
        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, null,
                mockAuditService, mockResourceService, mockIoService, null);
        assertThrows(WebApplicationException.class, () -> patchHandler.updateResource(mockResource).build());
    }

    @Test
    public void testPatchLdprs() {
        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockAuditService,
                        mockResourceService, mockIoService, baseUrl);

        final Response res = patchHandler.updateResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());
    }

    @Test
    public void testEntity() {
        final Triple triple = rdf.createTriple(identifier, RDFS.label, rdf.createLiteral("A label"));

        when(mockResource.stream(eq(Trellis.PreferUserManaged))).thenAnswer(x -> of(triple));
        when(mockLdpRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockAuditService,
                        mockResourceService, mockIoService, null);

        final Response res = patchHandler.updateResource(mockResource).build();
        assertEquals(NO_CONTENT, res.getStatusInfo());

        verify(mockIoService).update(any(Graph.class), eq(insert), eq(identifier.getIRIString()));

        verify(mockResourceService).put(eq(identifier), eq(LDP.RDFSource), any(Dataset.class));
    }

    @Test
    public void testPreferRepresentation() {
        when(mockLdpRequest.getPath()).thenReturn("resource");
        when(mockLdpRequest.getPrefer()).thenReturn(Prefer.valueOf("return=representation"));

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockAuditService,
                        mockResourceService, mockIoService, null);

        final Response res = patchHandler.updateResource(mockResource).build();

        assertEquals(OK, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertEquals("return=representation", res.getHeaderString(PREFERENCE_APPLIED));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
    }

    @Test
    public void testPreferHTMLRepresentation() {
        when(mockLdpRequest.getPath()).thenReturn("resource");
        when(mockLdpRequest.getPrefer()).thenReturn(Prefer.valueOf("return=representation"));
        when(mockLdpRequest.getHeaders().getAcceptableMediaTypes())
            .thenReturn(singletonList(MediaType.valueOf(RDFA.mediaType())));

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockAuditService,
                        mockResourceService, mockIoService, null);

        final Response res = patchHandler.updateResource(mockResource).build();

        assertEquals(OK, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertEquals("return=representation", res.getHeaderString(PREFERENCE_APPLIED));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertTrue(TEXT_HTML_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_HTML_TYPE));
    }

    @Test
    public void testDeleted() {
        when(mockResource.isDeleted()).thenReturn(true);
        when(mockLdpRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockAuditService,
                        mockResourceService, mockIoService, null);

        assertThrows(WebApplicationException.class, () -> patchHandler.updateResource(mockResource));
    }

    @Test
    public void testConflict() {
        when(mockRequest.evaluatePreconditions(any(Date.class), any(EntityTag.class)))
            .thenReturn(status(CONFLICT));
        when(mockLdpRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockAuditService,
                        mockResourceService, mockIoService, null);

        assertThrows(WebApplicationException.class, () -> patchHandler.updateResource(mockResource));
    }

    @Test
    public void testError() {
        when(mockResourceService.put(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")), any(IRI.class),
                    any(Dataset.class))).thenReturn(completedFuture(false));
        when(mockLdpRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockAuditService,
                        mockResourceService, mockIoService, null);

        final Response res = patchHandler.updateResource(mockResource).build();
        assertEquals(INTERNAL_SERVER_ERROR, res.getStatusInfo());
    }

    @Test
    public void testError2() {
        doThrow(RuntimeTrellisException.class).when(mockIoService)
            .update(any(Graph.class), eq(insert), eq(identifier.getIRIString()));
        when(mockLdpRequest.getPath()).thenReturn("resource");

        final PatchHandler patchHandler = new PatchHandler(mockLdpRequest, insert, mockAuditService,
                        mockResourceService, mockIoService, baseUrl);

        assertThrows(BadRequestException.class, () -> patchHandler.updateResource(mockResource));
    }

    private static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    private static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, "type");
    }
}
