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
package org.trellisldp.dropwizard;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.apache.jena.query.DatasetFactory.createTxnMem;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;

import org.trellisldp.api.DefaultIdentifierService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.NoopNamespaceService;
import org.trellisldp.app.BaseServiceBundler;
import org.trellisldp.app.DefaultConstraintServices;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.constraint.LdpConstraintService;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.http.core.DefaultTimemapGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.io.NoopProfileCache;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A simple service bundler for testing.
 */
public class SimpleServiceBundler extends BaseServiceBundler {

    SimpleServiceBundler() {
        final TriplestoreResourceService triplestoreService
                = new TriplestoreResourceService(connect(createTxnMem()));
        triplestoreService.initialize();
        resourceService = triplestoreService;
        auditService = new DefaultAuditService();
        mementoService = new NoopMementoService();
        eventService = new NoopEventService();
        timemapGenerator = new DefaultTimemapGenerator();
        constraintServices = new DefaultConstraintServices(singletonList(new LdpConstraintService()));
        ioService = new JenaIOService(new NoopNamespaceService(), null, new NoopProfileCache(),
                emptySet(), emptySet(), false);
        binaryService = new FileBinaryService(new DefaultIdentifierService(),
                resourceFilePath("data") + "/binaries", 2, 2);
    }
}
