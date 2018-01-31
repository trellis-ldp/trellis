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

import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.cache.Cache;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.trellisldp.api.CacheService;

/**
 * @author acoburn
 */
class TrellisCache<K, V> implements CacheService<K, V> {

    private static final Logger LOGGER = getLogger(TrellisCache.class);

    private final Cache<K, V> cache;

    public TrellisCache(final Cache<K, V> cache) {
        this.cache = cache;
    }

    public V get(final K key, final Function<? super K, ? extends V> mapper) {
        try {
            return cache.get(key, () -> mapper.apply(key));
        } catch (final ExecutionException ex) {
            LOGGER.warn("Error fetching {} from cache: {}", key, ex.getMessage());
            return null;
        }
    }
}
