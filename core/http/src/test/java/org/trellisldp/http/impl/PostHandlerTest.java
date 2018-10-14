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
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.core.Digest;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class PostHandlerTest extends HandlerBaseTest {

    @Test
    public void testPostLdprs() throws IOException {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.Container));
    }

    @Test
    public void testBadAudit() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockLdpRequest.getContentType()).thenReturn(TEXT_TURTLE);
        when(mockBundler.getAuditService()).thenReturn(new DefaultAuditService() {});
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(asyncException());

        final PostHandler handler = buildPostHandler("/simpleTriple.ttl", null, null);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))),
                "No exception when the backend audit service throws an error!");
    }

    @Test
    public void testDefaultType1() throws IOException {
        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));
    }

    @Test
    public void testDefaultType2() throws IOException {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");

        final PostHandler handler = buildPostHandler("/simpleData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, DELETED_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.NonRDFSource));
    }

    @Test
    public void testDefaultType3() throws IOException {
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));
    }

    @Test
    public void testDefaultType4() throws IOException {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final PostHandler handler = buildPostHandler("/simpleData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.NonRDFSource));
    }

    @Test
    public void testDefaultType5() throws IOException {
        when(mockLdpRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));
    }

    @Test
    public void testUnsupportedType() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        when(mockLdpRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", null);
        final Response res = assertThrows(BadRequestException.class, () ->
                handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join(),
                "No exception thrown when the IXN model isn't supported!").getResponse();

        assertEquals(BAD_REQUEST, res.getStatusInfo(), "Incorrect response code!");
        assertTrue(res.getLinks().stream().anyMatch(link ->
                link.getUri().toString().equals(UnsupportedInteractionModel.getIRIString()) &&
                link.getRel().equals(LDP.constrainedBy.getIRIString())), "Missing constraint Link header!");
    }

    @Test
    public void testRdfEntity() throws IOException {
        final String path = "newresource";
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + path);
        final Triple triple = rdf.createTriple(rdf.createIRI(baseUrl + path), DC.title,
                        rdf.createLiteral("A title"));

        when(mockIoService.supportedWriteSyntaxes()).thenReturn(asList(TURTLE, JSONLD, NTRIPLES));
        when(mockIoService.read(any(), eq(TURTLE), any())).thenAnswer(x -> Stream.of(triple));
        when(mockLdpRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = buildPostHandler("/simpleTriple.ttl", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + path), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));

        verify(mockBinaryService, never()).setContent(any(IRI.class), any(InputStream.class));
        verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + path));
        verify(mockResourceService).create(eq(identifier), eq(LDP.RDFSource), any(Dataset.class), any(), any());
    }

    @Test
    public void testBinaryEntity() throws IOException {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");

        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "new-resource");
        final PostHandler handler = buildPostHandler("/simpleData.txt", "new-resource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "new-resource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.NonRDFSource));
        assertAll("Check Binary response", checkBinaryEntityResponse(identifier));
    }

    @Test
    public void testEntityWithDigest() throws IOException {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getDigest()).thenReturn(new Digest("md5", "1VOyRwUXW1CPdC5nelt7GQ=="));

        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource-with-entity");
        final PostHandler handler = buildPostHandler("/simpleData.txt", "resource-with-entity", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "resource-with-entity"), res.getLocation(), "Incorrect Location hearder!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.NonRDFSource));
        assertAll("Check Binary response", checkBinaryEntityResponse(identifier));
    }

    @Test
    public void testEntityBadDigest() {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getDigest()).thenReturn(new Digest("md5", "blahblah"));

        final PostHandler handler = buildPostHandler("/simpleData.txt", "bad-digest", null);

        final Response res = assertThrows(WebApplicationException.class, () ->
                handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join(),
                "No exception thrown when there is a bad digest!").getResponse();
        assertEquals(BAD_REQUEST, res.getStatusInfo(), "Incorrect response type!");
    }

    @Test
    public void testBadDigest2() {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getDigest()).thenReturn(new Digest("foo", "blahblahblah"));

        final PostHandler handler = buildPostHandler("/simpleData.txt", "bad-digest", null);

        final Response res = assertThrows(WebApplicationException.class, () ->
                handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join(),
                "No exception thrown when there is an unsupported digest!").getResponse();
        assertEquals(BAD_REQUEST, res.getStatusInfo(), "Incorrect response code!");
    }

    @Test
    public void testBadEntityDigest() {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");
        when(mockLdpRequest.getDigest()).thenReturn(new Digest("md5", "blahblah"));

        final File entity = new File(new File(getClass().getResource("/simpleData.txt").getFile()).getParent());
        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, null);

        final Response res = assertThrows(WebApplicationException.class, () ->
                handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join(),
                "No exception thrown when the entity itself is bad!").getResponse();
        assertEquals(INTERNAL_SERVER_ERROR, res.getStatusInfo(), "Incorrect response code!");
    }

    @Test
    public void testEntityError() {
        when(mockLdpRequest.getContentType()).thenReturn("text/plain");

        final File entity = new File(getClass().getResource("/simpleData.txt").getFile() + ".nonexistent-suffix");
        final PostHandler handler = new PostHandler(mockLdpRequest, root, "newresource", entity, mockBundler, baseUrl);

        final Response res = assertThrows(WebApplicationException.class, () ->
                handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join(),
                "No exception thrown when the entity is non-existent!").getResponse();
        assertEquals(INTERNAL_SERVER_ERROR, res.getStatusInfo(), "Incorrect response code!");
    }

    @Test
    public void testError() throws IOException {
        when(mockResourceService.create(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + "newresource")),
                    any(IRI.class), any(Dataset.class), any(), any())).thenReturn(asyncException());
        when(mockLdpRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", baseUrl);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))),
                "No exception thrown when the backend errors!");
    }

    private PostHandler buildPostHandler(final String resourceName, final String id, final String baseUrl) {
        final File entity = new File(getClass().getResource(resourceName).getFile());
        return new PostHandler(mockLdpRequest, root, id, entity, mockBundler, baseUrl);
    }

    private Stream<Executable> checkBinaryEntityResponse(final IRI identifier) {
        return Stream.of(
                () -> verify(mockResourceService, description("ResourceService::create not called!"))
                            .create(eq(identifier), eq(LDP.NonRDFSource), any(Dataset.class), any(), any()),
                () -> verify(mockIoService, never().description("entity shouldn't be read!")).read(any(), any(), any()),
                () -> verify(mockBinaryService, description("content not set on binary service!"))
                            .setContent(iriArgument.capture(), any(InputStream.class), metadataArgument.capture()),
                () -> assertEquals("text/plain", metadataArgument.getValue().get(CONTENT_TYPE), "Invalid content-type"),
                () -> assertTrue(iriArgument.getValue().getIRIString().startsWith("file:///"), "Invalid binary ID!"));
    }
}
