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
package org.trellisldp.http.core;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.trellisldp.api.TrellisUtils.getInstance;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.Event;

/**
 * A simple Event implementation.
 */
public class SimpleEvent implements Event {

    private static final RDF rdf = getInstance();

    private final IRI identifier;
    private final IRI target;
    private final Instant created;
    private final IRI agent;
    private final List<IRI> activityTypes;
    private final List<IRI> targetTypes;

    /**
     * Create a new notification.
     * @param target the target resource
     * @param agent the agent associated with this event
     * @param activityTypes the activity types associated with this event
     * @param targetTypes the rdf types of the resource
     */
    public SimpleEvent(final String target, final IRI agent, final List<IRI> activityTypes,
            final List<IRI> targetTypes) {
        this.target = rdf.createIRI(target);
        this.identifier = rdf.createIRI("urn:uuid:" + randomUUID());
        this.agent = agent;
        this.activityTypes = activityTypes;
        this.created = now();
        this.targetTypes = targetTypes;
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public Collection<IRI> getAgents() {
        return asList(agent);
    }

    @Override
    public Optional<IRI> getTarget() {
        return of(target);
    }

    @Override
    public Collection<IRI> getTypes() {
        return activityTypes;
    }

    @Override
    public Collection<IRI> getTargetTypes() {
        return targetTypes;
    }

    @Override
    public Instant getCreated() {
        return created;
    }

    @Override
    public Optional<IRI> getInbox() {
        return empty();
    }
}
