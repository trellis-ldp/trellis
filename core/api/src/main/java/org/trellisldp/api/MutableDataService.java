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

import java.util.concurrent.CompletionStage;

import org.apache.commons.rdf.api.Dataset;

/**
 * A service that persists resources by <i>replacing</i> their records.
 *
 * @author ajs6f
 * @param <U> the type of resource that can be persisted by this service
 */
public interface MutableDataService<U> extends RetrievalService<U> {

    /**
     * Create a resource in the server.
     *
     * @implSpec the default implementation of this method is to proxy create requests to the {@link #replace} method.
     * @param metadata metadata for the new resource
     * @param dataset the dataset to be persisted
     * @return a new completion stage that, when the stage completes normally, indicates that the supplied data were
     * successfully created in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletionStage} will complete exceptionally and can be handled with
     * {@link CompletionStage#handle}, {@link CompletionStage#exceptionally} or similar methods.
     */
    default CompletionStage<Void> create(Metadata metadata, Dataset dataset) {
        return replace(metadata, dataset);
    }

    /**
     * Replace a resource in the server.
     *
     * @param metadata metadata for the resource
     * @param dataset the dataset to be persisted
     * @return a new completion stage that, when the stage completes normally, indicates that the supplied data
     * were successfully stored in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletionStage} will complete exceptionally and can be handled with
     * {@link CompletionStage#handle}, {@link CompletionStage#exceptionally} or similar methods.
     */
    CompletionStage<Void> replace(Metadata metadata, Dataset dataset);

    /**
     * Delete a resource from the server.
     *
     * @param metadata metadata for the resource
     * @return a new completion stage that, when the stage completes normally, indicates that the resource
     * was successfully deleted from the corresponding persistence layer. In the case of an unsuccessful delete
     * operation, the {@link CompletionStage} will complete exceptionally and can be handled with
     * {@link CompletionStage#handle}, {@link CompletionStage#exceptionally} or similar methods.
     */
    CompletionStage<Void> delete(Metadata metadata);

}
