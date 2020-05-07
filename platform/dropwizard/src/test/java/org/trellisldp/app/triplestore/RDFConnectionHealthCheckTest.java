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
package org.trellisldp.app.triplestore;

import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.junit.jupiter.api.Assertions.*;

import com.codahale.metrics.health.HealthCheck;

import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class RDFConnectionHealthCheckTest {

    private static final JenaRDF rdf = new JenaRDF();

    @Test
    void testIsConnected() {
        final JenaDataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final HealthCheck check = new RDFConnectionHealthCheck(rdfConnection);
        assertTrue(check.execute().isHealthy(), "RDFConnection isn't healthy!");
    }

    @Test
    void testNonConnected() {
        final JenaDataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        rdfConnection.close();
        final HealthCheck check = new RDFConnectionHealthCheck(rdfConnection);
        assertFalse(check.execute().isHealthy(), "Closed RDFConnection doesn't report as unhealthy!");
    }
}
