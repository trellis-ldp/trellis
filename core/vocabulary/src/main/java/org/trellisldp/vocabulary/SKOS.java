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
package org.trellisldp.vocabulary;

import static org.trellisldp.vocabulary.VocabUtils.createIRI;

import org.apache.commons.rdf.api.IRI;

/**
 * RDF Terms from the W3C SKOS Vocabulary.
 *
 * @see <a href="https://www.w3.org/2009/08/skos-reference/skos.html">W3C SKOS Vocabulary</a>
 *
 * @author acoburn
 */
public final class SKOS {

    /* Namespace */
    private static final String URI = "http://www.w3.org/2004/02/skos/core#";

    /* Classes */
    public static final IRI Concept = createIRI(getNamespace() + "Concept");
    public static final IRI ConceptScheme = createIRI(getNamespace() + "ConceptScheme");
    public static final IRI Collection = createIRI(getNamespace() + "Collection");
    public static final IRI OrderedCollection = createIRI(getNamespace() + "OrderedCollection");

    /* Propreties */
    public static final IRI inScheme = createIRI(getNamespace() + "inScheme");
    public static final IRI hasTopConcept = createIRI(getNamespace() + "hasTopConcept");
    public static final IRI topConceptOf = createIRI(getNamespace() + "topConceptOf");
    public static final IRI prefLabel = createIRI(getNamespace() + "prefLabel");
    public static final IRI altLabel = createIRI(getNamespace() + "altLabel");
    public static final IRI hiddenLabel = createIRI(getNamespace() + "hiddenLabel");
    public static final IRI notation = createIRI(getNamespace() + "notation");
    public static final IRI note = createIRI(getNamespace() + "note");
    public static final IRI changeNote = createIRI(getNamespace() + "changeNote");
    public static final IRI definition = createIRI(getNamespace() + "definition");
    public static final IRI editorialNote = createIRI(getNamespace() + "editorialNote");
    public static final IRI example = createIRI(getNamespace() + "example");
    public static final IRI historyNote = createIRI(getNamespace() + "historyNote");
    public static final IRI scopeNote = createIRI(getNamespace() + "scopeNote");
    public static final IRI semanticRelation = createIRI(getNamespace() + "semanticRelation");
    public static final IRI broader = createIRI(getNamespace() + "broader");
    public static final IRI narrower = createIRI(getNamespace() + "narrower");
    public static final IRI related = createIRI(getNamespace() + "related");
    public static final IRI broaderTransitive = createIRI(getNamespace() + "broaderTransitive");
    public static final IRI narrowerTransitive = createIRI(getNamespace() + "narrowerTransitive");
    public static final IRI member = createIRI(getNamespace() + "member");
    public static final IRI memberList = createIRI(getNamespace() + "memberList");
    public static final IRI mappingRelation = createIRI(getNamespace() + "mappingRelation");
    public static final IRI broadMatch = createIRI(getNamespace() + "broadMatch");
    public static final IRI narrowMatch = createIRI(getNamespace() + "narrowMatch");
    public static final IRI relatedMatch = createIRI(getNamespace() + "relatedMatch");
    public static final IRI exactMatch = createIRI(getNamespace() + "exactMatch");
    public static final IRI closeMatch = createIRI(getNamespace() + "closeMatch");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private SKOS() {
        // prevent instantiation
    }
}
