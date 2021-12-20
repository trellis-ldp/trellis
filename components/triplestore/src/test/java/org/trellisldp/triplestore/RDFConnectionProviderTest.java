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
import java.util.Optional;

import org.apache.jena.rdflink.RDFConnectionAdapter;
import org.apache.jena.rdflink.RDFLinkDataset;
import org.apache.jena.rdflink.RDFLinkHTTP;
import org.junit.jupiter.api.Test;

class RDFConnectionProviderTest {

    @Test
    void testRDFConnectionMemory() {
        final RDFConnectionProvider provider = new RDFConnectionProvider();
        provider.connectionString = Optional.empty();
        provider.init();
        assertTrue(((RDFConnectionAdapter) provider.getRdfConnection()).getLink() instanceof RDFLinkDataset);
    }

    @Test
    void testRDFConnectionRemote() {
        final RDFConnectionProvider provider = new RDFConnectionProvider();
        provider.connectionString = Optional.of("http://example.com/sparql");
        provider.init();
        assertTrue(((RDFConnectionAdapter) provider.getRdfConnection()).getLink() instanceof RDFLinkHTTP);
    }

    @Test
    void testRDFConnectionLocal() throws Exception {
        final File dir = new File(new File(getClass().getResource("/logback-test.xml").toURI()).getParent(), "data2");
        final RDFConnectionProvider provider = new RDFConnectionProvider();
        provider.connectionString = Optional.of(dir.getAbsolutePath());
        provider.init();
        assertTrue(((RDFConnectionAdapter) provider.getRdfConnection()).getLink() instanceof RDFLinkDataset);
    }
}
