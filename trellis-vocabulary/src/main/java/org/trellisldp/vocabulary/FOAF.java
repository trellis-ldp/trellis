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
 * RDF Terms from the FOAF Vocabulary
 *
 * @see <a href="http://xmlns.com/foaf/spec/">Foaf Vocabulary</a>
 *
 * @author acoburn
 */
public final class FOAF extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://xmlns.com/foaf/0.1/";

    /* Classes */
    public static final IRI Agent = createIRI(URI + "Agent");
    public static final IRI Document = createIRI(URI + "Document");
    public static final IRI Group = createIRI(URI + "Group");
    public static final IRI Image = createIRI(URI + "Image");
    public static final IRI Organization = createIRI(URI + "Organization");
    public static final IRI Person = createIRI(URI + "Person");

    /* Properties */
    public static final IRI homepage = createIRI(URI + "homepage");
    public static final IRI isPrimaryTopicOf = createIRI(URI + "isPrimaryTopicOf");
    public static final IRI knows = createIRI(URI + "knows");
    public static final IRI made = createIRI(URI + "made");
    public static final IRI maker = createIRI(URI + "maker");
    public static final IRI member = createIRI(URI + "member");
    public static final IRI mbox = createIRI(URI + "mbox");
    public static final IRI page = createIRI(URI + "page");
    public static final IRI primaryTopic = createIRI(URI + "primaryTopic");
    public static final IRI weblog = createIRI(URI + "weblog");

    private FOAF() {
        super();
    }
}
