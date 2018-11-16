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
package org.trellisldp.http.impl;

import static org.trellisldp.api.TrellisUtils.getInstance;

import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * @author acoburn
 */
class TrellisGraph implements AutoCloseable {

    private final Graph graph;

    /**
     * Create a new graph.
     *
     * @param graph the graph
     */
    public TrellisGraph(final Graph graph) {
        this.graph = graph;
    }

    @Override
    public void close() {
        try {
            graph.close();
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error closing graph", ex);
        }
    }

    /**
     * Add a triple to the graph.
     *
     * @param triple an RDF Triple
     */
    public void add(final Triple triple) {
        graph.add(triple);
    }

    /**
     * Stream triples from the graph.
     *
     * @return a stream of triples
     */
    public Stream<Triple> stream() {
        return graph.stream().map(Triple.class::cast);
    }

    /**
     * Get the underlying graph.
     *
     * @return the graph
     */
    public Graph asGraph() {
        return graph;
    }

    /**
     * Create a new graph.
     *
     * @return a graph
     */
    public static TrellisGraph createGraph() {
        return new TrellisGraph(getInstance().createGraph());
    }
}
