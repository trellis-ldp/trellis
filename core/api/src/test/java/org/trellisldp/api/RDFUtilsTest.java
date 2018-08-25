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

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.generate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.api.RDFUtils.toDataset;
import static org.trellisldp.api.RDFUtils.toGraph;

import java.util.Set;
import java.util.stream.Collector;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class RDFUtilsTest {

    private static final RDF rdf = getInstance();
    private static final Long size = 10000L;
    private static final RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withinRange('a', 'z').build();

    @Test
    public void testGetInstance() {
        assertNotNull(rdf, "RDF instance is null!");
    }

    @Test
    public void testCollectGraph() {
        final Graph graph = generate(() -> rdf.createTriple(getIRI(), getIRI(), getIRI()))
            .parallel().limit(size).collect(toGraph());

        assertTrue(size >= graph.size(), "Generated graph has too many triples!");
    }

    @Test
    public void testCollectDatasetConcurrent() {
        final Dataset dataset = generate(() -> rdf.createQuad(getIRI(), getIRI(), getIRI(), getIRI()))
            .parallel().limit(size).collect(toDataset().concurrent());

        assertTrue(size >= dataset.size(), "Generated dataset has too many triples!");
    }

    @Test
    public void testCollectDataset() {
        final Dataset dataset = generate(() -> rdf.createQuad(getIRI(), getIRI(), getIRI(), getIRI()))
            .parallel().limit(size).collect(toDataset());

        assertTrue(size >= dataset.size(), "Generated dataset has too many triples!");
    }

    @Test
    public void testDatasetCollectorFinisher() {
        final Dataset dataset = generate(() -> rdf.createQuad(getIRI(), getIRI(), getIRI(), getIRI()))
            .parallel().limit(size).collect(toDataset());

        final RDFUtils.DatasetCollector collector = toDataset();
        assertEquals(dataset, collector.finisher().apply(dataset), "Dataset finisher returns the wrong object!");
    }

    @Test
    public void testDatasetCombiner() {
        final Set<Quad> quads1 = generate(() -> rdf.createQuad(getIRI(), getIRI(), getIRI(), getIRI()))
            .parallel().limit(size).collect(toSet());
        final Set<Quad> quads2 = generate(() -> rdf.createQuad(getIRI(), getIRI(), getIRI(), getIRI()))
            .parallel().limit(size).collect(toSet());

        final Collector<Quad, Set<Quad>, Dataset> collector = toDataset().concurrent();
        assertEquals(quads1.size() + quads2.size(), collector.combiner().apply(quads1, quads2).size(),
                "Dataset combiner produces the wrong number of quads!");
    }

    private IRI getIRI() {
        return rdf.createIRI("ex:" + generator.generate(5));
    }
}
