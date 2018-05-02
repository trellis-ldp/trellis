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

import static java.util.stream.Stream.concat;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;

/**
 * Uses two underlying persistence services (an {@link ImmutableDataService} and
 * a {@link MutableDataService}) to provide the {@link ResourceService} API.
 *
 * @author ajs6f
 * @see ImmutableDataService
 * @see MutableDataService
 */
public abstract class JoiningResourceService implements ResourceService {

    private final ImmutableDataService<Resource> immutableData;

    private final MutableDataService<Resource> mutableData;

    /**
     * @param mutableData service in which to persist mutable data
     * @param immutableData service in which to persist immutable data
     */
    public JoiningResourceService(final MutableDataService<Resource> mutableData,
                    final ImmutableDataService<Resource> immutableData) {
        this.immutableData = immutableData;
        this.mutableData = mutableData;
    }

    @Override
    public Optional<? extends Resource> get(final IRI identifier) {
        final Optional<Resource> mutableFirst = mutableData.get(identifier).map(mutable -> {
            // perhaps only some resources possess immutable metadata
            final Optional<? extends Resource> immutable = immutableData.get(identifier);
            return immutable.isPresent() ? new RetrievableResource(mutable, immutable.get()) : mutable;
        });
        // fall through to immutable-only data
        return mutableFirst.isPresent() ? mutableFirst : immutableData.get(identifier);
    }

    @Override
    public Future<Boolean> add(final IRI id, final Session session, final Dataset dataset) {
        return immutableData.add(id, session, dataset);
    }

    @Override
    public Future<Boolean> create(final IRI id, final Session session, final IRI ixnModel, final IRI container,
            final Binary binary, final Dataset dataset) {
        return mutableData.create(id, session, ixnModel, container, binary, dataset);
    }

    @Override
    public Future<Boolean> replace(final IRI id, final Session session, final IRI ixnModel, final IRI container,
            final Binary binary, final Dataset dataset) {
        return mutableData.replace(id, session, ixnModel, container, binary, dataset);
    }

    @Override
    public Future<Boolean> delete(final IRI id, final Session session, final IRI ixnModel,
            final Dataset dataset) {
        return mutableData.delete(id, session, ixnModel, dataset);
    }

    /**
     * Only for use transmitting data back to persistent services.
     *
     * <p>Packages mutable or immutable data for underlying services.
     *
     * @author ajs6f
     *
     */
    protected static final class PersistableResource implements Resource {

        private final Instant modified = Instant.now();
        private final IRI id;
        private final IRI ixnModel;
        private final Dataset dataset;

        public PersistableResource(final IRI id, final IRI ixnModel, final Dataset dataset) {
            this.id = id;
            this.ixnModel = ixnModel;
            this.dataset = dataset;
        }

        @Override
        public IRI getIdentifier() {
            return id;
        }

        @Override
        public IRI getInteractionModel() {
            return ixnModel;
        }

        @Override
        public Stream<? extends Quad> stream() {
            return dataset.stream();
        }

        @Override
        public Instant getModified() {
            return modified;
        }

        @Override
        public Boolean hasAcl() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Only for use retrieving data from persistent services within this class.
     *
     * <p>Merges mutable and immutable data from underlying services.
     *
     * @author ajs6f
     *
     */
    protected static final class RetrievableResource implements Resource {

        private final Resource mutable;
        private final Resource immutable;

        public RetrievableResource(final Resource mutable, final Resource immutable) {
            this.mutable = mutable;
            this.immutable = immutable;
        }

        @Override
        public IRI getIdentifier() {
            return mutable.getIdentifier();
        }

        @Override
        public IRI getInteractionModel() {
            return mutable.getInteractionModel();
        }

        @Override
        public Stream<? extends Quad> stream() {
            return immutable == null ? mutable.stream() : concat(mutable.stream(), immutable.stream());
        }

        @Override
        public Instant getModified() {
            return mutable.getModified();
        }

        @Override
        public Boolean hasAcl() {
            return mutable.hasAcl();
        }
    }
}
