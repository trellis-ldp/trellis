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

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;

/**
 * A service that persists resources by appending to their records. Nothing that
 * is recorded by {@link #add} will be deleted by using {@code add} again.
 * 
 * @author ajs6f
 *
 * @param <T> the type of resource that can be persisted by this service
 */
public interface ImmutableDataService<T> extends RetrievalService<T> {


    /**
     * @param identifier the identifier under which to persist a dataset
     * @param session the session context for this operation
     * @param dataset a dataset to persist
     * @return a new completion stage that, when the stage completes normally, indicates that the supplied data
     * were successfully stored in the corresponding persistence layer. In the case of an unsuccessful write operation,
     * the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    CompletableFuture<Void> add(IRI identifier, Session session, Dataset dataset);

}
