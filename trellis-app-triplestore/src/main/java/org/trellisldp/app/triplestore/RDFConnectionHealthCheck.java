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
package org.trellisldp.app.triplestore;

import static com.codahale.metrics.health.HealthCheck.Result.healthy;
import static com.codahale.metrics.health.HealthCheck.Result.unhealthy;

import com.codahale.metrics.health.HealthCheck;

import org.apache.jena.rdfconnection.RDFConnection;

/**
 * Check the health of the RDF connection.
 */
public class RDFConnectionHealthCheck extends HealthCheck {

    private final RDFConnection rdfConnection;

    /**
     * Create an object that checks the health of an RDF Connection.
     * @param rdfConnection the RDF Connection
     */
    public RDFConnectionHealthCheck(final RDFConnection rdfConnection) {
        this.rdfConnection = rdfConnection;
    }

    @Override
    protected HealthCheck.Result check() throws InterruptedException {
        return rdfConnection.isClosed() ? unhealthy("RDF Connection is closed.") : healthy("RDF Connection is open.");
    }
}
