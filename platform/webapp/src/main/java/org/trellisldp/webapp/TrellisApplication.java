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

import static java.util.Objects.isNull;
import static org.apache.jena.query.DatasetFactory.createTxnMem;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.apache.jena.tdb2.DatabaseMgr.connectDatasetGraph;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.http.AgentAuthorizationFilter;
import org.trellisldp.http.CacheControlFilter;
import org.trellisldp.http.CrossOriginResourceSharingFilter;
import org.trellisldp.http.LdpResource;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A Trellis application.
 */
public class TrellisApplication extends ResourceConfig {

    private static final Configuration config = ConfigurationProvider.getConfiguration();

    /**
     * Create a Trellis application.
     */
    public TrellisApplication() {
        super();

        final String location = config.get("trellis.rdf.location");
        final RDFConnection rdfConnection;
        if (isNull(location)) {
            rdfConnection = connect(createTxnMem());
        } else if (location.startsWith("http://") || location.startsWith("https://")) {
            rdfConnection = connect(location);
        } else {
            rdfConnection = connect(wrap(connectDatasetGraph(location)));
        }

        final AgentService agentService = AppUtils.loadFirst(AgentService.class);

        final IdentifierService idService = AppUtils.loadFirst(IdentifierService.class);

        final EventService eventService = AppUtils.loadWithDefault(EventService.class, NoopEventService::new);

        final BinaryService binaryService = new FileBinaryService(idService);
        final MementoService mementoService = new FileMementoService();
        final NamespaceService namespaceService = new NamespacesJsonContext();
        final IOService ioService = new JenaIOService(namespaceService);

        final TriplestoreResourceService resourceService = new TriplestoreResourceService(
                rdfConnection, idService, mementoService, eventService);

        register(new LdpResource(resourceService, ioService, binaryService, agentService, resourceService));
        register(new AgentAuthorizationFilter(agentService));

        if (config.getOrDefault("trellis.cache.enabled", Boolean.class, false)) {
            register(new CacheControlFilter(
                        config.getOrDefault("trellis.cache.maxAge", Integer.class, 86400),
                        config.getOrDefault("trellis.cache.mustRevalidate", Boolean.class, true),
                        config.getOrDefault("trellis.cache.noCache", Boolean.class, false)));
        }

        if (config.getOrDefault("trellis.cors.enabled", Boolean.class, false)) {
            register(new CrossOriginResourceSharingFilter(
                        AppUtils.asCollection(config.get("trellis.cors.allowOrigin")),
                        AppUtils.asCollection(config.get("trellis.cors.allowMethods")),
                        AppUtils.asCollection(config.get("trellis.cors.allowHeaders")),
                        AppUtils.asCollection(config.get("trellis.cors.exposeHeaders")),
                        false, // <- Allow-Credentials not supported
                        config.getOrDefault("trellis.cors.maxAge", Integer.class, 180)));
        }
    }
}
