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
package org.trellisldp.test;

import static java.time.Instant.now;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.vocabulary.LDP.PreferContainment;
import static org.trellisldp.vocabulary.LDP.contains;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.TrellisUtils;
import org.trellisldp.vocabulary.LDP;

/**
 * A {@link ResourceService} that stores its contents in memory, for testing.
 */
public class InMemoryResourceService implements ResourceService {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryResourceService.class);

    private static final RDF rdfFactory = RDFFactory.getInstance();

    private static final CompletableFuture<Void> DONE = completedFuture(null);

    private static final AtomicLong serviceCounter = new AtomicLong();

    private final long serviceNumber = serviceCounter.getAndIncrement();

    private final String ID_PREFIX = getClass().getSimpleName() + "-" + serviceNumber + ":";

    private final AtomicLong idCounter = new AtomicLong();

    private final Map<IRI, InMemoryResource> resources = new ConcurrentHashMap<>();

    private final Map<IRI, Set<IRI>> containment = new ConcurrentHashMap<>();

    private final Map<IRI, Dataset> auditData = new ConcurrentHashMap<>();

    private static final Set<IRI> SUPPORTED_IXN_MODELS;

    static {
        final Set<IRI> models = new CopyOnWriteArraySet<>();
        models.add(LDP.RDFSource);
        models.add(LDP.NonRDFSource);
        models.add(LDP.Container);
        models.add(LDP.BasicContainer);
        SUPPORTED_IXN_MODELS = unmodifiableSet(models);
    }

    @Override
    public CompletionStage<? extends Resource> get(final IRI identifier) {
        final IRI normalized = TrellisUtils.normalizeIdentifier(identifier);
        if (resources.containsKey(normalized)) {
            LOG.debug("Retrieving resource: {}", normalized);
            final Resource resource = resources.get(normalized);
            getAudit(normalized).stream().peek(q -> LOG.debug("Retrieved audit tuple: {}", q))
                .forEach(resource.dataset()::add);
            final Set<IRI> contained = containment.getOrDefault(normalized, emptySet());
            contained.stream().map(c -> containmentQuad(c, identifier)).forEach(resource.dataset()::add);
            return completedFuture(resource);
        }
        LOG.debug("Resource: {} not found.", normalized);
        return completedFuture(MISSING_RESOURCE);
    }

    private static Quad containmentQuad(final IRI container, final IRI identifier) {
        final IRI c = identifier.getIRIString().endsWith("/")
            ? identifier : rdfFactory.createIRI(identifier.getIRIString() + "/");
        return rdfFactory.createQuad(PreferContainment, c, contains, container);
    }

    @Override
    public CompletionStage<Void> replace(final Metadata metadata, final Dataset data) {
        final IRI identifier = TrellisUtils.normalizeIdentifier(metadata.getIdentifier());
        final IRI ixnModel = metadata.getInteractionModel();
        final IRI container = metadata.getContainer().map(TrellisUtils::normalizeIdentifier).orElse(null);
        final BinaryMetadata binary = metadata.getBinary().orElse(null);
        final InMemoryResource newResource = new InMemoryResource(identifier, ixnModel, container, now(), data, binary);
        resources.put(identifier, newResource);
        metadata.getContainer().map(this::getContained).ifPresent(contained -> contained.add(identifier));
        return DONE;
    }

    @Override
    public CompletionStage<Void> delete(final Metadata metadata) {
        final IRI identifier = TrellisUtils.normalizeIdentifier(metadata.getIdentifier());
        resources.remove(identifier);
        metadata.getContainer().map(this::getContained).ifPresent(contained -> contained.remove(identifier));
        return DONE;
    }

    private Set<IRI> getContained(final IRI container) {
        return containment.computeIfAbsent(container, dummy -> concurrentHashSet());
    }

    private Dataset getAudit(final IRI identifier) {
        return auditData.computeIfAbsent(identifier, dummy -> rdfFactory.createDataset());
    }

    private static <T> Set<T> concurrentHashSet() {
        return ConcurrentHashMap.newKeySet();
    }

    @Override
    public CompletionStage<Void> add(final IRI identifier, final Dataset newData) {
        newData.stream().peek(q -> LOG.debug("Received audit tuple: {}", q))
            .forEach(getAudit(TrellisUtils.normalizeIdentifier(identifier))::add);
        return DONE;
    }

    @Override
    public CompletionStage<Void> touch(final IRI identifier) {
        resources.get(TrellisUtils.normalizeIdentifier(identifier)).modified = now();
        return DONE;
    }

    @Override
    public Set<IRI> supportedInteractionModels() {
        return SUPPORTED_IXN_MODELS;
    }

    @Override
    public String generateIdentifier() {
        return ID_PREFIX + idCounter.getAndIncrement();
    }

    private static final class InMemoryResource implements Resource {

        private final IRI identifier;

        private final IRI ixnModel;

        private final IRI container;

        private Instant modified;

        private final Dataset dataset;

        private final BinaryMetadata binaryMetadata;

        private InMemoryResource(final IRI identifier, final IRI ixnModel, final IRI container, final Instant modified,
                        final Dataset dataset, final BinaryMetadata binaryMetadata) {
            this.identifier = identifier;
            this.ixnModel = ixnModel;
            this.container = container;
            this.modified = modified;
            this.dataset = dataset;
            this.binaryMetadata = binaryMetadata;
        }

        @Override
        public Optional<BinaryMetadata> getBinaryMetadata() {
            return Optional.ofNullable(binaryMetadata);
        }

        @Override
        public IRI getIdentifier() {
            return identifier;
        }

        @Override
        public IRI getInteractionModel() {
            return ixnModel;
        }

        @Override
        public Instant getModified() {
            return modified;
        }

        @Override
        public Optional<IRI> getContainer() {
            return Optional.ofNullable(container);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<Quad> stream() {
            return (Stream<Quad>) dataset.stream();
        }

        @Override
        public Dataset dataset() {
            return dataset;
        }
    }
}
