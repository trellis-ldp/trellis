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
 * RDF Terms from the W3C SKOS Vocabulary.
 *
 * @see <a href="https://www.w3.org/2009/08/skos-reference/skos.html">W3C SKOS Vocabulary</a>
 *
 * @author acoburn
 */
public final class SKOS {

    /* Namespace */
    public static final String NS = "http://www.w3.org/2004/02/skos/core#";

    /* Classes */
    public static final IRI Concept = createIRI(NS + "Concept");
    public static final IRI ConceptScheme = createIRI(NS + "ConceptScheme");
    public static final IRI Collection = createIRI(NS + "Collection");
    public static final IRI OrderedCollection = createIRI(NS + "OrderedCollection");

    /* Propreties */
    public static final IRI inScheme = createIRI(NS + "inScheme");
    public static final IRI hasTopConcept = createIRI(NS + "hasTopConcept");
    public static final IRI topConceptOf = createIRI(NS + "topConceptOf");
    public static final IRI prefLabel = createIRI(NS + "prefLabel");
    public static final IRI altLabel = createIRI(NS + "altLabel");
    public static final IRI hiddenLabel = createIRI(NS + "hiddenLabel");
    public static final IRI notation = createIRI(NS + "notation");
    public static final IRI note = createIRI(NS + "note");
    public static final IRI changeNote = createIRI(NS + "changeNote");
    public static final IRI definition = createIRI(NS + "definition");
    public static final IRI editorialNote = createIRI(NS + "editorialNote");
    public static final IRI example = createIRI(NS + "example");
    public static final IRI historyNote = createIRI(NS + "historyNote");
    public static final IRI scopeNote = createIRI(NS + "scopeNote");
    public static final IRI semanticRelation = createIRI(NS + "semanticRelation");
    public static final IRI broader = createIRI(NS + "broader");
    public static final IRI narrower = createIRI(NS + "narrower");
    public static final IRI related = createIRI(NS + "related");
    public static final IRI broaderTransitive = createIRI(NS + "broaderTransitive");
    public static final IRI narrowerTransitive = createIRI(NS + "narrowerTransitive");
    public static final IRI member = createIRI(NS + "member");
    public static final IRI memberList = createIRI(NS + "memberList");
    public static final IRI mappingRelation = createIRI(NS + "mappingRelation");
    public static final IRI broadMatch = createIRI(NS + "broadMatch");
    public static final IRI narrowMatch = createIRI(NS + "narrowMatch");
    public static final IRI relatedMatch = createIRI(NS + "relatedMatch");
    public static final IRI exactMatch = createIRI(NS + "exactMatch");
    public static final IRI closeMatch = createIRI(NS + "closeMatch");

    private SKOS() {
        // prevent instantiation
    }
}
