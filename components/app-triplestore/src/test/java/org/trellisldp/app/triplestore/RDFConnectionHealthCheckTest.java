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

import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codahale.metrics.health.HealthCheck;

import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.DatasetConnection;

/**
 * @author acoburn
 */
public class RDFConnectionHealthCheckTest {

    private static final JenaRDF rdf = new JenaRDF();

    @Test
    public void testIsConnected() {
        final JenaDataset dataset = rdf.createDataset();
        final DatasetConnection<RDFConnection> conn = new DatasetConnection<>();
        conn.setConnection(connect(wrap(dataset.asJenaDatasetGraph())));
        final HealthCheck check = new RDFConnectionHealthCheck(conn);
        assertTrue(check.execute().isHealthy());
    }

    @Test
    public void testNonConnected() {
        final JenaDataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        rdfConnection.close();
        final DatasetConnection<RDFConnection> conn = new DatasetConnection<>();
        conn.setConnection(rdfConnection);
        final HealthCheck check = new RDFConnectionHealthCheck(conn);
        assertFalse(check.execute().isHealthy());
    }
}
