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

import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.getInstance;

import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.slf4j.Logger;

/**
 * @author acoburn
 */
public class TrellisDataset implements AutoCloseable {

    private static final Logger LOGGER = getLogger(TrellisDataset.class);

    private final Dataset dataset;

    /**
     * Create a new dataset
     * @param dataset the dataset
     */
    public TrellisDataset(final Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public void close() {
        try {
            dataset.close();
        } catch (final Exception ex) {
            LOGGER.error("Error closing graph: {}", ex.getMessage());
            throw new WebApplicationException("Error closing dataset", ex);
        }
    }

    /**
     * Add a quad to the dataset
     * @param quad an RDF Quad
     */
    public void add(final Quad quad) {
        dataset.add(quad);
    }

    /**
     * Get a graph from the dataset
     * @param graphName the graph name
     * @return the graph
     */
    public Optional<Graph> getGraph(final IRI graphName) {
        return dataset.getGraph(graphName);
    }

    /**
     * Get the underlying dataset
     * @return the dataset
     */
    public Dataset asDataset() {
        return dataset;
    }

    /**
     * Create a new dataset
     * @return a dataset
     */
    public static TrellisDataset createDataset() {
        return new TrellisDataset(getInstance().createDataset());
    }
}
