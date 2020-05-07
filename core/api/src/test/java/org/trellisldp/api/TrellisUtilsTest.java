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

import static java.util.Optional.of;
import static java.util.stream.Stream.generate;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.toDataset;
import static org.trellisldp.api.TrellisUtils.toGraph;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class TrellisUtilsTest {

    private static final RDF rdf = RDFFactory.getInstance();
    private static final long size = 10000L;
    private static final RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withinRange('a', 'z').build();

    @Test
    @SuppressWarnings("deprecation")
    void testGetInstance() {
        assertNotNull(TrellisUtils.getInstance(), "RDF instance is null!");
    }

    @Test
    void testCollectGraph() throws Exception {
        try (final Graph graph = generate(() -> rdf.createTriple(getIRI(), getIRI(), getIRI()))
                .parallel().limit(size).collect(toGraph())) {
            assertTrue(size >= graph.size(), "Generated graph has too many triples!");
        }
    }

    @Test
    void testCollectDataset() throws Exception {
        try (final Dataset dataset = generate(() -> rdf.createQuad(getIRI(), getIRI(), getIRI(), getIRI()))
                .parallel().limit(size).collect(toDataset())) {
            assertTrue(size >= dataset.size(), "Generated dataset has too many triples!");
        }
    }

    @Test
    void testDatasetCollectorFinisher() throws Exception {
        try (final Dataset dataset = generate(() -> rdf.createQuad(getIRI(), getIRI(), getIRI(), getIRI()))
                .parallel().limit(size).collect(toDataset())) {
            final TrellisUtils.DatasetCollector collector = toDataset();
            assertEquals(dataset, collector.finisher().apply(dataset), "Dataset finisher returns the wrong object!");
        }
    }

    @Test
    void testNormalizeIdentifier() {
        final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI resourceSlash = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/");
        final IRI child = rdf.createIRI("trellis:data/resource/child");
        final IRI childHash = rdf.createIRI("trellis:data/resource/child#hash");
        final IRI childSlashHash = rdf.createIRI("trellis:data/resource/child/#hash");
        assertEquals(root, TrellisUtils.normalizeIdentifier(root));
        assertEquals(resource, TrellisUtils.normalizeIdentifier(resource));
        assertEquals(resource, TrellisUtils.normalizeIdentifier(resourceSlash));
        assertEquals(child, TrellisUtils.normalizeIdentifier(child));
        assertEquals(child, TrellisUtils.normalizeIdentifier(childHash));
        assertEquals(child, TrellisUtils.normalizeIdentifier(childSlashHash));
    }

    @Test
    void testBuildTrellisIdentifier() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        assertEquals(identifier, TrellisUtils.buildTrellisIdentifier("/resource"));
        assertEquals(identifier, TrellisUtils.buildTrellisIdentifier("/resource/"));
        assertEquals(identifier, TrellisUtils.buildTrellisIdentifier("resource"));
        assertEquals(identifier, TrellisUtils.buildTrellisIdentifier("resource/"));
    }

    private IRI getIRI() {
        return rdf.createIRI("ex:" + generator.generate(5));
    }

    @Test
    void testGetContainer() {
        final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        assertEquals(of(root), TrellisUtils.getContainer(resource), "Resource parent isn't the root resource!");
        assertEquals(of(resource), TrellisUtils.getContainer(child), "Child resource doesn't point to parent!");
        assertFalse(TrellisUtils.getContainer(root).isPresent(), "Root resource has a parent!");
    }

}
