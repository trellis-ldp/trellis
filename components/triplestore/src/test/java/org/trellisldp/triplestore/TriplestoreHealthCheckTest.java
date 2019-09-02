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
package org.trellisldp.triplestore;

import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

public class TriplestoreHealthCheckTest {

    private static final JenaRDF rdf = new JenaRDF();

    @Test
    public void testUnhealthyDefault() {
        final HealthCheck check = new TriplestoreHealthCheck();
        assertEquals(HealthCheckResponse.State.DOWN, check.call().getState(), "RDFConnection isn't healthy!");
    }

    @Test
    public void testHealthy() {
        final JenaDataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreHealthCheck check = new TriplestoreHealthCheck();
        check.setRdfConnection(rdfConnection);
        assertEquals(HealthCheckResponse.State.UP, check.call().getState(), "RDFConnection isn't healthy!");
    }

    @Test
    public void testUnhealthy() {
        final JenaDataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        rdfConnection.close();
        final TriplestoreHealthCheck check = new TriplestoreHealthCheck();
        check.setRdfConnection(rdfConnection);
        assertEquals(HealthCheckResponse.State.DOWN, check.call().getState(),
                "Closed RDFConnection doesn't report as unhealthy!");
    }
}
