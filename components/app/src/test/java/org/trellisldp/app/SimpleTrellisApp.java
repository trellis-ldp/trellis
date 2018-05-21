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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.jena.query.DatasetFactory.createTxnMem;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.mockito.Mockito.mock;

import io.dropwizard.setup.Environment;

import java.util.Optional;

import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.DatasetConnection;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NoopEventService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.NoopNamespaceService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.file.FileBinaryService;
import org.trellisldp.id.UUIDGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A simple test app.
 */
public class SimpleTrellisApp extends AbstractTrellisApplication<TrellisConfiguration> {

    private TriplestoreResourceService resourceService;
    private FileBinaryService binaryService;
    private JenaIOService ioService;

    @Override
    protected ResourceService getResourceService() {
        return resourceService;
    }

    @Override
    protected BinaryService getBinaryService() {
        return binaryService;
    }

    @Override
    protected IOService getIOService() {
        return ioService;
    }

    @Override
    protected Optional<AuditService> getAuditService() {
        return empty();
    }

    @Override
    protected Optional<BinaryService.MultipartCapable> getMultipartUploadService() {
        return of(mock(BinaryService.MultipartCapable.class));
    }

    @Override
    protected void initialize(final TrellisConfiguration config, final Environment env) {
        super.initialize(config, env);
        ioService = new JenaIOService(new NoopNamespaceService(), null, null, emptySet(), emptySet());
        binaryService = new FileBinaryService(new UUIDGenerator(), resourceFilePath("data") + "/binaries", 2, 2);
        final DatasetConnection<RDFConnection> conn = new DatasetConnection<>();
        conn.setConnection(connect(createTxnMem()));
        resourceService = new TriplestoreResourceService(conn, new UUIDGenerator(),
                new NoopMementoService(), new NoopEventService());
    }
}
