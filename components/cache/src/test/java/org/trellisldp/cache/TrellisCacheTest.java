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
package org.trellisldp.cache;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.cache.Cache;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trellisldp.api.TrellisRuntimeException;

/**
 * @author acoburn
 */
@ExtendWith(MockitoExtension.class)
class TrellisCacheTest {

    @Mock
    private Cache<String, String> mockCache;

    @Test
    void testCache() {
        final TrellisCache<String, String> cache = new TrellisCache<>(newBuilder().maximumSize(5).build());
        assertEquals("longer", cache.get("long", x -> x + "er"), "Incorrect cache response!");
    }

    @Test
    void testCacheException() throws Exception {
        when(mockCache.get(any(), any())).thenThrow(ExecutionException.class);
        final TrellisCache<String, String> cache = new TrellisCache<>(mockCache);
        assertThrows(TrellisRuntimeException.class, () -> cache.get("long", x -> x + "er"));
    }
}
