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

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.of;
import static java.util.stream.Collector.Characteristics.CONCURRENT;
import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;

/**
 * The RDFUtils class provides a set of convenience methods related to
 * generating and processing RDF objects.
 *
 * @author acoburn
 */
public final class RDFUtils {

    private static RDF rdf = findFirst(RDF.class)
        .orElseThrow(() -> new RuntimeTrellisException("No RDF Commons implementation available!"));

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
     * The session property for a baseURL.
     */
    public static final String TRELLIS_SESSION_BASE_URL = "baseURL";

    /**
     * Get the Commons RDF instance in use.
     *
     * @return the RDF instance
     */
    public static RDF getInstance() {
        return rdf;
    }

    /**
     * Get a service.
     * @param service the interface or abstract class representing the service
     * @param <T> the class of the service type
     * @return the first service provider or empty Optional if no service providers are located
     */
    public static <T> Optional<T> findFirst(final Class<T> service) {
        // TODO - JDK9 replace with ServiceLoader::findFirst
        return Optional.of(ServiceLoader.load(service)).map(ServiceLoader::iterator).filter(Iterator::hasNext)
            .map(Iterator::next);
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
     * @return a {@link Collector} that accumulates a {@link Stream} of
     *         {@link Quad}s into a {@link Dataset}
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
            return unmodifiableSet(of(UNORDERED, IDENTITY_FINISH));
        }

        /**
         * Collect a stream of {@link Quad}s into a {@link Dataset} with concurrent
         * operation.
         *
         * @return a {@link Collector} that accumulates a {@link Stream} of
         *         {@link Quad}s into a {@link Dataset}
         */
        public ConcurrentDatasetCollector concurrent() {
            return new ConcurrentDatasetCollector();
        }
    }

    private static class ConcurrentDatasetCollector implements Collector<Quad, Set<Quad>, Dataset> {

        @Override
        public Supplier<Set<Quad>> supplier() {
            return () -> newSetFromMap(new ConcurrentHashMap<>());
        }

        @Override
        public BiConsumer<Set<Quad>, Quad> accumulator() {
            return Set::add;
        }

        @Override
        public BinaryOperator<Set<Quad>> combiner() {
            return (s1, s2) -> {
                s1.addAll(s2);
                return s1;
            };
        }

        @Override
        public Function<Set<Quad>, Dataset> finisher() {
            return set -> {
                final Dataset dataset = rdf.createDataset();
                set.forEach(dataset::add);
                return dataset;
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return unmodifiableSet(of(UNORDERED, CONCURRENT));
        }
    }

    private RDFUtils() {
        // prevent instantiation
    }
}
