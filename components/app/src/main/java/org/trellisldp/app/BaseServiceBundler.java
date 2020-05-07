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
package org.trellisldp.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.trellisldp.api.*;
import org.trellisldp.http.core.*;

/**
 * A {@link ServiceBundler} that supplies components for a Trellis application from its fields.
 *
 * <p>In this class, the fields are expected to be filled by injection, but in subclasses
 * the fields may be filled by any appropriate means.
 */
@ApplicationScoped
public class BaseServiceBundler implements ServiceBundler {

    @Inject
    protected MementoService mementoService;

    @Inject
    protected BinaryService binaryService;

    @Inject
    protected IOService ioService;

    @Inject
    protected EventService eventService;

    @Inject
    protected TimemapGenerator timemapGenerator;

    @Inject
    protected ResourceService resourceService;

    @Inject
    protected AuditService auditService;

    @Inject
    protected ConstraintServices constraintServices;

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
    public TimemapGenerator getTimemapGenerator() {
        return timemapGenerator;
    }

    @Override
    public Iterable<ConstraintService> getConstraintServices() {
        return constraintServices;
    }
}
