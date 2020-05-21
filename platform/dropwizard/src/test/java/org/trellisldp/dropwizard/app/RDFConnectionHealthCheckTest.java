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
package org.trellisldp.dropwizard.app;

import static org.apache.jena.commonsrdf.JenaCommonsRDF.toJena;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.junit.jupiter.api.Assertions.*;

import com.codahale.metrics.health.HealthCheck;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.RDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.RDFFactory;

/**
 * @author acoburn
 */
class RDFConnectionHealthCheckTest {

    private static final RDF rdf = RDFFactory.getInstance();

    @Test
    void testIsConnected() {
        final Dataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(toJena(dataset)));
        final HealthCheck check = new RDFConnectionHealthCheck(rdfConnection);
        assertTrue(check.execute().isHealthy(), "RDFConnection isn't healthy!");
    }

    @Test
    void testNonConnected() {
        final Dataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(toJena(dataset)));
        rdfConnection.close();
        final HealthCheck check = new RDFConnectionHealthCheck(rdfConnection);
        assertFalse(check.execute().isHealthy(), "Closed RDFConnection doesn't report as unhealthy!");
    }
}
