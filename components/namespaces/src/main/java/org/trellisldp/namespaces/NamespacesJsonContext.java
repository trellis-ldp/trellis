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
package org.trellisldp.namespaces;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.NamespaceService;

/**
 * Create a namespace service based on reading/writing the namespaces to a central JSON file.
 *
 * @author acoburn
 */
public class NamespacesJsonContext implements NamespaceService {

    /** The configuration key controlling the path to a JSON-formatted namespace file. **/
    public static final String NAMESPACES_PATH = "trellis.namespaces.path";

    private static final Logger LOGGER = getLogger(NamespacesJsonContext.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String filePath;
    private final Map<String, String> data;
    private final Map<String, String> dataRev = new ConcurrentHashMap<>();

    /**
     * Create a JSON-based Namespace service.
     */
    @Inject
    public NamespacesJsonContext() {
        this(ConfigurationProvider.getConfiguration().get(NAMESPACES_PATH));
    }

    /**
     * Create a JSON-based Namespace service.
     * @param path the path to the JSON file
     */
    public NamespacesJsonContext(final String path) {
        requireNonNull(path, "Namespace path may not be empty!");

        this.filePath = path;
        this.data = read(path);
        init();
    }

    @Override
    public Map<String, String> getNamespaces() {
        return unmodifiableMap(data);
    }

    @Override
    public Optional<String> getNamespace(final String prefix) {
        return ofNullable(data.get(prefix));
    }

    @Override
    public Optional<String> getPrefix(final String namespace) {
        return ofNullable(dataRev.get(namespace));
    }

    @Override
    public Boolean setPrefix(final String prefix, final String namespace) {
        requireNonNull(prefix, "The prefix value may not be null!");
        requireNonNull(namespace, "The namespce value may not be null!");

        if (dataRev.containsKey(namespace)) {
            LOGGER.warn("A prefix already exists for the namespace: {}", namespace);
            return false;
        }

        data.put(prefix, namespace);
        dataRev.put(namespace, prefix);
        write(filePath, data);
        return true;
    }

    private void init() {
        if (data.isEmpty()) {
            data.putAll(read(getClass().getResource("/defaultNamespaces.json").getPath()));
            write(filePath, data);
        }
        data.entrySet().forEach(e -> dataRev.put(e.getValue(), e.getKey()));
    }

    private static Map<String, String> read(final String filePath) {
        final File file = new File(filePath);
        final Map<String, String> namespaces = new ConcurrentHashMap<>();
        if (file.exists()) {
            try {
                of(MAPPER.readTree(new File(filePath))).filter(JsonNode::isObject).ifPresent(json ->
                    json.fields().forEachRemaining(node -> {
                        if (node.getValue().isTextual()) {
                            namespaces.put(node.getKey(), node.getValue().textValue());
                        }
                    }));
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return namespaces;
    }

    private static void write(final String filePath, final Map<String, String> data) {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), data);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
