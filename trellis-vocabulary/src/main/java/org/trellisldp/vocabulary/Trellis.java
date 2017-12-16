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
 * RDF Terms from the trellis vocabulary
 *
 * @see <a href="http://www.trellisldp.org/ns/trellis">Trellis vocabulary</a>
 *
 * @author acoburn
 */
public final class Trellis extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.trellisldp.org/ns/trellis#";

    /* Classes */
    public static final IRI ConstraintViolation = createIRI(URI + "ConstraintViolation");
    public static final IRI DeletedResource = createIRI(URI + "DeletedResource");
    public static final IRI BinaryUploadService = createIRI(URI + "BinaryUploadService");

    /* Properties */
    public static final IRI multipartUploadService = createIRI(URI + "multipartUploadService");

    /* Named Individuals */
    public static final IRI AnonymousUser = createIRI(URI + "AnonymousUser");
    public static final IRI InvalidType = createIRI(URI + "InvalidType");
    public static final IRI InvalidRange = createIRI(URI + "InvalidRange");
    public static final IRI InvalidCardinality = createIRI(URI + "InvalidCardinality");
    public static final IRI InvalidProperty = createIRI(URI + "InvalidProperty");
    public static final IRI PreferAccessControl = createIRI(URI + "PreferAccessControl");
    public static final IRI PreferAudit = createIRI(URI + "PreferAudit");
    public static final IRI PreferServerManaged = createIRI(URI + "PreferServerManaged");
    public static final IRI PreferUserManaged = createIRI(URI + "PreferUserManaged");
    public static final IRI RepositoryAdministrator = createIRI(URI + "RepositoryAdministrator");

    private Trellis() {
        super();
    }
}
