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
package org.trellisldp.io.impl;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;
import static org.apache.jena.riot.RDFFormat.JSONLD_COMPACT_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD_EXPAND_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD_FLATTEN_FLAT;
import static org.trellisldp.vocabulary.JSONLD.compacted;
import static org.trellisldp.vocabulary.JSONLD.compacted_flattened;
import static org.trellisldp.vocabulary.JSONLD.expanded;
import static org.trellisldp.vocabulary.JSONLD.expanded_flattened;
import static org.trellisldp.vocabulary.JSONLD.flattened;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.jena.riot.RDFFormat;

/**
 * Utilities used with the Jena-IO package
 * @author acoburn
 */
public final class IOUtils {

    private static final Map<IRI, RDFFormat> JSONLD_FORMATS = unmodifiableMap(Stream.of(
                new SimpleEntry<>(compacted, JSONLD_COMPACT_FLAT),
                new SimpleEntry<>(flattened, JSONLD_FLATTEN_FLAT),
                new SimpleEntry<>(expanded, JSONLD_EXPAND_FLAT),
                new SimpleEntry<>(compacted_flattened, JSONLD_FLATTEN_FLAT),
                new SimpleEntry<>(expanded_flattened, JSONLD_FLATTEN_FLAT))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

    /**
     * This will combine multiple JSON-LD profiles into a single profile. For example,
     * jsonld:compacted + jsonld:flattened = jsonld:compacted_flattened
     * The default (i.e. no arguments) is jsonld:expanded
     * Multiple, conflicting profiles (e.g. jsonld:compacted + jsonld:expanded) will result
     * in a "last profile wins" situation. Profile IRIs that are not part of the JSON-LD
     * vocabulary are ignored.
     */
    private static IRI mergeProfiles(final IRI... profiles) {
        Boolean isExpanded = true;
        Boolean isFlattened = false;

        for (final IRI uri : profiles) {
            if (compacted_flattened.equals(uri) || expanded_flattened.equals(uri)) {
                return uri;
            }

            if (flattened.equals(uri)) {
                isFlattened = true;
            } else if (compacted.equals(uri)) {
                isExpanded = false;
            } else if (expanded.equals(uri)) {
                isExpanded = true;
            }
        }
        if (isFlattened) {
            return isExpanded ? expanded_flattened : compacted_flattened;
        }
        return isExpanded ? expanded : compacted;
    }

    /**
     * Get the JSON-LD profile for the given profile(s)
     * @param profiles the profiles
     * @return the RDFFormat
     */
    public static RDFFormat getJsonLdProfile(final IRI... profiles) {
        return of(mergeProfiles(profiles)).map(JSONLD_FORMATS::get).orElse(JSONLD_EXPAND_FLAT);
    }

    private IOUtils() {
        // prevent instantiation
    }
}
