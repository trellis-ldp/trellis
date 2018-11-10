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

import java.util.concurrent.CompletableFuture;

import org.apache.commons.rdf.api.IRI;

/**
 * A service that persists resources by <i>replacing</i> their records.
 *
 * @author ajs6f
 * @param <U> the type of resource that can be persisted by this service
 * @param <V> the type of data used as a template for generating a resource
 */
public interface MutableDataService<U, V> extends RetrievalService<U> {

    /**
     * Create a resource in the server.
     *
     * @implSpec the default implementation of this method is to proxy create requests to the {@link #replace} method.
     * @param template the resource template
     * @return a new completion stage that, when the stage completes normally, indicates that the supplied data were
     * successfully created in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    default CompletableFuture<Void> create(V template) {
        return replace(template);
    }

    /**
     * Replace a resource in the server.
     *
     * @param template the resource template
     * @return a new completion stage that, when the stage completes normally, indicates that the supplied data
     * were successfully stored in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    CompletableFuture<Void> replace(V template);

    /**
     * Delete a resource from the server.
     *
     * @param identifier the identifier for the new resource
     * @param container an LDP container for this resource, {@code null} for none
     * @return a new completion stage that, when the stage completes normally, indicates that the resource
     * was successfully deleted from the corresponding persistence layer. In the case of an unsuccessful delete
     * operation, the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    CompletableFuture<Void> delete(IRI identifier, IRI container);

}
