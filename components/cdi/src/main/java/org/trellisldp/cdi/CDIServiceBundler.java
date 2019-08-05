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
package org.trellisldp.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.trellisldp.api.*;
import org.trellisldp.http.core.*;

/**
 * A ServiceBundler that supplies injected components for a Trellis application.
 */
@ApplicationScoped
public class CDIServiceBundler implements ServiceBundler {

    @Inject
    private MementoService mementoService;

    @Inject
    private BinaryService binaryService;

    @Inject
    private AgentService agentService;

    @Inject
    private IOService ioService;

    @Inject
    private EventService eventService;

    @Inject
    private Instance<ConstraintService> constraintServices;

    @Inject
    private TimemapGenerator timemapGenerator;

    @Inject
    private EtagGenerator etagGenerator;

    @Inject
    private ResourceService resourceService;

    @Inject
    private AuditService auditService;

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

    @Override
    public EtagGenerator getEtagGenerator() {
        return etagGenerator;
    }

    @Override
    public TimemapGenerator getTimemapGenerator() {
        return timemapGenerator;
    }

    @Override
    public Iterable<ConstraintService> getConstraintServices() {
        return constraintServices;
    }
}
