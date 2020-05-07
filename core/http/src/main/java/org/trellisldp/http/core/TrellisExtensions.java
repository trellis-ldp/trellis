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
package org.trellisldp.http.core;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_EXTENSION_GRAPHS;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;

import java.util.Map;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.eclipse.microprofile.config.Config;
import org.trellisldp.api.RDFFactory;

public final class TrellisExtensions {

    private static final RDF rdf = RDFFactory.getInstance();

    /**
     * Build a map suitable for extension graphs from a config string.
     * @param extensions the config value
     * @return the formatted map
     */
    public static Map<String, IRI> buildExtensionMap(final String extensions) {
        return stream(extensions.split(",")).map(item -> item.split("=")).filter(kv -> kv.length == 2)
            .filter(kv -> !kv[0].trim().isEmpty() && !kv[1].trim().isEmpty())
            .collect(toMap(kv -> kv[0].trim(), kv -> rdf.createIRI(kv[1].trim())));
    }

    /**
     * Build an extension map from configuration.
     * @param config the configuration
     * @return the formatted map
     */
    public static Map<String, IRI> buildExtensionMapFromConfig(final Config config) {
        return config.getOptionalValue(CONFIG_HTTP_EXTENSION_GRAPHS, String.class)
            .map(TrellisExtensions::buildExtensionMap).orElseGet(() -> singletonMap("acl", PreferAccessControl));
    }

    private TrellisExtensions() {
        // prevent instantiation
    }
}
