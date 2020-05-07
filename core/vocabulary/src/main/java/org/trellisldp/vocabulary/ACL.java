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
 * RDF Terms from the W3C ACL Vocabulary.
 *
 * @see <a href="https://www.w3.org/wiki/WebAccessControl">WebAccessControl Wiki</a>
 *
 * @author acoburn
 */
public final class ACL {

    /* Namespace */
    private static final String URI = "http://www.w3.org/ns/auth/acl#";

    /* Classes */
    public static final IRI Access = createIRI(getNamespace() + "Access");
    public static final IRI Append = createIRI(getNamespace() + "Append");
    public static final IRI AuthenticatedAgent = createIRI(getNamespace() + "AuthenticatedAgent");
    public static final IRI Authorization = createIRI(getNamespace() + "Authorization");
    public static final IRI Control = createIRI(getNamespace() + "Control");
    public static final IRI Read = createIRI(getNamespace() + "Read");
    public static final IRI Origin = createIRI(getNamespace() + "Origin");
    public static final IRI Write = createIRI(getNamespace() + "Write");

    /* Properties */
    public static final IRI accessControl = createIRI(getNamespace() + "accessControl");
    public static final IRI accessTo = createIRI(getNamespace() + "accessTo");
    public static final IRI accessToClass = createIRI(getNamespace() + "accessToClass");
    public static final IRI agent = createIRI(getNamespace() + "agent");
    public static final IRI agentClass = createIRI(getNamespace() + "agentClass");
    public static final IRI agentGroup = createIRI(getNamespace() + "agentGroup");
    public static final IRI default_ = createIRI(getNamespace() + "default");
    public static final IRI delegates = createIRI(getNamespace() + "delegates");
    public static final IRI mode = createIRI(getNamespace() + "mode");
    public static final IRI origin = createIRI(getNamespace() + "origin");
    public static final IRI owner = createIRI(getNamespace() + "owner");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private ACL() {
        // prevent instantiation
    }
}
