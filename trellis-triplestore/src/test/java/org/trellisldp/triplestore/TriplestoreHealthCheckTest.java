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

import static org.apache.jena.commonsrdf.JenaCommonsRDF.toJena;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.RDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.RDFFactory;

class TriplestoreHealthCheckTest {

    private static final RDF rdf = RDFFactory.getInstance();

    @Test
    void testUnhealthyDefault() {
        final HealthCheck check = new TriplestoreHealthCheck();
        assertEquals(HealthCheckResponse.Status.DOWN, check.call().getStatus(), "RDFConnection isn't healthy!");
    }

    @Test
    void testHealthy() {
        final Dataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = RDFConnection.connect(wrap(toJena(dataset)));
        final HealthCheck check = new TriplestoreHealthCheck(rdfConnection);
        assertEquals(HealthCheckResponse.Status.UP, check.call().getStatus(), "RDFConnection isn't healthy!");
    }

    @Test
    void testUnhealthy() {
        final Dataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = RDFConnection.connect(wrap(toJena(dataset)));
        rdfConnection.close();
        final HealthCheck check = new TriplestoreHealthCheck(rdfConnection);
        assertEquals(HealthCheckResponse.Status.DOWN, check.call().getStatus(),
                "Closed RDFConnection doesn't report as unhealthy!");
    }
}
