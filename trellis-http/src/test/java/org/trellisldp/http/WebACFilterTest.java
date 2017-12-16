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

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.ACL;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(JUnitPlatform.class)
public class WebACFilterTest {

    private final static String REPO1 = "repo1";
    private final static String REPO2 = "repo2";
    private final static String REPO3 = "repo3";
    private final static String REPO4 = "repo4";

    private final static String BASE_URL = "http://example.org/";

    private final static Set<IRI> allModes = new HashSet<>();

    static {
        allModes.add(ACL.Append);
        allModes.add(ACL.Read);
        allModes.add(ACL.Write);
        allModes.add(ACL.Control);
    }

    @Mock
    private AccessControlService mockAccessControlService;

    @Mock
    private ContainerRequestContext mockContext;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private MultivaluedMap<String, String> mockQueryParams;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(allModes);
        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockUriInfo.getQueryParameters()).thenReturn(mockQueryParams);
        when(mockQueryParams.getOrDefault(eq("ext"), eq(emptyList()))).thenReturn(emptyList());
        when(mockUriInfo.getPath()).thenReturn(REPO1);
    }

    @Test
    public void testFilterUnknownMethod() throws Exception {
        when(mockContext.getMethod()).thenReturn("FOO");

        final WebAcFilter filter = new WebAcFilter(emptyList(), mockAccessControlService);

        assertThrows(NotAllowedException.class, () -> filter.filter(mockContext));
    }

    @Test
    public void testFilterAppend() throws Exception {
        final Set<IRI> modes = new HashSet<>();
        when(mockContext.getMethod()).thenReturn("POST");
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(modes);

        final WebAcFilter filter = new WebAcFilter(emptyList(), mockAccessControlService);
        modes.add(ACL.Append);
        filter.filter(mockContext);

        modes.add(ACL.Write);
        filter.filter(mockContext);

        modes.remove(ACL.Append);
        filter.filter(mockContext);

        modes.clear();
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext));
    }
}
