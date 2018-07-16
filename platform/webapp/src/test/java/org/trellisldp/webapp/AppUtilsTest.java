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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.webapp.AppUtils.loadWithDefault;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionLocal;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.http.LdpResource;

public class AppUtilsTest {

    private static final Configuration config = ConfigurationProvider.getConfiguration();

    @Test
    public void testLoaderError() {
        assertThrows(RuntimeTrellisException.class, () -> AppUtils.loadFirst(LdpResource.class));
    }

    @Test
    public void testLoaderWithDefault() {
        assertFalse(loadWithDefault(MementoService.class, NoopMementoService::new) instanceof NoopMementoService);
        assertTrue(loadWithDefault(MementoService.class, NoopMementoService::new) instanceof FileMementoService);
    }

    @Test
    public void testCollectivist() {
        assertTrue(AppUtils.asCollection(null).isEmpty());
        assertTrue(AppUtils.asCollection(" 1 ,   3 , 2, 8").contains("2"));
        assertTrue(AppUtils.asCollection(" 1 ,   3 , 2, 8").contains("1"));
    }

    @Test
    public void testRDFConnectionMem() {
        final RDFConnection conn = AppUtils.getRDFConnection(null);

        assertNotNull(conn);
        assertTrue(conn instanceof RDFConnectionLocal);
        assertFalse(conn.isClosed());
    }

    @Test
    public void testRDFConnectionLocal() {
        final RDFConnection conn = AppUtils.getRDFConnection(config.get("trellis.rdf.location"));

        assertNotNull(conn);
        assertTrue(conn instanceof RDFConnectionLocal);
        assertFalse(conn.isClosed());
    }

    @Test
    public void testRDFConnectionRemote() {
        final RDFConnection conn = AppUtils.getRDFConnection("http://example.com");

        assertNotNull(conn);
        assertTrue(conn instanceof RDFConnectionRemote);
        assertFalse(conn.isClosed());
    }

    @Test
    public void testRDFConnectionRemoteSSL() {
        final RDFConnection conn = AppUtils.getRDFConnection("https://example.com");

        assertNotNull(conn);
        assertTrue(conn instanceof RDFConnectionRemote);
        assertFalse(conn.isClosed());
    }

    @Test
    public void testNoCORSFilter() {
       System.getProperties().setProperty("trellis.cors.enabled", "false");
       assertFalse(AppUtils.getCORSFilter().isPresent());
       System.getProperties().remove("trellis.cors.enabled");
    }

    @Test
    public void testCORSFilter() {
       assertTrue(AppUtils.getCORSFilter().isPresent());
    }

    @Test
    public void testNoCacheFilter() {
       System.getProperties().setProperty("trellis.cache.enabled", "false");
       assertFalse(AppUtils.getCacheControlFilter().isPresent());
       System.getProperties().remove("trellis.cache.enabled");
    }

    @Test
    public void testCacheFilter() {
       assertTrue(AppUtils.getCacheControlFilter().isPresent());
    }
}
