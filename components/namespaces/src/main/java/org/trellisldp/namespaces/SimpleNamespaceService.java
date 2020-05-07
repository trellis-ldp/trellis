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
package org.trellisldp.namespaces;

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.trellisldp.api.NamespaceService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.RDFS;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.VCARD;
import org.trellisldp.vocabulary.XSD;

/**
 * A simple, in-memory namespace service.
 *
 * <p>This service will load some standard namespaces/prefixes and read
 * system properties into the namespace maping if they are defined like so:
 * "trellis.ns-myprefix=http://example.com/namespace"
 */
@ApplicationScoped
public class SimpleNamespaceService implements NamespaceService {

    public static final String CONFIG_NAMESPACES_MAPPING = "trellis.namespaces.mapping";

    private final Map<String, String> namespaces = new HashMap<>();

    /**
     * Create a simple, in-memory namespace service.
     */
    public SimpleNamespaceService() {
        namespaces.put("ldp", LDP.getNamespace());
        namespaces.put("acl", ACL.getNamespace());
        namespaces.put("as", AS.getNamespace());
        namespaces.put("dc", DC.getNamespace());
        namespaces.put("rdf", RDF.getNamespace());
        namespaces.put("rdfs", RDFS.getNamespace());
        namespaces.put("skos", SKOS.getNamespace());
        namespaces.put("xsd", XSD.getNamespace());
        namespaces.put("foaf", FOAF.getNamespace());
        namespaces.put("vcard", VCARD.getNamespace());
        getConfig().getOptionalValue(CONFIG_NAMESPACES_MAPPING, String.class).map(SimpleNamespaceService::configToMap)
            .ifPresent(data -> data.forEach(namespaces::put));
    }

    @Override
    public Map<String, String> getNamespaces() {
        return unmodifiableMap(namespaces);
    }

    @Override
    public boolean setPrefix(final String prefix, final String namespace) {
        return true;
    }

    static Map<String, String> configToMap(final String config) {
        return stream(config.split(",")).map(item -> item.split("=")).filter(kv -> kv.length == 2)
            .filter(kv -> !kv[0].trim().isEmpty() && !kv[1].trim().isEmpty())
            .collect(toMap(kv -> kv[0].trim(), kv -> kv[1].trim()));
    }
}
