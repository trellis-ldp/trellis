/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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

import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.trellisldp.api.CacheService;
import org.trellisldp.cache.TrellisCache;
import org.trellisldp.webac.AuthorizedModes;
import org.trellisldp.webac.WebAcService.TrellisAuthorizationCache;


/** An authz cache. */
@ApplicationScoped
@TrellisAuthorizationCache
class AuthorizationCache implements CacheService<String, AuthorizedModes> {

    CacheService<String, AuthorizedModes> cache;

    @Inject
    @ConfigProperty(name = "trellis.quarkus.authz-cache-size", defaultValue = "1000")
    int size;

    @Inject
    @ConfigProperty(name = "trellis.quarkus.authz-cache-expire-seconds", defaultValue = "60")
    int expire;

    @PostConstruct
    void initialize() {
        cache = new TrellisCache<>(newBuilder().maximumSize(size).expireAfterWrite(expire, SECONDS).build());
    }

    @Override
    public AuthorizedModes get(final String key, final Function<String, AuthorizedModes> mapper) {
        return cache.get(key, mapper);
    }
}
