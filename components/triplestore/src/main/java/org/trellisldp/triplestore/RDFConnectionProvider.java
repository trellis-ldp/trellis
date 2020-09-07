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
package org.trellisldp.triplestore;

import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.trellisldp.triplestore.TriplestoreResourceService.CONFIG_TRIPLESTORE_RDF_LOCATION;
import static org.trellisldp.triplestore.TriplestoreResourceService.buildRDFConnection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.jena.rdfconnection.RDFConnection;

@ApplicationScoped
public class RDFConnectionProvider {

    private final RDFConnection rdfConnection;

    /**
     * Create an RDFConnection bean.
     */
    public RDFConnectionProvider() {
        this(buildRDFConnection(getConfig()
                    .getOptionalValue(CONFIG_TRIPLESTORE_RDF_LOCATION, String.class).orElse(null)));
    }

    /**
     * Create an RDFConnection provider with a user-supplied connection.
     * @param rdfConnection the RDF connection
     */
    public RDFConnectionProvider(final RDFConnection rdfConnection) {
        this.rdfConnection = rdfConnection;
    }

    @Produces
    public RDFConnection getRdfConnection() {
        return rdfConnection;
    }
}
