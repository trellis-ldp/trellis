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
 * RDF Terms from the W3C ACL Vocabulary.
 *
 * @see <a href="https://www.w3.org/wiki/WebAccessControl">WebAccessControl Wiki</a>
 *
 * @author acoburn
 */
public final class ACL {

    /* Namespace */
    public static final String NS = "http://www.w3.org/ns/auth/acl#";

    /* Classes */
    public static final IRI Access = createIRI(NS + "Access");
    public static final IRI Append = createIRI(NS + "Append");
    public static final IRI AuthenticatedAgent = createIRI(NS + "AuthenticatedAgent");
    public static final IRI Authorization = createIRI(NS + "Authorization");
    public static final IRI Control = createIRI(NS + "Control");
    public static final IRI Read = createIRI(NS + "Read");
    public static final IRI Origin = createIRI(NS + "Origin");
    public static final IRI Write = createIRI(NS + "Write");

    /* Properties */
    public static final IRI accessControl = createIRI(NS + "accessControl");
    public static final IRI accessTo = createIRI(NS + "accessTo");
    public static final IRI accessToClass = createIRI(NS + "accessToClass");
    public static final IRI agent = createIRI(NS + "agent");
    public static final IRI agentClass = createIRI(NS + "agentClass");
    public static final IRI agentGroup = createIRI(NS + "agentGroup");
    public static final IRI default_ = createIRI(NS + "default");
    public static final IRI defaultForNew_ = createIRI(NS + "defaultForNew_");
    public static final IRI delegates = createIRI(NS + "delegates");
    public static final IRI mode = createIRI(NS + "mode");
    public static final IRI origin = createIRI(NS + "origin");
    public static final IRI owner = createIRI(NS + "owner");

    private ACL() {
        // prevent instantiation
    }
}
