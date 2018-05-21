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

import static org.apache.jena.query.DatasetFactory.createTxnMem;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.apache.jena.rdfconnection.RDFConnectionRemote.create;
import static org.apache.jena.tdb2.DatabaseMgr.connectDatasetGraph;

import java.util.Optional;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.api.DatasetConnection;
import org.trellisldp.app.config.TrellisConfiguration;

/**
 * A {@link DatasetConnection} that provides a Jena RDFConnection implementation.
 */
public final class JenaRDFConnection {

    /**
     * get an RDFConnection.
     *
     * @param config a TrellisConfiguration
     * @return a Jena RDFConnection
     */
    public static DatasetConnection<RDFConnection> getRDFConnection(final TrellisConfiguration config) {
        final DatasetConnection<RDFConnection> conn = new DatasetConnection<>();
        final Optional<String> location = config.getResources().getDatasetLocation();
        if (location.isPresent()) {
            final String loc = location.get();
            if (loc.startsWith("http://") || loc.startsWith("https://")) {
                // Remote
                conn.setConnection(create().httpClient(buildHttpClient(config)).destination(loc).build());
                return conn;
            }
            // TDB2
            conn.setConnection(connect(wrap(connectDatasetGraph(loc))));
            return conn;
        }
        // in-memory
        conn.setConnection(connect(createTxnMem()));
        return conn;
    }

    private static HttpClient buildHttpClient(final TrellisConfiguration config) {
        final Optional<String> userName = config.getResources().getUserName();
        final Optional<String> password = config.getResources().getPassword();
        final CredentialsProvider provider = new BasicCredentialsProvider();
        if (userName.isPresent() && password.isPresent()) {
            final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                    userName.get(), password.get());
            provider.setCredentials(AuthScope.ANY, credentials);
        }
        return HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
    }

    private JenaRDFConnection() {
    }
}


