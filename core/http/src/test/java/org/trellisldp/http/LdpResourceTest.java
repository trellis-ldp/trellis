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
import static java.util.Collections.singleton;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.http.domain.LdpRequest;

/**
 * @author acoburn
 */
public class LdpResourceTest extends AbstractLdpResourceTest {

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

    @Mock
    private ContainerResponseContext mockResponseContext;

    @Override
    public Application configure() {

        initMocks(this);

        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);

        final ResourceConfig config = new ResourceConfig();

        config.register(new LdpResource(mockResourceService, ioService, mockBinaryService, mockAgentService,
                    mockAuditService));
        config.register(new AgentAuthorizationFilter(mockAgentService));
        config.register(new MultipartUploader(mockResourceService, mockBinaryResolver));
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

        final LdpResource filter = new LdpResource(mockResourceService, ioService, mockBinaryService, mockAgentService,
                    mockAuditService);

        filter.filter(mockContext);
        verify(mockContext, never()).abortWith(any());
    }

    @Test
    public void testNoBaseURL() throws Exception {
        final LdpResource matcher = new LdpResource(mockResourceService, ioService, mockBinaryService, mockAgentService,
                    mockAuditService, null);

        when(mockLdpRequest.getPath()).thenReturn("repo1/resource");
        when(mockLdpRequest.getBaseUrl()).thenReturn("http://my.example.com/");
        when(mockLdpRequest.getHeaders()).thenReturn(mockHttpHeaders);
        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(asList(WILDCARD_TYPE));
        when(mockLdpRequest.getRequest()).thenReturn(mockRequest);

        final Response res = matcher.getResourceHeaders(mockLdpRequest);
        assertTrue(getLinks(res).stream().anyMatch(l ->
                    l.getRel().equals("self") && l.getUri().toString().startsWith("http://my.example.com/")));
    }

    @Test
    public void testMultipartFilter() throws Exception {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("ext", "uploads");
        when(mockContext.getMethod()).thenReturn("POST");
        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://my.example.com/"));
        when(mockUriInfo.getPath()).thenReturn("uploads");
        when(mockUriInfo.getQueryParameters()).thenReturn(params);
        when(mockBinaryResolver.initiateUpload(any(), any())).thenReturn("upload-id");

        final MultipartUploader filter = new MultipartUploader(mockResourceService, mockBinaryResolver, null);
        filter.filter(mockContext);
        verify(mockContext).abortWith(any());
    }

    @Test
    public void testMultipartPostFilter() throws Exception {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("ext", "uploads");
        when(mockResponseContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(mockResponseContext.getLinks()).thenReturn(singleton(Link.fromUri("http://www.w3.org/ns/ldp#NonRDFSource")
                    .rel("type").build()));
        when(mockContext.getMethod()).thenReturn("GET");
        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://my.example.com/"));
        when(mockUriInfo.getPath()).thenReturn("uploads/upload-id");

        when(mockUriInfo.getQueryParameters()).thenReturn(params);
        when(mockBinaryResolver.initiateUpload(any(), any())).thenReturn("upload-id");

        final MultipartUploader filter = new MultipartUploader(mockResourceService, mockBinaryResolver, null);
        filter.filter(mockContext, mockResponseContext);

        verify(mockContext, never()).abortWith(any());
    }
}
