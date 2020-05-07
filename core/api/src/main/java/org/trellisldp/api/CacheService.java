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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.util.function.Function;

/**
 * A generalized caching service for Trellis.
 *
 * @author acoburn
 * @param <K> the type of keys for this cache
 * @param <V> the type of values for this cache
 */
public interface CacheService<K, V> {

    /**
     * Get a value from the cache.
     *
     * @param key the key
     * @param mappingFunction attempts to compute a mapping for the specified key
     * @return a value for that key, never {@code null}
     */
    V get(K key, Function<K, V> mappingFunction);

    /**
     * A {@link CacheService} used for JSON-LD profiles.
     *
     */
    @java.lang.annotation.Documented
    @java.lang.annotation.Retention(RUNTIME)
    @java.lang.annotation.Target({TYPE, METHOD, FIELD, PARAMETER})
    @javax.inject.Qualifier
    @interface TrellisProfileCache { }

}
