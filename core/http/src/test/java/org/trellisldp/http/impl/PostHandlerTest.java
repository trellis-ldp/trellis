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
import static java.util.Optional.of;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.vocabulary.Trellis.UnsupportedInteractionModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.http.core.Digest;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class PostHandlerTest extends BaseTestHandler {

    @Test
    public void testPostLdprs() throws IOException {
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.Container));
    }

    @Test
    public void testBadAudit() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.BasicContainer.getIRIString()).rel("type").build());
        when(mockTrellisRequest.getContentType()).thenReturn(TEXT_TURTLE);
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
        when(mockTrellisRequest.getContentType()).thenReturn("text/plain");

        final PostHandler handler = buildPostHandler("/simpleData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, DELETED_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.NonRDFSource));
    }

    @Test
    public void testDefaultType3() throws IOException {
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));
    }

    @Test
    public void testDefaultType4() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn("text/plain");
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Resource.getIRIString()).rel("type").build());

        final PostHandler handler = buildPostHandler("/simpleData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.NonRDFSource));
    }

    @Test
    public void testDefaultType5() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "newresource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));
    }

    @Test
    public void testUnsupportedType() throws IOException {
        when(mockResourceService.supportedInteractionModels()).thenReturn(emptySet());
        when(mockTrellisRequest.getLink()).thenReturn(fromUri(LDP.Container.getIRIString()).rel("type").build());

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
        final Triple triple = rdf.createTriple(rdf.createIRI(baseUrl + path), DC.title,
                        rdf.createLiteral("A title"));

        when(mockIoService.supportedWriteSyntaxes()).thenReturn(asList(TURTLE, JSONLD, NTRIPLES));
        when(mockIoService.read(any(), eq(TURTLE), any())).thenAnswer(x -> Stream.of(triple));
        when(mockTrellisRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = buildPostHandler("/simpleTriple.ttl", "newresource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + path), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));

        verify(mockBinaryService, never()).setContent(any(BinaryMetadata.class), any(InputStream.class), any());
        verify(mockIoService).read(any(InputStream.class), eq(TURTLE), eq(baseUrl + path));
        verify(mockResourceService).create(any(Metadata.class), any(Dataset.class));
    }

    @Test
    public void testBinaryEntity() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn("text/plain");

        final PostHandler handler = buildPostHandler("/simpleData.txt", "new-resource", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "new-resource"), res.getLocation(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.NonRDFSource));
        assertAll("Check Binary response", checkBinaryEntityResponse());
    }

    @Test
    public void testEntityWithDigest() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn("text/plain");
        when(mockTrellisRequest.getDigest()).thenReturn(new Digest("md5", "1VOyRwUXW1CPdC5nelt7GQ=="));

        final PostHandler handler = buildPostHandler("/simpleData.txt", "resource-with-entity", null);
        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join().build();

        assertEquals(CREATED, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(create(baseUrl + "resource-with-entity"), res.getLocation(), "Incorrect Location hearder!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.NonRDFSource));
        assertAll("Check Binary response", checkBinaryEntityResponse());
    }

    @Test
    public void testEntityBadDigest() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn("text/plain");
        when(mockTrellisRequest.getDigest()).thenReturn(new Digest("md5", "blahblah"));

        final PostHandler handler = buildPostHandler("/simpleData.txt", "bad-digest", null);

        final Response res = handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))
                .thenApply(ResponseBuilder::build)
                .exceptionally(err -> of(err).map(Throwable::getCause).filter(WebApplicationException.class::isInstance)
                    .map(WebApplicationException.class::cast).orElseGet(() -> new WebApplicationException(err))
                    .getResponse()).join();
        assertEquals(BAD_REQUEST, res.getStatusInfo(), "Incorrect response type!");
    }

    @Test
    public void testBadDigest() throws IOException {
        when(mockTrellisRequest.getContentType()).thenReturn("text/plain");
        when(mockTrellisRequest.getDigest()).thenReturn(new Digest("foo", "blahblahblah"));

        final PostHandler handler = buildPostHandler("/simpleData.txt", "bad-digest", null);

        final Response res = assertThrows(BadRequestException.class, () ->
                handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))).getResponse();
        assertEquals(BAD_REQUEST, res.getStatusInfo(), "Incorrect response code!");
    }

    @Test
    public void testError() throws IOException {
        when(mockResourceService.create(any(Metadata.class), any(Dataset.class))).thenReturn(asyncException());
        when(mockTrellisRequest.getContentType()).thenReturn("text/turtle");

        final PostHandler handler = buildPostHandler("/emptyData.txt", "newresource", baseUrl);

        assertThrows(CompletionException.class, () ->
                unwrapAsyncError(handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE))),
                "No exception thrown when the backend errors!");
    }

    @Test
    public void testBadRdfInputStream() throws IOException {
        final InputStream mockInputStream = mock(InputStream.class, inv -> {
            throw new IOException("Expected exception");
        });

        final PostHandler handler = new PostHandler(mockTrellisRequest, root, "bad-resource", mockInputStream,
                mockBundler, null);
        final Response res = assertThrows(WebApplicationException.class, () ->
                handler.createResource(handler.initialize(mockParent, MISSING_RESOURCE)).join()).getResponse();
        assertEquals(INTERNAL_SERVER_ERROR, res.getStatusInfo(), "Incorrect response code!");
    }


    private PostHandler buildPostHandler(final String resourceName, final String id, final String baseUrl)
                    throws IOException {
        final InputStream entity = getClass().getResource(resourceName).openStream();
        return new PostHandler(mockTrellisRequest, root, id, entity, mockBundler, baseUrl);
    }

    private Stream<Executable> checkBinaryEntityResponse() {
        return Stream.of(
                () -> verify(mockResourceService, description("ResourceService::create not called!"))
                            .create(any(Metadata.class), any(Dataset.class)),
                () -> verify(mockIoService, never().description("entity shouldn't be read!")).read(any(), any(), any()),
                () -> verify(mockBinaryService, description("content not set on binary service!"))
                            .setContent(metadataArgument.capture(), any(InputStream.class)),
                () -> assertEquals(of("text/plain"), metadataArgument.getValue().getMimeType(), "Invalid content-type"),
                () -> assertTrue(metadataArgument.getValue().getIdentifier().getIRIString().startsWith("file:///"),
                                 "Invalid binary ID!"));
    }
}
