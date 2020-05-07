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

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;

/**
 * This represents the data for a server event.
 *
 * <p>It is expected that these events are serialized as conforming ActivityStream messages.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-core/">Activity Streams 2.0</a>
 *
 * @author acoburn
 */
public interface Event {

    /**
     * Get an identifier for this event.
     *
     * @return an IRI for this event
     */
    IRI getIdentifier();

    /**
     * Get the Agents associated with this event.
     *
     * @return the agents associated with this event
     */
    Collection<IRI> getAgents();

    /**
     * Get the resource identifier, if one exists.
     *
     * @return an identifier for the resource that is the object of this event
     */
    Optional<IRI> getObject();

    /**
     * Get types for this event.
     *
     * @return the types for this event
     */
    Collection<IRI> getTypes();

    /**
     * Get the types for the resource that is the object of this event.
     *
     * @return the types for the resource related to this event
     */
    Collection<IRI> getObjectTypes();

    /**
     * Get the created date for this event.
     *
     * @return the date-time for this event
     */
    Instant getCreated();

    /**
     * Get the inbox corresponding to the resource corresponding to this event, if one exists.
     *
     * @return the inbox
     */
    Optional<IRI> getInbox();
}
