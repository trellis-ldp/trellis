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

import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;

/**
 * The TrellisUtils class provides a set of convenience methods related to generating and processing RDF objects.
 *
 * @author acoburn
 */
public final class TrellisUtils {

    private static final RDF rdf = RDFFactory.getInstance();

    private static final String SLASH = "/";

    /**
     * The internal trellis scheme.
     */
    public static final String TRELLIS_SCHEME = "trellis:";

    /**
     * The default internal IRI for the root container.
     */
    public static final String TRELLIS_DATA_PREFIX = TRELLIS_SCHEME + "data/";

    /**
     * The default internal blank node prefix.
     */
    public static final String TRELLIS_BNODE_PREFIX = TRELLIS_SCHEME + "bnode/";

    /**
     * The default internal session prefix.
     */
    public static final String TRELLIS_SESSION_PREFIX = TRELLIS_SCHEME + "session/";

    /**
     * The name of the trellis admin role.
     */
    public static final String TRELLIS_ADMIN_ROLE = "trellis_admin_role";

    /**
     * Get the Commons RDF instance in use.
     *
     * @deprecated Please use {@link RDFFactory#getInstance}
     * @return the RDF instance
     */
    @Deprecated
    public static RDF getInstance() {
        return rdf;
    }

    /**
     * Get the structural-logical container for this resource.
     *
     * @param identifier the resource identifier
     * @return a container, if one exists. Only the root resource would return empty here.
     */
    public static Optional<IRI> getContainer(final IRI identifier) {
        if (identifier.getIRIString().equals(TRELLIS_DATA_PREFIX)) {
            return empty();
        }
        final String path = identifier.getIRIString().substring(TRELLIS_DATA_PREFIX.length());
        final int index = Math.max(path.lastIndexOf('/'), 0);
        return of(rdf.createIRI(TRELLIS_DATA_PREFIX + path.substring(0, index)));
    }

    /**
     * For any identifier, normalize its form to remove any hashURIs or trailing slashes.
     * @param identifier the identifier
     * @return a normalized identifier
     */
    public static IRI normalizeIdentifier(final IRI identifier) {
        final String iri = identifier.getIRIString();
        if (iri.contains("#")) {
            final String normalized = iri.split("#")[0];
            if (normalized.endsWith(SLASH)) {
                return rdf.createIRI(normalized.substring(0, normalized.length() - 1));
            }
            return rdf.createIRI(normalized);
        } else if (iri.endsWith(SLASH) && !TRELLIS_DATA_PREFIX.equals(iri)) {
            return rdf.createIRI(iri.substring(0, iri.length() - 1));
        }
        return identifier;
    }

    /**
     * Build a Trellis identifier from a path string.
     * @param path the resource path
     * @return the trellis identifier
     */
    public static IRI buildTrellisIdentifier(final String path) {
        final String normalized = path.endsWith(SLASH) ? path.substring(0, path.length() - 1) : path;
        if (normalized.startsWith(SLASH)) {
            return rdf.createIRI(TRELLIS_DATA_PREFIX + normalized.substring(1));
        }
        return rdf.createIRI(TRELLIS_DATA_PREFIX + normalized);
    }

    /**
     * Collect a stream of Triples into a Graph.
     *
     * @return a graph
     */
    public static Collector<Triple, ?, Graph> toGraph() {
        return Collector.of(rdf::createGraph, Graph::add, (left, right) -> {
            right.iterate().forEach(left::add);
            return left;
        }, UNORDERED);
    }

    /**
     * Collect a stream of Quads into a Dataset.
     *
     * @return a {@link Collector} that accumulates a {@link Stream} of {@link Quad}s into a {@link Dataset}
     */
    public static DatasetCollector toDataset() {
        return new DatasetCollector();
    }

    static class DatasetCollector implements Collector<Quad, Dataset, Dataset> {

        @Override
        public Supplier<Dataset> supplier() {
            return rdf::createDataset;
        }

        @Override
        public BiConsumer<Dataset, Quad> accumulator() {
            return Dataset::add;
        }

        @Override
        public BinaryOperator<Dataset> combiner() {
            return (left, right) -> {
                right.iterate().forEach(left::add);
                return left;
            };
        }

        @Override
        public Function<Dataset, Dataset> finisher() {
            return x -> x;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return unmodifiableSet(EnumSet.of(UNORDERED, IDENTITY_FINISH));
        }
    }

    private TrellisUtils() {
        // prevent instantiation
    }
}
