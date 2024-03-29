/*
 * Copyright (c) Aaron Coburn and individual contributors
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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.function.Function;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.trellisldp.api.CacheService;
import org.trellisldp.cache.TrellisCache;

/** A JSON-LD context/profile cache. */
@ApplicationScoped
@CacheService.TrellisProfileCache
class ProfileCache implements CacheService<String, String> {

    CacheService<String, String> cache;

    @Inject
    @ConfigProperty(name = "trellis.quarkus.profile-cache-size", defaultValue = "100")
    int size;

    @Inject
    @ConfigProperty(name = "trellis.quarkus.profile-cache-expire-hours", defaultValue = "24")
    int expire;

    @PostConstruct
    void initialize() {
        cache = new TrellisCache<>(newBuilder().maximumSize(size).expireAfterWrite(expire, HOURS).build());
    }

    @Override
    public String get(final String key, final Function<String, String> mapper) {
        return cache.get(key, mapper);
    }
}
