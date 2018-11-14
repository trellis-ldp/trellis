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
package org.trellisldp.webac;

import org.apache.commons.rdf.api.Graph;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * A wrapped RDF graph.
 */
class WrappedGraph implements AutoCloseable {

    private final Graph innerGraph;

    /**
     * Create a new graph.
     *
     * @param graph the graph
     */
    protected WrappedGraph(final Graph graph) {
        this.innerGraph = graph;
    }

    @Override
    public void close() {
        try {
            innerGraph.close();
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error closing graph", ex);
        }
    }

    /**
     * Get the underlying graph.
     *
     * @return the graph
     */
    public Graph getGraph() {
        return innerGraph;
    }

    public static WrappedGraph wrap(final Graph graph) {
        return new WrappedGraph(graph);
    }
}
