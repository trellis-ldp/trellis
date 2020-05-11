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
package org.trellisldp.dropwizard;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.cache.Cache;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.auth.basic.BasicAuthFilter;
import org.trellisldp.auth.oauth.Authenticator;
import org.trellisldp.auth.oauth.NullAuthenticator;
import org.trellisldp.auth.oauth.OAuthFilter;
import org.trellisldp.auth.oauth.OAuthUtils;
import org.trellisldp.cache.TrellisCache;
import org.trellisldp.dropwizard.config.AuthConfiguration;
import org.trellisldp.dropwizard.config.CORSConfiguration;
import org.trellisldp.dropwizard.config.JwtAuthConfiguration;
import org.trellisldp.dropwizard.config.TrellisConfiguration;
import org.trellisldp.webac.WebAcService;

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

    public static WebAcService getWebacService(final TrellisConfiguration config,
            final ResourceService resourceService) {
        if (config.getAuth().getWebac().getEnabled()) {
            final Cache<String, Set<IRI>> authCache = newBuilder().maximumSize(config.getAuth().getWebac()
                    .getCacheSize()).expireAfterWrite(config.getAuth().getWebac()
                    .getCacheExpireSeconds(), SECONDS).build();
            final WebAcService webac = new WebAcService(resourceService, new TrellisCache<>(authCache));
            try {
                webac.initialize();
            } catch (final Exception ex) {
                throw new RuntimeTrellisException("Error initializing Access Control system", ex);
            }
            return webac;
        }
        return null;
    }

    public static List<ContainerRequestFilter> getAuthFilters(final TrellisConfiguration config) {
        // Authentication
        final List<ContainerRequestFilter> filters = new ArrayList<>();
        final AuthConfiguration auth = config.getAuth();
        final String realm = config.getAuth().getRealm();
        final Set<String> admins = new HashSet<>(config.getAuth().getAdminUsers());

        if (auth.getJwt().getEnabled()) {
            final OAuthFilter filter = new OAuthFilter();
            filter.setAuthenticator(getJwtAuthenticator(auth.getJwt()));
            filter.setChallenge("Bearer realm=\"" + realm + "\"");
            filter.setAdmins(admins);
            filters.add(filter);
        }

        if (auth.getBasic().getEnabled() && auth.getBasic().getUsersFile() != null) {
            final BasicAuthFilter filter = new BasicAuthFilter();
            filter.setFile(new File(auth.getBasic().getUsersFile()));
            filter.setChallenge("Basic realm=\"" + realm + "\"");
            filter.setAdmins(admins);
            filters.add(filter);
        }

        return filters;
    }

    public static CORSConfiguration getCorsConfiguration(final TrellisConfiguration config) {
        if (config.getCors().getEnabled()) {
            return config.getCors();
        }
        return null;
    }

    private TrellisUtils() {
        // prevent instantiation
    }
}
