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
 * RDF Terms from the FOAF Vocabulary.
 *
 * @see <a href="http://xmlns.com/foaf/spec/">Foaf Vocabulary</a>
 *
 * @author acoburn
 */
public final class FOAF {

    /* Namespace */
    private static final String URI = "http://xmlns.com/foaf/0.1/";

    /* Classes */
    public static final IRI Agent = createIRI(getNamespace() + "Agent");
    public static final IRI Document = createIRI(getNamespace() + "Document");
    public static final IRI Group = createIRI(getNamespace() + "Group");
    public static final IRI Image = createIRI(getNamespace() + "Image");
    public static final IRI Organization = createIRI(getNamespace() + "Organization");
    public static final IRI Person = createIRI(getNamespace() + "Person");
    public static final IRI Project = createIRI(getNamespace() + "Project");

    /* Properties */
    public static final IRI age = createIRI(getNamespace() + "age");
    public static final IRI familyName = createIRI(getNamespace() + "familyName");
    public static final IRI givenName = createIRI(getNamespace() + "givenName");
    public static final IRI homepage = createIRI(getNamespace() + "homepage");
    public static final IRI img = createIRI(getNamespace() + "img");
    public static final IRI isPrimaryTopicOf = createIRI(getNamespace() + "isPrimaryTopicOf");
    public static final IRI knows = createIRI(getNamespace() + "knows");
    public static final IRI made = createIRI(getNamespace() + "made");
    public static final IRI maker = createIRI(getNamespace() + "maker");
    public static final IRI member = createIRI(getNamespace() + "member");
    public static final IRI mbox = createIRI(getNamespace() + "mbox");
    public static final IRI name = createIRI(getNamespace() + "name");
    public static final IRI page = createIRI(getNamespace() + "page");
    public static final IRI primaryTopic = createIRI(getNamespace() + "primaryTopic");
    public static final IRI title = createIRI(getNamespace() + "title");
    public static final IRI weblog = createIRI(getNamespace() + "weblog");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private FOAF() {
        // prevent instantiation
    }
}
