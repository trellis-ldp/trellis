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

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;

/**
 * A service that persists resources by <i>replacing</i> their records.
 *
 * @author ajs6f
 * @param <U> the type of resource that can be persisted by this service
 */
public interface MutableDataService<U> extends RetrievalService<U> {

    /**
     * Put a resource into the server.
     *
     * @param identifier the identifier for the new resource
     * @param session the session context for this operation
     * @param ixnModel the LDP interaction model for this resource
     * @param container an LDP container for this resource
     * @param dataset the dataset
     * @return whether the resource was added
     */
    Future<Boolean> create(IRI identifier, Session session, IRI ixnModel, IRI container, Dataset dataset);

    /**
     * Replace a resource in the server.
     *
     * @param identifier the identifier for the new resource
     * @param session the session context for this operation
     * @param ixnModel the LDP interaction model for this resource
     * @param container an LDP container for this resource
     * @param dataset the dataset
     * @return whether the resource was replaced
     */
    Future<Boolean> replace(IRI identifier, Session session, IRI ixnModel, IRI container, Dataset dataset);


    /**
     * Delete a resource from the server.
     *
     * @param identifier the identifier for the new resource
     * @param session the session context for this operation
     * @param ixnModel the new LDP interaction model for this resource
     * @param dataset the dataset
     * @return whether the resource was deleted
     */
    Future<Boolean> delete(IRI identifier, Session session, IRI ixnModel, Dataset dataset);

}
