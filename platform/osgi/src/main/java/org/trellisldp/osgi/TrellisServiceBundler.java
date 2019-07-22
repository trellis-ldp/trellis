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

import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * Trellis OSGi ServiceBundler module.
 */
public class TrellisServiceBundler implements ServiceBundler {

    private final AgentService agentService;
    private final AuditService auditService;
    private final BinaryService binaryService;
    private final EventService eventService;
    private final IOService ioService;
    private final MementoService mementoService;
    private final ResourceService resourceService;

    /**
     * Create a Trellis service bundler with existing services.
     * @param mementoService the MementoService
     * @param binaryService the BinaryService
     * @param ioService the IOService
     * @param agentService the AgentService
     * @param eventService the EventService
     * @param service the TriplestoreResourceService
     */
    public TrellisServiceBundler(final MementoService mementoService, final BinaryService binaryService,
            final IOService ioService, final AgentService agentService, final EventService eventService,
            final TriplestoreResourceService service) {
        this.mementoService = mementoService;
        this.agentService = agentService;
        this.ioService = ioService;
        this.binaryService = binaryService;
        this.eventService = eventService;
        this.auditService = service;
        this.resourceService = service;
    }

    @Override
    public AgentService getAgentService() {
        return agentService;
    }

    @Override
    public ResourceService getResourceService() {
        return resourceService;
    }

    @Override
    public IOService getIOService() {
        return ioService;
    }

    @Override
    public BinaryService getBinaryService() {
        return binaryService;
    }

    @Override
    public AuditService getAuditService() {
        return auditService;
    }

    @Override
    public MementoService getMementoService() {
        return mementoService;
    }

    @Override
    public EventService getEventService() {
        return eventService;
    }
}
