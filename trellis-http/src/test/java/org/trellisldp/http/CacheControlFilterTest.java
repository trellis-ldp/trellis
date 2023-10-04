/*
 * Copyright (c) Aaron Coburn and individual contributors
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

import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.HEAD;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class CacheControlFilterTest {

    @Mock
    private ContainerRequestContext mockRequest;

    @Mock
    private ContainerResponseContext mockResponse;

    @Mock
    private MultivaluedMap<String, Object> mockResponseHeaders;

    @Mock
    private MultivaluedMap<String, String> mockRequestHeaders;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void testCacheControlNull() {

        when(mockRequest.getMethod()).thenReturn(GET);
        when(mockResponse.getStatusInfo()).thenReturn(OK);

        final CacheControlFilter filter = new CacheControlFilter();
        filter.setMaxAge(0);

        filter.filter(mockRequest, mockResponse);
        verify(mockResponse, never()).getHeaders();
    }

    @Test
    void testCacheControlHead() {

        when(mockRequest.getMethod()).thenReturn(HEAD);
        when(mockRequest.getHeaders()).thenReturn(mockRequestHeaders);
        when(mockResponse.getStatusInfo()).thenReturn(OK);
        when(mockResponse.getHeaders()).thenReturn(mockResponseHeaders);

        final CacheControlFilter filter = new CacheControlFilter();
        filter.setMaxAge(180);
        filter.setMustRevalidate(false);
        filter.setNoCache(true);

        filter.filter(mockRequest, mockResponse);
        verify(mockResponse).getHeaders();
    }

    @Test
    void testCacheControlPrivate() {

        when(mockRequest.getMethod()).thenReturn(HEAD);
        when(mockRequest.getHeaders()).thenReturn(mockRequestHeaders);
        when(mockResponse.getStatusInfo()).thenReturn(OK);
        when(mockResponse.getHeaders()).thenReturn(mockResponseHeaders);
        when(mockRequestHeaders.containsKey(AUTHORIZATION)).thenReturn(true);

        final CacheControlFilter filter = new CacheControlFilter();
        filter.setMaxAge(180);
        filter.setMustRevalidate(false);
        filter.setNoCache(true);

        filter.filter(mockRequest, mockResponse);
        verify(mockResponse).getHeaders();
    }
}
