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

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Date.from;
import static java.util.Optional.of;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.Syntax.LD_PATCH;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.core.HttpConstants.DESCRIPTION;
import static org.trellisldp.http.core.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.http.core.HttpConstants.PATCH;
import static org.trellisldp.http.core.HttpConstants.PREFER;
import static org.trellisldp.http.core.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.core.HttpConstants.RANGE;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_N_TRIPLES;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.JSONLD.compacted;

import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
class GetHandlerTest extends BaseTestHandler {

    private static final String ERR_ACCEPT_PATCH = "Incorrect Accept-Patch header!";
    private static final String ERR_ACCEPT_RANGES = "Unexpected Accept-Ranges header!";
    private static final String ERR_CONTENT_TYPE = "Incompatible content-type header!";
    private static final String ERR_LAST_MODIFIED = "Incorrect last modified date!";
    private static final String ERR_PREFERENCE_APPLIED = "Unexpected Preference-Applied header!";
    private static final String ERR_VARY_ACCEPT_DATETIME = "Unexpected Vary: accept-datetime header!";
    private static final String ERR_VARY_PREFER = "Unexpected Vary: prefer header!";
    private static final String ERR_VARY_RANGE = "Unexpected Vary: range header!";
    private static final String ERR_ACCEPT_POST = "Unexpected Accept-Post header!";

    private static final String EXT_DESCRIPTION = "?ext=description";
    private static final String CHECK_ALLOW = "Check Allow headers";

    private final BinaryMetadata testBinary = BinaryMetadata.builder(rdf.createIRI("file:///testResource.txt"))
        .mimeType("text/plain").build();

