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

import static java.util.Collections.singletonList;

import java.util.List;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;

/**
 * A class that represents a constraint violation in an RDF graph.
 *
 * @author acoburn
 */
public class ConstraintViolation {

    private final IRI constraint;

    private final List<Triple> triples;

    /**
     * Create a new constraint violation.
     *
     * @param constraint the constraint IRI
     * @param triple the triple
     */
    public ConstraintViolation (final IRI constraint, final Triple triple) {
        this(constraint, singletonList(triple));
    }

    /**
     * Create a new constraint violation.
     *
     * @param constraint the constraint IRI
     * @param triples the triples
     */
    public ConstraintViolation(final IRI constraint, final List<Triple> triples) {
        this.constraint = constraint;
        this.triples = triples;
    }

    /**
     * Get the constraint IRI for this violation.
     *
     * @return the constraint IRI
     */
    public IRI getConstraint() {
        return constraint;
    }

    /**
     * Get the triples causing the constraint violation.
     *
     * @return the triples
     */
    public List<Triple> getTriples() {
        return triples;
    }

    @Override
    public String toString() {
        return constraint.getIRIString() + ": " + triples;
    }
}
