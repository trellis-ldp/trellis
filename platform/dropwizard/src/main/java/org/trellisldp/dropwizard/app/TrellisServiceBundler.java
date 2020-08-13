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
package org.trellisldp.dropwizard.app;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.HOURS;

import com.google.common.cache.Cache;

import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;

import org.apache.jena.rdfconnection.RDFConnection;
import org.jdbi.v3.core.Jdbi;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.DefaultIdentifierService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFaWriterService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.app.BaseServiceBundler;
import org.trellisldp.app.DefaultConstraintServices;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.cache.TrellisCache;
import org.trellisldp.common.DefaultTimemapGenerator;
import org.trellisldp.constraint.LdpConstraintService;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.file.FileNamespaceService;
import org.trellisldp.jdbc.DBNamespaceService;
import org.trellisldp.jdbc.DBResourceService;
import org.trellisldp.jdbc.DBWrappedMementoService;
import org.trellisldp.jena.JenaIOService;
import org.trellisldp.rdfa.DefaultRdfaWriterService;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A triplestore-based service bundler for Trellis.
 *
 * <p>This service bundler implementation is used with a Dropwizard-based application.
 * It combines a Triplestore-based resource service along with file-based binary and
 * memento storage. RDF processing is handled with Apache Jena.
 */
public class TrellisServiceBundler extends BaseServiceBundler {

    /**
     * Create a new application service bundler.
     * @param config the application configuration
     * @param environment the dropwizard environment
     */
    public TrellisServiceBundler(final AppConfiguration config, final Environment environment) {
        // Use a database connection
        final Jdbi jdbi = new JdbiFactory().build(environment, config.getDataSourceFactory(), "trellis");

        auditService = new DefaultAuditService();
        timemapGenerator = new DefaultTimemapGenerator();
        constraintServices = new DefaultConstraintServices(singletonList(new LdpConstraintService()));
        mementoService = buildMementoService(config, jdbi);
        resourceService = buildResourceService(config, environment, jdbi);
        binaryService = buildBinaryService(config);
        ioService = buildIoService(config, jdbi);
        eventService = AppUtils.getNotificationService(config.getNotifications(), environment);
    }

    static ResourceService buildResourceService(final AppConfiguration config,
            final Environment environment, final Jdbi jdbi) {
        if (useTriplestore(config)) {
            // Use a triplestore
            final RDFConnection rdfConnection = TriplestoreResourceService.buildRDFConnection(config.getResources());

            // Health checks
            environment.healthChecks().register("rdfconnection", new RDFConnectionHealthCheck(rdfConnection));
            return new TriplestoreResourceService(rdfConnection);
        }
        return new DBResourceService(jdbi);
    }

    static MementoService buildMementoService(final AppConfiguration config, final Jdbi jdbi) {
        final MementoService mementoService = new FileMementoService(config.getMementos(),
                config.getIsVersioningEnabled());
        if (useTriplestore(config)) {
            return mementoService;
        }
        return new DBWrappedMementoService(jdbi, mementoService);
    }

    static IOService buildIoService(final AppConfiguration config, final Jdbi jdbi) {
        final long cacheSize = config.getJsonld().getCacheSize();
        final long hours = config.getJsonld().getCacheExpireHours();
        final Cache<String, String> cache = newBuilder().maximumSize(cacheSize).expireAfterAccess(hours, HOURS).build();
        final TrellisCache<String, String> profileCache = new TrellisCache<>(cache);
        final NamespaceService namespaceService = useTriplestore(config) ?
            new FileNamespaceService(config.getNamespaces()) : new DBNamespaceService(jdbi);
        final RDFaWriterService htmlSerializer = new DefaultRdfaWriterService(namespaceService,
                config.getAssets().getTemplate(), config.getAssets().getCss(), config.getAssets().getJs(),
                config.getAssets().getIcon());
        return new JenaIOService(namespaceService, htmlSerializer, profileCache,
                config.getJsonld().getAllowedContexts(), config.getJsonld().getAllowedContextDomains(),
                config.getUseRelativeIris());
    }

    static boolean useTriplestore(final AppConfiguration config) {
        return config.getResources() != null;
    }

    static BinaryService buildBinaryService(final AppConfiguration config) {
        return new FileBinaryService(new DefaultIdentifierService(), config.getBinaries(),
                config.getBinaryHierarchyLevels(), config.getBinaryHierarchyLength());
    }
}
