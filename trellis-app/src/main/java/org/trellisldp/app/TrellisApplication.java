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

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.trellisldp.app.TrellisUtils.getAuthFilters;
import static org.trellisldp.app.TrellisUtils.getCorsConfiguration;
import static org.trellisldp.app.TrellisUtils.getNotificationService;
import static org.trellisldp.app.TrellisUtils.getRDFConnection;
import static org.trellisldp.app.TrellisUtils.getWebacConfiguration;

import com.google.common.cache.Cache;

import io.dropwizard.Application;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.jms.JMSException;

import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.agent.SimpleAgent;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.CacheService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.app.health.RDFConnectionHealthCheck;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.http.AgentAuthorizationFilter;
import org.trellisldp.http.CacheControlFilter;
import org.trellisldp.http.CrossOriginResourceSharingFilter;
import org.trellisldp.http.LdpResource;
import org.trellisldp.http.WebAcFilter;
import org.trellisldp.id.UUIDGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.triplestore.TriplestoreResourceService;
import org.trellisldp.webac.WebACService;

/**
 * @author acoburn
 */
public class TrellisApplication extends Application<TrellisConfiguration> {

    private Environment environment;

    private TrellisConfiguration config;

    private EventService notificationService;

    private IdentifierService idService;

    private MementoService mementoService;

    private ResourceService resourceService;

    private NamespaceService namespaceService;

    private BinaryService binaryService;

    private CacheService<String, String> profileCache;

    private IOService ioService;

    private AuditService auditService;

    /**
     * The main entry point.
     * @param args the argument list
     * @throws Exception if something goes horribly awry
     */
    public static void main(final String[] args) throws Exception {
        new TrellisApplication().run(args);
    }

    @Override
    public String getName() {
        return "Trellis LDP";
    }

    @Override
    public void initialize(final Bootstrap<TrellisConfiguration> bootstrap) {
        // Not currently used
    }

    protected void init(final TrellisConfiguration config, final Environment environment) {
        this.environment = environment;
        this.config = config;
        this.notificationService = buildNotificationService(environment);
        this.idService = buildIdService();
        this.mementoService = buildMementoService();
        this.resourceService = buildResourceService(idService, mementoService, notificationService);
        this.namespaceService = buildNamespaceService();
        this.binaryService = buildBinaryService(idService);
        this.profileCache = buildCacheService();
        this.ioService = buildIoService(namespaceService, profileCache);
        this.auditService = buildAuditService();
    }

    @Override
    public void run(final TrellisConfiguration config,
                    final Environment environment) throws Exception {
        init(config, environment);

        getAuthFilters(config).ifPresent(filters -> environment.jersey().register(new ChainedAuthFilter<>(filters)));

        // Resource matchers
        environment.jersey().register(new LdpResource(resourceService, ioService, binaryService, auditService,
                                    config.getBaseUrl()));

        // Filters
        environment.jersey().register(new AgentAuthorizationFilter(new SimpleAgent()));
        environment.jersey().register(new CacheControlFilter(config.getCacheMaxAge()));

        // Authorization
        getWebacConfiguration(config).ifPresent(webacCache -> {
                final WebAcFilter filter = new WebAcFilter(new WebACService(resourceService, webacCache));
                filter.setChallenges(asList("Authorization"));
                environment.jersey().register(filter);
        });

        // CORS
        getCorsConfiguration(config).ifPresent(cors -> environment.jersey().register(
                new CrossOriginResourceSharingFilter(cors.getAllowOrigin(), cors.getAllowMethods(),
                    cors.getAllowHeaders(), cors.getExposeHeaders(), cors.getAllowCredentials(), cors.getMaxAge())));
    }

    protected <T extends ResourceService> T buildResourceService(final IdentifierService idService,
                    final MementoService mementoService, final EventService notificationService) {
        final RDFConnection rdfConnection = getRDFConnection(config);
        // Health checks
        environment.healthChecks().register("rdfconnection", new RDFConnectionHealthCheck(rdfConnection));
        return (T) new TriplestoreResourceService(rdfConnection, idService, mementoService, notificationService);
    }

    protected AuditService buildAuditService() {
        return resourceService != null && resourceService instanceof AuditService
                        ? (AuditService) resourceService
                        : new DefaultAuditService();
    }

    protected TrellisCache<String, String> buildCacheService() {
        final Long cacheSize = config.getJsonld().getCacheSize();
        final Long hours = config.getJsonld().getCacheExpireHours();
        final Cache<String, String> cache = newBuilder().maximumSize(cacheSize).expireAfterAccess(hours, HOURS).build();
        return new TrellisCache<>(cache);
    }

    protected JenaIOService buildIoService(final NamespaceService namespaceService,
                    final CacheService<String, String> profileCache) {
        return new JenaIOService(namespaceService, profileCache, TrellisUtils.getAssetConfiguration(config));
    }

    protected FileBinaryService buildBinaryService(final IdentifierService idService) {
        return new FileBinaryService(idService, config.getBinaries(), config.getBinaryHierarchyLevels(),
                        config.getBinaryHierarchyLength());
    }

    protected NamespacesJsonContext buildNamespaceService() {
        return new NamespacesJsonContext(config.getNamespaces());
    }

    protected FileMementoService buildMementoService() {
        return new FileMementoService(config.getMementos());
    }

    protected UUIDGenerator buildIdService() {
        return new UUIDGenerator();
    }

    protected EventService buildNotificationService(final Environment environment) {
        try {
            return getNotificationService(config.getNotifications(), environment);
        } catch (JMSException e) {
            throw new RuntimeTrellisException(e);
        }
    }
}
