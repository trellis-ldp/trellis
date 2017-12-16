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
 * RDF Terms from the W3C ACL Vocabulary
 *
 * @see <a href="https://www.w3.org/wiki/WebAccessControl">WebAccessControl Wiki</a>
 *
 * @author acoburn
 */
public final class ACL extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.w3.org/ns/auth/acl#";

    /* Classes */
    public static final IRI Access = createIRI(URI + "Access");
    public static final IRI Append = createIRI(URI + "Append");
    public static final IRI AuthenticatedAgent = createIRI(URI + "AuthenticatedAgent");
    public static final IRI Authorization = createIRI(URI + "Authorization");
    public static final IRI Control = createIRI(URI + "Control");
    public static final IRI Read = createIRI(URI + "Read");
    public static final IRI Origin = createIRI(URI + "Origin");
    public static final IRI Write = createIRI(URI + "Write");

    /* Properties */
    public static final IRI accessControl = createIRI(URI + "accessControl");
    public static final IRI accessTo = createIRI(URI + "accessTo");
    public static final IRI accessToClass = createIRI(URI + "accessToClass");
    public static final IRI agent = createIRI(URI + "agent");
    public static final IRI agentClass = createIRI(URI + "agentClass");
    public static final IRI agentGroup = createIRI(URI + "agentGroup");
    public static final IRI default_ = createIRI(URI + "default");
    public static final IRI delegates = createIRI(URI + "delegates");
    public static final IRI mode = createIRI(URI + "mode");
    public static final IRI owner = createIRI(URI + "owner");

    private ACL() {
        super();
    }
}
