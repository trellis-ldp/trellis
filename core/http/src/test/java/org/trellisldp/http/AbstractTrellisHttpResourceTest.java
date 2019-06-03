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
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_GONE;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PRECONDITION_FAILED;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
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
import static org.trellisldp.http.core.HttpConstants.LINK_TEMPLATE;
import static org.trellisldp.http.core.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.http.core.HttpConstants.PATCH;
import static org.trellisldp.http.core.HttpConstants.PREFER;
import static org.trellisldp.http.core.HttpConstants.RANGE;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON_TYPE;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_N_TRIPLES;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.InvalidCardinality;
import static org.trellisldp.vocabulary.Trellis.InvalidRange;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
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
import org.trellisldp.vocabulary.Memento;
import org.trellisldp.vocabulary.XSD;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractTrellisHttpResourceTest extends BaseTrellisHttpResourceTest {

    /* ****************************** *
     *           HEAD Tests
     * ****************************** */
    @Test
    public void testHeadDefaultType() {
        final Response res = target(RESOURCE_PATH).request().head();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incorrect content-type: " + res.getMediaType());
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incorrect content-type: " + res.getMediaType());
    }

    /* ******************************* *
     *            GET Tests
     * ******************************* */
    @Test
    public void testGetJson() throws IOException {
        final Response res = target("/" + RESOURCE_PATH).request().accept("application/ld+json").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
        assertAll("Check JSON-LD Response", checkJsonLdResponse(res));
        assertAll("Check LD-Template headers", checkLdTemplateHeaders(res));
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS)));
        assertAll("Check Vary headers", checkVary(res, asList(ACCEPT_DATETIME, PREFER)));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertAll("Check JSON-LD structure",
                checkJsonStructure(obj, asList("@context", "title"), asList("mode", "created")));
    }

    @Test
    public void testGetDefaultType() {
        final Response res = target(RESOURCE_PATH).request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incorrect content-type: " + res.getMediaType());
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incorrect content-type: " + res.getMediaType());
    }

    @Test
    public void testGetDefaultType2() {
        final Response res = target("resource").request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incorrect content-type: " + res.getMediaType());
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incorrect content-type: " + res.getMediaType());
    }

    @Test
    public void testScrewyAcceptDatetimeHeader() {
        final Response res = target(RESOURCE_PATH).request().header("Accept-Datetime",
                "it's pathetic how we both").get();

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testScrewyRange() {
        final Response res = target(BINARY_PATH).request().header("Range", "say it to my face, then").get();

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetRootSlash() {
        final Response res = target("/").request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incorrect content-type: " + res.getMediaType());
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incorrect content-type: " + res.getMediaType());
    }

    @Test
    public void testGetRoot() {
        final Response res = target("").request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertTrue(TEXT_TURTLE_TYPE.isCompatible(res.getMediaType()), "Incorrect content-type: " + res.getMediaType());
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incorrect content-type: " + res.getMediaType());
    }

    @Test
    public void testGetDatetime() {
        assumeTrue(getBaseUrl().startsWith("http://localhost"));
        final Response res = target(RESOURCE_PATH).request()
            .header(ACCEPT_DATETIME, RFC_1123_DATE_TIME.withZone(UTC).format(time)).get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant(),
                "Incorrect Memento-Datetime value!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), "hub")), "Missing rel=hub header!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + RESOURCE_PATH
                                    + "?version=1496262729"), "self")), "Missing rel=self header!");
        assertFalse(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + RESOURCE_PATH), "self")),
                "Unexpected versionless rel=self header");
        assertNotNull(res.getHeaderString(MEMENTO_DATETIME), "Missing Memento-Datetime header!");
        assertAll("Check Memento headers", checkMementoHeaders(res, RESOURCE_PATH));
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testGetTrailingSlash() {
        final Response res = target(RESOURCE_PATH + "/").request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(from(time), res.getLastModified(), "Incorrect modified date!");
        assertTrue(hasTimeGateLink(res, RESOURCE_PATH), "Missing rel=timegate link!");
        assertTrue(hasOriginalLink(res, RESOURCE_PATH), "Missing rel=original link!");
    }

    @Test
    public void testGetNotModified() {
        final Response res = target("").request().header("If-Modified-Since", "Wed, 12 Dec 2018 07:28:00 GMT").get();

        assertEquals(SC_NOT_MODIFIED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetNotModifiedInvalidDate() {
        final Response res = target("").request().header("If-Modified-Since", "Wed, 12 Dec 2017 07:28:00 GMT").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetNotModifiedInvalidSyntax() {
        final Response res = target("").request().header("If-Modified-Since", "Yesterday").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfNoneMatchStar() {
        final Response res = target("").request().header("If-None-Match", "*").get();

        assertEquals(SC_NOT_MODIFIED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfMatchWeak() {
        final String etag = target("").request().get().getEntityTag().getValue();

        final Response res = target("").request().header("If-Match", "W/\"" + etag + "\"").get();

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfMatch() {
        final String etag = target("").request().get().getEntityTag().getValue();

        final Response res = target("").request().header("If-Match", "\"" + etag + "\"").get();

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfNoneMatchFoo() {
        final Response res = target("").request().header("If-None-Match", "\"blah\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfNoneMatch() {
        final String etag = target("").request().get().getEntityTag().getValue();

        final Response res = target("").request().header("If-None-Match", "\"" + etag + "\"").get();

        assertEquals(SC_NOT_MODIFIED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfNoneMatchWeak() {
        final String etag = target("").request().get().getEntityTag().getValue();

        final Response res = target("").request().header("If-None-Match", "W/\"" + etag + "\"").get();

        assertEquals(SC_NOT_MODIFIED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfMatchBinaryWeak() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();

        final Response res = target(BINARY_PATH).request().header("If-Match", "W/\"" + etag + "\"").get();

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfMatchBinary() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();

        final Response res = target(BINARY_PATH).request().header("If-Match", "\"" + etag + "\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfNoneMatchBinary() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();

        final Response res = target(BINARY_PATH).request().header("If-None-Match", "\"" + etag + "\"").get();

        assertEquals(SC_NOT_MODIFIED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetIfNoneMatchWeakBinary() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();

        final Response res = target(BINARY_PATH).request().header("If-None-Match", "W/\"" + etag + "\"").get();

        assertEquals(SC_NOT_MODIFIED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetBinaryDescription() {
        final Response res = target(BINARY_PATH).request().accept("text/turtle").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), "hub")), "Missing rel=hub header!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + BINARY_PATH), "self")),
                "Missing rel=self header!");
        assertTrue(res.getMediaType().isCompatible(TEXT_TURTLE_TYPE), "Incorrect content-type: " + res.getMediaType());
        assertNull(res.getHeaderString(ACCEPT_RANGES), "Unexpected Accept-Ranges header!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
        assertAll("Check Vary headers", checkVary(res, asList(ACCEPT_DATETIME, PREFER)));
        assertAll("Check allowed methods",
                checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS, POST)));
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testGetBinary() throws IOException {
        final Response res = target(BINARY_PATH).request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), "hub")), "Missing rel=hub header!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + BINARY_PATH), "self")),
                "Missing rel=hub header!");
        assertAll("Check Binary response", checkBinaryResponse(res));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertEquals("Some input stream", entity, "Incorrect entity value!");
    }

    @Test
    public void testGetBinaryHeaders() throws IOException {
        final Response res = target(BINARY_PATH).request().head();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), "hub")), "Missing rel=hub header!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + BINARY_PATH), "self")),
                "Missing rel=hub header!");
        assertAll("Check Binary response", checkBinaryResponse(res));
        assertFalse(res.hasEntity(), "Unexpected entity!");
    }

    @Test
    public void testGetBinaryRange() throws IOException {
        final Response res = target(BINARY_PATH).request().header(RANGE, "bytes=3-10").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check Binary response", checkBinaryResponse(res));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertEquals("e input", entity, "Incorrect entity value!");
    }

    @Test
    public void testGetBinaryErrorSkip() throws IOException {
        when(mockBinaryService.get(eq(binaryInternalIdentifier))).thenAnswer(inv -> completedFuture(mockBinary));
        when(mockBinary.getContent()).thenReturn(mockInputStream);
        when(mockInputStream.skip(anyLong())).thenThrow(new IOException());
        final Response res = target(BINARY_PATH).request().header(RANGE, "bytes=300-400").get();
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetVersionError() {
        final Response res = target(BINARY_PATH).queryParam("version", "looking at my history").request().get();
        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetVersionNotFound() {
        final Response res = target(NON_EXISTENT_PATH).queryParam("version", "1496260729").request().get();
        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetTimemapNotFound() {
        final Response res = target(NON_EXISTENT_PATH).queryParam("ext", "timemap").request().get();
        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetTimegateNotFound() {
        final Response res = target(NON_EXISTENT_PATH).request()
            .header(ACCEPT_DATETIME, "Wed, 16 May 2018 13:18:57 GMT").get();
        assertEquals(SC_NOT_ACCEPTABLE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetBinaryVersion() throws IOException {
        final Response res = target(BINARY_PATH).queryParam("version", timestamp).request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check Memento headers", checkMementoHeaders(res, BINARY_PATH));
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.NonRDFSource));

        assertTrue(res.getMediaType().isCompatible(TEXT_PLAIN_TYPE), "Incorrect content-type: " + res.getMediaType());
        assertEquals("bytes", res.getHeaderString(ACCEPT_RANGES), "Incorrect Accept-Ranges header!");
        assertNotNull(res.getHeaderString(MEMENTO_DATETIME), "Missing Memento-Datetime header!");
        assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant(),
                "Incorrect Memento-Datetime header value!");
        assertAll("Check Vary headers", checkVary(res, singletonList(RANGE)));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertEquals("Some input stream", entity, "Incorrect entity value!");
    }

    @Test
    public void testPrefer() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; include=\"" + PreferServerManaged.getIRIString() + "\"")
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertAll("Check JSON-LD structure",
                checkJsonStructure(obj, asList("@context", "title"), asList("mode", "created")));
        assertEquals("A title", (String) obj.get("title"), "Incorrect title value!");
    }

    @Test
    public void testPrefer2() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockResource.stream()).thenAnswer(inv -> getPreferQuads());

        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"")
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertAll("Check JSON-LD structure", checkJsonStructure(obj, asList("@context", "title"),
                    asList("mode", "created", "contains", "member")));
        assertEquals("A title", (String) obj.get("title"), "Incorrect title value!");
    }

    @Test
    public void testPrefer3() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockResource.stream()).thenAnswer(inv -> getPreferQuads());

        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; omit=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"")
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertAll("Check JSON-LD structure", checkJsonStructure(obj, asList("@context", "contains", "member"),
                    asList("title", "mode", "created")));
    }

    @Test
    public void testGetJsonCompact() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDF response", checkLdfResponse(res));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertEquals("A title", (String) obj.get("title"), "Incorrect title property in JSON!");
        assertAll("Check JSON-LD structure",
                checkJsonStructure(obj, asList("@context", "title"), asList("mode", "created")));
    }

    @Test
    public void testGetJsonCompactLDF1() throws IOException {
        when(mockResource.stream()).thenAnswer(inv -> getLdfQuads());
        final Response res = target(RESOURCE_PATH).queryParam("subject", getBaseUrl() + RESOURCE_PATH)
            .queryParam("predicate", "http://purl.org/dc/terms/title").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDF response", checkLdfResponse(res));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertTrue(obj.get("title") instanceof List, "title property isn't a List!");
        @SuppressWarnings("unchecked")
        final List<Object> titles = (List<Object>) obj.get("title");
        assertTrue(titles.contains("A title"), "Incorrect title value!");
        assertEquals(2L, titles.size(), "Incorrect title property size!");
        assertEquals(getBaseUrl() + RESOURCE_PATH, obj.get("@id"), "Incorrect @id value!");
        assertAll("Check JSON-LD structure",
                checkJsonStructure(obj, asList("@context", "title"), asList("creator", "mode", "created")));
    }

    @Test
    public void testGetJsonCompactLDF2() throws IOException {
        when(mockResource.stream()).thenAnswer(inv -> getLdfQuads());

        final Response res = target(RESOURCE_PATH).queryParam("subject", getBaseUrl() + RESOURCE_PATH)
            .queryParam("object", "ex:Type").queryParam("predicate", "").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDF response", checkLdfResponse(res));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertEquals("ex:Type", obj.get("@type"), "Incorrect @type value!");
        assertEquals(getBaseUrl() + RESOURCE_PATH, obj.get("@id"), "Incorrect @id value!");
        assertAll("Check JSON-LD structure",
                checkJsonStructure(obj, asList("@type"), asList("@context", "creator", "title", "mode", "created")));
    }

    @Test
    public void testGetJsonCompactLDF3() throws IOException {
        when(mockResource.stream()).thenAnswer(inv -> getLdfQuads());

        final Response res = target(RESOURCE_PATH).queryParam("subject", getBaseUrl() + RESOURCE_PATH)
            .queryParam("object", "A title").queryParam("predicate", DC.title.getIRIString()).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(from(time), res.getLastModified(), "Incorrect modified date!");
        assertTrue(hasTimeGateLink(res, RESOURCE_PATH), "Missing rel=timegate link!");
        assertTrue(hasOriginalLink(res, RESOURCE_PATH), "Missing rel=original link!");

        assertAll("Check allowed methods", checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS)));
        assertAll("Check Vary headers", checkVary(res, asList(ACCEPT_DATETIME, PREFER)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_RANGES)));
        assertAll("Check JSON-LD Response", checkJsonLdResponse(res));
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertEquals("A title", obj.get("title"), "Incorrect title property!");
        assertEquals(getBaseUrl() + RESOURCE_PATH, obj.get("@id"), "Incorrect @id value!");
        assertAll("Check JSON-LD structure",
                checkJsonStructure(obj, asList("@context", "title"), asList("@type", "creator", "mode", "created")));
    }

    @Test
    public void testGetTimeMapLinkDefaultFormat() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(MediaType.valueOf(APPLICATION_LINK_FORMAT), res.getMediaType(), "Incorrect content-type!");
    }

    @Test
    public void testGetTimeMapLinkDefaultFormat2() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target("resource").queryParam("ext", "timemap").request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(MediaType.valueOf(APPLICATION_LINK_FORMAT), res.getMediaType(), "Incorrect content-type!");
    }

    @Test
    public void testGetTimeMapLinkInvalidFormat() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .accept("some/made-up-format").get();

        assertEquals(SC_NOT_ACCEPTABLE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetTimeMapLink() throws IOException {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.mementos(eq(identifier))).thenReturn(completedFuture(new TreeSet<>(asList(
                ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000), time))));

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .accept(APPLICATION_LINK_FORMAT).get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(MediaType.valueOf(APPLICATION_LINK_FORMAT), res.getMediaType(), "Incorrect content-type!");
        assertNull(res.getLastModified(), "Unexpected last-modified header!");
        assertAll("Check Memento headers", checkMementoHeaders(res, RESOURCE_PATH));
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check null headers",
                checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES, MEMENTO_DATETIME)));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        System.out.println(entity);
        final List<Link> entityLinks = stream(entity.split(",\n")).map(Link::valueOf).collect(toList());
        assertEquals(5L, entityLinks.size(), "Incorrect number of Link headers!");
        final List<Link> links = getLinks(res);
        final List<String> rels = asList("memento", "original", "timegate", "timemap", "first", "last");
        assertAll("Check link headers", links.stream().filter(l -> l.getRels().stream().anyMatch(rels::contains))
                .map(l -> () -> assertTrue(entityLinks.stream().map(Link::getUri).anyMatch(l.getUri()::equals),
                        "Link not in response: " + l)));
    }

    @Test
    public void testGetTimeMapJsonDefault() throws IOException {
        when(mockMementoService.mementos(eq(identifier))).thenReturn(completedFuture(new TreeSet<>(asList(
                ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000), time))));

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .accept("application/ld+json").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertNull(res.getLastModified(), "Incorrect last modified date!");
        assertAll("Check Simple JSON-LD", checkSimpleJsonLdResponse(res, LDP.RDFSource));
        assertAll("Check Memento headers", checkMementoHeaders(res, RESOURCE_PATH));
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check null headers",
                checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES, MEMENTO_DATETIME)));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity,
                new TypeReference<Map<String, Object>>(){});

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> graph = (List<Map<String, Object>>) obj.get("@graph");

        assertEquals(5L, graph.size(), "Incorrect @graph size!");
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH) &&
                    x.containsKey("timegate") && x.containsKey("timemap") && x.containsKey("memento")),
                "Missing memento-related properties in graph for given @id");
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap") &&
                    x.containsKey("hasBeginning") &&
                    x.containsKey("hasEnd")),
                "Missing hasBeginning/hasEnd properties in timemap graph!");
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729") &&
                    x.containsKey("hasTime")), "Missing hasTime property in timemap graph for version 1!");
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729") &&
                    x.containsKey("hasTime")), "Missing hasTime property in timemap graph for version 2!");
        assertTrue(graph.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729") &&
                    x.containsKey("hasTime")), "Missign hasTime property in timemap graph for version 3!");
    }

    @Test
    public void testGetTimeMapJson() throws IOException {
        when(mockMementoService.mementos(eq(identifier))).thenReturn(completedFuture(new TreeSet<>(asList(
                ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000), time))));

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#expanded\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertNull(res.getLastModified(), "Incorrect last-modified header!");
        assertAll("Check Simple JSON-LD", checkSimpleJsonLdResponse(res, LDP.RDFSource));
        assertAll("Check Memento headers", checkMementoHeaders(res, RESOURCE_PATH));
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check null headers",
                checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES, MEMENTO_DATETIME)));

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final List<Map<String, Object>> obj = MAPPER.readValue(entity,
                new TypeReference<List<Map<String, Object>>>(){});

        assertEquals(5L, obj.size(), "Incorrect number of properties in timemap JSON-LD!");
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH) &&
                    x.containsKey("http://mementoweb.org/ns#timegate") &&
                    x.containsKey("http://mementoweb.org/ns#timemap") &&
                    x.containsKey("http://mementoweb.org/ns#memento")),
                "Missing expected memento properties in expanded JSON-LD!");
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?ext=timemap") &&
                    x.containsKey("http://www.w3.org/2006/time#hasBeginning") &&
                    x.containsKey("http://www.w3.org/2006/time#hasEnd")),
                "Missing hasBeginning/hasEnd properties in expanded JSON-LD!");
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496260729") &&
                    x.containsKey("http://www.w3.org/2006/time#hasTime")),
                "Missing hasTime property in first memento!");
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496261729") &&
                    x.containsKey("http://www.w3.org/2006/time#hasTime")),
                "Missing hasTime property in second memento!");
        assertTrue(obj.stream().anyMatch(x -> x.containsKey("@id") &&
                    x.get("@id").equals(getBaseUrl() + RESOURCE_PATH + "?version=1496262729") &&
                    x.containsKey("http://www.w3.org/2006/time#hasTime")),
                "Missing hasTime property in third memento!");
    }

    @Test
    public void testGetVersionJson() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(from(time), res.getLastModified(), "Incorrect last-modified header!");
        assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant(),
                "Incorrect Memento-Datetime header!");
        assertAll("Check Simple JSON-LD", checkSimpleJsonLdResponse(res, LDP.RDFSource));
        assertAll("Check Memento headers", checkMementoHeaders(res, RESOURCE_PATH));
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES)));
    }

    @Test
    public void testGetVersionContainerJson() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.Container);
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(from(time), res.getLastModified(), "Incorrect last-modified header!");
        assertEquals(time, parse(res.getHeaderString(MEMENTO_DATETIME), RFC_1123_DATE_TIME).toInstant(),
                "Incorrect Memento-Datetime header!");
        assertAll("Check Simple JSON-LD", checkSimpleJsonLdResponse(res, LDP.Container));
        assertAll("Check Memento headers", checkMementoHeaders(res, RESOURCE_PATH));
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, ACCEPT_RANGES)));
    }

    @Test
    public void testGetNoAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request().get();

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetBinaryAcl() {
        when(mockBinaryResource.hasAcl()).thenReturn(true);
        final Response res = target(BINARY_PATH).queryParam("ext", "acl").request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describes")), "Unexpected rel=describes");
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describedby")),
                "Unexpected rel=describedby");
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("canonical")), "Unexpected rel=canonical");
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("alternate")), "Unexpected rel=alternate");
    }

    @Test
    public void testGetBinaryLinks() {
        final Response res = target(BINARY_PATH).request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describes")), "Unexpected rel=describes");
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describedby")), "Missing rel=describedby");
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("canonical")), "Missing rel=canonical");
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("alternate")), "Unexpected rel=alternate");
    }

    @Test
    public void testGetBinaryDescriptionLinks() {
        final Response res = target(BINARY_PATH).request().accept("text/turtle").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describes")), "Missing rel=describes");
        assertFalse(getLinks(res).stream().anyMatch(l -> l.getRel().equals("describedby")),
                "Unexpected rel=describedby");
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("canonical")), "Missing rel=canonical");
        assertTrue(getLinks(res).stream().anyMatch(l -> l.getRel().equals("alternate")), "Missing rel=alternate");
    }

    @Test
    public void testGetAclJsonCompact() throws IOException {
        when(mockResource.hasAcl()).thenReturn(true);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .accept("application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header");
        assertEquals(from(time), res.getLastModified(), "Incorrect last-modified header!");
        assertFalse(hasTimeGateLink(res, RESOURCE_PATH), "Unexpected rel=timegate link");
        assertFalse(hasOriginalLink(res, RESOURCE_PATH), "Unexpected rel=original link");
        assertTrue(res.hasEntity(), "Missing entity!");

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertEquals(ACL.Control.getIRIString(), (String) obj.get("mode"), "Incorrect ACL mode property!");
        assertAll("Check Simple JSON-LD", checkSimpleJsonLdResponse(res, LDP.RDFSource));
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(PATCH, GET, HEAD, OPTIONS)));
        assertAll("Check Vary headers", checkVary(res, asList(ACCEPT_DATETIME)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_RANGES)));
        assertAll("Check JSON-LD structure", checkJsonStructure(obj, asList("@context", "mode"), asList("title")));
    }

    @Test
    public void testGetResource() {
        final Response res = target(RESOURCE_PATH).request().get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetNotFound() {
        final Response res = target(NON_EXISTENT_PATH).request().get();

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetGone() {
        final Response res = target(DELETED_PATH).request().get();

        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testGetCORSInvalid() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", "http://foo.com")
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check null headers", checkNullHeaders(res, asList("Access-Control-Allow-Origin",
                        "Access-Control-Allow-Credentials", "Access-Control-Max-Age",
                        "Access-Control-Allow-Headers", "Access-Control-Allow-Methods")));
    }

    @Test
    public void testGetException() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException("Expected exception");
        }));
        final Response res = target(RESOURCE_PATH).request().get();
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), "Unexpected response code!");
    }

    /* ******************************* *
     *            OPTIONS Tests
     * ******************************* */
    @Test
    public void testOptionsLDPRS() {
        final Response res = target(RESOURCE_PATH).request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(MEMENTO_DATETIME)));
    }

    @Test
    public void testOptionsLDPNR() {
        final Response res = target(BINARY_PATH).request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS)));
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.NonRDFSource));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, MEMENTO_DATETIME)));
    }

    @Test
    public void testOptionsLDPC() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        final Response res = target(RESOURCE_PATH).request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertNotNull(res.getHeaderString(ACCEPT_POST), "Missing Accept-Post header!");
        assertAll("Check allowed methods",
                checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS, POST)));
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.Container));
        assertAll("Check null headers", checkNullHeaders(res, asList(MEMENTO_DATETIME)));

        final List<String> acceptPost = asList(res.getHeaderString(ACCEPT_POST).split(","));
        assertEquals(3L, acceptPost.size(), "Accept-Post header has wrong number of elements!");
        assertTrue(acceptPost.contains("text/turtle"), "Turtle missing from Accept-Post");
        assertTrue(acceptPost.contains(APPLICATION_LD_JSON), "JSON-LD missing from Accept-Post");
        assertTrue(acceptPost.contains(APPLICATION_N_TRIPLES), "N-Triples missing from Accept-Post");
    }

    @Test
    public void testOptionsACL() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, MEMENTO_DATETIME)));
    }

    @Test
    public void testOptionsACLBinary() {
        final Response res = target(BINARY_PATH).queryParam("ext", "acl").request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, MEMENTO_DATETIME)));
    }

    @Test
    public void testOptionsNonexistent() {
        final Response res = target(NON_EXISTENT_PATH).request().options();

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testOptionsVersionNotFound() {
        final Response res = target(NON_EXISTENT_PATH).queryParam("version", "1496260729").request().options();
        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testOptionsGone() {
        final Response res = target(DELETED_PATH).request().options();

        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testOptionsSlash() {
        final Response res = target(RESOURCE_PATH + "/").request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, MEMENTO_DATETIME)));
    }

    @Test
    public void testOptionsTimemap() {
        when(mockMementoService.mementos(eq(identifier))).thenReturn(completedFuture(new TreeSet<>(asList(
                ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000), time))));

        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, MEMENTO_DATETIME)));
    }

    @Test
    public void testOptionsTimemapBinary() {
        when(mockMementoService.mementos(eq(identifier))).thenReturn(completedFuture(new TreeSet<>(asList(
                ofEpochSecond(timestamp - 2000), ofEpochSecond(timestamp - 1000), time))));

        final Response res = target(BINARY_PATH).queryParam("ext", "timemap").request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_PATCH, MEMENTO_DATETIME)));
    }

    @Test
    public void testOptionsVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_PATCH, ACCEPT_POST)));
    }

    @Test
    public void testOptionsVersionBinary() {
        final Response res = target(BINARY_PATH).queryParam("version", timestamp).request().options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check allowed methods", checkAllowedMethods(res, asList(GET, HEAD, OPTIONS)));
        assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_PATCH, ACCEPT_POST)));
    }

    @Test
    public void testOptionsException() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException("Expected exception");
        }));
        final Response res = target(RESOURCE_PATH).request().options();
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), "Unexpected response code!");
    }

    /* ******************************* *
     *            POST Tests
     * ******************************* */
    @Test
    public void testPost() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE)),
                    eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(),
                "Incorrect Location header!");
        assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostRoot() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE)), eq(MAX)))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target("").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + RANDOM_VALUE, res.getLocation().toString(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        verify(myEventService, times(2)).emit(any());
    }

    @Test
    public void testPostInvalidLink() {
        final Response res = target(RESOURCE_PATH).request().header("Link", "I never really liked his friends")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostToTimemap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostTypeWrongType() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"non-existent\"")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostTypeMismatch() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostConflict() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(mockResource));

        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostUnknownLinkType() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://example.com/types/Foo>; rel=\"type\"")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(),
                "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostBadContent() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostToLdpRs() {
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
                .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostSlug() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        verify(myEventService, times(2)).emit(any());
    }

    @Test
    public void testPostSlugWithSlash() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/child_grandchild"))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child/grandchild")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + CHILD_PATH + "_grandchild", res.getLocation().toString(),
                "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostEncodedSlugWithSlash() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/child_grandchild"))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%2Fgrandchild")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + CHILD_PATH + "_grandchild", res.getLocation().toString(),
                "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostSlugWithWhitespace() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/child_grandchild"))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child grandchild")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + CHILD_PATH + "_grandchild", res.getLocation().toString(),
                "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostEncodedSlugWithEncodedWhitespace() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/child_grandchild"))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%09grandchild")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + CHILD_PATH + "_grandchild", res.getLocation().toString(),
                "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostEncodedSlugWithInvalidEncoding() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%0 grandchild")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(),
                "Incorrect Location header!");
        assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostEmptySlug() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(),
                "Incorrect Location header!");
        assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostEmptyEncodedSlug() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "%20%09")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + RESOURCE_PATH + "/" + RANDOM_VALUE, res.getLocation().toString(),
                "Incorrect Location header!");
        assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostSlugWithHashURI() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child#hash")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostSlugWithEncodedHashURI() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%23hash")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostSlugWithQuestionMark() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child?foo=bar")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostSlugWithEncodedQuestionMark() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);

        final Response res = target(RESOURCE_PATH).request().header(SLUG, "child%3Ffoo=bar")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + CHILD_PATH, res.getLocation().toString(), "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPostVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request().header(SLUG, "test")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request().header(SLUG, "test")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostIndirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target().request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"An indirect container\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(2)).emit(any());
    }

    @Test
    public void testPostIndirectContainerHashUri() {
        final IRI hashResourceId = rdf.createIRI(TRELLIS_DATA_PREFIX + NEW_RESOURCE + "#foo");
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(hashResourceId));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target().request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"An indirect container\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(2)).emit(any());
    }

    @Test
    public void testPostIndirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target().request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A self-contained LDP-IC\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(2)).emit(any());
    }

    @Test
    public void testPostIndirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(identifier));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target().request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"An LDP-IC\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(3)).emit(any());
    }

    @Test
    public void testPostDirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target().request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"An LDP-DC\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(2)).emit(any());
    }

    @Test
    public void testPostDirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target().request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A self-contained LDP-DC\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(2)).emit(any());
    }

    @Test
    public void testPostDirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(identifier));
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target().request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"An LDP-DC resource\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(3)).emit(any());
    }

    @Test
    public void testPostBadJsonLdSemantics() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("{\"@id\": \"\", \"@type\": \"some type\"}", APPLICATION_LD_JSON_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostBadJsonLdSyntax() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request().post(entity("{\"@id:", APPLICATION_LD_JSON_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostConstraint() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://www.w3.org/ns/ldp#inbox> \"Some literal\" .",
                    TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidRange, LDP.constrainedBy.getIRIString())), "Missing constrainedBy link");
    }

    @Test
    public void testPostIgnoreContains() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH).request()
            .post(entity("<> <http://www.w3.org/ns/ldp#contains> <./other> . ",
                    TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostNonexistent() {
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + NON_EXISTENT_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(NON_EXISTENT_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostGone() {
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + DELETED_PATH + "/" + RANDOM_VALUE))))
            .thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        final Response res = target(DELETED_PATH).request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostBinary() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockMementoService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/newresource")),
                    any(Instant.class))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        final Response res = target(RESOURCE_PATH).request().header(SLUG, "newresource")
            .post(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")), "No describedby link!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.NonRDFSource));
    }

    @Test
    public void testPostTimeMap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostSlash() {
        final Response res = target(RESOURCE_PATH + "/").request().header(SLUG, "test")
            .post(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPostException() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException("Expected exception");
        }));
        final Response res = target(RESOURCE_PATH).request().post(entity("", TEXT_TURTLE_TYPE));
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), "Unexpected response code!");
    }

    /* ******************************* *
     *            PUT Tests
     * ******************************* */
    @Test
    public void testPutExisting() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPutTypeWrongType() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"non-existent\"")
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertFalse(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")),
                "Unexpected describedby link!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPutUncontainedIndirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));
        when(mockResource.getContainer()).thenReturn(empty());

        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(1)).emit(any());
    }

    @Test
    public void testPutUncontainedIndirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));
        when(mockResource.getContainer()).thenReturn(empty());

        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        if (getConfig().getOptionalValue(CONFIG_HTTP_PUT_UNCONTAINED, Boolean.class).orElse(Boolean.FALSE)) {
            // only one event if configured with PUT-UNCONTAINED
            verify(myEventService, times(1)).emit(any());
        } else {
            verify(myEventService, times(2)).emit(any());
        }
    }

    @Test
    public void testPutUncontainedIndirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        final Resource mockChildResource = mock(Resource.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockResourceService.get(childIdentifier)).thenAnswer(inv -> completedFuture(mockChildResource));
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(childIdentifier));
        when(mockResource.getContainer()).thenReturn(empty());

        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(1)).emit(any());
    }

    @Test
    public void testPutIndirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));

        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(1)).emit(any());
    }

    @Test
    public void testPutIndirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));

        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(2)).emit(any());
    }

    @Test
    public void testPutIndirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        final Resource mockChildResource = mock(Resource.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockResourceService.get(childIdentifier)).thenAnswer(inv -> completedFuture(mockChildResource));
        when(mockChildResource.getIdentifier()).thenReturn(childIdentifier);
        when(mockChildResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(childIdentifier));

        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(2)).emit(any());
    }

    @Test
    public void testPutDirectContainer() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(newresourceIdentifier));

        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(1)).emit(any());
    }

    @Test
    public void testPutDirectContainerSelf() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(root));

        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(1)).emit(any());
    }

    @Test
    public void testPutDirectContainerResource() {
        final EventService myEventService = mock(EventService.class);
        when(mockBundler.getEventService()).thenReturn(myEventService);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(of(identifier));

        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        verify(myEventService, times(1)).emit(any());
    }

    @Test
    public void testPutExistingBinaryDescription() {
        final Response res = target(BINARY_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPutExistingUnknownLink() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Link", "<http://example.com/types/Foo>; rel=\"type\"")
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPutExistingIgnoreProperties() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" ;"
                        + " <http://example.com/foo> <http://www.w3.org/ns/ldp#IndirectContainer> ;"
                        + " a <http://example.com/Type1>, <http://www.w3.org/ns/ldp#BasicContainer> .",
                        TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPutExistingSubclassLink() {
        final Response res = target(RESOURCE_PATH).request()
            .header("Link", LDP.Container + "; rel=\"type\"")
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.Container));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPutExistingMalformed() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutConstraint() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \"Some literal\" .",
                    TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream()
                .anyMatch(hasLink(InvalidRange, LDP.constrainedBy.getIRIString())), "Missing constrainedBy header!");
    }

    @Test
    public void testPutIgnoreContains() {
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://www.w3.org/ns/ldp#contains> <./other> . ",
                    TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutNew() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/test");
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(identifier), eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH + "/test").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertEquals(getBaseUrl() + RESOURCE_PATH + "/test", res.getHeaderString(CONTENT_LOCATION),
                "Incorrect Location header!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPutDeleted() {
        final Response res = target(DELETED_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CREATED, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertEquals(getBaseUrl() + DELETED_PATH, res.getHeaderString(CONTENT_LOCATION), "Incorrect Location header!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPutVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPutAclOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPutAclOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPutOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())),
                "Missing constrainedBy header!");
    }

    @Test
    public void testPutOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        final Response res = target(RESOURCE_PATH).request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())),
                "Missing constrainedBy header!");
    }

    @Test
    public void testPutBinary() {
        final Response res = target(BINARY_PATH).request().put(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().map(Link::getRel).anyMatch(isEqual("describedby")), "No describedby link!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.NonRDFSource));
    }

    @Test
    public void testPutBinaryToACL() {
        final Response res = target(BINARY_PATH).queryParam("ext", "acl").request()
            .put(entity("some data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NOT_ACCEPTABLE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfMatch() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();

        final Response res = target(BINARY_PATH).request().header("If-Match", "\"" + etag + "\"")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfMatchWeak() {
        final String etag = target("").request().get().getEntityTag().getValue();

        final Response res = target("").request().header("If-Match", "W/\"" + etag + "\"")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfNoneMatchEtag() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();

        final Response res = target(BINARY_PATH).request().header("If-None-Match", "\"" + etag + "\"")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfNoneMatchRdfEtag() {
        final String etag = target("").request().get().getEntityTag().getValue();

        final Response res = target("").request().header("If-None-Match", "\"" + etag + "\"")
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfNoneMatchRdfWeakEtag() {
        final String etag = target("").request().get().getEntityTag().getValue();

        final Response res = target("").request().header("If-None-Match", "W/\"" + etag + "\"")
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfNoneMatchWeakEtag() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();

        final Response res = target(BINARY_PATH).request().header("If-None-Match", "W/\"" + etag + "\"")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfNoneMatch() {
        final Response res = target(BINARY_PATH).request().header("If-None-Match", "\"foo\", \"bar\"")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfMatchStar() {
        final Response res = target(BINARY_PATH).request().header("If-Match", "*")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfMatchMultiple() {
        final String etag = target(BINARY_PATH).request().get().getEntityTag().getValue();
        final Response res = target(BINARY_PATH).request().header("If-Match", "\"blah\", \"" + etag + "\"")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfNoneMatchStar() {
        final Response res = target(BINARY_PATH).request().header("If-None-Match", "*")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutBadIfMatch() {
        final Response res = target(BINARY_PATH).request().header("If-Match", "4db2c60044c906361ac212ae8684e8ad")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_BAD_REQUEST, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutIfUnmodified() {
        final Response res = target(BINARY_PATH).request()
            .header("If-Unmodified-Since", "Tue, 29 Aug 2017 07:14:52 GMT")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutPreconditionFailed() {
        final Response res = target(BINARY_PATH).request().header("If-Match", "\"blahblahblah\"")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutPreconditionFailed2() {
        final Response res = target(BINARY_PATH).request()
            .header("If-Unmodified-Since", "Wed, 19 Oct 2016 10:15:00 GMT")
            .put(entity("some different data.", TEXT_PLAIN_TYPE));

        assertEquals(SC_PRECONDITION_FAILED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutSlash() {
        final Response res = target(RESOURCE_PATH + "/").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPutTimeMap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .put(entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPutException() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException("Expected exception");
        }));
        final Response res = target(RESOURCE_PATH).request().put(entity("", TEXT_TURTLE_TYPE));
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), "Unexpected response code!");
    }

    /* ******************************* *
     *            DELETE Tests
     * ******************************* */
    @Test
    public void testDeleteExisting() {
        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testDeleteNonexistent() {
        final Response res = target(NON_EXISTENT_PATH).request().delete();

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testDeleteDeleted() {
        final Response res = target(DELETED_PATH).request().delete();

        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testDeleteVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request().delete();

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testDeleteNonExistant() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/test");
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(identifier), eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH + "/test").request().delete();

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testDeleteWithChildren() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockVersionedResource.stream(eq(LDP.PreferContainment))).thenAnswer(inv -> Stream.of(
                    rdf.createTriple(identifier, LDP.contains, rdf.createIRI(identifier.getIRIString() + "/child"))));

        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testDeleteNoChildren1() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockVersionedResource.stream(eq(LDP.PreferContainment))).thenAnswer(inv -> Stream.empty());

        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testDeleteNoChildren2() {
        when(mockVersionedResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockVersionedResource.stream(eq(LDP.PreferContainment))).thenAnswer(inv -> Stream.empty());

        final Response res = target(RESOURCE_PATH).request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testDeleteAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testDeleteTimeMap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request().delete();
        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testDeleteSlash() {
        final Response res = target(RESOURCE_PATH + "/").request().delete();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Resource)), "Unexpected ldp:Resource link!");
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.RDFSource)), "Unexpected ldp:RDFSource link!");
        assertFalse(getLinks(res).stream().anyMatch(hasType(LDP.Container)), "Unexpected ldp:Container link!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testDeleteException() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException("Expected exception");
        }));
        final Response res = target(RESOURCE_PATH).request().delete();
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), "Unexpected response code!");
    }

    /* ********************* *
     *      PATCH tests
     * ********************* */
    @Test
    public void testPatchVersion() {
        final Response res = target(RESOURCE_PATH).queryParam("version", timestamp).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPatchTimeMap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPatchExisting() {
        final Response res = target(RESOURCE_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPatchRoot() {
        final Response res = target().request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.BasicContainer));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPatchMissing() {
        final Response res = target(NON_EXISTENT_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));
        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPatchGone() {
        final Response res = target(DELETED_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));
        assertEquals(SC_GONE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPatchExistingIgnoreLdpType() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"")
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" ;"
                        + " <http://example.com/foo> <http://www.w3.org/ns/ldp#IndirectContainer> ;"
                        + " a <http://example.com/Type1>, <http://www.w3.org/ns/ldp#BasicContainer> } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertFalse(entity.contains("BasicContainer"), "Unexpected BasicContainer type!");
        assertTrue(entity.contains("Type1"), "Missing Type1 type!");
    }

    @Test
    public void testPatchExistingJsonLd() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation")
            .header("Accept", "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"")
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A new title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");

        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        final Map<String, Object> obj = MAPPER.readValue(entity, new TypeReference<Map<String, Object>>(){});

        assertAll("Check JSON-LD structure",
                checkJsonStructure(obj, asList("@context", "title"), asList("mode", "created")));
        assertEquals("A new title", (String) obj.get("title"), "Incorrect title value!");
    }

    @Test
    public void testPatchExistingBinary() {
        final Response res = target(BINARY_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPatchExistingResponse() throws IOException {
        final Response res = target(RESOURCE_PATH).request()
            .header("Prefer", "return=representation; include=\"" + LDP.PreferMinimalContainer.getIRIString() + "\"")
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
        final String entity = IOUtils.toString((InputStream) res.getEntity(), UTF_8);
        assertTrue(entity.contains("A title"), "Incorrect title value!");
    }

    @Test
    public void testPatchConstraint() {
        final Response res = target(RESOURCE_PATH).request()
            .method("PATCH", entity("INSERT { <> a \"Some literal\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidRange, LDP.constrainedBy.getIRIString())),
                "Missing constrainedBy link header!");
    }

    @Test
    public void testPatchToTimemap() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "timemap").request()
            .method("PATCH", entity("<> <http://purl.org/dc/terms/title> \"A title\" .", TEXT_TURTLE_TYPE));

        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPatchNew() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_PATH + "/test");
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockMementoService.get(eq(identifier), eq(MAX))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));

        final Response res = target(RESOURCE_PATH + "/test").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NOT_FOUND, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPatchAcl() {
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPatchOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        final Response res = target(RESOURCE_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())),
                "Missing constrainedBy link header!");
    }

    @Test
    public void testPatchOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        final Response res = target(RESOURCE_PATH).request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_CONFLICT, res.getStatus(), "Unexpected response code!");
        assertTrue(getLinks(res).stream().anyMatch(hasLink(InvalidCardinality, LDP.constrainedBy.getIRIString())),
                "Missing constrainedBy link header!");
    }

    @Test
    public void testPatchAclOnDc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPatchAclOnIc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        final Response res = target(RESOURCE_PATH).queryParam("ext", "acl").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource));
    }

    @Test
    public void testPatchInvalidContent() {
        final Response res = target(RESOURCE_PATH).request().method("PATCH", entity("blah blah blah", "invalid/type"));

        assertEquals(SC_UNSUPPORTED_MEDIA_TYPE, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPatchSlash() {
        final Response res = target(RESOURCE_PATH + "/").request()
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!");
    }

    @Test
    public void testPatchNotAcceptable() {
        final Response res = target(RESOURCE_PATH).request().accept("text/foo")
            .method("PATCH", entity("INSERT { <> <http://purl.org/dc/terms/title> \"A title\" } WHERE {}",
                        APPLICATION_SPARQL_UPDATE));

        assertEquals(SC_NOT_ACCEPTABLE, res.getStatus(), "Unexpected response code!");
    }

    @Test
    public void testPatchException() {
        when(mockResourceService.get(eq(identifier))).thenAnswer(inv -> supplyAsync(() -> {
            throw new RuntimeTrellisException("Expected exception");
        }));
        final Response res = target(RESOURCE_PATH).request().method("PATCH", entity("", APPLICATION_SPARQL_UPDATE));
        assertEquals(SC_INTERNAL_SERVER_ERROR, res.getStatus(), "Unexpected response code!");
    }

    /**
     * Some other method
     */
    @Test
    public void testOtherMethod() {
        final Response res = target(RESOURCE_PATH).request().method("FOO");
        assertEquals(SC_METHOD_NOT_ALLOWED, res.getStatus(), "Unexpected response code!");
    }

    /* ************************************ *
     *      Test cache control headers
     * ************************************ */
    @Test
    public void testCacheControl() {
        final Response res = target(RESOURCE_PATH).request().get();
        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertNotNull(res.getHeaderString(CACHE_CONTROL), "Missing Cache-Control header!");
        assertTrue(res.getHeaderString(CACHE_CONTROL).contains("max-age="), "Incorrect Cache-Control: max-age value!");
    }

    @Test
    public void testCacheControlOptions() {
        final Response res = target(RESOURCE_PATH).request().options();
        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertNull(res.getHeaderString(CACHE_CONTROL), "Unexpected Cache-Control header!");
    }

    protected static List<Link> getLinks(final Response res) {
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

    protected static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    protected static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, "type");
    }

    private Stream<Quad> getLdfQuads() {
        return Stream.of(
            rdf.createQuad(PreferUserManaged, identifier, DC.creator, rdf.createLiteral("User")),
            rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), DC.title, rdf.createIRI("ex:title")),
            rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
            rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("Other title")),
            rdf.createQuad(PreferUserManaged, identifier, type, rdf.createIRI("ex:Type")),
            rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), type, rdf.createIRI("ex:Type")),
            rdf.createQuad(PreferUserManaged, rdf.createIRI("ex:foo"), type, rdf.createIRI("ex:Other")),
            rdf.createQuad(PreferServerManaged, identifier, DC.created,
                rdf.createLiteral("2017-04-01T10:15:00Z", XSD.dateTime)),
            rdf.createQuad(PreferAccessControl, identifier, type, ACL.Authorization),
            rdf.createQuad(PreferAccessControl, identifier, ACL.mode, ACL.Control));
    }

    private Stream<Quad> getPreferQuads() {
        return Stream.of(
            rdf.createQuad(PreferUserManaged, identifier, DC.title, rdf.createLiteral("A title")),
            rdf.createQuad(PreferServerManaged, identifier, DC.created,
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
        final List<String> vheaders = res.getStringHeaders().get(VARY);
        return Stream.of(RANGE, ACCEPT_DATETIME, PREFER).map(header -> vary.contains(header)
                ? () -> assertTrue(vheaders.contains(header), "Missing Vary header: " + header)
                : () -> assertFalse(vheaders.contains(header), "Unexpected Vary header: " + header));
    }

    private Stream<Executable> checkLdTemplateHeaders(final Response res) {
        final List<String> templates = res.getStringHeaders().get(LINK_TEMPLATE);
        return Stream.of(
            () -> assertEquals(2L, templates.size(), "Incorrect Link-Template header count!"),
            () -> assertTrue(templates.contains("<" + getBaseUrl() + RESOURCE_PATH
                    + "{?subject,predicate,object}>; rel=\"" + LDP.RDFSource.getIRIString() + "\""),
                             "Template for Linked Data Fragments not found!"),
            () -> assertTrue(templates.contains("<" + getBaseUrl() + RESOURCE_PATH
                    + "{?version}>; rel=\"" + Memento.Memento.getIRIString() + "\""),
                             "Template for Memento queries not found!"));
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
                    l.getUri().toString().equals(getBaseUrl() + path + "?version=1496262729")),
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
                () -> assertNull(res.getHeaderString(MEMENTO_DATETIME), "Unexpected Memento-Datetime header!"),
                () -> assertAll("Check Vary header", checkVary(res, asList(RANGE, ACCEPT_DATETIME))),
                () -> assertAll("Check allowed methods",
                                checkAllowedMethods(res, asList(PUT, DELETE, GET, HEAD, OPTIONS))),
                () -> assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.NonRDFSource)));
    }

    private Stream<Executable> checkJsonLdResponse(final Response res) {
        return Stream.of(
                () -> assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()),
                                 "Incorrect JSON-LD content-type!"),
                () -> assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE),
                                 "Incompatible JSON-LD content-type!"),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(HUB), "hub")),
                                 "Missing rel=hub Link header!"),
                () -> assertTrue(getLinks(res).stream().anyMatch(hasLink(rdf.createIRI(getBaseUrl() + RESOURCE_PATH),
                                                                         "self")), "Missing rel=self Link header!"),
                () -> assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH),
                                   "Incorrect Accept-Patch header!"),
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
                    () -> assertFalse(obj.containsKey(key), "JSON-LD caontained extraneous key: " + key)));
    }

    private Stream<Executable> checkSimpleJsonLdResponse(final Response res, final IRI ldpType) {
        return Stream.of(
                () -> assertTrue(APPLICATION_LD_JSON_TYPE.isCompatible(res.getMediaType()),
                                 "Incompatible JSON-LD content-type: " + res.getMediaType()),
                () -> assertTrue(res.getMediaType().isCompatible(APPLICATION_LD_JSON_TYPE),
                                 "Incorrect JSON-LD content-type: " + res.getMediaType()),
                () -> assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, ldpType)));
    }

    private Stream<Executable> checkLdfResponse(final Response res) {
        return Stream.of(
                () -> assertEquals(from(time), res.getLastModified(), "Incorrect modification date!"),
                () -> assertTrue(hasTimeGateLink(res, RESOURCE_PATH), "Missing rel=timegate link!"),
                () -> assertTrue(hasOriginalLink(res, RESOURCE_PATH), "Missing rel=original link!"),
                () -> assertAll("Check allowed methods",
                                checkAllowedMethods(res, asList(PATCH, PUT, DELETE, GET, HEAD, OPTIONS))),
                () -> assertAll("Check Vary header", checkVary(res, asList(ACCEPT_DATETIME, PREFER))),
                () -> assertAll("Check null headers", checkNullHeaders(res, asList(ACCEPT_POST, ACCEPT_RANGES))),
                () -> assertAll("Check json-ld response", checkJsonLdResponse(res)),
                () -> assertAll("Check LDP type Link headers", checkLdpTypeHeaders(res, LDP.RDFSource)));
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
