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
package org.trellisldp.http.core;

import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.ResourceService;

/**
 * The ServiceBundler interface collects all the services required for a full
 * Trellis application.
 */
public interface ServiceBundler {

    /**
     * Get the {@link ResourceService} for the application.
     * @return a service for interacting with resources.
     */
    ResourceService getResourceService();

    /**
     * Get the {@link IOService} for the application.
     * @return a service for handling RDF serialization and parsing.
     */
    IOService getIOService();

    /**
     * Get the {@link BinaryService} for the application.
     * @return a service for handling binary content.
     */
    BinaryService getBinaryService();

    /**
     * Get the {@link AuditService} for the application.
     * @return the service for handling audit events.
     */
    AuditService getAuditService();

    /**
     * Get the {@link MementoService} for the application.
     * @return the service for interacting with memento resources.
     */
    MementoService getMementoService();

    /**
     * Get the {@link EventService} for the application.
     * @return the service for emiting notifications.
     */
    EventService getEventService();

    /**
     * Get the constraint services for this application.
     * @return an interator of the constraint service(s).
     */
    Iterable<ConstraintService> getConstraintServices();

    /**
     * Get the TimemapGenerator for this application.
     * @return the service for generating TimeMaps.
     */
    TimemapGenerator getTimemapGenerator();
}
