/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
package org.trellisldp.openliberty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.trellisldp.api.*;
import org.trellisldp.common.DefaultTimemapGenerator;
import org.trellisldp.common.TimemapGenerator;

/**
 * A managed bean that generates an RDF connection for the triplestore resource service.
 */
@ApplicationScoped
public class TrellisServiceSupplier {

    private EventService eventService = new NoopEventService();

    private MementoService mementoService = new NoopMementoService();

    private TimemapGenerator timemapGenerator = new DefaultTimemapGenerator();

    @Produces
    EventService getEventService() {
        return eventService;
    }

    @Produces
    MementoService getMementoService() {
        return mementoService;
    }

    @Produces
    TimemapGenerator getTimemapGenerator() {
        return timemapGenerator;
    }
}
