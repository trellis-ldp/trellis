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
package org.trellisldp.triplestore;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.trellisldp.api.RDFUtils.TRELLIS_SESSION_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;

import java.time.Instant;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.api.Session;

/**
 * A simple Session implementation.
 */
class SimpleSession implements Session {

    private final Instant created = now();
    private final IRI identifier = getInstance().createIRI(TRELLIS_SESSION_PREFIX + randomUUID());
    private final IRI agent;

    public SimpleSession(final IRI agent) {
        this.agent = agent;
    }

    @Override
    public IRI getAgent() {
        return agent;
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public Optional<IRI> getDelegatedBy() {
        return Optional.empty();
    }

    @Override
    public Instant getCreated() {
        return created;
    }
}
