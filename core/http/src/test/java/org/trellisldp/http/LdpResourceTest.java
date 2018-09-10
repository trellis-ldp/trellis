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

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.LdpRequest;

/**
 * @author acoburn
 */
public class LdpResourceTest extends AbstractLdpResourceTest {

    @Mock
    private AsyncResponse mockResponse;

    @Mock
    private ContainerRequestContext mockContext;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private LdpRequest mockLdpRequest;

    @Mock
    private HttpHeaders mockHttpHeaders;

    @Mock
    private Request mockRequest;

    @Captor
    private ArgumentCaptor<Response> captor;

    @Override
    protected String getBaseUrl() {
        return getBaseUri().toString();
    }

    @Override
    public Application configure() {

        initMocks(this);

        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);

        final ResourceConfig config = new ResourceConfig();

        config.register(new LdpResource(mockBundler, null));
        config.register(new AgentAuthorizationFilter(mockAgentService));
        config.register(new CacheControlFilter(86400, true, false));
        config.register(new WebSubHeaderFilter(HUB));
        config.register(new CrossOriginResourceSharingFilter(asList(origin), asList("PATCH", "POST", "PUT"),
                        asList("Link", "Content-Type", "Accept-Datetime", "Accept"),
                        asList("Link", "Content-Type", "Memento-Datetime"), true, 100));
        return config;
    }

    @Test
    public void testTestRootSlash() throws Exception {

        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getPath()).thenReturn("/");
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

        final LdpResource filter = new LdpResource(mockBundler);

        filter.filter(mockContext);
        verify(mockContext, never().description("Trailing slash should trigger a redirect!")).abortWith(any());
    }

    @Test
    public void testNoBaseURL() throws Exception {
        final LdpResource matcher = new LdpResource(mockBundler, null);

        when(mockLdpRequest.getPath()).thenReturn("repo1/resource");
        when(mockLdpRequest.getBaseUrl()).thenReturn("http://my.example.com/");
        when(mockLdpRequest.getHeaders()).thenReturn(mockHttpHeaders);
        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(asList(WILDCARD_TYPE));
        when(mockLdpRequest.getRequest()).thenReturn(mockRequest);

        matcher.getResourceHeaders(mockResponse, mockLdpRequest);
        verify(mockResponse).resume(captor.capture());

        final Response res = captor.getValue();
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().equals("self") && l.getUri().toString().startsWith("http://my.example.com/")),
                "Missing rel=self header with correct prefix!");
    }

    @Test
    public void testInitializeExistingLdpResource() throws Exception {
        final ResourceService mockService = mock(ResourceService.class);
        when(mockBundler.getResourceService()).thenReturn(mockService);
        when(mockService.get(eq(root))).thenAnswer(inv -> completedFuture(mockRootResource));

        final LdpResource matcher = new LdpResource(mockBundler);
        matcher.initialize();

        verify(mockService, never().description("Don't re-initialize the root if it already exists"))
            .create(eq(root), any(Session.class), any(IRI.class), any(Dataset.class), any(), any());
        verify(mockService, never().description("Don't re-initialize the root if it already exists"))
            .replace(eq(root), any(Session.class), any(IRI.class), any(Dataset.class), any(), any());
        verify(mockService, description("Verify that the root resource is fetched only once")).get(root);
    }

    @Test
    public void testInitializeExistingLdpResourceWithNoACL() throws Exception {
        final ResourceService mockService = mock(ResourceService.class);
        when(mockBundler.getResourceService()).thenReturn(mockService);
        when(mockService.get(eq(root))).thenAnswer(inv -> completedFuture(mockRootResource));
        when(mockRootResource.hasAcl()).thenReturn(false);
        when(mockService.replace(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class), any(), any()))
            .thenReturn(completedFuture(null));

        final LdpResource matcher = new LdpResource(mockBundler);
        matcher.initialize();

        verify(mockService, never().description("When re-initializing the root ACL, create should not be called"))
            .create(eq(root), any(Session.class), any(IRI.class), any(Dataset.class), any(), any());
        verify(mockService, description("Use replace when re-initializing the root ACL"))
            .replace(eq(root), any(Session.class), any(IRI.class), any(Dataset.class), any(), any());
        verify(mockService, description("Verify that the root resource is fetched only once")).get(root);
    }

    @Test
    public void testInitializeoNoLdpResource() throws Exception {
        final ResourceService mockService = mock(ResourceService.class);
        when(mockBundler.getResourceService()).thenReturn(mockService);
        when(mockService.get(eq(root))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockService.create(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class), any(), any()))
            .thenReturn(completedFuture(null));

        final LdpResource matcher = new LdpResource(mockBundler);
        matcher.initialize();

        verify(mockService, description("Re-create a missing root resource on initialization"))
            .create(eq(root), any(Session.class), any(IRI.class), any(Dataset.class), any(), any());
        verify(mockService, never().description("Don't try to replace a non-existent root on initialization"))
            .replace(eq(root), any(Session.class), any(IRI.class), any(Dataset.class), any(), any());
        verify(mockService, description("Verify that the root resource is fetched only once")).get(root);
    }

    @Test
    public void testInitializeoDeletedLdpResource() throws Exception {
        final ResourceService mockService = mock(ResourceService.class);
        when(mockBundler.getResourceService()).thenReturn(mockService);
        when(mockService.get(eq(root))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        when(mockService.create(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class), any(), any()))
            .thenReturn(completedFuture(null));

        final LdpResource matcher = new LdpResource(mockBundler);
        matcher.initialize();

        verify(mockService, description("A previously deleted root resource should be re-created upon initialization"))
            .create(eq(root), any(Session.class), any(IRI.class), any(Dataset.class), any(), any());
        verify(mockService, never().description("replace shouldn't be called when re-initializing a deleted root"))
            .replace(eq(root), any(Session.class), any(IRI.class), any(Dataset.class), any(), any());
        verify(mockService, description("Verify that the root resource is fetched only once")).get(root);
    }
}
