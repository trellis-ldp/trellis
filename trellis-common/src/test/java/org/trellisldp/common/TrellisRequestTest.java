/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
package org.trellisldp.common;

import static jakarta.ws.rs.HttpMethod.GET;
import static java.net.URI.create;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.net.URLEncoder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author acoburn
 */
@ExtendWith(MockitoExtension.class)
class TrellisRequestTest {

    @Mock
    Request mockRequest;

    @Mock
    UriInfo mockUriInfo;

    @Mock
    HttpHeaders mockHeaders;

    @Test
    void testTrellisRequest() {
        final URI uri = create("http://example.com/");

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Content-Type", RdfMediaType.TEXT_TURTLE);
        final MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        queryParams.add("ext", "foo");
        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", "resource");

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(queryParams);
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);
        when(mockRequest.getMethod()).thenReturn(GET);
        when(mockHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(RdfMediaType.TEXT_TURTLE_TYPE));

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("http://example.com/", req.getBaseUrl());
        assertEquals(headers, req.getHeaders());
        assertTrue(req.getAcceptableMediaTypes().contains(RdfMediaType.TEXT_TURTLE_TYPE));
        assertEquals(GET, req.getMethod());
        assertNull(req.getSecurityContext());
        assertFalse(req.hasTrailingSlash());
        assertEquals("resource", req.getPath());
        assertEquals("foo", req.getExt());
        assertEquals(RdfMediaType.TEXT_TURTLE, req.getContentType());
    }

    @Test
    void testTrellisRequestXForwarded() {
        final URI uri = create("http://example.com/");

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("X-Forwarded-Proto", "https");
        headers.putSingle("X-Forwarded-Host", "app.example.com");
        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", "resource/");

        when(mockUriInfo.getPath()).thenReturn("resource/");
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("https://app.example.com/", req.getBaseUrl());
        assertTrue(req.hasTrailingSlash());
        assertEquals("resource", req.getPath());
        assertNull(req.getVersion());
        assertNull(req.getRange());
        assertNull(req.getPrefer());
        assertNull(req.getDatetime());
        assertNull(req.getLink());
        assertNull(req.getSlug());
    }

    @Test
    void testTrellisRequestForwarded() {
        final URI uri = create("http://example.com/");

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Forwarded", "host=app.example.com;proto=https");

        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", "resource");

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("https://app.example.com/", req.getBaseUrl());
    }

    @Test
    void testTrellisRequestForwardedWithPort() {
        final URI uri = create("http://example.com/");

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Forwarded", "host=app.example.com:9000;proto=https");

        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", "resource");

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("https://app.example.com:9000/", req.getBaseUrl());
    }

    @Test
    void testTrellisRequestBadXForwardedPort() {
        final URI uri = create("http://example.com/");

        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", "resource");

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("X-Forwarded-Proto", "foo");
        headers.putSingle("X-Forwarded-Host", "app.example.com");

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("http://app.example.com/", req.getBaseUrl());
    }

    @Test
    void testSlugHeader() {
        final URI uri = create("http://example.com/");
        final MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Slug", "new-resource");
        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", "resource");

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(queryParams);
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);
        when(mockRequest.getMethod()).thenReturn(GET);
        when(mockHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(RdfMediaType.TEXT_TURTLE_TYPE));

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("new-resource", req.getSlug());
    }

    @Test
    void testEmptySlugHeader() {
        final URI uri = create("http://example.com/");
        final MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Slug", "");
        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", "resource");

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(queryParams);
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);
        when(mockRequest.getMethod()).thenReturn(GET);
        when(mockHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(RdfMediaType.TEXT_TURTLE_TYPE));

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertNull(req.getSlug());
    }

    @Test
    void testLinkHeader() {
        final URI uri = create("http://example.com/");
        final MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"");
        headers.add("Link", "<http://example.com/SomeType>; rel=\"type\"");
        headers.add("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"about\"");
        final String rawLink = "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"";
        headers.add("Link", rawLink);
        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", "resource");

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(queryParams);
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);
        when(mockRequest.getMethod()).thenReturn(GET);
        when(mockHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(RdfMediaType.TEXT_TURTLE_TYPE));

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals(Link.valueOf(rawLink), req.getLink());
    }

    @Test
    void testSkippedLinkHeaders() {
        final URI uri = create("http://example.com/");
        final MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"");
        headers.add("Link", "<http://example.com/SomeType>; rel=\"type\"");
        headers.add("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"about\"");
        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", "resource");

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(queryParams);
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);
        when(mockRequest.getMethod()).thenReturn(GET);
        when(mockHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(RdfMediaType.TEXT_TURTLE_TYPE));

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertNull(req.getLink());
    }

    @ParameterizedTest
    @ValueSource(strings = { "[", "]", "%", ":", "?", "#", "\"", "\\", "|", "^", "`" })
    void testExcapeChars(final String character) {
        final String path = "before" + character + "after";
        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", path);

        when(mockUriInfo.getPath()).thenReturn(path);
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockHeaders.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(create("http://example.com"));
        when(mockRequest.getMethod()).thenReturn(GET);
        when(mockHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(RdfMediaType.TEXT_TURTLE_TYPE));

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals(URLEncoder.encode(path, UTF_8), req.getPath());
    }

    @ParameterizedTest
    @ValueSource(strings = { "before[middle]after", "a:b%c", "c%b:a", "a?b#c", "a%b?c",
                             "A|B", "before^after", "x%y|z" })
    void testExcapeMultipleChars(final String character) {
        final String path = "before" + character + "after";
        final MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.add("path", path);

        when(mockUriInfo.getPath()).thenReturn(path);
        when(mockUriInfo.getPathParameters()).thenReturn(pathParams);
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockHeaders.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(create("http://example.com"));
        when(mockRequest.getMethod()).thenReturn(GET);
        when(mockHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(RdfMediaType.TEXT_TURTLE_TYPE));

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals(URLEncoder.encode(path, UTF_8), req.getPath());
    }
}
