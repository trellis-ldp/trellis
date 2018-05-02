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
 * RDF Terms from the trellis vocabulary.
 *
 * @see <a href="https://www.trellisldp.org/ns/trellis.html">Trellis vocabulary</a>
 *
 * @author acoburn
 */
public final class Trellis {

    /* Namespace */
    public static final String NS = "http://www.trellisldp.org/ns/trellis#";

    /* Classes */
    public static final IRI ConstraintViolation = createIRI(NS + "ConstraintViolation");
    public static final IRI DeletedResource = createIRI(NS + "DeletedResource");
    public static final IRI BinaryUploadService = createIRI(NS + "BinaryUploadService");

    /* Properties */
    public static final IRI multipartUploadService = createIRI(NS + "multipartUploadService");

    /* Named Individuals */
    public static final IRI AdministratorAgent = createIRI(NS + "AdministratorAgent");
    public static final IRI AnonymousAgent = createIRI(NS + "AnonymousAgent");
    public static final IRI InvalidType = createIRI(NS + "InvalidType");
    public static final IRI InvalidRange = createIRI(NS + "InvalidRange");
    public static final IRI InvalidCardinality = createIRI(NS + "InvalidCardinality");
    public static final IRI InvalidProperty = createIRI(NS + "InvalidProperty");
    public static final IRI PreferAccessControl = createIRI(NS + "PreferAccessControl");
    public static final IRI PreferAudit = createIRI(NS + "PreferAudit");
    public static final IRI PreferServerManaged = createIRI(NS + "PreferServerManaged");
    public static final IRI PreferUserManaged = createIRI(NS + "PreferUserManaged");
    public static final IRI ReadOnlyResource = createIRI(NS + "ReadOnlyResource");
    public static final IRI UnsupportedInteractionModel = createIRI(NS + "UnsupportedInteractionModel");

    private Trellis() {
        // prevent instantiation
    }
}
