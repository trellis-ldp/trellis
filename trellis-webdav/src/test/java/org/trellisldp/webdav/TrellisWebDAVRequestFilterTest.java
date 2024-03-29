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
package org.trellisldp.webdav;

import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.common.HttpConstants.SLUG;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.common.ServiceBundler;

class TrellisWebDAVRequestFilterTest {

    private static final RDF rdf = RDFFactory.getInstance();
    private static final String PATH = "resource";

    @Mock
    private ServiceBundler mockBundler;

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private Resource mockResource;

    @Mock
    private ContainerRequestContext mockContext;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private MultivaluedMap<String, String> mockHeaders;

    @Mock
    private PathSegment mockPathSegment;

    @Mock
    private UriBuilder mockUriBuilder;

    private TrellisWebDAVRequestFilter filter;

    @BeforeEach
    void setUp() {
        openMocks(this);

        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockResourceService.get(rdf.createIRI(TRELLIS_DATA_PREFIX + PATH)))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        doCallRealMethod().when(mockResourceService).getResourceIdentifier(any(), any());
        when(mockContext.getMethod()).thenReturn(PUT);
        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockContext.getHeaders()).thenReturn(mockHeaders);
        when(mockUriBuilder.path(any(String.class))).thenReturn(mockUriBuilder);
        when(mockUriInfo.getBaseUriBuilder()).thenReturn(mockUriBuilder);
        when(mockUriInfo.getBaseUri()).thenReturn(URI.create("http://example.com/"));
        when(mockUriInfo.getPath()).thenReturn(PATH);
        when(mockUriInfo.getPathSegments()).thenReturn(singletonList(mockPathSegment));
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockPathSegment.getPath()).thenReturn(PATH);

        filter = new TrellisWebDAVRequestFilter();
        filter.services = mockBundler;
        filter.createUncontained = true;
    }

    @Test
    void testNoArgCtor() {
        assertDoesNotThrow(() -> new TrellisWebDAVRequestFilter());
    }

    @Test
    void testTestPutUncontainedMissing() {

        filter.filter(mockContext);
        verify(mockContext).setMethod(POST);
        verify(mockHeaders).putSingle(SLUG, PATH);
    }

    @Test
    void testTestPutContainedMissing() {
        final TrellisWebDAVRequestFilter filter2 = new TrellisWebDAVRequestFilter();
        filter2.services = mockBundler;

        filter2.filter(mockContext);
        verify(mockContext, never()).setMethod(POST);
        verify(mockHeaders, never()).putSingle(SLUG, PATH);
    }


    @Test
    void testTestPutUncontainedDeleted() {

        when(mockResourceService.get(rdf.createIRI(TRELLIS_DATA_PREFIX + PATH)))
            .thenAnswer(inv -> completedFuture(DELETED_RESOURCE));

        filter.filter(mockContext);
        verify(mockContext).setMethod(POST);
        verify(mockHeaders).putSingle(SLUG, PATH);
    }

    @Test
    void testTestPutUncontainedExisting() {

        when(mockResourceService.get(rdf.createIRI(TRELLIS_DATA_PREFIX + PATH)))
            .thenAnswer(inv -> completedFuture(mockResource));

        filter.filter(mockContext);
        verify(mockContext, never()).setMethod(POST);
        verify(mockHeaders, never()).putSingle(SLUG, PATH);
    }


    @Test
    void testTestPutUncontainedNoPaths() {

        when(mockUriInfo.getPathSegments()).thenReturn(emptyList());

        filter.filter(mockContext);
        verify(mockContext, never()).setMethod(POST);
        verify(mockHeaders, never()).putSingle(SLUG, PATH);
    }

    @Test
    void testTestPutUncontainedEmptyPaths() {

        when(mockPathSegment.getPath()).thenReturn("");

        filter.filter(mockContext);
        verify(mockContext, never()).setMethod(POST);
        verify(mockHeaders, never()).putSingle(SLUG, PATH);
    }
}
