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
package org.trellisldp.common;

import static java.time.Instant.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.Notification;
import org.trellisldp.api.RDFFactory;

/**
 * A simple Notification implementation.
 */
public class SimpleNotification implements Notification {

    private static final RDF rdf = RDFFactory.getInstance();

    private final IRI identifier;
    private final IRI object;
    private final Instant created;
    private final IRI agent;
    private final String objectState;
    private final List<IRI> activityTypes;
    private final List<IRI> objectTypes;

    /**
     * Create a new notification.
     * @param object the resource
     * @param agent the agent associated with this notification
     * @param activityTypes the activity types associated with this notification
     * @param objectTypes the rdf types of the resource
     * @param objectState a state indicator for the resource
     */
    public SimpleNotification(final String object, final IRI agent, final List<IRI> activityTypes,
            final List<IRI> objectTypes, final String objectState) {
        this.identifier = rdf.createIRI("urn:uuid:" + randomUUID());
        this.created = now();
        this.agent = agent;
        this.activityTypes = activityTypes;
        this.object = rdf.createIRI(object);
        this.objectTypes = objectTypes;
        this.objectState = objectState;
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public Collection<IRI> getAgents() {
        return singletonList(agent);
    }

    @Override
    public Optional<IRI> getObject() {
        return Optional.of(object);
    }

    @Override
    public Collection<IRI> getTypes() {
        return activityTypes;
    }

    @Override
    public Collection<IRI> getObjectTypes() {
        return objectTypes;
    }

    @Override
    public Optional<String> getObjectState() {
        return Optional.ofNullable(objectState);
    }

    @Override
    public Instant getCreated() {
        return created;
    }
}
