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
package org.trellisldp.api;

import java.util.Locale;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFSyntax;

/**
 * Additional RDF Syntax definitions.
 */
public final class Syntax {

    private static final RDF rdf = RDFFactory.getInstance();

    public static final RDFSyntax SPARQL_UPDATE = new TrellisSyntax("SPARQL-Update", "SPARQL 1.1 Update",
            "application/sparql-update", ".ru", "http://www.w3.org/TR/sparql11-update/", false);

    public static final RDFSyntax LD_PATCH = new TrellisSyntax("LD-Patch", "Linked Data Patch Format", "text/ldpatch",
            ".ldp", "http://www.w3.org/ns/formats/LD_Patch", false);

    private static class TrellisSyntax implements RDFSyntax {

        private final String name;
        private final String title;
        private final String mediaType;
        private final String fileExtension;
        private final boolean supportsDataset;
        private final IRI iri;

        public TrellisSyntax(final String name, final String title, final String mediaType, final String fileExtension,
                final String iri, final boolean supportsDataset) {
            this.name = name;
            this.title = title;
            this.mediaType = mediaType.toLowerCase(Locale.ROOT);
            this.fileExtension = fileExtension.toLowerCase(Locale.ROOT);
            this.supportsDataset = supportsDataset;
            this.iri = rdf.createIRI(iri);
        }

        @Override
        public String mediaType() {
            return mediaType;
        }

        @Override
        public String fileExtension() {
            return fileExtension;
        }

        @Override
        public boolean supportsDataset() {
            return supportsDataset;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public IRI iri() {
            return iri;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof RDFSyntax)) {
                return false;
            }
            final RDFSyntax other = (RDFSyntax) obj;
            return mediaType.equalsIgnoreCase(other.mediaType());
        }

        @Override
        public int hashCode() {
            return mediaType.hashCode();
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private Syntax() {
        // prevent instantiation
    }
}
