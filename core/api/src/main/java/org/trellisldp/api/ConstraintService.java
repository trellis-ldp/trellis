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

import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;

/**
 * The ConstraintService defines rules that constrain RDF triples
 * on a graph for a particular resource type.
 *
 * @author acoburn
 */
public interface ConstraintService {

    /**
     * Check a graph against an LDP interaction model.
     *
     * @param identifier the resource identifier
     * @param interactionModel the interaction model
     * @param graph the graph
     * @param domain the domain of the resource
     * @return any constraint violations on the graph
     */
    Stream<ConstraintViolation> constrainedBy(IRI identifier, IRI interactionModel, Graph graph, String domain);

    /**
     * Check a graph against an LDP interaction model.
     *
     * @param identifier the resource identifier
     * @param interactionModel the interaction model
     * @param graph the graph
     * @return any constraint violations on the graph
     */
    default Stream<ConstraintViolation> constrainedBy(IRI identifier, IRI interactionModel, Graph graph) {
        return constrainedBy(identifier, interactionModel, graph, TrellisUtils.TRELLIS_DATA_PREFIX);
    }
}
