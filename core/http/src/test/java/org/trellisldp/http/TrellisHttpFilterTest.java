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
package org.trellisldp.http;

import static java.util.Collections.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class TrellisHttpFilterTest {

    @Mock
    private ContainerRequestContext mockContext;

    @Mock
    private UriInfo mockUriInfo;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void testTestRootSlash() {

        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getPath()).thenReturn("/");
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

        final TrellisHttpFilter filter = new TrellisHttpFilter();
        filter.setMutatingMethods(emptyList());
        filter.setExtensions(emptyMap());

        filter.filter(mockContext);
        verify(mockContext, never().description("Trailing slash should trigger a redirect!")).abortWith(any());
    }

    @Test
    void testUnwisePath() {
        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getPath()).thenReturn("/foo/bar/one|two");
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

        final TrellisHttpFilter filter = new TrellisHttpFilter();
        filter.setMutatingMethods(emptyList());
        filter.setExtensions(emptyMap());

        filter.filter(mockContext);
        verify(mockContext, never()).abortWith(any());
    }
}
