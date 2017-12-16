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

import org.apache.commons.rdf.api.IRI;

/**
 * RDF Terms from the W3C RDF Syntax Vocabulary
 *
 * @see <a href="https://www.w3.org/TR/rdf-schema/">RDF Schema 1.1</a> and
 * <a href="https://www.w3.org/TR/rdf11-concepts/">RDF 1.1 Concepts and Abstract Syntax</a>
 *
 * @author acoburn
 */
public final class RDF extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /* Classes */
    public static final IRI Property = createIRI(URI + "Property");
    public static final IRI Statement = createIRI(URI + "Statement");
    public static final IRI Bag = createIRI(URI + "Bag");
    public static final IRI Seq = createIRI(URI + "Seq");
    public static final IRI Alt = createIRI(URI + "Alt");
    public static final IRI List = createIRI(URI + "List");

    /* Datatypes */
    public static final IRI XMLLiteral = createIRI(URI + "XMLLiteral");
    public static final IRI HTML = createIRI(URI + "HTML");
    public static final IRI langString = createIRI(URI + "langString");

    /* List */
    public static final IRI nil = createIRI(URI + "nil");

    /* Properties */
    public static final IRI type = createIRI(URI + "type");
    public static final IRI subject = createIRI(URI + "subject");
    public static final IRI predicate = createIRI(URI + "predicate");
    public static final IRI object = createIRI(URI + "object");
    public static final IRI value = createIRI(URI + "value");
    public static final IRI first = createIRI(URI + "first");
    public static final IRI rest = createIRI(URI + "rest");

    private RDF() {
        super();
    }
}
