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

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.core.HttpConstants.ACL;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;

import java.net.URI;
import java.util.stream.Stream;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.rdf.api.Dataset;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.http.core.TrellisRequest;

/**
 * @author acoburn
 */
class TrellisHttpResourceTest extends AbstractTrellisHttpResourceTest {

    @Mock
    private TrellisRequest mockTrellisRequest;

    @Mock
    private HttpHeaders mockHttpHeaders;

    @Mock
    private Request mockRequest;

    @Mock
    private UriInfo mockUriInfo;

    @Override
    String getBaseUrl() {
        return getBaseUri().toString();
    }

    @Override
    protected Application configure() {

        initMocks(this);

        final ResourceConfig config = new ResourceConfig();
        config.register(new TrellisHttpResource(mockBundler, singletonMap(ACL, PreferAccessControl), null));
        config.register(new CacheControlFilter());
        config.register(new WebSubHeaderFilter(HUB));
        config.register(new TrellisHttpFilter());
        return config;
    }

    @Test
    void testNoArgCtor() {
        assertDoesNotThrow(() -> new TrellisHttpResource());
    }

    @Test
    void testNoBaseURL() throws Exception {
        final TrellisHttpResource matcher = new TrellisHttpResource(mockBundler,
                singletonMap(ACL, PreferAccessControl), null);

        when(mockUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>(singletonMap("path", "resource")));
        when(mockUriInfo.getPath()).thenReturn("resource");
        when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://my.example.com/"));
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(mockHttpHeaders.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(WILDCARD_TYPE));

        try (final Response res = matcher.getResourceHeaders(mockRequest, mockUriInfo, mockHttpHeaders)
                .toCompletableFuture().join()) {
            assertTrue(getLinks(res).stream().anyMatch(l ->
                        l.getRel().equals("self") && l.getUri().toString().startsWith("http://my.example.com/")),
                    "Missing rel=self header with correct prefix!");
        }
    }

    @Test
    void testInitializeExistingLdpResourceWithFailure() throws Exception {
        final ResourceService mockService = mock(ResourceService.class);
        when(mockBundler.getResourceService()).thenReturn(mockService);
        when(mockService.get(eq(root))).thenAnswer(inv -> runAsync(() -> {
            throw new RuntimeTrellisException("Expected exception");
        }));

        final TrellisHttpResource matcher = new TrellisHttpResource(mockBundler);
        assertDoesNotThrow(() -> matcher.initialize());
        assertAll("Verify interactions with init-errored resource service", verifyInteractions(mockService));
    }

    @Test
    void testInitializeExistingLdpResource() throws Exception {
        final ResourceService mockService = mock(ResourceService.class);
        when(mockBundler.getResourceService()).thenReturn(mockService);
        when(mockService.get(eq(root))).thenAnswer(inv -> completedFuture(mockRootResource));

        final TrellisHttpResource matcher = new TrellisHttpResource(mockBundler);
        matcher.initialize();
        assertAll("Verify interactions with resource service", verifyInteractions(mockService));
    }

    @Test
    void testInitializeoNoLdpResource() throws Exception {
        final ResourceService mockService = mock(ResourceService.class);
        when(mockBundler.getResourceService()).thenReturn(mockService);
        when(mockService.get(eq(root))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockService.create(any(Metadata.class), any(Dataset.class))).thenReturn(completedFuture(null));

        final TrellisHttpResource matcher = new TrellisHttpResource(mockBundler);
        matcher.initialize();

        verify(mockService, description("Re-create a missing root resource on initialization"))
            .create(any(Metadata.class), any(Dataset.class));
        verify(mockService, never().description("Don't try to replace a non-existent root on initialization"))
            .replace(any(Metadata.class), any(Dataset.class));
        verify(mockService, description("Verify that the root resource is fetched only once")).get(root);
    }

    @Test
    void testInitializeoDeletedLdpResource() throws Exception {
        final ResourceService mockService = mock(ResourceService.class);
        when(mockBundler.getResourceService()).thenReturn(mockService);
        when(mockService.get(eq(root))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        when(mockService.create(any(Metadata.class), any(Dataset.class))).thenReturn(completedFuture(null));

        final TrellisHttpResource matcher = new TrellisHttpResource(mockBundler);
        matcher.initialize();

        verify(mockService, description("A previously deleted root resource should be re-created upon initialization"))
            .create(any(Metadata.class), any(Dataset.class));
        verify(mockService, never().description("replace shouldn't be called when re-initializing a deleted root"))
            .replace(any(Metadata.class), any(Dataset.class));
        verify(mockService, description("Verify that the root resource is fetched only once")).get(root);
    }

    private Stream<Executable> verifyInteractions(final ResourceService svc) {
        return of(
                () -> verify(svc, never().description("Don't re-initialize the root if it already exists"))
                        .create(any(Metadata.class), any(Dataset.class)),
                () -> verify(svc, never().description("Don't re-initialize the root if it already exists"))
                        .replace(any(Metadata.class), any(Dataset.class)),
                () -> verify(svc, description("Verify that the root resource is fetched only once")).get(root));
    }
}
