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
package org.trellisldp.rdfa;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Triple;

/**
 * A triple object with additional labels.
 *
 * @author acoburn
 */
class LabelledTriple {

    private final Triple triple;
    private final String predLabel;
    private final String objLabel;

    /**
     * Create a LabelledTriple.
     *
     * @param triple the triple
     * @param predicate the label for the predicate
     * @param object the label for the object
     */
    public LabelledTriple(final Triple triple, final String predicate, final String object) {
        this.triple = triple;
        this.predLabel = predicate;
        this.objLabel = object;
    }

    /**
     * Get the subject of the triple as a string.
     *
     * @return a string form of the subject
     */
    public String getSubject() {
        if (triple.getSubject() instanceof IRI) {
            return ((IRI) triple.getSubject()).getIRIString();
        }
        return triple.getSubject().ntriplesString();
    }

    /**
     * Get the predicate of the triple as a string.
     *
     * @return the string form of the predicate
     */
    public String getPredicate() {
        return triple.getPredicate().getIRIString();
    }

    /**
     * Get the object of the triple as a string.
     *
     * @return the string form of the object
     */
    public String getObject() {
        if (triple.getObject() instanceof Literal) {
            return ((Literal) triple.getObject()).getLexicalForm();
        } else if (triple.getObject() instanceof IRI) {
            return ((IRI) triple.getObject()).getIRIString();
        }
        return triple.getObject().ntriplesString();
    }

    /**
     * Get the label for the predicate.
     *
     * @return the predicate label
     */
    public String getPredicateLabel() {
        if (predLabel != null) {
            return predLabel;
        }
        return getPredicate();
    }

    /**
     * Get the label for the object.
     *
     * @return the object label
     */
    public String getObjectLabel() {
        if (objLabel != null) {
            return objLabel;
        }
        return getObject();
    }

    /**
     * Determine whether the object is an IRI.
     *
     * @return true if the object is an IRI; false otherwise
     */
    public boolean getObjectIsIRI() {
        return triple.getObject() instanceof IRI;
    }
}
