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
package org.trellisldp.webdav.impl;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.trellisldp.api.TrellisUtils.getInstance;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Quad;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * @author acoburn
 */
public class TrellisDataset implements AutoCloseable {

    private final Dataset commonsDataset;

    /**
     * Create a new dataset.
     *
     * @param dataset the dataset
     */
    public TrellisDataset(final Dataset dataset) {
        this.commonsDataset = dataset;
    }

    /**
     * Add a quad to the dataset.
     *
     * @param quad an RDF Quad
     */
    public void add(final Quad quad) {
        commonsDataset.add(quad);
    }

    /**
     * Get the underlying dataset.
     *
     * @return the dataset
     */
    public Dataset asDataset() {
        return commonsDataset;
    }

    @Override
    public void close() {
        try {
            commonsDataset.close();
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error closing dataset", ex);
        }
    }

    /**
     * Create a new dataset.
     *
     * @return a dataset
     */
    public static TrellisDataset createDataset() {
        return new TrellisDataset(getInstance().createDataset());
    }

    @Override
    public String toString() {
        return asDataset().stream()
                        .map(q -> format("%1$s %2$s %3$s %4$s .",
                                        q.getSubject().ntriplesString(),
                                        q.getPredicate().ntriplesString(),
                                        q.getObject().ntriplesString(),
                                        q.getGraphName().map(BlankNodeOrIRI::ntriplesString).orElse("")))
                        .collect(joining("\n"));
    }
}
