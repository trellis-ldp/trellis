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
package org.trellisldp.app;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Collections.emptySet;
import static org.apache.jena.query.DatasetFactory.createTxnMem;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;

import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.NoopNamespaceService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.id.UUIDGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A simple service bundler for testing.
 */
public class SimpleServiceBundler implements ServiceBundler {

    private final IdentifierService idService = new UUIDGenerator();
    private final MementoService mementoService = new NoopMementoService();
    private final AgentService agentService = new SimpleAgentService();
    private final IOService ioService = new JenaIOService(new NoopNamespaceService(), null, null, emptySet(),
            emptySet());
    private final BinaryService binaryService = new FileBinaryService(idService,
            resourceFilePath("data") + "/binaries", 2, 2);
    private final TriplestoreResourceService triplestoreService
        = new TriplestoreResourceService(connect(createTxnMem()), idService, new NoopEventService());

    @Override
    public AgentService getAgentService() {
        return agentService;
    }

    @Override
    public ResourceService getResourceService() {
        return triplestoreService;
    }

    @Override
    public IOService getIOService() {
        return ioService;
    }

    @Override
    public BinaryService getBinaryService() {
        return binaryService;
    }

    @Override
    public AuditService getAuditService() {
        return triplestoreService;
    }

    @Override
    public MementoService getMementoService() {
        return mementoService;
    }
}
