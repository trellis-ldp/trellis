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
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.Event;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;

/**
 * A simple Event implementation.
 */
public class SimpleEvent implements Event {

    private static final RDF rdf = getInstance();

    private final IRI identifier;
    private final IRI target;
    private final Dataset data;
    private final Instant created;
    private final Set<IRI> graphsForTypes = new HashSet<>();

    /**
     * Create a new notification.
     * @param target the target resource
     * @param data the corresponding data
     */
    public SimpleEvent(final String target, final Dataset data) {
        this.target = rdf.createIRI(target);
        this.data = data;
        this.identifier = rdf.createIRI("urn:uuid:" + randomUUID());
        this.created = now();
        this.graphsForTypes.add(Trellis.PreferServerManaged);
        this.graphsForTypes.add(Trellis.PreferUserManaged);
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public Collection<IRI> getAgents() {
        return data.getGraph(Trellis.PreferAudit)
            .map(graph -> graph.stream(null, PROV.wasAssociatedWith, null).map(Triple::getObject)
                    .filter(term -> term instanceof IRI).map(term -> (IRI) term).collect(toList()))
            .orElseGet(Collections::emptyList);
    }

    @Override
    public Optional<IRI> getTarget() {
        return of(target);
    }

    @Override
    public Collection<IRI> getTypes() {
        return data.getGraph(Trellis.PreferAudit)
            .map(graph -> graph.stream(null, type, null).map(Triple::getObject)
                    .filter(term -> term instanceof IRI).map(term -> (IRI) term).collect(toList()))
            .orElseGet(Collections::emptyList);
    }

    @Override
    public Collection<IRI> getTargetTypes() {
        return data.stream().filter(quad -> quad.getGraphName().filter(graphsForTypes::contains).isPresent())
            .filter(quad -> quad.getPredicate().equals(type)).map(Quad::getObject)
            .filter(term -> term instanceof IRI).map(term -> (IRI) term).distinct().collect(toList());
    }

    @Override
    public Instant getCreated() {
        return created;
    }

    @Override
    public Optional<IRI> getInbox() {
        return data.getGraph(Trellis.PreferUserManaged)
            .flatMap(graph -> graph.stream(null, LDP.inbox, null).map(Triple::getObject)
                    .filter(term -> term instanceof IRI).map(term -> (IRI) term).findFirst());
    }
}
