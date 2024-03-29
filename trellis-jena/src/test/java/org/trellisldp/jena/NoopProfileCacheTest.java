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
package org.trellisldp.jena;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.trellisldp.api.CacheService;

class NoopProfileCacheTest {

    @Test
    void testPassthroughCache() {
        final List<String> list = new CopyOnWriteArrayList<>();
        final Function<String, String> mapper = key -> {
            list.add(key);
            return key + "-some-suffix";
        };

        final CacheService<String, String> cache = new NoopProfileCache();
        assertEquals("one-some-suffix", cache.get("one", mapper), "Cache response didn't match!");
        assertEquals("two-some-suffix", cache.get("two", mapper), "Cache response didn't match!");
        assertEquals("one-some-suffix", cache.get("one", mapper), "Cache response didn't match!");
        assertEquals(3L, list.size(), "Incorrect invocation count!");
    }
}
