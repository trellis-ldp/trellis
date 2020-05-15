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
package org.trellisldp.jdbc;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.ServiceLoader.load;

import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.LDP;

/**
 * Utilities for the DB resource service.
 */
final class DBUtils {

    private static final RDF rdf = RDFFactory.getInstance();

    public static String getObjectValue(final RDFTerm term) {
        if (term instanceof IRI) {
            return ((IRI) term).getIRIString();
        } else if (term instanceof Literal) {
            return ((Literal) term).getLexicalForm();
        }
        return null;
    }

    public static String getObjectLang(final RDFTerm term) {
        if (term instanceof Literal) {
            return ((Literal) term).getLanguageTag().orElse(null);
        }
        return null;
    }

    public static String getObjectDatatype(final RDFTerm term) {
        if (term instanceof Literal) {
            return ((Literal) term).getDatatype().getIRIString();
        }
        return null;
    }

    public static BinaryMetadata getBinaryMetadata(final IRI ixnModel, final String location, final String format) {
        if (LDP.NonRDFSource.equals(ixnModel) && location != null) {
            return BinaryMetadata.builder(rdf.createIRI(location)).mimeType(format).build();
        }
        return null;
    }

    static <T> Optional<T> findFirst(final Class<T> service) {
        final Iterator<T> services = load(service).iterator();
        return services.hasNext() ? of(services.next()) : empty();
    }

    private DBUtils() {
        // prevent instantiation
    }
}
