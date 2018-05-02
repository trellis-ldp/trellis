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
 * RDFS Terms from the W3C RDF Schema Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/rdf-schema/">RDF Schema 1.1</a> and
 * <a href="https://www.w3.org/TR/rdf11-concepts/">RDF 1.1 Concepts and Abstract Syntax</a>
 *
 * @author acoburn
 */
public final class RDFS {

    /* Namespace */
    public static final String NS = "http://www.w3.org/2000/01/rdf-schema#";

    /* Classes */
    public static final IRI Resource = createIRI(NS + "Resource");
    public static final IRI Class = createIRI(NS + "Class");
    public static final IRI Literal = createIRI(NS + "Literal");
    public static final IRI Container = createIRI(NS + "Container");
    public static final IRI ContainerMembershipProperty = createIRI(NS + "ContainerMembershipProperty");
    public static final IRI Datatype = createIRI(NS + "Datatype");

    /* Properties */
    public static final IRI subClassOf = createIRI(NS + "subClassOf");
    public static final IRI subPropertyOf = createIRI(NS + "subPropertyOf");
    public static final IRI comment = createIRI(NS + "comment");
    public static final IRI label = createIRI(NS + "label");
    public static final IRI domain = createIRI(NS + "domain");
    public static final IRI range = createIRI(NS + "range");
    public static final IRI seeAlso = createIRI(NS + "seeAlso");
    public static final IRI isDefinedBy = createIRI(NS + "isDefinedBy");
    public static final IRI member = createIRI(NS + "member");

    private RDFS() {
        // prevent instantiation
    }
}
