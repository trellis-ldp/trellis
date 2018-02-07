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

import java.util.concurrent.Future;

/**
 * A service that persists resources by appending to their records. Nothing that
 * is recorded by {@link #add} will be deleted by using {@code add} again.
 * 
 * @author ajs6f
 *
 * @param <T> the type of identifier for resources that can be persisted by this service
 * @param <U> the type of resource that can be persisted by this service
 */
public interface ImmutableDataService<T, U> extends RetrievalService<T, U> {

    /**
     * @param identifier the identifier under which to persist a resource
     * @param resource a resource to persist
     * @return whether the resource was successfully persisted
     */
    Future<Boolean> add(T identifier, U resource);
}