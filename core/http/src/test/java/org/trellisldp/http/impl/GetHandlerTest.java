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

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.asList;
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
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
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
import org.trellisldp.http.core.EtagGenerator;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;
import org.trellisldp.vocabulary.SKOS;

/**
 * @author acoburn
 */
public class GetHandlerTest extends BaseTestHandler {

    private static final EtagGenerator etagGenerator = new EtagGenerator() { };

    private BinaryMetadata testBinary = BinaryMetadata.builder(rdf.createIRI("file:///testResource.txt"))
        .mimeType("text/plain").build();

    @Test
    public void testGetLdprs() {
        when(mockTrellisRequest.getBaseUrl()).thenReturn("http://example.org");

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo(), "Incorrect response type!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertEquals(from(time), res.getLastModified(), "Incorrect modified date!");
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incompatible media type!");
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incompatible media type!");
        assertNull(res.getHeaderString(PREFERENCE_APPLIED), "Unexpected Preference-Applied header!");
        assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
        assertAll("Check LDP type Link headers", checkLdpType(res, LDP.RDFSource));
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));

        final EntityTag etag = res.getEntityTag();
        assertTrue(etag.isWeak(), "ETag isn't weak for an RDF document!");
        assertEquals(etagGenerator.getValue(mockResource), etag.getValue(), "Unexpected ETag value!");

        final List<Object> varies = res.getHeaders().get(VARY);
        assertFalse(varies.contains(RANGE), "Unexpected Vary: range header!");
        assertTrue(varies.contains(ACCEPT_DATETIME), "Unexpected Vary: accept-datetime header!");
        assertTrue(varies.contains(PREFER), "Unexpected Vary: prefer header!");
    }

    @Test
    public void testGetPreferLdprs() {
        when(mockIoService.supportedUpdateSyntaxes()).thenReturn(singletonList(LD_PATCH));
        when(mockTrellisRequest.getPrefer())
            .thenReturn(Prefer.valueOf("return=representation; include=\"http://www.w3.org/ns/ldp#PreferContainment"));

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo(), "Incorrect response status!");
        assertEquals("text/ldpatch", res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertEquals("return=representation", res.getHeaderString(PREFERENCE_APPLIED), "Incorrect Preference-Applied!");
        assertEquals(from(time), res.getLastModified(), "Incorrect modified header!");
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incompatible content-type header!");
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incompatible content-type header!");
        assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
        assertAll("Check LDP type headers", checkLdpType(res, LDP.RDFSource));
    }

    @Test
    public void testGetVersionedLdprs() {
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, true, true, true, null, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(from(time), res.getLastModified(), "Incorrect modified date!");
        assertEquals(ofInstant(time, UTC).format(RFC_1123_DATE_TIME), res.getHeaderString(MEMENTO_DATETIME),
                "Incorrect Memento-Datetime header!");
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incompatible Content-Type header!");
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incompatible Content-Type header!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
        assertNull(res.getHeaderString(ACCEPT_PATCH), "Unexpected Accept-Patch header!");
        assertNull(res.getHeaderString(PREFERENCE_APPLIED), "Unexpected Preference-Applied header!");
        assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
        assertAll("Check LDP type headers", checkLdpType(res, LDP.RDFSource));
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS)));

        final EntityTag etag = res.getEntityTag();
        assertTrue(etag.isWeak(), "ETag header is not weak for an RDF resource!");
        assertEquals(etagGenerator.getValue(mockResource), etag.getValue(), "Unexpected ETag value!");

        final List<Object> varies = res.getHeaders().get(VARY);
        assertTrue(varies.contains(PREFER), "Missing Vary: prefer header!");
        assertFalse(varies.contains(RANGE), "Unexpected Vary: range header!");
        assertFalse(varies.contains(ACCEPT_DATETIME), "Unexpected Vary: accept-datetime header!");
    }

    @Test
    public void testExtraLinks() {
        final String inbox = "http://ldn.example.com/inbox";
        final String annService = "http://annotation.example.com/resource";

        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.of(
                new SimpleEntry<>(annService, OA.annotationService.getIRIString()),
                new SimpleEntry<>(SKOS.Concept.getIRIString(), "type"),
                new SimpleEntry<>(inbox, "inbox")));

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, baseUrl);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo(), "Incorrect response code!");
        assertTrue(res.getLinks().stream().anyMatch(hasType(SKOS.Concept)), "Missing extra type link header!");
        assertTrue(res.getLinks().stream().anyMatch(hasLink(rdf.createIRI(inbox), "inbox")), "No ldp:inbox header!");
        assertTrue(res.getLinks().stream().anyMatch(hasLink(rdf.createIRI(annService),
                        OA.annotationService.getIRIString())), "Missing extra annotationService link header!");
    }

    @Test
    public void testNotAcceptableLdprs() {
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(APPLICATION_JSON_TYPE));

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, baseUrl);

        final Response res = assertThrows(NotAcceptableException.class, () ->
                handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource))),
                "No error thrown when given an unaccepable media type!").getResponse();
        assertEquals(NOT_ACCEPTABLE, res.getStatusInfo(), "Incorrect response code!");
    }

    @Test
    public void testMinimalLdprs() {
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(APPLICATION_LD_JSON_TYPE));
        when(mockTrellisRequest.getPrefer()).thenReturn(Prefer.valueOf("return=minimal"));

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, baseUrl);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(from(time), res.getLastModified(), "Incorrect modified date!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertEquals("return=minimal", res.getHeaderString(PREFERENCE_APPLIED), "Incorrect Preference-Applied header!");
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()), "Incompatible media type!");
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE), "Incompatible media type!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
        assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
        assertAll("Check LDP type headers", checkLdpType(res, LDP.RDFSource));
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));

        final EntityTag etag = res.getEntityTag();
        assertTrue(etag.isWeak(), "ETag header isn't weak for LDP-RS!");
        assertEquals(etagGenerator.getValue(mockResource), etag.getValue(), "Unexpected ETag value!");

        final List<Object> varies = res.getHeaders().get(VARY);
        assertTrue(varies.contains(ACCEPT_DATETIME), "Missing Vary: accept-datetime header!");
        assertTrue(varies.contains(PREFER), "Missing Vary: prefer header!");
        assertFalse(varies.contains(RANGE), "Unexpected Vary: range header!");
    }

    @Test
    public void testGetLdpc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockIoService.supportedWriteSyntaxes()).thenReturn(Stream.of(TURTLE, NTRIPLES, JSONLD).collect(toList()));
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(
                    MediaType.valueOf(APPLICATION_LD_JSON + "; profile=\"" + compacted.getIRIString() + "\"")));

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, null);

        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();
        assertEquals(OK, res.getStatusInfo(), "Incorrect response code");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertEquals(from(time), res.getLastModified(), "Incorrect modified date!");
        assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()), "Incompatible content-type header!");
        assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE), "Incompatible content-type header!");
        assertFalse(res.getLinks().stream().map(Link::getRel).anyMatch(isEqual("describes")),
                "Unexpected rel=describes");
        assertFalse(res.getLinks().stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected rel=describedby");
        assertFalse(res.getLinks().stream().map(Link::getRel).anyMatch(isEqual("canonical")),
                "Unexpected rel=canonical");
        assertNull(res.getHeaderString(PREFERENCE_APPLIED), "Unexpected Preference-Applied header!");
        assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
        assertAll("Check LDP type link headers", checkLdpType(res, LDP.Container));
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH, POST)));

        final String acceptPost = res.getHeaderString(ACCEPT_POST);
        assertNotNull(acceptPost, "Missing Accept-Post header!");
        assertTrue(acceptPost.contains("text/turtle"), "Accept-Post doesn't contain Turtle!");
        assertTrue(acceptPost.contains(APPLICATION_LD_JSON), "Accept-Post doesn't advertise JSON-LD!");
        assertTrue(acceptPost.contains(APPLICATION_N_TRIPLES), "Accept-Post doesn't advertise N-Triples!");

        final EntityTag etag = res.getEntityTag();
        assertTrue(etag.isWeak(), "ETag isn't weak for RDF!");
        assertEquals(etagGenerator.getValue(mockResource), etag.getValue(), "Incorrect ETag value for LDP-C!");

        final List<Object> varies = res.getHeaders().get(VARY);
        assertTrue(varies.contains(ACCEPT_DATETIME), "Missing Vary: accept-datetime header!");
        assertTrue(varies.contains(PREFER), "Missing Vary: prefer header!");
        assertFalse(varies.contains(RANGE), "Unexpected Vary: range header!");
    }

    @Test
    public void testGetHTML() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockTrellisRequest.getAcceptableMediaTypes())
            .thenReturn(singletonList(MediaType.valueOf(RDFA.mediaType())));

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertTrue(TEXT_HTML_TYPE.isCompatible(res.getMediaType()), "Incompatible content-type!");
        assertTrue(res.getMediaType().isCompatible(TEXT_HTML_TYPE), "Incompatible content-type!");
        assertNull(res.getHeaderString(PREFERENCE_APPLIED), "Unexpected Preference-Applied header!");
        assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
        assertAll("Check LDP type link headers", checkLdpType(res, LDP.Container));
    }

    @Test
    public void testGetBinaryDescription() {
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertAll("Check binary description", checkBinaryDescription(res));
    }

    @Test
    public void testGetBinaryDescription2() {
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getExt()).thenReturn(DESCRIPTION);

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, null);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertAll("Check binary description", checkBinaryDescription(res));
    }

    private Stream<Executable> checkBinaryDescription(final Response res) {
        return Stream.of(
                () -> assertEquals(OK, res.getStatusInfo(), "Incorrect response code!"),
                () -> assertEquals(-1, res.getLength(), "Incorrect response size!"),
                () -> assertEquals(from(time), res.getLastModified(), "Incorrect modified date!"),
                () -> assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incompatible content-type!"),
                () -> assertTrue(res.getLinks().stream()
                        .anyMatch(link -> link.getRel().equals("describes") &&
                            !link.getUri().toString().endsWith("?ext=description")), "MIssing rel=describes header!"),
                () -> assertTrue(res.getLinks().stream()
                        .anyMatch(link -> link.getRel().equals("canonical") &&
                            link.getUri().toString().endsWith("?ext=description")), "Missing rel=canonical header!"),
                () -> assertAll("Check LDP type link headers", checkLdpType(res, LDP.RDFSource)));
    }

    @Test
    public void testGetBinary() {
        when(mockResource.getBinaryMetadata()).thenReturn(of(testBinary));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(WILDCARD_TYPE));

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, baseUrl);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(-1, res.getLength(), "Incorrect response length!");
        assertEquals(from(time), res.getLastModified(), "Incorrect content-type header!");
        assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE), "Incorrect content-type header!");
        assertTrue(res.getLinks().stream()
                .anyMatch(link -> link.getRel().equals("describedby") &&
                    link.getUri().toString().endsWith("?ext=description")), "Missing rel=describedby header!");
        assertTrue(res.getLinks().stream()
                .anyMatch(link -> link.getRel().equals("canonical") &&
                    !link.getUri().toString().endsWith("?ext=description")), "Missing rel=canonical header!");
        assertAll("Check LDP type link headers", checkLdpType(res, LDP.NonRDFSource));
    }

    @Test
    public void testGetAcl() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResource.hasAcl()).thenReturn(true);
        when(mockTrellisRequest.getExt()).thenReturn("acl");

        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, true, null, baseUrl);
        final Response res = handler.getRepresentation(handler.standardHeaders(handler.initialize(mockResource)))
            .build();

        assertEquals(OK, res.getStatusInfo(), "Incorrect response code!");
        assertAll("Check LDP type link headers", checkLdpType(res, LDP.RDFSource));
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PATCH)));
    }

    @Test
    public void testFilterMementoLink() {
        final Instant time1 = now();
        final Instant time2 = time1.plusSeconds(10L);
        final SortedSet<Instant> mementos = new TreeSet<>();
        mementos.add(time1);
        mementos.add(time2);
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, false,
                null, baseUrl);
        final Response res = handler.addMementoHeaders(handler.standardHeaders(handler.initialize(mockResource)),
                mementos).build();

        assertAll("Check MementoHeaders",
                checkMementoLinks(res.getStringHeaders().get(LINK).stream().map(Link::valueOf).collect(toList()), 2L));
    }

    @Test
    public void testLimitMementoHeaders() {
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
        final GetHandler handler = new GetHandler(mockTrellisRequest, mockBundler, false, true, false,
                null, baseUrl);
        final Response res = handler.addMementoHeaders(handler.standardHeaders(handler.initialize(mockResource)),
                mementos).build();

        assertAll("Check MementoHeaders",
                checkMementoLinks(res.getStringHeaders().get(LINK).stream().map(Link::valueOf).collect(toList()), 2L));
    }

    private Stream<Executable> checkMementoLinks(final List<Link> links, final long mementos) {
        return Stream.of(
                () -> assertEquals(mementos, links.stream().filter(link -> link.getRels().contains("memento")).count()),
                () -> assertEquals(1L, links.stream().filter(link -> link.getRels().contains("first")).count()),
                () -> assertEquals(1L, links.stream().filter(link -> link.getRels().contains("last")).count()));
    }
}
