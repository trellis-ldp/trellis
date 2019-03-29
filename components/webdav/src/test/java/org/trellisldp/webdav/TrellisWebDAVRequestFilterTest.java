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
package org.trellisldp.webdav;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.http.core.HttpConstants.SLUG;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;

public class TrellisWebDAVRequestFilterTest {

    private static RDF rdf = getInstance();

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
    public void setUp() {
        initMocks(this);

        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + PATH))))
            .thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockContext.getMethod()).thenReturn(PUT);
        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockContext.getHeaders()).thenReturn(mockHeaders);
        when(mockUriBuilder.path(any(String.class))).thenReturn(mockUriBuilder);
        when(mockUriInfo.getBaseUriBuilder()).thenReturn(mockUriBuilder);
        when(mockUriInfo.getPath()).thenReturn(PATH);
        when(mockUriInfo.getPathSegments()).thenReturn(asList(mockPathSegment));
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockPathSegment.getPath()).thenReturn(PATH);

        filter = new TrellisWebDAVRequestFilter(mockBundler, true, null);
    }

    @Test
    public void testTestPutUncontainedMissing() throws Exception {

        filter.filter(mockContext);
        verify(mockContext).setMethod(eq(POST));
        verify(mockHeaders).putSingle(eq(SLUG), eq(PATH));
    }

    @Test
    public void testTestPutContainedMissing() throws Exception {
        final TrellisWebDAVRequestFilter filter2 = new TrellisWebDAVRequestFilter(mockBundler, false, null);

        filter2.filter(mockContext);
        verify(mockContext, never()).setMethod(eq(POST));
        verify(mockHeaders, never()).putSingle(eq(SLUG), eq(PATH));
    }


    @Test
    public void testTestPutUncontainedDeleted() throws Exception {

        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + PATH))))
            .thenAnswer(inv -> completedFuture(DELETED_RESOURCE));

        filter.filter(mockContext);
        verify(mockContext).setMethod(eq(POST));
        verify(mockHeaders).putSingle(eq(SLUG), eq(PATH));
    }

    @Test
    public void testTestPutUncontainedExisting() throws Exception {

        when(mockResourceService.get(eq(rdf.createIRI(TRELLIS_DATA_PREFIX + PATH))))
            .thenAnswer(inv -> completedFuture(mockResource));

        filter.filter(mockContext);
        verify(mockContext, never()).setMethod(eq(POST));
        verify(mockHeaders, never()).putSingle(eq(SLUG), eq(PATH));
    }


    @Test
    public void testTestPutUncontainedNoPaths() throws Exception {

        when(mockUriInfo.getPathSegments()).thenReturn(emptyList());

        filter.filter(mockContext);
        verify(mockContext, never()).setMethod(eq(POST));
        verify(mockHeaders, never()).putSingle(eq(SLUG), eq(PATH));
    }

    @Test
    public void testTestPutUncontainedEmptyPaths() throws Exception {

        when(mockPathSegment.getPath()).thenReturn("");

        filter.filter(mockContext);
        verify(mockContext, never()).setMethod(eq(POST));
        verify(mockHeaders, never()).putSingle(eq(SLUG), eq(PATH));
    }
}
