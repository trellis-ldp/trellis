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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static java.util.ServiceLoader.load;
import static org.apache.jena.query.DatasetFactory.create;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.apache.jena.tdb2.DatabaseMgr.connectDatasetGraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.DatasetConnection;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.RuntimeTrellisException;
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
        final DatasetConnection<RDFConnection> conn = new DatasetConnection<>();
        if (isNull(location)) {
            conn.setConnection(connect(create()));
        } else if (location.startsWith("http://") || location.startsWith("https://")) {
            conn.setConnection(connect(location));
        } else {
            conn.setConnection(connect(wrap(connectDatasetGraph(location))));
        }

        final AgentService agentService = loadFirst(AgentService.class).orElseThrow(() ->
                new RuntimeTrellisException("No loadable AgentService on the classpath"));

        final IdentifierService idService = loadFirst(IdentifierService.class).orElseThrow(() ->
                new RuntimeTrellisException("No loadable IdentifierService on the classpath"));

        final EventService eventService = loadFirst(EventService.class).orElseGet(NoopEventService::new);

        final BinaryService binaryService = new FileBinaryService(idService);
        final MementoService mementoService = new FileMementoService();
        final NamespaceService namespaceService = new NamespacesJsonContext();
        final IOService ioService = new JenaIOService(namespaceService);

        final TriplestoreResourceService resourceService = new TriplestoreResourceService(
                conn, idService, mementoService, eventService);

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
                        asCollection(config.get("trellis.cors.allowOrigin")),
                        asCollection(config.get("trellis.cors.allowMethods")),
                        asCollection(config.get("trellis.cors.allowHeaders")),
                        asCollection(config.get("trellis.cors.exposeHeaders")),
                        false, // <- Allow-Credentials not supported
                        config.getOrDefault("trellis.cors.maxAge", Integer.class, 180)));
        }
    }

    private static Collection<String> asCollection(final String value) {
        return isNull(value) ? emptyList() :  asList(value.split("\\s*,\\s*"));
    }

    private static <T> Optional<T> loadFirst(final Class<T> service) {
        return of(load(service).iterator()).filter(Iterator::hasNext).map(Iterator::next);
    }
}
