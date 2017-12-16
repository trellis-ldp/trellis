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
package org.trellisldp.api;

import static java.util.stream.Stream.generate;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.api.RDFUtils.toGraph;
import static org.trellisldp.api.RDFUtils.toDataset;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class RDFUtilsTest {

    private static final RDF rdf = getInstance();
    private static final Long size = 10000L;
    private static final RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withinRange('a', 'z').build();

    @Test
    public void testGetInstance() {
        assertNotNull(rdf);
    }

    @Test
    public void testCollectGraph() {
        final Graph graph = generate(() -> rdf.createTriple(getIRI(), getIRI(), getIRI()))
            .parallel().limit(size).collect(toGraph());

        assertTrue(size >= graph.size());
    }

    @Test
    public void testCollectDataset() {
        final Dataset dataset = generate(() -> rdf.createQuad(getIRI(), getIRI(), getIRI(), getIRI()))
            .parallel().limit(size).collect(toDataset());

        assertTrue(size >= dataset.size());
    }

    private IRI getIRI() {
        return rdf.createIRI("ex:" + generator.generate(5));
    }
}