    @Test
    void testGetLdprs() {
        when(mockTrellisRequest.getBaseUrl()).thenReturn("http://example.org");

        final GetConfiguration config = new GetConfiguration(false, true, true, null, null);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertEquals(from(time), res.getLastModified(), ERR_LAST_MODIFIED);
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE);
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE);
            assertNull(res.getHeaderString(PREFERENCE_APPLIED), ERR_PREFERENCE_APPLIED);
            assertNull(res.getHeaderString(ACCEPT_RANGES), ERR_ACCEPT_RANGES);
            assertNull(res.getHeaderString(ACCEPT_POST), ERR_ACCEPT_POST);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource));
            assertAll(CHECK_ALLOW, checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));

            final EntityTag etag = res.getEntityTag();
            assertTrue(etag.isWeak(), "ETag isn't weak for an RDF document!");
            assertEquals(sha256Hex(mockResource.getRevision()), etag.getValue(), "Unexpected ETag value!");

            final List<String> varies = getVaryHeaders(res);
            assertFalse(varies.contains(RANGE), ERR_VARY_RANGE);
            assertTrue(varies.contains(ACCEPT_DATETIME), ERR_VARY_ACCEPT_DATETIME);
            assertTrue(varies.contains(PREFER), ERR_VARY_PREFER);
        }
    }

    @Test
    void testGetPreferLdprs() {
        when(mockIoService.supportedUpdateSyntaxes()).thenReturn(singletonList(LD_PATCH));
        when(mockTrellisRequest.getPrefer())
            .thenReturn(Prefer.valueOf("return=representation; include=\"http://www.w3.org/ns/ldp#PreferContainment"));

        final GetConfiguration config = new GetConfiguration(false, true, true, null, null);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals("text/ldpatch", res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertEquals("return=representation", res.getHeaderString(PREFERENCE_APPLIED),
                    "Incorrect Preference-Applied!");
            assertEquals(from(time), res.getLastModified(), ERR_LAST_MODIFIED);
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE);
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE);
            assertNull(res.getHeaderString(ACCEPT_RANGES), ERR_ACCEPT_RANGES);
            assertNull(res.getHeaderString(ACCEPT_POST), ERR_ACCEPT_POST);
            assertAll("Check LDP type headers", checkLdpType(res, LDP.RDFSource));
        }
    }

    @Test
    void testGetVersionedLdprs() {
        final GetConfiguration config = new GetConfiguration(true, true, true, null, null);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(from(time), res.getLastModified(), ERR_LAST_MODIFIED);
            assertEquals(ofInstant(time, UTC).format(RFC_1123_DATE_TIME), res.getHeaderString(MEMENTO_DATETIME),
                    "Incorrect Memento-Datetime header!");
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incompatible Content-Type header!");
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incompatible Content-Type header!");
            assertNull(res.getHeaderString(ACCEPT_POST), ERR_ACCEPT_POST);
            assertNull(res.getHeaderString(ACCEPT_PATCH), "Unexpected Accept-Patch header!");
            assertNull(res.getHeaderString(PREFERENCE_APPLIED), ERR_PREFERENCE_APPLIED);
            assertNull(res.getHeaderString(ACCEPT_RANGES), ERR_ACCEPT_RANGES);
            assertAll("Check LDP type headers", checkLdpType(res, LDP.RDFSource));
            assertAll(CHECK_ALLOW, checkAllowHeader(res, asList(GET, HEAD, OPTIONS)));

            final EntityTag etag = res.getEntityTag();
            assertTrue(etag.isWeak(), "ETag header is not weak for an RDF resource!");
            assertEquals(sha256Hex(mockResource.getRevision()), etag.getValue(), "Unexpected ETag value!");

            final List<String> varies = getVaryHeaders(res);
            assertTrue(varies.contains(PREFER), "Missing Vary: prefer header!");
            assertFalse(varies.contains(RANGE), ERR_VARY_RANGE);
            assertFalse(varies.contains(ACCEPT_DATETIME), ERR_VARY_ACCEPT_DATETIME);
        }
    }

    @Test
    void testExtraLinks() {
        final String inbox = "http://ldn.example.com/inbox";
        final String annService = "http://annotation.example.com/resource";

        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.of(
                new SimpleEntry<>(annService, OA.annotationService.getIRIString()),
                new SimpleEntry<>(SKOS.Concept.getIRIString(), "type"),
                new SimpleEntry<>(inbox, "inbox")));

        final GetConfiguration config = new GetConfiguration(false, true, true, null, baseUrl);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertTrue(res.getLinks().stream().anyMatch(hasType(SKOS.Concept)), "Missing extra type link header!");
            assertTrue(res.getLinks().stream().anyMatch(hasLink(rdf.createIRI(inbox), "inbox")),
                    "No ldp:inbox header!");
            assertTrue(res.getLinks().stream().anyMatch(hasLink(rdf.createIRI(annService),
                            OA.annotationService.getIRIString())), "Missing extra annotationService link header!");
        }
    }

    @Test
    void testNotAcceptableLdprs() {
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(APPLICATION_JSON_TYPE));

        final GetConfiguration config = new GetConfiguration(false, true, true, null, baseUrl);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);

        try (final Response res = assertThrows(NotAcceptableException.class, () ->
                handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource))),
                "No error thrown when given an unaccepable media type!").getResponse()) {
            assertEquals(NOT_ACCEPTABLE, res.getStatusInfo(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testMinimalLdprs() {
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(APPLICATION_LD_JSON_TYPE));
        when(mockTrellisRequest.getPrefer()).thenReturn(Prefer.valueOf("return=minimal"));

        final GetConfiguration config = new GetConfiguration(false, true, true, null, baseUrl);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(from(time), res.getLastModified(), ERR_LAST_MODIFIED);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertEquals("return=minimal", res.getHeaderString(PREFERENCE_APPLIED),
                    "Incorrect Preference-Applied header!");
            assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE);
            assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE), ERR_CONTENT_TYPE);
            assertNull(res.getHeaderString(ACCEPT_POST), ERR_ACCEPT_POST);
            assertNull(res.getHeaderString(ACCEPT_RANGES), ERR_ACCEPT_RANGES);
            assertAll("Check LDP type headers", checkLdpType(res, LDP.RDFSource));
            assertAll(CHECK_ALLOW, checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));

            final EntityTag etag = res.getEntityTag();
            assertTrue(etag.isWeak(), "ETag header isn't weak for LDP-RS!");
            assertEquals(sha256Hex(mockResource.getRevision()), etag.getValue(), "Unexpected ETag value!");

            final List<String> varies = getVaryHeaders(res);
            assertTrue(varies.contains(ACCEPT_DATETIME), "Missing Vary: accept-datetime header!");
            assertTrue(varies.contains(PREFER), "Missing Vary: prefer header!");
            assertFalse(varies.contains(RANGE), ERR_VARY_RANGE);
        }
    }

    @Test
    void testGetLdpc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockIoService.supportedWriteSyntaxes()).thenReturn(Stream.of(TURTLE, NTRIPLES, JSONLD).collect(toList()));
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(
                    MediaType.valueOf(APPLICATION_LD_JSON + "; profile=\"" + compacted.getIRIString() + "\"")));

        final GetConfiguration config = new GetConfiguration(false, true, true, null, null);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);

        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertEquals(from(time), res.getLastModified(), ERR_LAST_MODIFIED);
            assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE);
            assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE), ERR_CONTENT_TYPE);
            assertFalse(res.getLinks().stream().map(Link::getRel).anyMatch(isEqual("describes")),
                    "Unexpected rel=describes");
            assertFalse(res.getLinks().stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                    "Unexpected rel=describedby");
            assertFalse(res.getLinks().stream().map(Link::getRel).anyMatch(isEqual("canonical")),
                    "Unexpected rel=canonical");
            assertNull(res.getHeaderString(PREFERENCE_APPLIED), ERR_PREFERENCE_APPLIED);
            assertNull(res.getHeaderString(ACCEPT_RANGES), ERR_ACCEPT_RANGES);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.Container));
            assertAll(CHECK_ALLOW, checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH, POST)));

            final String acceptPost = res.getHeaderString(ACCEPT_POST);
            assertNotNull(acceptPost, "Missing Accept-Post header!");
            assertTrue(acceptPost.contains("text/turtle"), "Accept-Post doesn't contain Turtle!");
            assertTrue(acceptPost.contains(APPLICATION_LD_JSON), "Accept-Post doesn't advertise JSON-LD!");
            assertTrue(acceptPost.contains(APPLICATION_N_TRIPLES), "Accept-Post doesn't advertise N-Triples!");

            final EntityTag etag = res.getEntityTag();
            assertTrue(etag.isWeak(), "ETag isn't weak for RDF!");
            assertEquals(sha256Hex(mockResource.getRevision()), etag.getValue(), "Incorrect ETag value for LDP-C!");

            final List<String> varies = getVaryHeaders(res);
            assertTrue(varies.contains(ACCEPT_DATETIME), "Missing Vary: accept-datetime header!");
            assertTrue(varies.contains(PREFER), "Missing Vary: prefer header!");
            assertFalse(varies.contains(RANGE), ERR_VARY_RANGE);
        }
    }

    @Test
    void testGetHTML() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockTrellisRequest.getAcceptableMediaTypes())
            .thenReturn(singletonList(MediaType.valueOf(RDFA.mediaType())));

        final GetConfiguration config = new GetConfiguration(false, true, true, null, null);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertTrue(TEXT_HTML_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE);
            assertTrue(res.getMediaType().isCompatible(TEXT_HTML_TYPE), ERR_CONTENT_TYPE);
            assertNull(res.getHeaderString(PREFERENCE_APPLIED), ERR_PREFERENCE_APPLIED);
            assertNull(res.getHeaderString(ACCEPT_RANGES), ERR_ACCEPT_RANGES);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.Container));
        }
    }

    @Test
    void testGetBinaryDescription() {
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);

        final GetConfiguration config = new GetConfiguration(false, true, true, null, null);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertAll("Check binary description", checkBinaryDescription(res));
        }
    }

    @Test
    void testGetBinaryDescription2() {
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getExt()).thenReturn(DESCRIPTION);

        final GetConfiguration config = new GetConfiguration(false, true, true, null, null);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertAll("Check binary description", checkBinaryDescription(res));
        }
    }

    private Stream<Executable> checkBinaryDescription(final Response res) {
        return Stream.of(
                () -> assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE),
                () -> assertEquals(-1, res.getLength(), "Incorrect response size!"),
                () -> assertEquals(from(time), res.getLastModified(), ERR_LAST_MODIFIED),
                () -> assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE),
                () -> assertTrue(res.getLinks().stream()
                        .anyMatch(link -> link.getRel().equals("describes") &&
                            !link.getUri().toString().endsWith(EXT_DESCRIPTION)), "MIssing rel=describes header!"),
                () -> assertTrue(res.getLinks().stream()
                        .anyMatch(link -> link.getRel().equals("canonical") &&
                            link.getUri().toString().endsWith(EXT_DESCRIPTION)), "Missing rel=canonical header!"),
                () -> assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource)));
    }

    @Test
    void testGetBinary() {
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(WILDCARD_TYPE));

        final GetConfiguration config = new GetConfiguration(false, true, true, null, baseUrl);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(-1, res.getLength(), "Incorrect response length!");
            assertEquals(from(time), res.getLastModified(), "Incorrect content-type header!");
            assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE), "Incorrect content-type header!");
            assertTrue(res.getLinks().stream().anyMatch(link -> link.getRel().equals("describedby") &&
                        link.getUri().toString().endsWith(EXT_DESCRIPTION)), "Missing rel=describedby header!");
            assertTrue(res.getLinks().stream().anyMatch(link -> link.getRel().equals("canonical") &&
                        !link.getUri().toString().endsWith(EXT_DESCRIPTION)), "Missing rel=canonical header!");
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.NonRDFSource));
        }
    }

    @Test
    void testGetAcl() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                    rdf.createQuad(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Read)));
        when(mockTrellisRequest.getExt()).thenReturn("acl");

        final GetConfiguration config = new GetConfiguration(false, true, true, null, baseUrl);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
                        .toCompletableFuture().join().build()) {
            assertEquals(OK, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LINK_TYPES, checkLdpType(res, LDP.RDFSource));
            assertAll(CHECK_ALLOW, checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PATCH)));
        }
    }

    @Test
    void testFilterMementoLink() {
        final Instant time1 = now();
        final Instant time2 = time1.plusSeconds(10L);
        final SortedSet<Instant> mementos = new TreeSet<>();
        mementos.add(time1);
        mementos.add(time2);
        final GetConfiguration config = new GetConfiguration(false, true, false, null, baseUrl);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.addMementoHeaders(handler.standardHeaders(handler.initialize(mockResource)),
                mementos).build()) {
            assertAll("Check MementoHeaders",
                    checkMementoLinks(res.getStringHeaders().get(LINK).stream().map(Link::valueOf).collect(toList())));
        }
    }

    @Test
    void testLimitMementoHeaders() {
        final Instant time1 = now();
        final Instant time2 = time1.plusSeconds(10L);
        final Instant time3 = time1.plusSeconds(20L);
        final Instant time4 = time1.plusSeconds(30L);
        final Instant time5 = time1.plusSeconds(40L);
        final SortedSet<Instant> mementos = new TreeSet<>();
        mementos.add(time1);
        mementos.add(time2);
        mementos.add(time3);
        mementos.add(time4);
        mementos.add(time5);
        final GetConfiguration config = new GetConfiguration(false, true, false, null, baseUrl);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, extensions, config);
        try (final Response res = handler.addMementoHeaders(handler.standardHeaders(handler.initialize(mockResource)),
                mementos).build()) {
            assertAll("Check MementoHeaders",
                    checkMementoLinks(res.getStringHeaders().get(LINK).stream().map(Link::valueOf).collect(toList())));
        }
    }

    private Stream<Executable> checkMementoLinks(final List<Link> links) {
        return Stream.of(
                () -> assertEquals(2L, links.stream().filter(link -> link.getRels().contains("memento")).count()),
                () -> assertEquals(1L, links.stream().filter(link -> link.getRels().contains("first")).count()),
                () -> assertEquals(1L, links.stream().filter(link -> link.getRels().contains("last")).count()));
    }

    private List<String> getVaryHeaders(final Response res) {
        return stream(res.getHeaderString(VARY).split(",")).map(String::trim).collect(toList());
    }
}
