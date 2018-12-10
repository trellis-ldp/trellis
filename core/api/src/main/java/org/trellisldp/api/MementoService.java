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

import java.time.Instant;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.rdf.api.IRI;

/**
 * An interface for a Memento subsystem. Mementos of {@link Resource}s may be made and retrieved using this service.
 * Mementos may also be recorded by other means, including by the persistence layer independently of Trellis, but unless
 * they are retrieved via this service, Trellis will not publish them as HTTP resources. Mementos of NonRDFSources (like
 * any other {@link Resource}) may also be made and retrieved here, but the associated {@link java.io.InputStream}s are
 * made (like all binary {@link java.io.InputStream}s) via a {@link BinaryService} implementation.
 */
public interface MementoService {

    /**
     * Create a new Memento for a resource, retrieved from a {@link ResourceService}.
     * @param resourceService the resource service.
     * @param identifier the identifier.
     * @apiNote the default implementation of this method fetches a resource from a {@link ResourceService} that is
     *          external to the Memento service. In the case that the two services are managed by the same persistence
     *          layer, it may not be necessary to fetch a {@link Resource} from the persistence layer, in which case
     *          this method can be overridden as a no-op method, e.g. {@code return completedFuture(null);}.
     * @return a new completion stage that, when the stage completes normally, indicates that the Memento resource was
     * successfully created in the corresponding persistence layer.
     */
    default CompletableFuture<Void> put(final ResourceService resourceService, final IRI identifier) {
        return resourceService.get(identifier).thenCompose(this::put);
    }

    /**
     * Create a new Memento for a resource.
     * @param resource the resource
     * @return a new completion stage that, when the stage completes normally, indicates that the Memento resource was
     * successfully created in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    CompletableFuture<Void> put(Resource resource);

    /**
     * Fetch a Memento resource for the given time.
     * @param identifier the resource identifier
     * @param time the requested time
     * @return the new completion stage, containing the fetched resource
     */
    CompletableFuture<Resource> get(IRI identifier, Instant time);

    /**
     * Get the times for all of the Mementos of the given resource.
     * @param identifier the resource identifier
     * @return the new completion stage containing a collection of Memento dateTimes
     */
    CompletableFuture<SortedSet<Instant>> mementos(IRI identifier);
}
