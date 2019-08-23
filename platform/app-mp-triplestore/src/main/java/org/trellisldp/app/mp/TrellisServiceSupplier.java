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
package org.trellisldp.app.mp.triplestore;

import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.trellisldp.triplestore.TriplestoreResourceService.CONFIG_TRIPLESTORE_RDF_LOCATION;
import static org.trellisldp.triplestore.TriplestoreResourceService.buildRDFConnection;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.api.*;

/**
 * A managed bean that generates an RDF connection for the triplestore resource service.
 */
@ApplicationScoped
public class TrellisServiceSupplier {

    @Produces
    private RDFConnection rdfConnection;

    @Produces
    private EventService eventService = new NoopEventService();

    @PostConstruct
    private void init() {
        rdfConnection = buildRDFConnection(getConfig().getOptionalValue(CONFIG_TRIPLESTORE_RDF_LOCATION, String.class)
            .orElse(null));
    }
}
