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

package org.trellisldp.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.commons.rdf.api.IRI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.vocabulary.ACL;

public class NoopAccessControlServiceTest {

    @Mock
    private Session mockSession;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testAccessControl() {
        final IRI resource = TrellisUtils.getInstance().createIRI("trellis:data/resource");
        final AccessControlService svc = new NoopAccessControlService();
        assertTrue(svc.getAccessModes(resource, mockSession).contains(ACL.Control));
        assertTrue(svc.getAccessModes(resource, mockSession).contains(ACL.Read));
        assertTrue(svc.getAccessModes(resource, mockSession).contains(ACL.Write));
        assertTrue(svc.getAccessModes(resource, mockSession).contains(ACL.Append));
    }
}
