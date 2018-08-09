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

import static java.net.URI.create;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Session;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.domain.Digest;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class PostHandlerTest extends HandlerBaseTest {

    @Test
    public void testPostLdprs() throws IOException {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);
        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());
        // will never store audit
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(false));
        final AuditService badAuditService = new DefaultAuditService() {};
        when(mockBundler.getAuditService()).thenReturn(badAuditService);
        final PostHandler handler = new PostHandler(mockLdpRequest, root, null, entity, mockBundler, null);

        assertEquals(INTERNAL_SERVER_ERROR, handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .join().build().getStatusInfo());
    }

    @Test
    public void testDefaultType1() throws IOException {
        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .join().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testDefaultType2() throws IOException {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, DELETED_RESOURCE))
                .join().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testDefaultType3() throws IOException {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testDefaultType4() throws IOException {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testDefaultType5() throws IOException {
        when(mockLdpRequest.getContentType()).thenReturn("text/turtle");
        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());

        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());
    }

    @Test
    public void testUnsupportedType() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();
        assertEquals(BAD_REQUEST, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(link ->
                link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                link.getRel().equals(LDP.constrainedBy.getIRIString())));
    }

    @Test
    public void testEntity() throws IOException {
        final String path = "newresource";
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + path);
        final Triple triple = rdf.createTriple(rdf.createIRI(baseUrl + path), DC.title,
                        rdf.createLiteral("A title"));
        when(mockIoService.supportedWriteSyntaxes()).thenReturn(asList(TURTLE, JSONLD, NTRIPLES));
        when(mockIoService.read(any(), eq(TURTLE), any())).thenAnswer(x -> Stream.of(triple));
        final File entity = new File(getClass().getResource("/simpleTriple.ttl").getFile());

        when(mockLdpRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + path), res.getLocation());

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));

        verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + path));

        verify(mockResourceService).create(eq(identifier), any(Session.class), eq(LDP.RDFSource), any(Dataset.class),
                        any(), any());
    }

    @Test
    public void testEntity2() throws IOException {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "newresource");
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");

        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());

        verify(mockIoService, never()).read(any(), any(), any());

        verify(mockBinaryService).setContent(iriArgument.capture(), any(InputStream.class),
                metadataArgument.capture());
        assertTrue(iriArgument.getValue().getIRIString().startsWith("file:///"));
        assertEquals("text/plain", metadataArgument.getValue().get(CONTENT_TYPE));

        verify(mockResourceService).create(eq(identifier), any(Session.class), eq(LDP.NonRDFSource), any(Dataset.class),
                        any(), any());
    }

    @Test
    public void testEntity3() throws IOException {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "newresource");
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getDigest()).thenReturn(new Digest("md5", "1VOyRwUXW1CPdC5nelt7GQ=="));

        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();
        assertEquals(CREATED, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.Resource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.RDFSource)));
        assertFalse(res.getLinks().stream().anyMatch(hasType(LDP.Container)));
        assertTrue(res.getLinks().stream().anyMatch(hasType(LDP.NonRDFSource)));
        assertEquals(create(baseUrl + "newresource"), res.getLocation());

        verify(mockIoService, never()).read(any(), any(), any());

        verify(mockBinaryService).setContent(iriArgument.capture(), any(InputStream.class),
                metadataArgument.capture());
        assertTrue(iriArgument.getValue().getIRIString().startsWith("file:///"));
        assertEquals("text/plain", metadataArgument.getValue().get(CONTENT_TYPE));

        verify(mockResourceService).create(eq(identifier), any(Session.class), eq(LDP.NonRDFSource), any(Dataset.class),
                        any(), any());
    }

    @Test
    public void testEntityBadDigest() {
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getDigest()).thenReturn(new Digest("md5", "blahblah"));

        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        assertEquals(BAD_REQUEST, handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .join().build().getStatusInfo());
    }

    @Test
    public void testBadDigest2() {
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile());
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getDigest()).thenReturn(new Digest("foo", "blahblah"));

        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        assertEquals(BAD_REQUEST, handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .join().build().getStatusInfo());
    }

    @Test
    public void testBadEntityDigest() {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getDigest()).thenReturn(new Digest("md5", "blahblah"));
        final File entity = new File(new File(getClass().getResource("/simpleData.txt").getFile()).getParent());

        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        assertEquals(INTERNAL_SERVER_ERROR, handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .join().build().getStatusInfo());
    }

    @Test
    public void testEntityError() {
        final File entity = new File(getClass().getResource("/simpleData.txt").getFile() + ".nonexistent-suffix");
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");

        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, baseUrl);

        assertEquals(INTERNAL_SERVER_ERROR, handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .join().build().getStatusInfo());
    }

    @Test
    public void testError() throws IOException {
        when(mockResourceService.create(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + "newresource")), any(Session.class),
                    any(IRI.class), any(Dataset.class), any(), any())).thenReturn(completedFuture(false));
        when(mockLdpRequest.getContentType()).thenReturn("text/turtle");

        final File entity = new File(getClass().getResource("/emptyData.txt").getFile());
        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, baseUrl);

        assertEquals(INTERNAL_SERVER_ERROR, handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .join().build().getStatusInfo());
    }

    private static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    private static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, "type");
    }
}
