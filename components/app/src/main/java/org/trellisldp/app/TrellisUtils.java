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
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.cache.Cache;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.rdf.api.IRI;
import org.slf4j.Logger;
import org.trellisldp.api.CacheService;
import org.trellisldp.app.auth.AnonymousAuthFilter;
import org.trellisldp.app.auth.AnonymousAuthenticator;
import org.trellisldp.app.auth.BasicAuthenticator;
import org.trellisldp.app.auth.FederatedJwtAuthenticator;
import org.trellisldp.app.auth.JwtAuthenticator;
import org.trellisldp.app.config.AuthConfiguration;
import org.trellisldp.app.config.CORSConfiguration;
import org.trellisldp.app.config.JwtAuthConfiguration;
import org.trellisldp.app.config.TrellisConfiguration;

/**
 * Convenience utilities for the trellis-app.
 */
final class TrellisUtils {

    private static final Logger LOGGER = getLogger(TrellisUtils.class);

    public static Optional<Authenticator<String, Principal>> getJwtAuthenticator(final JwtAuthConfiguration config) {
        if (nonNull(config.getKeyStore())) {
            final File keystoreFile = new File(config.getKeyStore());
            if (keystoreFile.exists()) {
                try (final FileInputStream fis = new FileInputStream(keystoreFile)) {
                    final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    ks.load(fis, config.getKeyStorePassword().toCharArray());
                    final List<String> keyIds = filterKeyIds(ks, config.getKeyIds());
                    switch (keyIds.size()) {
                        case 0:
                            return empty();
                        case 1:
                            return of(new JwtAuthenticator(ks.getCertificate(keyIds.get(0)).getPublicKey()));
                        default:
                            return of(new FederatedJwtAuthenticator(ks, keyIds));
                    }
                } catch (final IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException ex) {
                    LOGGER.error("Error reading keystore: {}", ex.getMessage());
                    LOGGER.warn("Ignoring JWT authenticator with keystore: {}", config.getKeyStore());
                }
            } else {
                LOGGER.error("Keystore file does not exist: {}", config.getKeyStore());
            }
            return empty();
        }
        return ofNullable(config.getKey()).filter(key -> !key.isEmpty())
            .map(key -> new JwtAuthenticator(key, config.getBase64Encoded()));
    }

    public static Optional<List<AuthFilter>> getAuthFilters(final TrellisConfiguration config) {
        // Authentication
        final List<AuthFilter> filters = new ArrayList<>();
        final AuthConfiguration auth = config.getAuth();

        if (auth.getJwt().getEnabled()) {
            getJwtAuthenticator(auth.getJwt()).ifPresent(authenticator ->
                filters.add(new OAuthCredentialAuthFilter.Builder<Principal>()
                        .setAuthenticator(authenticator)
                        .setPrefix("Bearer")
                        .buildAuthFilter()));
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

    public static Optional<CORSConfiguration> getCorsConfiguration(final TrellisConfiguration config) {
        if (config.getCors().getEnabled()) {
            return of(config.getCors());
        }
        return empty();
    }

    private static List<String> filterKeyIds(final KeyStore ks, final List<String> keyIds) throws KeyStoreException {
        final List<String> ids = new ArrayList<>();
        for (final String keyId : keyIds) {
            if (ks.containsAlias(keyId)) {
                ids.add(keyId);
            }
        }
        return ids;
    }

    private TrellisUtils() {
        // prevent instantiation
    }
}
