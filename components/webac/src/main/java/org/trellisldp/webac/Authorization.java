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
package org.trellisldp.webac;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.ACL;

/**
 * This class provides access to data defined in an WebAC Authorization graph.
 *
 * <p>Access to a resource can be controlled via WebAccessControl, an RDF-based access control system. A resource
 * can define an ACL resource via the Link header, using rel=acl. It can also point to an ACL resource using a triple
 * in the resource's own RDF graph via acl:accessControl. Absent an acl:accessControl triple, the parent resource
 * is checked, up to the server's root resource.
 *
 * <p>An ACL resource may contain multiple acl:Authorization sections. In an LDP context, this may be represented with
 * ldp:contains triples. Another common pattern is to refer to the acl:Authorization sections with blank nodes.
 *
 * @see <a href="https://www.w3.org/wiki/WebAccessControl">W3C WebAccessControl</a>
 * and <a href="https://github.com/solid/web-access-control-spec">Solid WebAC specification</a>
 *
 * @author acoburn
 */
public class Authorization {

    private static final RDF rdf = RDFFactory.getInstance();
    private static final Set<IRI> predicates = new HashSet<>(asList(ACL.agent, ACL.agentClass, ACL.agentGroup,
                ACL.mode, ACL.accessTo, ACL.default_));

    private final BlankNodeOrIRI identifier;
    private final Map<IRI, Set<IRI>> dataMap;

    /**
     * Create an Authorization object from a graph and an identifier.
     *
     * @param identifier the identifier
     * @param graph the graph
     * @return the Authorization object
     */
    public static Authorization from(final BlankNodeOrIRI identifier, final Graph graph) {
        return new Authorization(identifier, graph);
    }

    /**
     * Create an Authorization object from an RDF graph.
     *
     * @param identifier the subject IRI
     * @param graph the RDF graph
     */
    public Authorization(final BlankNodeOrIRI identifier, final Graph graph) {
        this.identifier = requireNonNull(identifier, "The Authorization identifier may not be null!");
        requireNonNull(graph, "The input graph may not be null!");

        this.dataMap = graph.stream(identifier, null, null)
            .filter(triple -> predicates.contains(triple.getPredicate()))
            .filter(triple -> triple.getObject() instanceof IRI)
            .collect(groupingBy(Triple::getPredicate, mapping(Authorization::normalizeIdentifier,
                            collectingAndThen(toSet(), Collections::unmodifiableSet))));
    }

    static IRI normalizeIdentifier(final Triple triple) {
        if (triple.getObject() instanceof IRI) {
            if (triple.getPredicate().equals(ACL.accessTo) || triple.getPredicate().equals(ACL.default_)) {
                final String obj = ((IRI) triple.getObject()).getIRIString();
                if (obj.endsWith("/") && !obj.equals(TRELLIS_DATA_PREFIX)) {
                    return rdf.createIRI(obj.substring(0, obj.length() - 1));
                }
            }
            return (IRI) triple.getObject();
        }
        return null;
    }

    /**
     * Retrieve the identifier for this Authorization.
     *
     * @return the identifier
     */
    public BlankNodeOrIRI getIdentifier() {
        return identifier;
    }

    /**
     * Retrieve the agents that are associated with this Authorization.
     *
     * @return the Agent values
     */
    public Set<IRI> getAgent() {
        return dataMap.getOrDefault(ACL.agent, emptySet());
    }

    /**
     * Retrieve the agent classes that are associated with this Authorization.
     *
     * @return the Agent class values
     */
    public Set<IRI> getAgentClass() {
        return dataMap.getOrDefault(ACL.agentClass, emptySet());
    }

    /**
     * Retrieve the agent groups that are associated with this Authorization.
     *
     * @return the Agent groups values
     */
    public Set<IRI> getAgentGroup() {
        return dataMap.getOrDefault(ACL.agentGroup, emptySet());
    }

    /**
     * Retrieve the access modes that are associated with this Authorization.
     *
     * @return the access mode values
     */
    public Set<IRI> getMode() {
        return dataMap.getOrDefault(ACL.mode, emptySet());
    }

    /**
     * Retrieve the resource identifiers to which this Authorization applies.
     *
     * @return the accessTo values
     */
    public Set<IRI> getAccessTo() {
        return dataMap.getOrDefault(ACL.accessTo, emptySet());
    }

    /**
     * Retrieve the directories for which this authorization is used for new resources in the container.
     *
     * @return the resource identifiers
     */
    public Set<IRI> getDefault() {
        return dataMap.getOrDefault(ACL.default_, emptySet());
    }
}
