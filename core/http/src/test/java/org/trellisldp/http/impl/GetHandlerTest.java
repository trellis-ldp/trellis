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
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Date.from;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.notModified;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.Syntax.LD_PATCH;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.domain.HttpConstants.DESCRIPTION;
import static org.trellisldp.http.domain.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.PATCH;
import static org.trellisldp.http.domain.HttpConstants.PREFER;
import static org.trellisldp.http.domain.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.domain.HttpConstants.RANGE;
import static org.trellisldp.http.domain.HttpConstants.WANT_DIGEST;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.JSONLD.compacted;

import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.Binary;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;
import org.trellisldp.vocabulary.SKOS;

/**
 * @author acoburn
 */
public class GetHandlerTest extends HandlerBaseTest {

    private static final Instant binaryTime = ofEpochSecond(1496262750);

    private Binary testBinary = new Binary(rdf.createIRI("file:///testResource.txt"), binaryTime, "text/plain", 100L);

    @Test
    public void testGetLdprs() {
        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo());
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertEquals(from(time), res.getLastModified());
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
        assertNull(res.getHeaderString(PREFERENCE_APPLIED));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertAll(checkLdpType(res, LDP.RDFSource));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));

        final EntityTag etag = res.getEntityTag();
        assertTrue(etag.isWeak());
        assertEquals(md5Hex(time.toEpochMilli() + "." + time.getNano() + ".." + baseUrl), etag.getValue());

        final List<Object> varies = res.getHeaders().get(VARY);
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertTrue(varies.contains(PREFER));
    }

    @Test
    public void testGetPreferLdprs() {
        when(mockIoService.supportedUpdateSyntaxes()).thenReturn(singletonList(LD_PATCH));
        when(mockLdpRequest.getPrefer())
            .thenReturn(Prefer.valueOf("return=representation; include=\"http://www.w3.org/ns/ldp#PreferContainment"));

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo());
        assertEquals("text/ldpatch", res.getHeaderString(ACCEPT_PATCH));
        assertEquals("return=representation", res.getHeaderString(PREFERENCE_APPLIED));
        assertEquals(from(time), res.getLastModified());
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertAll(checkLdpType(res, LDP.RDFSource));
    }

    @Test
    public void testGetVersionedLdprs() {
        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, true, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo());
        assertEquals(from(time), res.getLastModified());
        assertEquals(ofInstant(time, UTC).format(RFC_1123_DATE_TIME), res.getHeaderString(MEMENTO_DATETIME));
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(PREFERENCE_APPLIED));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertAll(checkLdpType(res, LDP.RDFSource));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS)));

        final EntityTag etag = res.getEntityTag();
        assertTrue(etag.isWeak());
        assertEquals(md5Hex(time.toEpochMilli() + "." + time.getNano() + ".." + baseUrl), etag.getValue());

        final List<Object> varies = res.getHeaders().get(VARY);
        assertTrue(varies.contains(PREFER));
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
        assertFalse(varies.contains(ACCEPT_DATETIME));
    }

    @Test
    public void testCache() {
        when(mockRequest.evaluatePreconditions(eq(from(time)), any(EntityTag.class)))
                .thenReturn(notModified());

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, baseUrl);

        assertEquals(NOT_MODIFIED, handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                .build().getStatusInfo());
    }

    @Test
    public void testCacheLdpNr() {
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(WILDCARD_TYPE));
        when(mockRequest.evaluatePreconditions(eq(from(binaryTime)), any(EntityTag.class)))
                .thenReturn(notModified());

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, baseUrl);

        assertEquals(NOT_MODIFIED, handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                .build().getStatusInfo());
    }

    @Test
    public void testExtraLinks() {
        final String inbox = "http://ldn.example.com/inbox";
        final String annService = "http://annotation.example.com/resource";

        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.of(
                new SimpleEntry<>(annService, OA.annotationService.getIRIString()),
                new SimpleEntry<>(SKOS.Concept.getIRIString(), "type"),
                new SimpleEntry<>(inbox, "inbox")));

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, baseUrl);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo());
        assertTrue(res.getLinks().stream().anyMatch(hasType(SKOS.Concept)));
        assertTrue(res.getLinks().stream().anyMatch(hasLink(rdf.createIRI(inbox), "inbox")));
        assertTrue(res.getLinks().stream().anyMatch(hasLink(rdf.createIRI(annService),
                        OA.annotationService.getIRIString())));
    }

    @Test
    public void testNotAcceptableLdprs() {
        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(APPLICATION_JSON_TYPE));

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, baseUrl);

        assertEquals(NOT_ACCEPTABLE, handler.getRepresentation(handler.standardHeaders(handler.initialize(
                            mockResource))).build().getStatusInfo());
    }

    @Test
    public void testMinimalLdprs() {
        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(APPLICATION_LD_JSON_TYPE));
        when(mockLdpRequest.getPrefer()).thenReturn(Prefer.valueOf("return=minimal"));

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, baseUrl);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertEquals(from(time), res.getLastModified());
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertEquals("return=minimal", res.getHeaderString(PREFERENCE_APPLIED));
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertAll(checkLdpType(res, LDP.RDFSource));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));

        final EntityTag etag = res.getEntityTag();
        assertTrue(etag.isWeak());
        final String preferHash = new ArrayList().hashCode() + "." + new ArrayList().hashCode();
        assertEquals(md5Hex(time.toEpochMilli() + "." + time.getNano() + "." + preferHash + "." + baseUrl),
                etag.getValue());

        final List<Object> varies = res.getHeaders().get(VARY);
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertTrue(varies.contains(PREFER));
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
    }

    @Test
    public void testGetLdpc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockIoService.supportedWriteSyntaxes()).thenReturn(Stream.of(TURTLE, NTRIPLES, JSONLD).collect(toList()));
        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(
                    MediaType.valueOf(APPLICATION_LD_JSON + "; profile=\"" + compacted.getIRIString() + "\"")));

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, null);

        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();
        assertEquals(OK, res.getStatusInfo());
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertEquals(from(time), res.getLastModified());
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE));
        assertFalse(res.getLinks().stream().anyMatch(link -> link.getRel().equals("describes")));
        assertFalse(res.getLinks().stream().anyMatch(link -> link.getRel().equals("describedby")));
        assertFalse(res.getLinks().stream().anyMatch(link -> link.getRel().equals("canonical")));
        assertNull(res.getHeaderString(PREFERENCE_APPLIED));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertAll(checkLdpType(res, LDP.Container));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH, POST)));

        final String acceptPost = res.getHeaderString(ACCEPT_POST);
        assertNotNull(acceptPost);
        assertTrue(acceptPost.contains("text/turtle"));
        assertTrue(acceptPost.contains(APPLICATION_LD_JSON));
        assertTrue(acceptPost.contains(APPLICATION_N_TRIPLES));

        final EntityTag etag = res.getEntityTag();
        assertTrue(etag.isWeak());
        assertEquals(md5Hex(time.toEpochMilli() + "." + time.getNano() + ".." + baseUrl), etag.getValue());

        final List<Object> varies = res.getHeaders().get(VARY);
        assertTrue(varies.contains(ACCEPT_DATETIME));
        assertTrue(varies.contains(PREFER));
        assertFalse(varies.contains(RANGE));
        assertFalse(varies.contains(WANT_DIGEST));
    }

    @Test
    public void testGetHTML() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(MediaType.valueOf(RDFA.mediaType())));

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo());
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertTrue(TEXT_HTML_TYPE.isCompatible(res.getMediaType()));
        assertTrue(res.getMediaType().isCompatible(TEXT_HTML_TYPE));
        assertNull(res.getHeaderString(PREFERENCE_APPLIED));
        assertNull(res.getHeaderString(ACCEPT_RANGES));
        assertAll(checkLdpType(res, LDP.Container));
    }

    @Test
    public void testGetBinaryDescription() {
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertAll(checkBinaryDescription(res));
    }

    @Test
    public void testGetBinaryDescription2() {
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockLdpRequest.getExt()).thenReturn(DESCRIPTION);

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertAll(checkBinaryDescription(res));
    }

    private Stream<Executable> checkBinaryDescription(final Response res) {
        return Stream.of(
                () -> assertEquals(OK, res.getStatusInfo()),
                () -> assertEquals(-1, res.getLength()),
                () -> assertEquals(from(time), res.getLastModified()),
                () -> assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE)),
                () -> assertTrue(res.getLinks().stream()
                        .anyMatch(link -> link.getRel().equals("describes") &&
                            !link.getUri().toString().endsWith("?ext=description"))),
                () -> assertTrue(res.getLinks().stream()
                        .anyMatch(link -> link.getRel().equals("canonical") &&
                            link.getUri().toString().endsWith("?ext=description"))),
                () -> assertAll(checkLdpType(res, LDP.RDFSource)));
    }

    @Test
    public void testGetBinary() throws IOException {
        when(mockResource.getBinary()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(WILDCARD_TYPE));

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, baseUrl);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo());
        assertEquals(-1, res.getLength());
        assertEquals(from(binaryTime), res.getLastModified());
        assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE));
        assertTrue(res.getLinks().stream()
                .anyMatch(link -> link.getRel().equals("describedby") &&
                    link.getUri().toString().endsWith("?ext=description")));
        assertTrue(res.getLinks().stream()
                .anyMatch(link -> link.getRel().equals("canonical") &&
                    !link.getUri().toString().endsWith("?ext=description")));
        assertAll(checkLdpType(res, LDP.NonRDFSource));
    }

    @Test
    public void testGetAcl() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResource.hasAcl()).thenReturn(true);
        when(mockLdpRequest.getExt()).thenReturn("acl");

        final GetHandler handler = new GetHandler(mockLdpRequest, mockBundler, false, baseUrl);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo());
        assertAll(checkLdpType(res, LDP.RDFSource));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PATCH)));
    }
}
