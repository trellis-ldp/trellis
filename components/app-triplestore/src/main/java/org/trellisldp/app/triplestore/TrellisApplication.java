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

package org.trellisldp.app.triplestore;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.HOURS;

import com.google.common.cache.Cache;

import io.dropwizard.setup.Environment;

import java.util.Optional;

import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.DatasetConnection;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFaWriterService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.app.AbstractTrellisApplication;
import org.trellisldp.app.TrellisCache;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.id.UUIDGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.rdfa.HtmlSerializer;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A deployable Trellis application.
 */
public class TrellisApplication extends AbstractTrellisApplication<TrellisConfiguration> {

    private TriplestoreResourceService resourceService;

    private BinaryService binaryService;

    private IOService ioService;

    /**
     * The main entry point.
     *
     * @param args the argument list
     * @throws Exception if something goes horribly awry
     */
    public static void main(final String[] args) throws Exception {
        new TrellisApplication().run(args);
    }

    @Override
    protected ResourceService getResourceService() {
        return resourceService;
    }

    @Override
    protected IOService getIOService() {
        return ioService;
    }

    @Override
    protected BinaryService getBinaryService() {
        return binaryService;
    }

    @Override
    protected Optional<AuditService> getAuditService() {
        return of(resourceService);
    }

    @Override
    protected Optional<BinaryService.MultipartCapable> getMultipartUploadService() {
        return empty();
    }

    @Override
    protected void initialize(final TrellisConfiguration config, final Environment environment) {
        super.initialize(config, environment);

        final IdentifierService idService = new UUIDGenerator();

        this.resourceService = buildResourceService(idService, config, environment);
        this.binaryService = buildBinaryService(idService, config);
        this.ioService = buildIoService(config);
    }

    private TriplestoreResourceService buildResourceService(final IdentifierService idService, final
    TrellisConfiguration config, final Environment environment) {
        final MementoService mementoService = new FileMementoService(config.getMementos());
        final DatasetConnection datasetConnection = JenaRDFConnection.getRDFConnection(config);
        final EventService notificationService = AppUtils.getNotificationService(config.getNotifications(),
                environment);

        // Health checks
        environment.healthChecks().register("rdfConnection", new RDFConnectionHealthCheck(datasetConnection));
        return new TriplestoreResourceService(datasetConnection, idService, mementoService, notificationService);
    }

    private IOService buildIoService(final TrellisConfiguration config) {
        final Long cacheSize = config.getJsonld().getCacheSize();
        final Long hours = config.getJsonld().getCacheExpireHours();
        final Cache<String, String> cache = newBuilder().maximumSize(cacheSize).expireAfterAccess(hours, HOURS).build();
        final TrellisCache<String, String> profileCache = new TrellisCache<>(cache);
        final NamespaceService namespaceService = new NamespacesJsonContext(config.getNamespaces());
        final RDFaWriterService htmlSerializer = new HtmlSerializer(namespaceService, config.getAssets().getTemplate(),
                config.getAssets().getCss(), config.getAssets().getJs(), config.getAssets().getIcon());
        return new JenaIOService(namespaceService, htmlSerializer, profileCache,
                config.getJsonld().getContextWhitelist(), config.getJsonld().getContextDomainWhitelist());
    }

    private BinaryService buildBinaryService(final IdentifierService idService, final TrellisConfiguration config) {
        return new FileBinaryService(idService, config.getBinaries(), config.getBinaryHierarchyLevels(),
                config.getBinaryHierarchyLength());
    }
}
