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
package org.trellisldp.osgi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.core.ServiceBundler;

@RunWith(MockitoJUnitRunner.class)
public class TrellisServiceBundlerTest {
    @Mock
    private AgentService agentService;

    @Mock
    private EventService eventService;

    @Mock
    private IOService ioService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private BinaryService binaryService;

    @Mock
    private MementoService mementoService;

    @Mock
    private AuditService auditService;

    @Test
    public void testServiceBundler() {
        final ServiceBundler bundler = new TrellisServiceBundler(mementoService, binaryService,
                ioService, agentService, eventService, auditService, resourceService);
        assertEquals(ioService, bundler.getIOService());
        assertEquals(auditService, bundler.getAuditService());
        assertEquals(resourceService, bundler.getResourceService());
        assertEquals(agentService, bundler.getAgentService());
        assertEquals(binaryService, bundler.getBinaryService());
        assertEquals(mementoService, bundler.getMementoService());
        assertEquals(eventService, bundler.getEventService());
    }
}
