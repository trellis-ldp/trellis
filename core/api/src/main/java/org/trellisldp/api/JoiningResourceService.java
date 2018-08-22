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
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
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
    public CompletableFuture<? extends Resource> get(final IRI identifier) {
        return mutableData.get(identifier).thenCombine(immutableData.get(identifier), (mutable, immutable) -> {
            if (MISSING_RESOURCE.equals(mutable) && MISSING_RESOURCE.equals(immutable)) {
                return MISSING_RESOURCE;
            } else if (MISSING_RESOURCE.equals(mutable)) {
                return immutable;
            } else if (MISSING_RESOURCE.equals(immutable)) {
                return mutable;
            } else {
                return new RetrievableResource(mutable, immutable);
            }
        });
    }

    @Override
    public CompletableFuture<Void> add(final IRI id, final Session session, final Dataset dataset) {
        return immutableData.add(id, session, dataset);
    }

    @Override
    public CompletableFuture<Void> create(final IRI id, final Session session, final IRI ixnModel,
            final Dataset dataset, final IRI container, final Binary binary) {
        return mutableData.create(id, session, ixnModel, dataset, container, binary);
    }

    @Override
    public CompletableFuture<Void> replace(final IRI id, final Session session, final IRI ixnModel,
            final Dataset dataset, final IRI container, final Binary binary) {
        return mutableData.replace(id, session, ixnModel, dataset, container, binary);
    }

    @Override
    public CompletableFuture<Void> delete(final IRI id, final Session session, final IRI ixnModel,
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
