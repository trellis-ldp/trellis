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

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.RDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.api.DefaultIdentifierService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.ResourceService;
import org.trellisldp.test.AbstractResourceServiceTests;

/**
 * ResourceService tests.
 */
public class ResourceServiceTest extends AbstractResourceServiceTests {

    private static final RDF rdf = RDFFactory.getInstance();

    private final Dataset dataset = rdf.createDataset();
    private final RDFConnection rdfConnection = RDFConnection.connect(wrap(toJena(dataset)));
    private final ResourceService svc = buildResourceService(rdfConnection);

    @Override
    public ResourceService getResourceService() {
        return svc;
    }

    ResourceService buildResourceService(final RDFConnection connection) {
        final TriplestoreResourceService service = new TriplestoreResourceService();
        service.rdfConnection = connection;
        service.idService = new DefaultIdentifierService();
        service.initialize();
        return service;
    }
}
