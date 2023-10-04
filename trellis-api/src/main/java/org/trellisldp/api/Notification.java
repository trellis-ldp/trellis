/*
 * Copyright (c) Aaron Coburn and individual contributors
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
 * This represents the data for a server notification.
 *
 * <p>It is expected that these notifications are serialized as conforming ActivityStream messages.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-core/">Activity Streams 2.0</a>
 *
 * @author acoburn
 */
public interface Notification {

    /**
     * Get an identifier for this notification.
     *
     * @return an IRI for this notification
     */
    IRI getIdentifier();

    /**
     * Get the created date for this notification.
     *
     * @return the date-time for this notification
     */
    Instant getCreated();

    /**
     * Get types for this notification.
      *
     * @return the types for this notification
     */
    Collection<IRI> getTypes();

    /**
     * Get the Agents associated with this notification.
     *
     * @return the agents associated with this notification
     */
    Collection<IRI> getAgents();

    /**
     * Get the resource identifier, if one exists.
     *
     * @return an identifier for the resource that is the object of this notification
     */
    Optional<IRI> getObject();

    /**
     * Get object types for this notification.
      *
     * @return the types for the resource that is the object of this notification
     */
    Collection<IRI> getObjectTypes();

    /**
     * Get a state value for the object, if one exists.
     *
     * @return a state value for the resource, if relevant
     */
    Optional<String> getObjectState();
}
