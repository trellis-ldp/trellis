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
 * RDF Terms from the W3C SKOS Vocabulary
 *
 * @see <a href="https://www.w3.org/2009/08/skos-reference/skos.html">W3C SKOS Vocabulary</a>
 *
 * @author acoburn
 */
public final class SKOS extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.w3.org/2004/02/skos/core#";

    /* Classes */
    public static final IRI Concept = createIRI(URI + "Concept");
    public static final IRI ConceptScheme = createIRI(URI + "ConceptScheme");
    public static final IRI Collection = createIRI(URI + "Collection");
    public static final IRI OrderedCollection = createIRI(URI + "OrderedCollection");

    /* Propreties */
    public static final IRI inScheme = createIRI(URI + "inScheme");
    public static final IRI hasTopConcept = createIRI(URI + "hasTopConcept");
    public static final IRI topConceptOf = createIRI(URI + "topConceptOf");
    public static final IRI prefLabel = createIRI(URI + "prefLabel");
    public static final IRI altLabel = createIRI(URI + "altLabel");
    public static final IRI hiddenLabel = createIRI(URI + "hiddenLabel");
    public static final IRI notation = createIRI(URI + "notation");
    public static final IRI note = createIRI(URI + "note");
    public static final IRI changeNote = createIRI(URI + "changeNote");
    public static final IRI definition = createIRI(URI + "definition");
    public static final IRI editorialNote = createIRI(URI + "editorialNote");
    public static final IRI example = createIRI(URI + "example");
    public static final IRI historyNote = createIRI(URI + "historyNote");
    public static final IRI scopeNote = createIRI(URI + "scopeNote");
    public static final IRI semanticRelation = createIRI(URI + "semanticRelation");
    public static final IRI broader = createIRI(URI + "broader");
    public static final IRI narrower = createIRI(URI + "narrower");
    public static final IRI related = createIRI(URI + "related");
    public static final IRI broaderTransitive = createIRI(URI + "broaderTransitive");
    public static final IRI narrowerTransitive = createIRI(URI + "narrowerTransitive");
    public static final IRI member = createIRI(URI + "member");
    public static final IRI memberList = createIRI(URI + "memberList");
    public static final IRI mappingRelation = createIRI(URI + "mappingRelation");
    public static final IRI broadMatch = createIRI(URI + "broadMatch");
    public static final IRI narrowMatch = createIRI(URI + "narrowMatch");
    public static final IRI relatedMatch = createIRI(URI + "relatedMatch");
    public static final IRI exactMatch = createIRI(URI + "exactMatch");
    public static final IRI closeMatch = createIRI(URI + "closeMatch");

    private SKOS() {
        super();
    }
}
