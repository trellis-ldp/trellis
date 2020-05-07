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

import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.core.Response.Status.OK;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class WebSubHeaderFilterTest {

    @Mock
    private ContainerRequestContext mockRequest;

    @Mock
    private ContainerResponseContext mockResponse;

    @Mock
    private MultivaluedMap<String, Object> mockHeaders;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void testWebSubNull() {

        when(mockRequest.getMethod()).thenReturn(GET);
        when(mockResponse.getStatusInfo()).thenReturn(OK);

        final WebSubHeaderFilter filter = new WebSubHeaderFilter();

        filter.filter(mockRequest, mockResponse);
        verify(mockResponse, never()).getHeaders();
    }

    @Test
    void testWebSubHead() {

        when(mockRequest.getMethod()).thenReturn(HEAD);
        when(mockResponse.getStatusInfo()).thenReturn(OK);
        when(mockResponse.getHeaders()).thenReturn(mockHeaders);

        final WebSubHeaderFilter filter = new WebSubHeaderFilter("http://example.com");

        filter.filter(mockRequest, mockResponse);
        verify(mockResponse).getHeaders();
    }
}
