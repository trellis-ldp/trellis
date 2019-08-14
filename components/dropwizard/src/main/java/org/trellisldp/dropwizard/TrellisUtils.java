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
package org.trellisldp.dropwizard;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.cache.Cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.api.CacheService;
import org.trellisldp.auth.basic.BasicAuthFilter;
import org.trellisldp.auth.oauth.Authenticator;
import org.trellisldp.auth.oauth.NullAuthenticator;
import org.trellisldp.auth.oauth.OAuthFilter;
import org.trellisldp.auth.oauth.OAuthUtils;
import org.trellisldp.dropwizard.config.AuthConfiguration;
import org.trellisldp.dropwizard.config.CORSConfiguration;
import org.trellisldp.dropwizard.config.JwtAuthConfiguration;
import org.trellisldp.dropwizard.config.TrellisConfiguration;

/**
 * Convenience utilities for the trellis-dropwizard component.
 */
final class TrellisUtils {

    public static Authenticator getJwtAuthenticator(final JwtAuthConfiguration config) {
        final Authenticator jwksAuthenticator = OAuthUtils.buildAuthenticatorWithJwk(
                config.getJwks());
        if (jwksAuthenticator != null) {
            return jwksAuthenticator;
        }

        final Authenticator keystoreAuthenticator = OAuthUtils.buildAuthenticatorWithTruststore(
                config.getKeyStore(), config.getKeyStorePassword().toCharArray(), config.getKeyIds());
        if (keystoreAuthenticator != null) {
            return keystoreAuthenticator;
        }

        final Authenticator sharedKeyAuthenticator = OAuthUtils.buildAuthenticatorWithSharedSecret(config.getKey());
        if (sharedKeyAuthenticator != null) {
            return sharedKeyAuthenticator;
        }

        return new NullAuthenticator();
    }

    public static Optional<CacheService<String, Set<IRI>>> getWebacCache(final TrellisConfiguration config) {
        if (config.getAuth().getWebac().getEnabled()) {
            final Cache<String, Set<IRI>> authCache = newBuilder().maximumSize(config.getAuth().getWebac()
                    .getCacheSize()).expireAfterWrite(config.getAuth().getWebac()
                    .getCacheExpireSeconds(), SECONDS).build();
            return of(new TrellisCache<>(authCache));
        }
        return empty();
    }

    public static List<ContainerRequestFilter> getAuthFilters(final TrellisConfiguration config) {
        // Authentication
        final List<ContainerRequestFilter> filters = new ArrayList<>();
        final AuthConfiguration auth = config.getAuth();

        if (auth.getJwt().getEnabled()) {
            filters.add(new OAuthFilter(getJwtAuthenticator(auth.getJwt())));
        }

        if (auth.getBasic().getEnabled() && auth.getBasic().getUsersFile() != null) {
            filters.add(new BasicAuthFilter(auth.getBasic().getUsersFile()));
        }

        return filters;
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
