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
package org.trellisldp.app.triplestore;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.HOURS;

import com.google.common.cache.Cache;

import io.dropwizard.setup.Environment;

import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.DefaultIdentifierService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFaWriterService;
import org.trellisldp.app.BaseServiceBundler;
import org.trellisldp.app.DefaultConstraintServices;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.cache.TrellisCache;
import org.trellisldp.constraint.LdpConstraintService;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.file.FileNamespaceService;
import org.trellisldp.http.core.DefaultTimemapGenerator;
import org.trellisldp.io.JenaIOService;
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
        auditService = new DefaultAuditService();
        mementoService = new FileMementoService(config.getMementos(), config.getIsVersioningEnabled());
        timemapGenerator = new DefaultTimemapGenerator();
        constraintServices = new DefaultConstraintServices(singletonList(new LdpConstraintService()));
        resourceService = buildResourceService(config, environment);
        binaryService = buildBinaryService(config);
        ioService = buildIoService(config);
        eventService = AppUtils.getNotificationService(config.getNotifications(), environment);
    }

    private static TriplestoreResourceService buildResourceService(final AppConfiguration config,
            final Environment environment) {
        final RDFConnection rdfConnection = TriplestoreResourceService.buildRDFConnection(config.getResources());

        // Health checks
        environment.healthChecks().register("rdfconnection", new RDFConnectionHealthCheck(rdfConnection));
        return new TriplestoreResourceService(rdfConnection);
    }

    private static IOService buildIoService(final AppConfiguration config) {
        final long cacheSize = config.getJsonld().getCacheSize();
        final long hours = config.getJsonld().getCacheExpireHours();
        final Cache<String, String> cache = newBuilder().maximumSize(cacheSize).expireAfterAccess(hours, HOURS).build();
        final TrellisCache<String, String> profileCache = new TrellisCache<>(cache);
        final NamespaceService namespaceService = new FileNamespaceService(config.getNamespaces());
        final RDFaWriterService htmlSerializer = new DefaultRdfaWriterService(namespaceService,
                config.getAssets().getTemplate(), config.getAssets().getCss(), config.getAssets().getJs(),
                config.getAssets().getIcon());
        return new JenaIOService(namespaceService, htmlSerializer, profileCache,
                config.getJsonld().getContextWhitelist(), config.getJsonld().getContextDomainWhitelist(),
                config.getUseRelativeIris());
    }

    private static BinaryService buildBinaryService(final AppConfiguration config) {
        return new FileBinaryService(new DefaultIdentifierService(), config.getBinaries(),
                config.getBinaryHierarchyLevels(), config.getBinaryHierarchyLength());
    }
}
