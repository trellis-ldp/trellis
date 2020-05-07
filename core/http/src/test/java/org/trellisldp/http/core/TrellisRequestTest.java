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
package org.trellisldp.http.core;

import static java.net.URI.create;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class TrellisRequestTest {

    @Test
    void testTrellisRequest() {
        final URI uri = create("http://example.com/");

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        final Request mockRequest = mock(Request.class);
        final UriInfo mockUriInfo = mock(UriInfo.class);
        final HttpHeaders mockHeaders = mock(HttpHeaders.class);

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("http://example.com/", req.getBaseUrl());
    }

    @Test
    void testTrellisRequestXForwarded() {
        final URI uri = create("http://example.com/");

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("X-Forwarded-Proto", "https");
        headers.putSingle("X-Forwarded-Host", "app.example.com");

        final Request mockRequest = mock(Request.class);
        final UriInfo mockUriInfo = mock(UriInfo.class);
        final HttpHeaders mockHeaders = mock(HttpHeaders.class);

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("https://app.example.com/", req.getBaseUrl());
    }

    @Test
    void testTrellisRequestForwarded() {
        final URI uri = create("http://example.com/");

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Forwarded", "host=app.example.com;proto=https");

        final Request mockRequest = mock(Request.class);
        final UriInfo mockUriInfo = mock(UriInfo.class);
        final HttpHeaders mockHeaders = mock(HttpHeaders.class);

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
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

        final Request mockRequest = mock(Request.class);
        final UriInfo mockUriInfo = mock(UriInfo.class);
        final HttpHeaders mockHeaders = mock(HttpHeaders.class);

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("https://app.example.com:9000/", req.getBaseUrl());
    }

    @Test
    void testTrellisRequestBadXForwardedPort() {
        final URI uri = create("http://example.com/");

        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("X-Forwarded-Proto", "foo");
        headers.putSingle("X-Forwarded-Host", "app.example.com");

        final Request mockRequest = mock(Request.class);
        final UriInfo mockUriInfo = mock(UriInfo.class);
        final HttpHeaders mockHeaders = mock(HttpHeaders.class);

        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockUriInfo.getBaseUri()).thenReturn(uri);
        when(mockHeaders.getRequestHeaders()).thenReturn(headers);

        final TrellisRequest req = new TrellisRequest(mockRequest, mockUriInfo, mockHeaders);
        assertEquals("http://app.example.com/", req.getBaseUrl());
    }

}
