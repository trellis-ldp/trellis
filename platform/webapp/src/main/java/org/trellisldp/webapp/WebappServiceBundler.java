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
package org.trellisldp.webapp;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A triplestore-based service bundler implementation for Trellis.
 *
 * <p>This implementation is experimental. It can be used with applications running in web containers
 * such as Tomcat or Jetty. This implementation combines a triplestore-based persistence layer with
 * file-based storage for mementos and binaries. RDF processing makes use of Apache Jena.
 */
public class WebappServiceBundler implements ServiceBundler {

    private static final Configuration config = ConfigurationProvider.getConfiguration();

    private final AgentService agentService;
    private final MementoService mementoService;
    private final AuditService auditService;
    private final TriplestoreResourceService resourceService;
    private final BinaryService binaryService;
    private final IOService ioService;

    /**
     * Create a new application service bundler.
     */
    public WebappServiceBundler() {
        final IdentifierService idService = AppUtils.loadFirst(IdentifierService.class);
        final RDFConnection rdfConnection = AppUtils.getRDFConnection(config.get("trellis.rdf.location"));
        final EventService eventService = AppUtils.loadWithDefault(EventService.class, NoopEventService::new);

        agentService = AppUtils.loadFirst(AgentService.class);
        binaryService = new FileBinaryService(idService);
        mementoService = new FileMementoService();
        ioService = new JenaIOService(new NamespacesJsonContext());
        auditService = resourceService = new TriplestoreResourceService(rdfConnection, idService, eventService);
    }

    @Override
    public IOService getIOService() {
        return ioService;
    }

    @Override
    public ResourceService getResourceService() {
        return resourceService;
    }

    @Override
    public BinaryService getBinaryService() {
        return binaryService;
    }

    @Override
    public AgentService getAgentService() {
        return agentService;
    }

    @Override
    public AuditService getAuditService() {
        return auditService;
    }

    @Override
    public MementoService getMementoService() {
        return mementoService;
    }
}
