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
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.ACL;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TrellisWebDAVAuthzFilterTest {

    private static final Set<IRI> allModes = new HashSet<>();

    static {
        allModes.add(ACL.Read);
        allModes.add(ACL.Write);
        allModes.add(ACL.Append);
        allModes.add(ACL.Control);
    }

    @Mock
    private AccessControlService mockAccessControlService;

    @Mock
    private ContainerRequestContext mockContext;

    @Mock
    private MultivaluedMap<String, String> mockQueryParams;

    @Mock
    private UriInfo mockUriInfo;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(allModes);
        when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        when(mockQueryParams.getOrDefault(eq("ext"), eq(emptyList()))).thenReturn(emptyList());
        when(mockUriInfo.getPath()).thenReturn("");
        when(mockUriInfo.getQueryParameters()).thenReturn(mockQueryParams);
    }

    @Test
    public void testFilterUnknownMethod() throws Exception {
        when(mockContext.getMethod()).thenReturn("BAR");

        final TrellisWebDAVAuthzFilter filter = new TrellisWebDAVAuthzFilter(mockAccessControlService);
        assertDoesNotThrow(() -> filter.filter(mockContext), "Exception thrown with unknown method!");
    }

    @Test
    public void testFilterAppend() throws Exception {
        final Set<IRI> modes = new HashSet<>();
        when(mockContext.getMethod()).thenReturn("MKCOL");
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(modes);

        final TrellisWebDAVAuthzFilter filter = new TrellisWebDAVAuthzFilter(mockAccessControlService);
        modes.add(ACL.Append);
        assertDoesNotThrow(() -> filter.filter(mockContext), "Unexpected exception after adding acl:Append ability!");

        modes.add(ACL.Write);
        assertDoesNotThrow(() -> filter.filter(mockContext), "Unexpected exception after adding acl:Write ability!");

        modes.remove(ACL.Append);
        assertDoesNotThrow(() -> filter.filter(mockContext), "Unexpected exception after removing acl:Append ability!");

        modes.clear();
        assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext),
                "No expception thrown when not authorized!");
    }

    @Test
    public void testFilterChallenges() throws Exception {
        when(mockContext.getMethod()).thenReturn("MKCOL");
        when(mockAccessControlService.getAccessModes(any(IRI.class), any(Session.class))).thenReturn(emptySet());

        final TrellisWebDAVAuthzFilter filter = new TrellisWebDAVAuthzFilter(mockAccessControlService,
                asList("Foo", "Bar"), "my-realm");

        final List<Object> challenges = assertThrows(NotAuthorizedException.class, () -> filter.filter(mockContext),
                "No auth exception thrown with no access modes!").getChallenges();

        assertTrue(challenges.contains("Foo realm=\"my-realm\""), "Foo not among challenges!");
        assertTrue(challenges.contains("Bar realm=\"my-realm\""), "Bar not among challenges!");
    }
}
