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

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import com.google.common.cache.Cache;

import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.constraint.LdpConstraints;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.http.core.DefaultEtagGenerator;
import org.trellisldp.http.core.DefaultTimemapGenerator;
import org.trellisldp.http.core.EtagGenerator;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.TimemapGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A triplestore-based service bundler implementation for Trellis.
 *
 * <p>This implementation is experimental. It can be used with applications running in web containers
 * such as Tomcat or Jetty. This implementation combines a triplestore-based persistence layer with
 * file-based storage for mementos and binaries. RDF processing makes use of Apache Jena.
 */
public class WebappServiceBundler implements ServiceBundler {

    private final AgentService agentService;
    private final MementoService mementoService;
    private final AuditService auditService;
    private final TriplestoreResourceService resourceService;
    private final BinaryService binaryService;
    private final IOService ioService;
    private final EventService eventService;
    private final TimemapGenerator timemapGenerator;
    private final EtagGenerator etagGenerator;
    private final List<ConstraintService> constraintServices;

    /**
     * Create a new application service bundler.
     */
    public WebappServiceBundler() {
        final Config config = getConfig();
        final NamespaceService nsService = AppUtils.loadFirst(NamespaceService.class);
        final Cache<String, String> cache = newBuilder()
            .maximumSize(config.getOptionalValue("trellis.webapp.cache.size", Long.class).orElse(100L))
            .expireAfterAccess(config.getOptionalValue("trellis.webapp.cache.hours", Long.class).orElse(24L), HOURS)
            .build();
        final TrellisCache profileCache = new TrellisCache(cache);

        eventService = AppUtils.loadWithDefault(EventService.class, NoopEventService::new);
        agentService = AppUtils.loadFirst(AgentService.class);
        binaryService = new FileBinaryService();
        mementoService = new FileMementoService();
        ioService = new JenaIOService(nsService, null, profileCache);
        auditService = resourceService = new TriplestoreResourceService();
        constraintServices = singletonList(new LdpConstraints());
        timemapGenerator = new DefaultTimemapGenerator();
        etagGenerator = new DefaultEtagGenerator();
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

    @Override
    public EventService getEventService() {
        return eventService;
    }

    @Override
    public EtagGenerator getEtagGenerator() {
        return etagGenerator;
    }

    @Override
    public TimemapGenerator getTimemapGenerator() {
        return timemapGenerator;
    }

    @Override
    public Iterable<ConstraintService> getConstraintServices() {
        return constraintServices;
    }
}
