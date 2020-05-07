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

import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;

/**
 * A no-op resource service that can be used with CDI and proxy objects.
 */
@NoopImplementation
public class NoopResourceService implements ResourceService {

    private static final CompletableFuture<Void> NO_RESULT = completedFuture(null);

    @Override
    public CompletionStage<Resource> get(final IRI identifier) {
        return completedFuture(MISSING_RESOURCE);
    }

    @Override
    public CompletionStage<Void> replace(final Metadata metadata, final Dataset dataset) {
        return NO_RESULT;
    }

    @Override
    public CompletionStage<Void> delete(final Metadata metadata) {
        return NO_RESULT;
    }

    @Override
    public CompletionStage<Void> add(final IRI identifier, final Dataset dataset) {
        return NO_RESULT;
    }

    @Override
    public CompletionStage<Void> touch(final IRI identifier) {
        return NO_RESULT;
    }

    @Override
    public Set<IRI> supportedInteractionModels() {
        return emptySet();
    }

    @Override
    public String generateIdentifier() {
        return TRELLIS_DATA_PREFIX;
    }
}
