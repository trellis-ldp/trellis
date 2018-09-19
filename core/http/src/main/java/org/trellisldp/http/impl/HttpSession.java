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
package org.trellisldp.http.impl;

import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.trellisldp.api.RDFUtils.TRELLIS_SCHEME;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.vocabulary.Trellis.AnonymousAgent;

import java.time.Instant;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.api.Session;

/**
 * @author acoburn
 */
public class HttpSession implements Session {

    private final IRI identifier = getInstance().createIRI(TRELLIS_SCHEME + "session/" + randomUUID());
    private final IRI agent;
    private final IRI delegatedBy;
    private final Instant created;

    /**
     * Create an HTTP-based session.
     *
     * @param agent the agent
     * @param delegatedBy the delegatedBy value
     */
    public HttpSession(final IRI agent, final IRI delegatedBy) {
        this.agent = agent;
        this.delegatedBy = delegatedBy;
        this.created = now();
    }

    /**
     * Create an HTTP-based session.
     *
     * @param agent the agent
     */
    public HttpSession(final IRI agent) {
        this(agent, null);
    }

    /**
     * Create an HTTP-based session.
     */
    public HttpSession() {
        this(AnonymousAgent);
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public IRI getAgent() {
        return agent;
    }

    @Override
    public Optional<IRI> getDelegatedBy() {
        return ofNullable(delegatedBy);
    }

    @Override
    public Instant getCreated() {
        return created;
    }
}
