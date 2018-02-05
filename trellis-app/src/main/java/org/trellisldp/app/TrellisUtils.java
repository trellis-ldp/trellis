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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.apache.jena.query.DatasetFactory.createTxnMem;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.apache.jena.tdb2.DatabaseMgr.connectDatasetGraph;

import com.google.common.cache.Cache;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.rdf.api.IRI;
import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.api.CacheService;
import org.trellisldp.app.auth.AnonymousAuthFilter;
import org.trellisldp.app.auth.AnonymousAuthenticator;
import org.trellisldp.app.auth.BasicAuthenticator;
import org.trellisldp.app.auth.JwtAuthenticator;
import org.trellisldp.app.config.AuthConfiguration;
import org.trellisldp.app.config.CORSConfiguration;
import org.trellisldp.app.config.TrellisConfiguration;

/**
 * @author acoburn
 */
final class TrellisUtils {

    public static Map<String, String> getAssetConfiguration(final TrellisConfiguration config) {
        final Map<String, String> assetMap = new HashMap<>();
        assetMap.put("icon", config.getAssets().getIcon());
        assetMap.put("css", config.getAssets().getCss().stream().map(String::trim).collect(joining(",")));
        assetMap.put("js", config.getAssets().getJs().stream().map(String::trim).collect(joining(",")));

        return assetMap;
    }

    public static Optional<List<AuthFilter>> getAuthFilters(final TrellisConfiguration config) {
        // Authentication
        final List<AuthFilter> filters = new ArrayList<>();
        final AuthConfiguration auth = config.getAuth();

        if (auth.getJwt().getEnabled()) {
            filters.add(new OAuthCredentialAuthFilter.Builder<Principal>()
                    .setAuthenticator(new JwtAuthenticator(auth.getJwt().getKey(), auth.getJwt().getBase64Encoded()))
                    .setPrefix("Bearer")
                    .buildAuthFilter());
        }

        if (auth.getBasic().getEnabled()) {
            filters.add(new BasicCredentialAuthFilter.Builder<Principal>()
                    .setAuthenticator(new BasicAuthenticator(auth.getBasic().getUsersFile()))
                    .setRealm("Trellis Basic Authentication")
                    .buildAuthFilter());
        }

        if (auth.getAnon().getEnabled()) {
            filters.add(new AnonymousAuthFilter.Builder()
                .setAuthenticator(new AnonymousAuthenticator())
                .buildAuthFilter());
        }

        if (filters.isEmpty()) {
            return empty();
        }
        return of(filters);
    }

    public static Optional<CacheService<String, Set<IRI>>> getWebacConfiguration(final TrellisConfiguration config) {
        if (config.getAuth().getWebac().getEnabled()) {
            final Cache<String, Set<IRI>> authCache = newBuilder().maximumSize(config.getAuth().getWebac()
                    .getCacheSize()).expireAfterWrite(config.getAuth().getWebac()
                    .getCacheExpireSeconds(), SECONDS).build();
            return of(new TrellisCache<>(authCache));
        }
        return empty();
    }

    public static RDFConnection getRDFConnection(final TrellisConfiguration config) {
        final Optional<String> location = ofNullable(config.getResources());
        if (location.isPresent()) {
            final String loc = location.get();
            if (loc.startsWith("http://") || loc.startsWith("https://")) {
                // Remote
                return connect(loc);
            }
            // TDB2
            return connect(wrap(connectDatasetGraph(loc)));
        }
        // in-memory
        return connect(createTxnMem());
    }

    public static Optional<CORSConfiguration> getCorsConfiguration(final TrellisConfiguration config) {
        if (config.getCors().getEnabled()) {
            return of(config.getCors());
        }
        return empty();
    }

    private TrellisUtils() {
        // prevent instantiation
    }
}
