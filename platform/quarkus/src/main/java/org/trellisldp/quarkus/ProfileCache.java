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
import static java.util.concurrent.TimeUnit.HOURS;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import com.google.common.cache.Cache;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.Config;
import org.trellisldp.api.CacheService.TrellisProfileCache;
import org.trellisldp.cache.TrellisCache;

/** A JSON-LD context/profile cache. */
@ApplicationScoped
@TrellisProfileCache
public class ProfileCache extends TrellisCache<String, String> {

    /** A configuration key that sets the profile cache maximum size. */
    public static final String CONFIG_QUARKUS_PROFILE_CACHE_SIZE = "trellis.quarkus.profile-cache-size";

    /** A configuration key that sets the profile cache expiry time (in hours). */
    public static final String CONFIG_QUARKUS_PROFILE_CACHE_EXPIRE_HOURS = "trellis.quarkus.profile-cache-expire-hours";

    /** Create a cache suitable for JSON-LD profile requests. */
    public ProfileCache() {
        super(buildCache(getConfig()));
    }

    private static Cache<String, String> buildCache(final Config config) {
        final int size = config.getOptionalValue(CONFIG_QUARKUS_PROFILE_CACHE_SIZE, Integer.class).orElse(100);
        final int expire = config.getOptionalValue(CONFIG_QUARKUS_PROFILE_CACHE_EXPIRE_HOURS, Integer.class).orElse(24);
        return newBuilder().maximumSize(size).expireAfterWrite(expire, HOURS).build();
    }
}
