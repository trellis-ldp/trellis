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
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;

/**
 * This interface represents a user's session when interacting with a Trellis resource.
 *
 * <p>Users, groups and delegates are represented as {@link IRI}s.
 *
 * @author acoburn
 */
public interface Session {

    /**
     * Get a session identifier.
     *
     * @return a session identifier
     */
    IRI getIdentifier();

    /**
     * Get an agent identifier.
     *
     * @return an identifier for a user/agent
     */
    IRI getAgent();

    /**
     * Get the user that delegated access, if one exists.
     *
     * @return the user who delegated access
     */
    Optional<IRI> getDelegatedBy();

    /**
     * Get the date when the session was created.
     *
     * @return the creation date
     */
    Instant getCreated();
}
