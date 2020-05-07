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
package org.trellisldp.quarkus;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import com.google.common.cache.Cache;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.rdf.api.IRI;
import org.eclipse.microprofile.config.Config;
import org.trellisldp.cache.TrellisCache;
import org.trellisldp.webac.WebAcService.TrellisAuthorizationCache;


/** An authz cache. */
@ApplicationScoped
@TrellisAuthorizationCache
public class AuthorizationCache extends TrellisCache<String, Set<IRI>> {

    /** The configuration key for setting the maximum authZ cache size. */
    public static final String CONFIG_QUARKUS_AUTHZ_CACHE_SIZE = "trellis.quarkus.authz-cache-size";

    /** The configuration key for setting the authZ cache expiry. */
    public static final String CONFIG_QUARKUS_AUTHZ_CACHE_EXPIRE_SECONDS = "trellis.quarkus.authz-cache-expire-seconds";

    /** Create a cache suitable for authorization data. */
    public AuthorizationCache() {
        super(buildCache(getConfig()));
    }

    private static Cache<String, Set<IRI>> buildCache(final Config config) {
        final int size = config.getOptionalValue(CONFIG_QUARKUS_AUTHZ_CACHE_SIZE, Integer.class).orElse(1000);
        final int expire = config.getOptionalValue(CONFIG_QUARKUS_AUTHZ_CACHE_EXPIRE_SECONDS, Integer.class)
            .orElse(600);
        return newBuilder().maximumSize(size).expireAfterWrite(expire, SECONDS).build();
    }
}
