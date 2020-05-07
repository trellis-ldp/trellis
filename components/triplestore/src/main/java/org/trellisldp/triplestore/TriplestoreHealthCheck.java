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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.jena.rdfconnection.RDFConnection;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

/**
 * Check the health of the RDF connection.
 */
@Liveness
@Readiness
@ApplicationScoped
public class TriplestoreHealthCheck implements HealthCheck {

    private final RDFConnection rdfConnection;

    /**
     * For use with RESTeasy and CDI proxies.
     *
     * @apiNote This construtor is used by CDI runtimes that require a public, no-argument constructor.
     *          It should not be invoked directly in user code.
     */
    public TriplestoreHealthCheck() {
        this(null);
    }

    /**
     * Create an object that checks the health of an RDF Connection.
     * @param rdfConnection the RDF Connection
     */
    @Inject
    public TriplestoreHealthCheck(final RDFConnection rdfConnection) {
        this.rdfConnection = rdfConnection;
    }

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named(TriplestoreHealthCheck.class.getSimpleName())
            .state(rdfConnection != null && !rdfConnection.isClosed()).build();
    }
}
