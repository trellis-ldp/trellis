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
package org.trellisldp.webapp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.webapp.AppUtils.loadWithDefault;

import org.junit.jupiter.api.Test;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.http.TrellisHttpResource;

public class AppUtilsTest {

    @Test
    public void testLoaderError() {
        assertThrows(RuntimeTrellisException.class, () -> AppUtils.loadFirst(TrellisHttpResource.class),
                "No exception on loader error!");
    }

    @Test
    public void testLoaderWithDefault() {
        assertFalse(loadWithDefault(MementoService.class, NoopMementoService::new) instanceof NoopMementoService,
                "Loader unexpectedly used default service!");
        assertTrue(loadWithDefault(MementoService.class, NoopMementoService::new) instanceof FileMementoService,
                "Checking loader instance type");
    }

    @Test
    public void testCollectivist() {
        assertTrue(AppUtils.asCollection(null).isEmpty(), "check null input");
        assertTrue(AppUtils.asCollection(" 1 ,   3 , 2, 8").contains("2"), "Check that 2 appears in collection");
        assertTrue(AppUtils.asCollection(" 1 ,   3 , 2, 8").contains("1"), "Check that 1 appears in collection");
    }

    @Test
    public void testNoCORSFilter() {
        try {
            System.setProperty(AppUtils.CONFIG_WEBAPP_CORS_ENABLED, "false");
            assertFalse(AppUtils.getCORSFilter().isPresent(), "Unexpected CORS filter!");
        } finally {
            System.clearProperty(AppUtils.CONFIG_WEBAPP_CORS_ENABLED);
        }
    }

    @Test
    public void testCORSFilter() {
       assertTrue(AppUtils.getCORSFilter().isPresent(), "Missing CORS filter!");
    }

    @Test
    public void testNoCacheFilter() {
        try {
            System.setProperty(AppUtils.CONFIG_WEBAPP_CACHE_ENABLED, "false");
            assertFalse(AppUtils.getCacheControlFilter().isPresent(), "Unexpected cache filter!");
        } finally {
            System.clearProperty(AppUtils.CONFIG_WEBAPP_CACHE_ENABLED);
        }
    }

    @Test
    public void testCacheFilter() {
       assertTrue(AppUtils.getCacheControlFilter().isPresent(), "Missing cache filter!");
    }
}
