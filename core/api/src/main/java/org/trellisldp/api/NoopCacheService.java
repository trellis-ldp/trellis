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
package org.trellisldp.api;

import java.util.function.Function;

/**
 * A no-op (pass-through) cache service for Trellis.
 * @param <K> the type of key to use
 * @param <V> the type of value to cache
 */
public class NoopCacheService<K, V> implements CacheService<K, V> {

    @Override
    public V get(final K key, final Function<K, V> mappingFunction) {
        return mappingFunction.apply(key);
    }
}
