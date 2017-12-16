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
 * RDF Terms from the Memento Vocabulary
 *
 * @see <a href="https://tools.ietf.org/html/rfc7089">HTTP Framework for Time-Based Access to Resource States</a>
 *
 * @author acoburn
 */
public final class Memento extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://mementoweb.org/ns#";

    /* Classes */
    public static final IRI Memento = createIRI(URI + "Memento");
    public static final IRI OriginalResource = createIRI(URI + "OriginalResource");
    public static final IRI TimeGate = createIRI(URI + "TimeGate");
    public static final IRI TimeMap = createIRI(URI + "TimeMap");

    /* Properties */
    public static final IRI memento = createIRI(URI + "memento");
    public static final IRI mementoDatetime = createIRI(URI + "mementoDatetime");
    public static final IRI timegate = createIRI(URI + "timegate");
    public static final IRI timemap = createIRI(URI + "timemap");
    public static final IRI original = createIRI(URI + "original");

    private Memento() {
        super();
    }
}
