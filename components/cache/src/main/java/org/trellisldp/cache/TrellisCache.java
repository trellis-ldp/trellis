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
package org.trellisldp.cache;

import com.google.common.cache.Cache;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.trellisldp.api.CacheService;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * A simple Guava-based cache service.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class TrellisCache<K, V> implements CacheService<K, V> {

    private final Cache<K, V> cache;

    /**
     * Create a Trellis cache.
     * @param cache the guava cache
     */
    public TrellisCache(final Cache<K, V> cache) {
        this.cache = cache;
    }

    @Override
    public V get(final K key, final Function<K, V> mapper) {
        try {
            return cache.get(key, () -> mapper.apply(key));
        } catch (final ExecutionException ex) {
            throw new RuntimeTrellisException("Error fetching " + key + " from cache", ex);
        }
    }
}
