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

import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.cache.Cache;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * @author acoburn
 */
class TrellisCacheTest {

    @Mock
    private Cache<String, String> mockCache;

    @BeforeEach
    void setUp() throws ExecutionException {
        initMocks(this);
        when(mockCache.get(any(), any())).thenThrow(ExecutionException.class);
    }

    @Test
    void testCache() {
        final TrellisCache<String, String> cache = new TrellisCache<>(newBuilder().maximumSize(5).build());
        assertEquals("longer", cache.get("long", x -> x + "er"), "Incorrect cache response!");
    }

    @Test
    void testCacheException() {
        final TrellisCache<String, String> cache = new TrellisCache<>(mockCache);
        assertThrows(RuntimeTrellisException.class, () -> cache.get("long", x -> x + "er"));
    }
}
