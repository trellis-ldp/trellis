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
package org.trellisldp.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.MAX;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Date.from;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.servlet.http.HttpServletResponse.*;
import static javax.ws.rs.HttpMethod.*;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.core.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_PUT_UNCONTAINED;
import static org.trellisldp.http.core.HttpConstants.EXT;
import static org.trellisldp.http.core.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.http.core.HttpConstants.PATCH;
import static org.trellisldp.http.core.HttpConstants.PREFER;
import static org.trellisldp.http.core.HttpConstants.RANGE;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.HttpConstants.TIMEMAP;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_N_TRIPLES;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.InvalidCardinality;
import static org.trellisldp.vocabulary.Trellis.InvalidRange;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferAudit;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.trellisldp.api.EventService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.XSD;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractTrellisHttpResourceTest extends BaseTrellisHttpResourceTest {

    private static final String ACL_PARAM = "acl";

    private static final String ERR_ACCEPT_PATCH = "Incorrect Accept-Patch header!";
    private static final String ERR_ACCEPT_POST = "Incorrect Accept-Post header!";
    private static final String ERR_CONTENT_TYPE = "Incorrect content-type: ";
    private static final String ERR_DESCRIBEDBY = "Unexpected describedby link!";
    private static final String ERR_LAST_MODIFIED = "Incorrect last-modified header!";
    private static final String ERR_LOCATION = "Incorrect Location header!";
    private static final String ERR_MEMENTO_DATETIME = "Unexpected Memento-Datetime header!";
    private static final String ERR_RESPONSE_CODE = "Unexpected response code!";
    private static final String ERR_TITLE_VALUE = "Incorrect title value!";
    private static final String ERR_HUB = "Missing rel=hub header!";

    private static final String CHECK_ALLOWED_METHODS = "Check allowed methods";
    private static final String CHECK_JSONLD_RESPONSE = "Check JSON-LD response";
    private static final String CHECK_JSONLD_SIMPLE = "Check simple JSON-LD";
    private static final String CHECK_JSONLD_STRUCTURE = "Check JSON-LD structure";
    private static final String CHECK_LDP_LINKS = "Check LDP type Link headers";
    private static final String CHECK_MEMENTO_HEADERS = "Check Memento headers";
    private static final String CHECK_NULL_HEADERS = "Check null headers";
    private static final String CHECK_VARY_HEADERS = "Check Vary headers";

    private static final String COMPACT_JSONLD
        = "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"";
    private static final String EXPANDED_JSONLD
        = "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#expanded\"";
    private static final String INSERT_TITLE = "INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}";
    private static final String TITLE_TRIPLE = "<> <http://purl.org/dc/terms/title> \"A title\" .";
    private static final String TITLE_VALUE = "A title";
    private static final String TEXT_DATA = "some different data.";
    private static final String TEST_PATH = "/test";
    private static final String EXPECTED_EXCEPTION = "Expected exception";
    private static final String DESCRIBES = "describes";
    private static final String DESCRIBEDBY = "describedby";
    private static final String HUB_PARAM = "hub";
    private static final String CREATED = "created";
    private static final String CONTEXT = "@context";
    private static final String ID = "@id";
    private static final String TITLE = "title";
    private static final String MODE = "mode";
    private static final String SELF = "self";

    private static final String VAL_VERSION = "version";
    private static final String VERSION_PARAM = "?version=1496262729";
    private static final String PATH_REL_CHILD = "/child";
    private static final String PATH_REL_GRANDCHILD = "/child_grandchild";
    private static final String GRANDCHILD_SUFFIX = "_grandchild";
    private static final String WEAK_PREFIX = "W/\"";
    private static final String PREFER_PREFIX = "return=representation; include=\"";

    /* ****************************** *
     *           HEAD Tests
     * ****************************** */
    @Test
    void testHeadDefaultType() {
        try (final Response res = target(RESOURCE_PATH).request().head()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE + res.getMediaType());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE + res.getMediaType());
        }
    }

    /* ******************************* *
     *            GET Tests
     * ******************************* */
    @Test
    void testGetJson() throws IOException {
        try (final Response res = target("/" + RESOURCE_PATH).request().accept("application/trig, application/ld+json")
                .get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(ACCEPT_POST), ERR_ACCEPT_POST);
            assertAll(CHECK_JSONLD_RESPONSE, checkJsonLdResponse(res));
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS)));
            assertAll(CHECK_VARY_HEADERS, checkVary(res, asList(ACCEPT_DATETIME, PREFER)));

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

            assertAll(CHECK_JSONLD_STRUCTURE,
                    checkJsonStructure(obj, asList(CONTEXT, TITLE), asList(MODE, CREATED)));
        }
    }

    @Test
    void testGetDefaultType() {
        try (final Response res = target(RESOURCE_PATH).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE + res.getMediaType());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE + res.getMediaType());
        }
    }

    @Test
    void testGetDefaultType2() {
        try (final Response res = target("resource").request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE + res.getMediaType());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE + res.getMediaType());
        }
    }

    @Test
    void testScrewyAcceptDatetimeHeader() {
        try (final Response res = target(RESOURCE_PATH).request().header("Accept-Datetime", "invalid time").get()) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testScrewyRange() {
        try (final Response res = target(BINARY_PATH).request().header("Range", "invalid range").get()) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetRootSlash() {
        try (final Response res = target("/").request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE + res.getMediaType());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE + res.getMediaType());
        }
    }

    @Test
    void testGetRoot() {
        try (final Response res = target("").request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), ERR_CONTENT_TYPE + res.getMediaType());
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE + res.getMediaType());
        }
    }

    @Test
    void testGetDatetime() {
        assumeTrue(getBaseUrl().startsWith("http://localhost"));
        try (final Response res = target(RESOURCE_PATH).request()
                .header(ACCEPT_DATETIME, RFC_1123_DATE_TIME.withZone(UTC).format(time)).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant(),
                    "Incorrect Memento-Datetime value!");
            assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), HUB_PARAM)), ERR_HUB);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + RESOURCE_PATH
                                        + VERSION_PARAM), SELF)), "Missing rel=self header!");
            assertFalse(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + RESOURCE_PATH), SELF)),
                    "Unexpected versionless rel=self header");
            assertNotNull(res.getHeaderString(MEMENTO_DATETIME), "Missing Memento-Datetime header!");
            assertAll(CHECK_MEMENTO_HEADERS, checkMementoHeaders(res, RESOURCE_PATH));
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testGetTrailingSlash() {
        assumeTrue(getBaseUrl().startsWith("http://localhost"));
        try (final Response res = target(RESOURCE_PATH + "/").request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(from(time), res.getLastModified(), "Incorrect modified date!");
            assertTrue(hasTimeGateLink(res, RESOURCE_PATH), "Missing rel=timegate link!");
            assertTrue(hasOriginalLink(res, RESOURCE_PATH), "Missing rel=original link!");
        }
    }

    @Test
    void testGetNoTrailingSlash() {
        assumeTrue(getBaseUrl().startsWith("http://localhost"));
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        try (final Response res = target(RESOURCE_PATH).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(from(time), res.getLastModified(), "Incorrect modified date!");
            assertTrue(hasTimeGateLink(res, RESOURCE_PATH + "/"), "Missing rel=timegate link!");
            assertTrue(hasOriginalLink(res, RESOURCE_PATH + "/"), "Missing rel=original link!");
        }
    }

    @Test
    void testGetNotModified() {
        try (final Response res = target("").request().header(IF_MODIFIED_SINCE, "Wed, 12 Dec 2018 07:28:00 GMT")
                .get()) {
            assertEquals(SC_NOT_MODIFIED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetNotModifiedInvalidDate() {
        try (final Response res = target("").request().header(IF_MODIFIED_SINCE, "Wed, 12 Dec 2017 07:28:00 GMT")
                .get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetNotModifiedInvalidSyntax() {
        try (final Response res = target("").request().header(IF_MODIFIED_SINCE, "Yesterday").get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfNoneMatchStar() {
        try (final Response res = target("").request().header(IF_NONE_MATCH, "*").get()) {
            assertEquals(SC_NOT_MODIFIED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfMatchWeak() {
        final String etag = target("").request().get().getEntityTag().getValue();
        try (final Response res = target("").request().header(IF_MATCH, WEAK_PREFIX + etag + "\"").get()) {
            assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfMatch() {
        final String etag = target("").request().get().getEntityTag().getValue();
        try (final Response res = target("").request().header(IF_MATCH, "\"" + etag + "\"").get()) {
            assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfNoneMatchFoo() {
        try (final Response res = target("").request().header(IF_NONE_MATCH, "\"blah\"").get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfNoneMatch() {
        final String etag = target("").request().get().getEntityTag().getValue();
        try (final Response res = target("").request().header(IF_NONE_MATCH, "\"" + etag + "\"").get()) {
            assertEquals(SC_NOT_MODIFIED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfNoneMatchWeak() {
        final String etag = target("").request().get().getEntityTag().getValue();
        try (final Response res = target("").request().header(IF_NONE_MATCH, WEAK_PREFIX + etag + "\"").get()) {
            assertEquals(SC_NOT_MODIFIED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfMatchBinaryWeak() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();
        try (final Response res = target(BINARY_PATH).request().header(IF_MATCH, WEAK_PREFIX + etag + "\"").get()) {
            assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfMatchBinary() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();
        try (final Response res = target(BINARY_PATH).request().header(IF_MATCH, "\"" + etag + "\"").get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfNoneMatchBinary() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();
        try (final Response res = target(BINARY_PATH).request().header(IF_NONE_MATCH, "\"" + etag + "\"").get()) {
            assertEquals(SC_NOT_MODIFIED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetIfNoneMatchWeakBinary() {
        final String etag = WEAK_PREFIX + target(BINARY_PATH).request().get().getEntityTag().getValue() + "\"";
        try (final Response res = target(BINARY_PATH).request().header(IF_NONE_MATCH, etag).get()) {
            assertEquals(SC_NOT_MODIFIED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetBinaryDescription() {
        try (final Response res = target(BINARY_PATH).request().accept("application/trig, text/turtle").get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), HUB_PARAM)), ERR_HUB);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + BINARY_PATH), SELF)),
                    "Missing rel=self header!");
            assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), ERR_CONTENT_TYPE + res.getMediaType());
            assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
            assertAll(CHECK_VARY_HEADERS, checkVary(res, asList(ACCEPT_DATETIME, PREFER)));
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS, POST)));
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testGetBinary() throws IOException {
        try (final Response res = target(BINARY_PATH).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), HUB_PARAM)), ERR_HUB);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + BINARY_PATH), SELF)),
                    ERR_HUB);
            assertAll("Check Binary response", checkBinaryResponse(res));

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            assertEquals("Some input stream", entity, "Incorrect entity value!");
        }
    }

    @Test
    void testGetBinaryHeaders() {
        try (final Response res = target(BINARY_PATH).request().head()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), HUB_PARAM)), ERR_HUB);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + BINARY_PATH), SELF)),
                    ERR_HUB);
            assertAll("Check Binary response", checkBinaryResponse(res));
            assertFalse(res.hasEntity(), "Unexpected entity!");
        }
    }

    @Test
    void testGetBinaryRange() throws IOException {
        try (final Response res = target(BINARY_PATH).request().header(RANGE, "bytes=3-10").get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll("Check Binary response", checkBinaryResponse(res));

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            assertEquals("e input", entity, "Incorrect entity value!");
        }
    }

    @Test
    void testGetBinaryErrorSkip() throws IOException {
        when(mockBinaryService.get(eq(binaryInternalIdentifier))).thenAnswer(inv -> completedFuture(mockBinary));
        when(mockBinary.getContent()).thenReturn(mockInputStream);
        when(mockInputStream.skip(anyLong())).thenThrow(new IOException());
        try (final Response res = target(BINARY_PATH).request().header(RANGE, "bytes=300-400").get()) {
            assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetVersionError() {
        try (final Response res = target(BINARY_PATH).queryParam(VAL_VERSION, "look at my history").request().get()) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetVersionNotFound() {
        try (final Response res = target(NON_EXISTENT_PATH).queryParam(VAL_VERSION, "1496260729").request().get()) {
            assertEquals(SC_NOT_FOUND, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetTimemapNotFound() {
        try (final Response res = target(NON_EXISTENT_PATH).queryParam(EXT, TIMEMAP).request().get()) {
            assertEquals(SC_NOT_FOUND, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetTimegateNotFound() {
        try (final Response res = target(NON_EXISTENT_PATH).request()
                .header(ACCEPT_DATETIME, "Wed, 16 May 2018 13:18:57 GMT").get()) {
            assertEquals(SC_NOT_ACCEPTABLE, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetBinaryVersion() throws IOException {
        try (final Response res = target(BINARY_PATH).queryParam(VAL_VERSION, timestamp).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_ALLOWED_METHODS, checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
            assertAll(CHECK_MEMENTO_HEADERS, checkMementoHeaders(res, BINARY_PATH));
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.NonRDFSource));

            assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE), ERR_CONTENT_TYPE + res.getMediaType());
            assertEquals("bytes", res.getHeaderString(ACCEPT_RANGES), "Incorrect Accept-Ranges header!");
            assertNotNull(res.getHeaderString(MEMENTO_DATETIME), "Missing Memento-Datetime header!");
            assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant(),
                    "Incorrect Memento-Datetime header value!");
            assertAll(CHECK_VARY_HEADERS, checkVary(res, singletonList(RANGE)));

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            assertEquals("Some input stream", entity, "Incorrect entity value!");
        }
    }

    @Test
    void testPrefer() throws IOException {
        try (final Response res = target(RESOURCE_PATH).request()
                .header(PREFER, PREFER_PREFIX + PreferServerManaged.getIRIString() + "\"")
                .accept(COMPACT_JSONLD).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

            assertAll(CHECK_JSONLD_STRUCTURE,
                    checkJsonStructure(obj, asList(CONTEXT, TITLE), asList(MODE, CREATED)));
            assertEquals(TITLE_VALUE, obj.get(TITLE), ERR_TITLE_VALUE);
        }
    }

    @Test
    void testPrefer2() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockResource.stream()).thenAnswer(inv -> getPreferQuads());
        try (final Response res = target(RESOURCE_PATH + "/").request().header(PREFER,
                    PREFER_PREFIX + LDP.PreferMinimalContainer.getIRIString() + "\"")
                .accept(COMPACT_JSONLD).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

            assertAll(CHECK_JSONLD_STRUCTURE, checkJsonStructure(obj, asList(CONTEXT, TITLE),
                        asList(MODE, CREATED, "contains", "member")));
            assertEquals(TITLE_VALUE, obj.get(TITLE), ERR_TITLE_VALUE);
        }
    }

    @Test
    void testPrefer3() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockResource.stream()).thenAnswer(inv -> getPreferQuads());
        try (final Response res = target(RESOURCE_PATH + "/").request()
                .header(PREFER, "return=representation; omit=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"")
                .accept(COMPACT_JSONLD).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

            assertAll(CHECK_JSONLD_STRUCTURE, checkJsonStructure(obj, asList(CONTEXT, "contains", "member"),
                        asList(TITLE, MODE, CREATED)));
        }
    }

    @Test
    void testPrefer4() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockResource.stream()).thenAnswer(inv -> getPreferQuads());
        try (final Response res = target(RESOURCE_PATH + "/").request()
                .header(PREFER, PREFER_PREFIX + PreferAccessControl.getIRIString() + "\"")
                .accept(COMPACT_JSONLD).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

            assertAll(CHECK_JSONLD_STRUCTURE, checkJsonStructure(obj,
                        asList(CONTEXT, TITLE, "contains", "member"), asList(MODE, CREATED)));
            assertEquals(TITLE_VALUE, obj.get(TITLE), ERR_TITLE_VALUE);
        }
    }

    @Test
    void testGetJsonCompact() throws IOException {
        try (final Response res = target(RESOURCE_PATH).request()
                .accept(COMPACT_JSONLD).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll("Check LDF response", checkLdfResponse(res));

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

            assertEquals(TITLE_VALUE, obj.get(TITLE), "Incorrect title property in JSON!");
            assertAll(CHECK_JSONLD_STRUCTURE,
                    checkJsonStructure(obj, asList(CONTEXT, TITLE), asList(MODE, CREATED)));
        }
    }

    @Test
    void testGetTimeMapLinkDefaultFormat() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(MediaType.valueOf(APPLICATION_LINK_FORMAT), res.getMediaType(), "Incorrect content-type!");
        }
    }

    @Test
    void testGetTimeMapLinkDefaultFormat2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        try (final Response res = target("resource").queryParam(EXT, TIMEMAP).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(MediaType.valueOf(APPLICATION_LINK_FORMAT), res.getMediaType(), "Incorrect content-type!");
        }
    }

    @Test
    void testGetTimeMapLinkInvalidFormat() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request()
                .accept("some/made-up-format").get()) {
            assertEquals(SC_NOT_ACCEPTABLE, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetTimeMapLink() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.mementos(eq(identifier))).thenReturn(completedFuture(new TreeSet<>(asList(
                ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000), time))));
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request()
                .accept(APPLICATION_LINK_FORMAT).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(MediaType.valueOf(APPLICATION_LINK_FORMAT), res.getMediaType(), "Incorrect content-type!");
            assertNull(res.getLastModified(), "Unexpected last-modified header!");
            assertAll(CHECK_MEMENTO_HEADERS, checkMementoHeaders(res, RESOURCE_PATH));
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertAll(CHECK_ALLOWED_METHODS, checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS,
                    checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES, MEMENTO_DATETIME)));

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final List<Link> entityLinks = stream(entity.split(",\n")).map(Link::valueOf).collect(toList());
            assertEquals(5L, entityLinks.size(), "Incorrect number of Link headers!");
            final List<Link> links = getLinks(res);
            final List<String> rels = asList("memento", "original", "timegate", "timemap", "first", "last");
            assertAll("Check link headers", links.stream().filter(l -> l.getRels().stream().anyMatch(rels::contains))
                    .map(l -> () -> assertTrue(entityLinks.stream().map(Link::getUri).anyMatch(l.getUri()::equals),
                            "Link not in response: " + l)));
        }
    }

    @Test
    void testGetTimeMapJsonDefault() throws IOException {
        when(mockMementoService.mementos(eq(identifier))).thenReturn(completedFuture(new TreeSet<>(asList(
                ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000), time))));
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request()
                .accept("application/ld+json").get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getLastModified(), "Incorrect last modified date!");
            assertAll(CHECK_JSONLD_SIMPLE, checkSimpleJsonLdResponse(res, LDP.RDFSource));
            assertAll(CHECK_MEMENTO_HEADERS, checkMementoHeaders(res, RESOURCE_PATH));
            assertAll(CHECK_ALLOWED_METHODS, checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS,
                    checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES, MEMENTO_DATETIME)));

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final Map<String, Object> obj = MAPPER.readValue(entity,
                    new TypeReference<Map<String, Object>>(){});

            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> graph = (List<Map<String, Object>>) obj.get("@graph");

            assertEquals(5L, graph.size(), "Incorrect @graph size!");
            assertTrue(graph.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH) &&
                        x.containsKey("timegate") && x.containsKey("timemap") && x.containsKey("memento")),
                    "Missing memento-related properties in graph for given @id");
            assertTrue(graph.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap") &&
                        x.containsKey("hasBeginning") &&
                        x.containsKey("hasEnd")),
                    "Missing hasBeginning/hasEnd properties in timemap graph!");
            assertTrue(graph.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729") &&
                        x.containsKey("hasTime")), "Missing hasTime property in timemap graph for version 1!");
            assertTrue(graph.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729") &&
                        x.containsKey("hasTime")), "Missing hasTime property in timemap graph for version 2!");
            assertTrue(graph.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH + VERSION_PARAM) &&
                        x.containsKey("hasTime")), "Missign hasTime property in timemap graph for version 3!");
        }
    }

    @Test
    void testGetTimeMapJson() throws IOException {
        when(mockMementoService.mementos(eq(identifier))).thenReturn(completedFuture(new TreeSet<>(asList(
                ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000), time))));
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request()
                .accept(EXPANDED_JSONLD).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getLastModified(), ERR_LAST_MODIFIED);
            assertAll(CHECK_JSONLD_SIMPLE, checkSimpleJsonLdResponse(res, LDP.RDFSource));
            assertAll(CHECK_MEMENTO_HEADERS, checkMementoHeaders(res, RESOURCE_PATH));
            assertAll(CHECK_ALLOWED_METHODS, checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS,
                    checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES, MEMENTO_DATETIME)));

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final List<Map<String, Object>> obj = MAPPER.readValue(entity,
                    new TypeReference<List<Map<String, Object>>>(){});

            assertEquals(5L, obj.size(), "Incorrect number of properties in timemap JSON-LD!");
            assertTrue(obj.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH) &&
                        x.containsKey("http://mementoweb.org/ns#timegate") &&
                        x.containsKey("http://mementoweb.org/ns#timemap") &&
                        x.containsKey("http://mementoweb.org/ns#memento")),
                    "Missing expected memento properties in expanded JSON-LD!");
            assertTrue(obj.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap") &&
                        x.containsKey("http://www.w3.org/2006/time#hasBeginning") &&
                        x.containsKey("http://www.w3.org/2006/time#hasEnd")),
                    "Missing hasBeginning/hasEnd properties in expanded JSON-LD!");
            assertTrue(obj.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729") &&
                        x.containsKey("http://www.w3.org/2006/time#hasTime")),
                    "Missing hasTime property in first memento!");
            assertTrue(obj.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729") &&
                        x.containsKey("http://www.w3.org/2006/time#hasTime")),
                    "Missing hasTime property in second memento!");
            assertTrue(obj.stream().anyMatch(x -> x.containsKey(ID) &&
                        x.get(ID).equals(getBaseUrl() + RESOURCE_PATH + VERSION_PARAM) &&
                        x.containsKey("http://www.w3.org/2006/time#hasTime")),
                    "Missing hasTime property in third memento!");
        }
    }

    @Test
    void testGetVersionJson() {
        try (final Response res = target(RESOURCE_PATH).queryParam(VAL_VERSION, timestamp).request()
                .accept(COMPACT_JSONLD).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(from(time), res.getLastModified(), ERR_LAST_MODIFIED);
            assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant(),
                    "Incorrect Memento-Datetime header!");
            assertAll(CHECK_JSONLD_SIMPLE, checkSimpleJsonLdResponse(res, LDP.RDFSource));
            assertAll(CHECK_MEMENTO_HEADERS, checkMementoHeaders(res, RESOURCE_PATH));
            assertAll(CHECK_ALLOWED_METHODS, checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES)));
        }
    }

    @Test
    void testGetVersionContainerJson() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.Container);
        try (final Response res = target(RESOURCE_PATH + "/").queryParam(VAL_VERSION, timestamp).request()
                .accept(COMPACT_JSONLD).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(from(time), res.getLastModified(), ERR_LAST_MODIFIED);
            assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant(),
                    "Incorrect Memento-Datetime header!");
            assertAll(CHECK_JSONLD_SIMPLE, checkSimpleJsonLdResponse(res, LDP.Container));
            assertAll(CHECK_MEMENTO_HEADERS, checkMementoHeaders(res, RESOURCE_PATH + "/"));
            assertAll(CHECK_ALLOWED_METHODS, checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES)));
        }
    }

    @Test
    void testGetNoAcl() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request().get()) {
            assertEquals(SC_NOT_FOUND, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetBinaryAcl() {
        when(mockBinaryResource.stream(eq(PreferAccessControl))).thenAnswer(inv -> Stream.of(
                    rdf.createQuad(PreferAccessControl, binaryIdentifier, ACL.mode, ACL.Read)));
        try (final Response res = target(BINARY_PATH).queryParam(EXT, ACL_PARAM).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals(DESCRIBES)),
                    "Unexpected rel=describes");
            assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals(DESCRIBEDBY)),
                    "Unexpected rel=describedby");
            assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("canonical")),
                    "Unexpected rel=canonical");
            assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("alternate")),
                    "Unexpected rel=alternate");
        }
    }

    @Test
    void testGetBinaryLinks() {
        try (final Response res = target(BINARY_PATH).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals(DESCRIBES)),
                    "Unexpected rel=describes");
            assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals(DESCRIBEDBY)),
                    "Missing rel=describedby");
            assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("canonical")),
                    "Missing rel=canonical");
            assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("alternate")),
                    "Unexpected rel=alternate");
        }
    }

    @Test
    void testGetBinaryDescriptionLinks() {
        try (final Response res = target(BINARY_PATH).request().accept("text/turtle").get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals(DESCRIBES)), "Missing rel=describes");
            assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals(DESCRIBEDBY)),
                    "Unexpected rel=describedby");
            assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("canonical")), "Missing rel=canonical");
            assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("alternate")), "Missing rel=alternate");
        }
    }

    @Test
    void testGetAclJsonCompact() throws IOException {
        when(mockResource.stream(eq(PreferAccessControl))).thenAnswer(inv -> Stream.of(
                    rdf.createQuad(PreferAccessControl, binaryIdentifier, ACL.mode, ACL.Read)));
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request()
                .accept("application/trig, " + COMPACT_JSONLD).get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertEquals(from(time), res.getLastModified(), ERR_LAST_MODIFIED);
            assertFalse(hasTimeGateLink(res, RESOURCE_PATH), "Unexpected rel=timegate link");
            assertFalse(hasOriginalLink(res, RESOURCE_PATH), "Unexpected rel=original link");
            assertTrue(res.hasEntity(), "Missing entity!");

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

            assertEquals(ACL.Control.getIRIString(), obj.get(MODE), "Incorrect ACL mode property!");
            assertAll(CHECK_JSONLD_SIMPLE, checkSimpleJsonLdResponse(res, LDP.RDFSource));
            assertAll(CHECK_ALLOWED_METHODS, checkAllowedMethods(res, asList(PATCH, GET, HEAD, OPTIONS)));
            assertAll(CHECK_VARY_HEADERS, checkVary(res, singletonList(ACCEPT_DATETIME)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_RANGES)));
            assertAll(CHECK_JSONLD_STRUCTURE, checkJsonStructure(obj, asList(CONTEXT, MODE),
                        singletonList(TITLE)));
        }
    }

    @Test
    void testGetResource() {
        try (final Response res = target(RESOURCE_PATH).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetNotFound() {
        try (final Response res = target(NON_EXISTENT_PATH).request().get()) {
            assertEquals(SC_NOT_FOUND, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetGone() {
        try (final Response res = target(DELETED_PATH).request().get()) {
            assertEquals(SC_GONE, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testGetException() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException(EXPECTED_EXCEPTION);
        }));
        try (final Response res = target(RESOURCE_PATH).request().get()) {
            assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    /* ******************************* *
     *            OPTIONS Tests
     * ******************************* */
    @Test
    void testOptionsLDPRS() {
        try (final Response res = target(RESOURCE_PATH).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS, POST)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
        }
    }

    @Test
    void testOptionsLDPNR() {
        try (final Response res = target(BINARY_PATH).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS, POST)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
        }
    }

    @Test
    void testOptionsLDPC() {
        try (final Response res = target(RESOURCE_PATH).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS, POST)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));

            final List<String> acceptPost = asList(res.getHeaderString(ACCEPT_POST).split(","));
            assertEquals(4L, acceptPost.size(), "Accept-Post header has wrong number of elements!");
            assertTrue(acceptPost.contains("text/turtle"), "Turtle missing from Accept-Post");
            assertTrue(acceptPost.contains(APPLICATION_LD_JSON), "JSON-LD missing from Accept-Post");
            assertTrue(acceptPost.contains(APPLICATION_N_TRIPLES), "N-Triples missing from Accept-Post");
            assertTrue(acceptPost.contains(WILDCARD), "Wildcard missing from Accept-Post");
        }
    }

    @Test
    void testOptionsACL() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
        }
    }

    @Test
    void testOptionsACLBinary() {
        try (final Response res = target(BINARY_PATH).queryParam(EXT, ACL_PARAM).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
        }
    }

    @Test
    void testOptionsNonexistent() {
        try (final Response res = target(NON_EXISTENT_PATH).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
        }
    }

    @Test
    void testOptionsVersionNotFound() {
        try (final Response res = target(NON_EXISTENT_PATH).queryParam(VAL_VERSION, "1496260729").request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
        }
    }

    @Test
    void testOptionsGone() {
        try (final Response res = target(DELETED_PATH).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
        }
    }

    @Test
    void testOptionsSlash() {
        try (final Response res = target(RESOURCE_PATH + "/").request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
        }
    }

    @Test
    void testOptionsTimemap() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
        }
    }

    @Test
    void testOptionsTimemapBinary() {
        try (final Response res = target(BINARY_PATH).queryParam(EXT, TIMEMAP).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
        }
    }

    @Test
    void testOptionsVersion() {
        try (final Response res = target(RESOURCE_PATH).queryParam(VAL_VERSION, timestamp).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
        }
    }

    @Test
    void testOptionsVersionBinary() {
        try (final Response res = target(BINARY_PATH).queryParam(VAL_VERSION, timestamp).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertAll(CHECK_ALLOWED_METHODS,
                    checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, POST, OPTIONS)));
            assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, singletonList(MEMENTO_DATETIME)));
            assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
        }
    }

    /* ******************************* *
     *            POST Tests
     * ******************************* */
    @Test
    void testPost() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE)),
                    eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request()
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(), ERR_LOCATION);
            assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual(DESCRIBEDBY)), ERR_DESCRIBEDBY);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostRoot() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE)), eq(MAX)))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target("").request()
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + RANDOM_VALUE, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    void testPostInvalidLink() {
        try (final Response res = target(RESOURCE_PATH).request().header(LINK, "I never really liked his friends")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostToTimemap() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request()
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostTypeWrongType() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request()
                .header(LINK, "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"non-existent\"")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostTypeMismatch() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request()
                .header(LINK, "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostConflict() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(mockResource));
        try (final Response res = target(RESOURCE_PATH).request()
                .header(LINK, "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CONFLICT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostUnknownLinkType() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request()
                .header(LINK, "<http://example.com/types/Foo>; rel=\"type\"")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostBadContent() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request()
                .post(entity("<> <http://purl.org/dc/terms/title> A title\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostToLdpRs() {
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
                .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request()
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostSlug() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        try (final Response res = target(RESOURCE_PATH + "/").request().header(SLUG, "child")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    void testPostSlugWithSlash() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + PATH_REL_GRANDCHILD))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "child/grandchild")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + CHILD_PATH + GRANDCHILD_SUFFIX, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostEncodedSlugWithSlash() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + PATH_REL_GRANDCHILD))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%2Fgrandchild")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + CHILD_PATH + GRANDCHILD_SUFFIX, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostSlugWithWhitespace() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + PATH_REL_GRANDCHILD))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "child grandchild")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + CHILD_PATH + GRANDCHILD_SUFFIX, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostEncodedSlugWithEncodedWhitespace() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + PATH_REL_GRANDCHILD))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%09grandchild")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + CHILD_PATH + GRANDCHILD_SUFFIX, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostEncodedSlugWithInvalidEncoding() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%0 grandchild")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(), ERR_LOCATION);
            assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual(DESCRIBEDBY)), ERR_DESCRIBEDBY);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostEmptySlug() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(), ERR_LOCATION);
            assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual(DESCRIBEDBY)), ERR_DESCRIBEDBY);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostEmptyEncodedSlug() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "%20%09")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(), ERR_LOCATION);
            assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual(DESCRIBEDBY)), ERR_DESCRIBEDBY);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostSlugWithHashURI() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "child#hash")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostSlugWithEncodedHashURI() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%23hash")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostSlugWithQuestionMark() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "child?foo=bar")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostSlugWithEncodedQuestionMark() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%3Ffoo=bar")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPostVersion() {
        try (final Response res = target(RESOURCE_PATH).queryParam(VAL_VERSION, timestamp).request()
                .header(SLUG, "test").post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostAcl() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request().header(SLUG, "test")
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostIndirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target().request()
                .post(entity("<> <http://purl.org/dc/terms/title> \"An indirect container\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    void testPostIndirectContainerHashUri() {
        final IRI hashResourceId = rdf.createIRI(TRELLIS_DATA_PREFIX + NEW_RESOURCE + "#foo");
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(hashResourceId));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target().request()
                .post(entity("<> <http://purl.org/dc/terms/title> \"An indirect container\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    void testPostIndirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target().request()
                .post(entity("<> <http://purl.org/dc/terms/title> \"A self-contained LDP-IC\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    void testPostIndirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(identifier));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target().request()
                .post(entity("<> <http://purl.org/dc/terms/title> \"An LDP-IC\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(3)).emit(any());
        }
    }

    @Test
    void testPostDirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target().request()
                .post(entity("<> <http://purl.org/dc/terms/title> \"An LDP-DC\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    void testPostDirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target().request()
                .post(entity("<> <http://purl.org/dc/terms/title> \"A self-contained LDP-DC\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    void testPostDirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(identifier));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target().request()
                .post(entity("<> <http://purl.org/dc/terms/title> \"An LDP-DC resource\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(3)).emit(any());
        }
    }

    @Test
    void testPostBadJsonLdSemantics() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request()
                .post(entity("{\"@id\": \"\", \"@type\": \"some type\"}", APPLICATION_LD_JSON_TYPE))) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostBadJsonLdSyntax() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request().post(entity("{\"@id:", APPLICATION_LD_JSON_TYPE))) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostConstraint() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request()
                .post(entity("<> <http://www.w3.org/ns/ldp#inbox> \"Some literal\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CONFLICT, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream()
                    .anyMatch(hasLink(InvalidRange, LDP.constrainedBy.getIRIString())), "Missing constrainedBy link");
        }
    }

    @Test
    void testPostIgnoreContains() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request()
                .post(entity("<> <http://www.w3.org/ns/ldp#contains> <./other> . ", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostNonexistent() {
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + NON_EXISTENT_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(NON_EXISTENT_PATH).request()
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NOT_FOUND, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostGone() {
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + DELETED_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        try (final Response res = target(DELETED_PATH).request()
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_GONE, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostBinary() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/newresource")),
                    any(Instant.class))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH).request().header(SLUG, "newresource")
                .post(entity("some data.", TEXT_PLAIN_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual(DESCRIBEDBY)),
                    "No describedby link!");
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.NonRDFSource));
        }
    }

    @Test
    void testPostTimeMap() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request()
                .post(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPostException() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException(EXPECTED_EXCEPTION);
        }));
        try (final Response res = target(RESOURCE_PATH).request().post(entity("", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    /* ******************************* *
     *            PUT Tests
     * ******************************* */
    @Test
    void testPutExisting() {
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual(DESCRIBEDBY)), ERR_DESCRIBEDBY);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPutTypeWrongType() {
        try (final Response res = target(RESOURCE_PATH).request()
                .header(LINK, "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"non-existent\"")
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual(DESCRIBEDBY)), ERR_DESCRIBEDBY);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPutUncontainedIndirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));
        when(mockResource.getContainer()).thenReturn(empty());
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(1)).emit(any());
        }
    }

    @Test
    void testPutUncontainedIndirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));
        when(mockResource.getContainer()).thenReturn(empty());
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            if (getConfig().getOptionalValue(CONFIG_HTTP_PUT_UNCONTAINED, Boolean.class).orElse(Boolean.FALSE)) {
                // only one event if configured with PUT-UNCONTAINED
                verify(myEventService, times(1)).emit(any());
            } else {
                verify(myEventService, times(2)).emit(any());
            }
        }
    }

    @Test
    void testPutUncontainedIndirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        final Resource mockChildResource = mock(Resource.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockResourceService.get(childIdentifier)).thenAnswer(inv -> completedFuture(mockChildResource));
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(childIdentifier));
        when(mockResource.getContainer()).thenReturn(empty());
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(1)).emit(any());
        }
    }

    @Test
    void testPutIndirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(1)).emit(any());
        }
    }

    @Test
    void testPutIndirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    void testPutIndirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        final Resource mockChildResource = mock(Resource.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockResourceService.get(childIdentifier)).thenAnswer(inv -> completedFuture(mockChildResource));
        when(mockChildResource.getIdentifier()).thenReturn(childIdentifier);
        when(mockChildResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(childIdentifier));
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    void testPutDirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(1)).emit(any());
        }
    }

    @Test
    void testPutDirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(1)).emit(any());
        }
    }

    @Test
    void testPutDirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(identifier));
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            verify(myEventService, times(1)).emit(any());
        }
    }

    @Test
    void testPutExistingBinaryDescription() {
        try (final Response res = target(BINARY_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPutExistingUnknownLink() {
        try (final Response res = target(RESOURCE_PATH).request()
                .header(LINK, "<http://example.com/types/Foo>; rel=\"type\"")
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPutExistingIgnoreProperties() {
        try (final Response res = target(RESOURCE_PATH).request().put(entity(
                        "<> <http://purl.org/dc/terms/title> \"A title\" ;"
                      + " <http://example.com/foo> <http://www.w3.org/ns/ldp#IndirectContainer> ;"
                      + " a <http://example.com/Type1>, <http://www.w3.org/ns/ldp#BasicContainer> .",
                        TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPutExistingSubclassLink() {
        try (final Response res = target(RESOURCE_PATH).request()
                .header(LINK, LDP.Container + "; rel=\"type\"")
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.Container));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPutExistingMalformed() {
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity("<> <http://purl.org/dc/terms/title \"A title\" .", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutConstraint() {
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity("<> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \"Some literal\" .",
                    TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CONFLICT, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidRange, LDP.constrainedBy.getIRIString())), "Missing constrainedBy header!");
        }
    }

    @Test
    void testPutIgnoreContains() {
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity("<> <http://www.w3.org/ns/ldp#contains> <./other> . ", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutNew() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + TEST_PATH);
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(identifier), eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH + TEST_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertEquals(getBaseUrl() + RESOURCE_PATH + TEST_PATH, res.getHeaderString(CONTENT_LOCATION), ERR_LOCATION);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPutDeleted() {
        try (final Response res = target(DELETED_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertEquals(getBaseUrl() + DELETED_PATH, res.getHeaderString(CONTENT_LOCATION), ERR_LOCATION);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPutVersion() {
        try (final Response res = target(RESOURCE_PATH).queryParam(VAL_VERSION, timestamp).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutAcl() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPutAclOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPutAclOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPutOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CONFLICT, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())),
                    "Missing constrainedBy header!");
        }
    }

    @Test
    void testPutOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        try (final Response res = target(RESOURCE_PATH).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_CONFLICT, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())),
                    "Missing constrainedBy header!");
        }
    }

    @Test
    void testPutBinary() {
        try (final Response res = target(BINARY_PATH).request().put(entity("some data.", TEXT_PLAIN_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual(DESCRIBEDBY)),
                    "No describedby link!");
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.NonRDFSource));
        }
    }

    @Test
    void testPutBinaryToACL() {
        try (final Response res = target(BINARY_PATH).queryParam(EXT, ACL_PARAM).request()
                .put(entity("some data.", TEXT_PLAIN_TYPE))) {
            assertEquals(SC_NOT_ACCEPTABLE, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfMatch() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();
        try (final Response res = target(BINARY_PATH).request().header(IF_MATCH, "\"" + etag + "\"")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfMatchWeak() {
        final String etag = target("").request().get().getEntityTag().getValue();
        try (final Response res = target("").request().header(IF_MATCH, WEAK_PREFIX + etag + "\"")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfNoneMatchEtag() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();
        try (final Response res = target(BINARY_PATH).request().header(IF_NONE_MATCH, "\"" + etag + "\"")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfNoneMatchRdfEtag() {
        final String etag = target("").request().get().getEntityTag().getValue();
        try (final Response res = target("").request().header(IF_NONE_MATCH, "\"" + etag + "\"")
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfNoneMatchRdfWeakEtag() {
        final String etag = target("").request().get().getEntityTag().getValue();
        try (final Response res = target("").request().header(IF_NONE_MATCH, WEAK_PREFIX + etag + "\"")
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfNoneMatchWeakEtag() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();
        try (final Response res = target(BINARY_PATH).request().header(IF_NONE_MATCH, WEAK_PREFIX + etag + "\"")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfNoneMatch() {
        try (final Response res = target(BINARY_PATH).request().header(IF_NONE_MATCH, "\"foo\", \"bar\"")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfMatchStar() {
        try (final Response res = target(BINARY_PATH).request().header(IF_MATCH, "*")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfMatchMultiple() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();
        try (final Response res = target(BINARY_PATH).request().header(IF_MATCH, "\"blah\", \"" + etag + "\"")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfNoneMatchStar() {
        try (final Response res = target(BINARY_PATH).request().header(IF_NONE_MATCH, "*")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutBadIfMatch() {
        try (final Response res = target(BINARY_PATH).request().header(IF_MATCH, "4db2c60044c906361ac212ae8684e8ad")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_BAD_REQUEST, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutIfUnmodified() {
        try (final Response res = target(BINARY_PATH).request()
                .header(IF_UNMODIFIED_SINCE, "Tue, 29 Aug 2017 07:14:52 GMT")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutPreconditionFailed() {
        try (final Response res = target(BINARY_PATH).request().header(IF_MATCH, "\"blahblahblah\"")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutPreconditionFailed2() {
        try (final Response res = target(BINARY_PATH).request()
                .header(IF_UNMODIFIED_SINCE, "Wed, 19 Oct 2016 10:15:00 GMT")
                .put(entity(TEXT_DATA, TEXT_PLAIN_TYPE))) {
            assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutSlash() {
        try (final Response res = target(RESOURCE_PATH + "/").request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPutTimeMap() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request()
                .put(entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPutException() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException(EXPECTED_EXCEPTION);
        }));
        try (final Response res = target(RESOURCE_PATH).request().put(entity("", TEXT_TURTLE_TYPE))) {
            assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    /* ******************************* *
     *            DELETE Tests
     * ******************************* */
    @Test
    void testDeleteExisting() {
        try (final Response res = target(RESOURCE_PATH).request().delete()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testDeleteNonexistent() {
        try (final Response res = target(NON_EXISTENT_PATH).request().delete()) {
            assertEquals(SC_NOT_FOUND, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testDeleteDeleted() {
        try (final Response res = target(DELETED_PATH).request().delete()) {
            assertEquals(SC_GONE, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testDeleteVersion() {
        try (final Response res = target(RESOURCE_PATH).queryParam(VAL_VERSION, timestamp).request().delete()) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testDeleteNonExistant() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + TEST_PATH);
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(identifier), eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH + TEST_PATH).request().delete()) {
            assertEquals(SC_NOT_FOUND, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testDeleteWithChildren() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockVersionedResource.stream(eq(LDP.PreferContainment))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(identifier, LDP.contains, rdf.createIRI(identifier.getIRIString() + PATH_REL_CHILD))));
        try (final Response res = target(RESOURCE_PATH).request().delete()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testDeleteNoChildren1() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockVersionedResource.stream(eq(LDP.PreferContainment))).thenAnswer(inv -> Stream.empty());
        try (final Response res = target(RESOURCE_PATH).request().delete()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testDeleteNoChildren2() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockVersionedResource.stream(eq(LDP.PreferContainment))).thenAnswer(inv -> Stream.empty());
        try (final Response res = target(RESOURCE_PATH).request().delete()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testDeleteAcl() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request().delete()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testDeleteTimeMap() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request().delete()) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testDeleteSlash() {
        try (final Response res = target(RESOURCE_PATH + "/").request().delete()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Resource)), "Unexpected ldp:Resource link!");
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)), "Unexpected ldp:RDFSource link!");
            assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)), "Unexpected ldp:Container link!");
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testDeleteException() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException(EXPECTED_EXCEPTION);
        }));
        try (final Response res = target(RESOURCE_PATH).request().delete()) {
            assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    /* ********************* *
     *      PATCH tests
     * ********************* */
    @Test
    void testPatchVersion() {
        try (final Response res = target(RESOURCE_PATH).queryParam(VAL_VERSION, timestamp).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchTimeMap() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchExisting() {
        try (final Response res = target(RESOURCE_PATH).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPatchRoot() {
        try (final Response res = target().request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.BasicContainer));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPatchMissing() {
        try (final Response res = target(NON_EXISTENT_PATH).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchGone() {
        try (final Response res = target(DELETED_PATH).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchExistingIgnoreLdpType() throws IOException {
        try (final Response res = target(RESOURCE_PATH).request().header(PREFER,
                    PREFER_PREFIX + LDP.PreferMinimalContainer.getIRIString() + "\"")
                .method(PATCH, entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" ;"
                        + " <http://example.com/foo> <http://www.w3.org/ns/ldp#IndirectContainer> ;"
                        + " a <http://example.com/Type1>, <http://www.w3.org/ns/ldp#BasicContainer> } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            assertFalse(entity.contains("BasicContainer"), "Unexpected BasicContainer type!");
            assertTrue(entity.contains("Type1"), "Missing Type1 type!");
        }
    }

    @Test
    void testPatchExistingJsonLd() throws IOException {
        try (final Response res = target(RESOURCE_PATH).request()
                .header(PREFER, "return=representation")
                .header("Accept", COMPACT_JSONLD)
                .method(PATCH, entity("INSERT { <> <http://purl.org/dc/terms/title> \"A new title\" } WHERE {}",
                            APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);

            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

            assertAll(CHECK_JSONLD_STRUCTURE,
                    checkJsonStructure(obj, asList(CONTEXT, TITLE), asList(MODE, CREATED)));
            assertEquals("A new title", obj.get(TITLE), ERR_TITLE_VALUE);
        }
    }

    @Test
    void testPatchExistingBinary() {
        try (final Response res = target(BINARY_PATH).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPatchExistingResponse() throws IOException {
        try (final Response res = target(RESOURCE_PATH).request().header(PREFER,
                    PREFER_PREFIX + LDP.PreferMinimalContainer.getIRIString() + "\"")
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
            final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
            assertTrue(entity.contains(TITLE_VALUE), ERR_TITLE_VALUE);
        }
    }

    @Test
    void testPatchConstraint() {
        try (final Response res = target(RESOURCE_PATH).request()
                .method(PATCH, entity("INSERT { <> a \"Some literal\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_CONFLICT, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidRange, LDP.constrainedBy.getIRIString())),
                    "Missing constrainedBy link header!");
        }
    }

    @Test
    void testPatchToTimemap() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, TIMEMAP).request()
                .method(PATCH, entity(TITLE_TRIPLE, TEXT_TURTLE_TYPE))) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchNew() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + TEST_PATH);
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(identifier), eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        try (final Response res = target(RESOURCE_PATH + TEST_PATH).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_CREATED, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPatchAcl() {
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPatchOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        try (final Response res = target(RESOURCE_PATH).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_CONFLICT, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())),
                    "Missing constrainedBy link header!");
        }
    }

    @Test
    void testPatchOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        try (final Response res = target(RESOURCE_PATH).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_CONFLICT, res.getStatus(), ERR_RESPONSE_CODE);
            assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())),
                    "Missing constrainedBy link header!");
        }
    }

    @Test
    void testPatchAclOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPatchAclOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        try (final Response res = target(RESOURCE_PATH).queryParam(EXT, ACL_PARAM).request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource));
        }
    }

    @Test
    void testPatchInvalidContent() {
        try (final Response res = target(RESOURCE_PATH).request()
                .method(PATCH, entity("blah blah blah", "invalid/type"))) {
            assertEquals(SC_UNSUPPORTED_MEDIA_TYPE, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPatchSlash() {
        try (final Response res = target(RESOURCE_PATH + "/").request()
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME);
        }
    }

    @Test
    void testPatchNotAcceptable() {
        try (final Response res = target(RESOURCE_PATH).request().accept("text/foo")
                .method(PATCH, entity(INSERT_TITLE, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_NOT_ACCEPTABLE, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    @Test
    void testPatchException() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException(EXPECTED_EXCEPTION);
        }));
        try (final Response res = target(RESOURCE_PATH).request()
                .method(PATCH, entity("", APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    /**
     * Some other method
     */
    @Test
    void testOtherMethod() {
        try (final Response res = target(RESOURCE_PATH).request().method("FOO")) {
            assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), ERR_RESPONSE_CODE);
        }
    }

    /* ************************************ *
     *      Test cache control headers
     * ************************************ */
    @Test
    void testCacheControl() {
        try (final Response res = target(RESOURCE_PATH).request().get()) {
            assertEquals(SC_OK, res.getStatus(), ERR_RESPONSE_CODE);
            assertNotNull(res.getHeaderString(CACHE_CONTROL), "Missing Cache-Control header!");
            assertTrue(res.getHeaderString(CACHE_CONTROL).contains("max-age="),
                    "Incorrect Cache-Control: max-age value!");
        }
    }

    @Test
    void testCacheControlOptions() {
        try (final Response res = target(RESOURCE_PATH).request().options()) {
            assertEquals(SC_NO_CONTENT, res.getStatus(), ERR_RESPONSE_CODE);
            assertNull(res.getHeaderString(CACHE_CONTROL), "Unexpected Cache-Control header!");
        }
    }

    static List<Link> getLinks(final Response res) {
        // Jersey's client doesn't parse complex link headers correctly
        final List<String> links = res.getStringHeaders().get(LINK);
        if (links != null) {
            return links.stream().map(Link::valueOf).collect(toList());
        }
        return emptyList();
    }

    private boolean hasTimeGateLink(final Response res, final String path) {
        return getLinks(res).stream().anyMatch(l ->
                l.getRel().contains("timegate") && l.getUri().toString().equals(getBaseUrl() + path));
    }

    private boolean hasOriginalLink(final Response res, final String path) {
        return getLinks(res).stream().anyMatch(l ->
                l.getRel().contains("original") && l.getUri().toString().equals(getBaseUrl() + path));
    }

    static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, "type");
    }

    private Stream<Quad> getPreferQuads() {
        return Stream.of(
            rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral(TITLE_VALUE)),
            rdf.createQuad(PreferAudit, identifier, DC.created,
                rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
            rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains,
                rdf.createIRI("trellis:data/resource/child1")),
            rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains,
                rdf.createIRI("trellis:data/resource/child2")),
            rdf.createQuad(LDP.PreferContainment, identifier, LDP.contains,
                rdf.createIRI("trellis:data/resource/child3")),
            rdf.createQuad(LDP.PreferMembership, identifier, LDP.member,
                rdf.createIRI("trellis:data/resource/other")),
            rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
            rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
            rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control));
    }

    private Stream<Executable> checkVary(final Response res, final List<String> vary) {
        final List<String> vheaders = stream(res.getHeaderString(VARY).split(",")).map(String::trim).collect(toList());
        return Stream.of(RANGE, ACCEPT_DATETIME, PREFER).map(header -> vary.contains(header)
                ? () -> assertTrue(vheaders.contains(header), "Missing Vary header: " + header)
                : () -> assertFalse(vheaders.contains(header), "Unexpected Vary header: " + header));
    }

    private static Stream<IRI> ldpResourceSupertypes(final IRI ldpType) {
        return Stream.of(ldpType).filter(t -> LDP.getSuperclassOf(t) != null || LDP.Resource.equals(t))
            .flatMap(t -> Stream.concat(ldpResourceSupertypes(LDP.getSuperclassOf(t)), Stream.of(t)));
    }

    private Stream<Executable> checkLdpTypeHeaders(final Response res, final IRI ldpType) {
        final Set<String> subTypes = ldpResourceSupertypes(ldpType).map(IRI::getIRIString).collect(toSet());
        final Set<String> responseTypes = getLinks(res).stream().filter(link -> "type".equals(link.getRel()))
            .map(link -> link.getUri().toString()).collect(toSet());
        return Stream.concat(
                subTypes.stream().map(t -> () -> assertTrue(responseTypes.contains(t),
                        "Response type doesn't contain LDP subtype: " + t)),
                responseTypes.stream().map(t -> () -> assertTrue(subTypes.contains(t),
                    "Subtype " + t + " not present in response type for: " + t)));
    }

    private Stream<Executable> checkMementoHeaders(final Response res, final String path) {
        final List<Link> links = getLinks(res);
        return Stream.of(
                () -> assertTrue(links.stream().anyMatch(l -> l.getRels().contains("first") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + path + "?version=1496260729")),
                                 "Missing expected first rel=memento Link!"),
                () -> assertTrue(links.stream().anyMatch(l -> l.getRels().contains("last") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(time).equals(l.getParams().get("datetime")) &&
                    l.getUri().toString().equals(getBaseUrl() + path + VERSION_PARAM)),
                                 "Missing expected last rel=memento Link!"),
                () -> assertTrue(links.stream().anyMatch(l -> l.getRels().contains("timemap") &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp - 2000))
                        .equals(l.getParams().get("from")) &&
                    RFC_1123_DATE_TIME.withZone(UTC).format(ofEpochSecond(timestamp))
                        .equals(l.getParams().get("until")) &&
                    APPLICATION_LINK_FORMAT.equals(l.getType()) &&
                    l.getUri().toString().equals(getBaseUrl() + path + "?ext=timemap")), "Missing valid timemap link!"),
                () -> assertTrue(hasTimeGateLink(res, path), "No rel=timegate Link!"),
                () -> assertTrue(hasOriginalLink(res, path), "No rel=original Link!"));
    }

    private Stream<Executable> checkBinaryResponse(final Response res) {
        return Stream.of(
                () -> assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE), "Incompatible content-type!"),
                () -> assertNotNull(res.getHeaderString(ACCEPT_RANGES), "Missing Accept-Ranges header!"),
                () -> assertNull(res.getHeaderString(MEMENTO_DATETIME), ERR_MEMENTO_DATETIME),
                () -> assertAll(CHECK_VARY_HEADERS, checkVary(res, asList(RANGE, ACCEPT_DATETIME))),
                () -> assertAll(CHECK_ALLOWED_METHODS,
                                checkAllowedMethods(res, asList(PUT, DELETE, GET, HEAD, OPTIONS))),
                () -> assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.NonRDFSource)));
    }

    private Stream<Executable> checkJsonLdResponse(final Response res) {
        return Stream.of(
                () -> assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()),
                                 "Incorrect JSON-LD content-type!"),
                () -> assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE),
                                 "Incompatible JSON-LD content-type!"),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), HUB_PARAM)), ERR_HUB),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + RESOURCE_PATH),
                                                                         SELF)), "Missing rel=self Link header!"),
                () -> assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH),
                () -> assertTrue(res.hasEntity(), "Missing JSON-LD entity!"));
    }

    private Stream<Executable> checkNullHeaders(final Response res, final List<String> headers) {
        return headers.stream().map(h -> () -> assertNull(res.getHeaderString(h), "Unexpected header: " + h));
    }

    private Stream<Executable> checkJsonStructure(final Map<String, Object> obj, final List<String> include,
            final List<String> omit) {
        return Stream.concat(
                include.stream().map(key ->
                    () -> assertTrue(obj.containsKey(key), "JSON-LD didn't contain expected key: " + key)),
                omit.stream().map(key ->
                    () -> assertFalse(obj.containsKey(key), "JSON-LD contained extraneous key: " + key)));
    }

    private Stream<Executable> checkSimpleJsonLdResponse(final Response res, final IRI ldpType) {
        return Stream.of(
                () -> assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()),
                                 "Incompatible JSON-LD content-type: " + res.getMediaType()),
                () -> assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE),
                                 "Incorrect JSON-LD content-type: " + res.getMediaType()),
                () -> assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, ldpType)));
    }

    private Stream<Executable> checkLdfResponse(final Response res) {
        return Stream.of(
                () -> assertEquals(from(time), res.getLastModified(), "Incorrect modification date!"),
                () -> assertTrue(hasTimeGateLink(res, RESOURCE_PATH), "Missing rel=timegate link!"),
                () -> assertTrue(hasOriginalLink(res, RESOURCE_PATH), "Missing rel=original link!"),
                () -> assertAll(CHECK_ALLOWED_METHODS,
                                checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS))),
                () -> assertAll(CHECK_VARY_HEADERS, checkVary(res, asList(ACCEPT_DATETIME, PREFER))),
                () -> assertAll(CHECK_NULL_HEADERS, checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_RANGES))),
                () -> assertAll(CHECK_JSONLD_RESPONSE, checkJsonLdResponse(res)),
                () -> assertAll(CHECK_LDP_LINKS, checkLdpTypeHeaders(res, LDP.RDFSource)));
    }

    private Stream<Executable> checkAllowedMethods(final Response res, final List<String> expected) {
        final Set<String> actual = res.getAllowedMethods();
        return Stream.concat(
                actual.stream().map(method -> () -> assertTrue(expected.contains(method), "Method " + method
                        + " was not present in the list of expected methods!")),
                expected.stream().map(method -> () -> assertTrue(actual.contains(method), "Method " + method
                        + " was not in the response header!")));
    }
}
