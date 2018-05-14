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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;

/**
 * An interface for a Memento subsystem.
 */
public interface MementoService {

    /**
     * Create a new Memento for a resource.
     * @param identifier the resource identifier
     * @param time the time of the Memento
     * @param data the data to save
     */
    void put(IRI identifier, Instant time, Stream<? extends Quad> data);

    /**
     * Fetch a Memento resource for the given time.
     * @param identifier the resource identifier
     * @param time the requested time
     * @return the resource if it exists
     */
    Optional<Resource> get(IRI identifier, Instant time);

    /**
     * List all of the Mementos for a resource.
     * @param identifier the resource identifier
     * @return a list of Memento dateTime ranges
     */
    List<Range<Instant>> list(IRI identifier);

    /**
     * Delete a Memento resource.
     * @param identifier the resource identifier
     * @param time the version at the given time
     * @return true on success; false otherwise
     */
    Boolean delete(IRI identifier, Instant time);
}
