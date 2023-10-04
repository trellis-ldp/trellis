/*
 * Copyright (c) Aaron Coburn and individual contributors
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
package org.trellisldp.triplestore;

import static org.trellisldp.triplestore.TriplestoreResourceService.CONFIG_TRIPLESTORE_RDF_LOCATION;
import static org.trellisldp.triplestore.TriplestoreResourceService.buildRDFConnection;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.Optional;

import org.apache.jena.rdfconnection.RDFConnection;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RDFConnectionProvider {

    @Inject
    @ConfigProperty(name = CONFIG_TRIPLESTORE_RDF_LOCATION)
    Optional<String> connectionString;

    private RDFConnection rdfConnection;

    /**
     * Create an RDFConnection bean.
     */
    @PostConstruct
    void init() {
        rdfConnection = buildRDFConnection(connectionString.orElse(null));
    }

    @Produces
    public RDFConnection getRdfConnection() {
        return rdfConnection;
    }
}
