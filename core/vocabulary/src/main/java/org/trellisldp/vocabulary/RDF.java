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
    public static final String NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /* Classes */
    public static final IRI PlainLiteral = createIRI(NS + "PlainLiteral");
    public static final IRI Property = createIRI(NS + "Property");
    public static final IRI Statement = createIRI(NS + "Statement");
    public static final IRI Bag = createIRI(NS + "Bag");
    public static final IRI Seq = createIRI(NS + "Seq");
    public static final IRI Alt = createIRI(NS + "Alt");
    public static final IRI List = createIRI(NS + "List");

    /* Datatypes */
    public static final IRI XMLLiteral = createIRI(NS + "XMLLiteral");
    public static final IRI HTML = createIRI(NS + "HTML");
    public static final IRI langString = createIRI(NS + "langString");

    /* List */
    public static final IRI nil = createIRI(NS + "nil");

    /* Properties */
    public static final IRI type = createIRI(NS + "type");
    public static final IRI subject = createIRI(NS + "subject");
    public static final IRI predicate = createIRI(NS + "predicate");
    public static final IRI object = createIRI(NS + "object");
    public static final IRI value = createIRI(NS + "value");
    public static final IRI first = createIRI(NS + "first");
    public static final IRI rest = createIRI(NS + "rest");

    private RDF() {
        // prevent instantiation
    }
}
