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

import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.id.UUIDGenerator;
import org.trellisldp.test.AbstractResourceServiceTests;

/**
 * ResourceService tests.
 */
public class ResourceServiceTest extends AbstractResourceServiceTests {

    private static final JenaRDF rdf = new JenaRDF();
    private static final IdentifierService idService = new UUIDGenerator();

    private final JenaDataset dataset = rdf.createDataset();
    private final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
    private final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                new NoopMementoService(), new NoopEventService());
    private final Session session = new SimpleSession(rdf.createIRI("user:test"));

    @Override
    public ResourceService getResourceService() {
        return svc;
    }

    @Override
    public Session getSession() {
        return session;
    }

}
