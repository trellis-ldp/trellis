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
package org.trellisldp.http.domain;

import static java.lang.Float.compare;
import static java.lang.Float.parseFloat;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

/**
 * A class representing an HTTP Want-Digest header
 *
 * @author acoburn
 *
 * @see <a href="https://tools.ietf.org/html/rfc3230">RFC 3230</a>
 */
public class WantDigest {

    private static final Logger LOGGER = getLogger(WantDigest.class);

    private final List<String> algorithms;

    /**
     * Create a Want-Digest header representation
     * @param wantDigest the value of the Want-Digest header
     */
    public WantDigest(final String wantDigest) {
        if (nonNull(wantDigest)) {
            this.algorithms = stream(wantDigest.split(",")).map(String::trim).map(alg -> {
                final String[] parts = alg.split(";", 2);
                if (parts.length == 2) {
                    return new SimpleImmutableEntry<>(parts[0], getValue(parts[1]));
                }
                return new SimpleImmutableEntry<>(parts[0], 1.0f);
            }).sorted((e1, e2) -> compare(e2.getValue(), e1.getValue())).map(Map.Entry::getKey)
            .map(String::toUpperCase).collect(toList());
        } else {
            this.algorithms = emptyList();
        }
    }

    /**
     * Fetch the list of specified algorithms in preference order
     * @return the algorithms
     */
    public List<String> getAlgorithms() {
        return algorithms;
    }

    private float getValue(final String val) {
        if (val.startsWith("q=")) {
            try {
                return parseFloat(val.substring(2));
            } catch (final NumberFormatException ex) {
                LOGGER.warn("Invalid q value for Want-Digest request header ({}), setting to 0.0", val);
            }
        } else {
            LOGGER.warn("Invalid parameter value for Want-Digest request header ({}), setting to 0.0", val);
        }
        return 0.0f;
    }
}
