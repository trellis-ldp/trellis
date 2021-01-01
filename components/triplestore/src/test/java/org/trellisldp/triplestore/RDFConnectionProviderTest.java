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
package org.trellisldp.triplestore;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.apache.jena.rdfconnection.RDFConnectionLocal;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RDFConnectionProviderTest {

    @AfterEach
    void cleanup() {
        System.clearProperty(TriplestoreResourceService.CONFIG_TRIPLESTORE_RDF_LOCATION);
    }

    @Test
    void testRDFConnectionMemory() {
        final RDFConnectionProvider provider = new RDFConnectionProvider();
        assertTrue(provider.getRdfConnection() instanceof RDFConnectionLocal);
    }

    @Test
    void testRDFConnectionRemote() {
        System.setProperty(TriplestoreResourceService.CONFIG_TRIPLESTORE_RDF_LOCATION, "http://example.com/sparql");
        final RDFConnectionProvider provider = new RDFConnectionProvider();
        assertTrue(provider.getRdfConnection() instanceof RDFConnectionRemote);
    }

    @Test
    void testRDFConnectionLocal() throws Exception {
        final File dir = new File(new File(getClass().getResource("/logback-test.xml").toURI()).getParent(), "data2");
        System.setProperty(TriplestoreResourceService.CONFIG_TRIPLESTORE_RDF_LOCATION, dir.getAbsolutePath());
        final RDFConnectionProvider provider = new RDFConnectionProvider();
        assertTrue(provider.getRdfConnection() instanceof RDFConnectionLocal);
    }
}
