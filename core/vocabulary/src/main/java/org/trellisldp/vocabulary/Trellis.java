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
 * RDF Terms from the trellis vocabulary.
 *
 * @see <a href="https://www.trellisldp.org/ns/trellis.html">Trellis vocabulary</a>
 *
 * @author acoburn
 */
public final class Trellis {

    /* Namespace */
    private static final String URI = "http://www.trellisldp.org/ns/trellis#";

    /* Classes */
    public static final IRI ConstraintViolation = createIRI(getNamespace() + "ConstraintViolation");
    public static final IRI DeletedResource = createIRI(getNamespace() + "DeletedResource");

    /* Named Individuals */
    public static final IRI AdministratorAgent = createIRI(getNamespace() + "AdministratorAgent");
    public static final IRI AnonymousAgent = createIRI(getNamespace() + "AnonymousAgent");
    public static final IRI InvalidType = createIRI(getNamespace() + "InvalidType");
    public static final IRI InvalidRange = createIRI(getNamespace() + "InvalidRange");
    public static final IRI InvalidCardinality = createIRI(getNamespace() + "InvalidCardinality");
    public static final IRI InvalidProperty = createIRI(getNamespace() + "InvalidProperty");
    public static final IRI PreferAccessControl = createIRI(getNamespace() + "PreferAccessControl");
    public static final IRI PreferAudit = createIRI(getNamespace() + "PreferAudit");
    public static final IRI PreferServerManaged = createIRI(getNamespace() + "PreferServerManaged");
    public static final IRI PreferUserManaged = createIRI(getNamespace() + "PreferUserManaged");
    public static final IRI SerializationAbsolute = createIRI(getNamespace() + "SerializationAbsolute");
    public static final IRI SerializationRelative = createIRI(getNamespace() + "SerializationRelative");
    public static final IRI UnsupportedInteractionModel = createIRI(getNamespace() + "UnsupportedInteractionModel");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private Trellis() {
        // prevent instantiation
    }
}
