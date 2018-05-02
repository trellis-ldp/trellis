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
package org.trellisldp.vocabulary;

import static org.trellisldp.vocabulary.VocabUtils.createIRI;

import org.apache.commons.rdf.api.IRI;

/**
 * RDF Terms from the W3C RDF Syntax Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/rdf-schema/">RDF Schema 1.1</a> and
 * <a href="https://www.w3.org/TR/rdf11-concepts/">RDF 1.1 Concepts and Abstract Syntax</a>
 *
 * @author acoburn
 */
public final class RDF {

    /* Namespace */
    private static final String URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    /* Classes */
    public static final IRI Property = createIRI(getNamespace() + "Property");
    public static final IRI Statement = createIRI(getNamespace() + "Statement");
    public static final IRI Bag = createIRI(getNamespace() + "Bag");
    public static final IRI Seq = createIRI(getNamespace() + "Seq");
    public static final IRI Alt = createIRI(getNamespace() + "Alt");
    public static final IRI List = createIRI(getNamespace() + "List");

    /* Datatypes */
    public static final IRI XMLLiteral = createIRI(getNamespace() + "XMLLiteral");
    public static final IRI HTML = createIRI(getNamespace() + "HTML");
    public static final IRI langString = createIRI(getNamespace() + "langString");

    /* List */
    public static final IRI nil = createIRI(getNamespace() + "nil");

    /* Properties */
    public static final IRI type = createIRI(getNamespace() + "type");
    public static final IRI subject = createIRI(getNamespace() + "subject");
    public static final IRI predicate = createIRI(getNamespace() + "predicate");
    public static final IRI object = createIRI(getNamespace() + "object");
    public static final IRI value = createIRI(getNamespace() + "value");
    public static final IRI first = createIRI(getNamespace() + "first");
    public static final IRI rest = createIRI(getNamespace() + "rest");

    private RDF() {
        // prevent instantiation
    }
}
