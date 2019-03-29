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
package org.trellisldp.http.core;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.slf4j.Logger;

/**
 * A class representing an HTTP Slug header.
 *
 * <p>Any trailing hashURI values (#foo) are removed as are any query parameters (?bar).
 * Spaces and slashes are converted to underscores.
 *
 * @author acoburn
 *
 * @see <a href="https://tools.ietf.org/html/rfc5023">RFC 5023</a>
 */
public class Slug {

    private static final Logger LOGGER = getLogger(Slug.class);

    private final String slugValue;

    /**
     * Create a new Slug object.
     * @param value the value of the Slug header.
     */
    public Slug(final String value) {
        this.slugValue = replaceChars(requireNonNull(value, "Must be a non-null value!"));
    }

    /**
     * Get the value of the Slug header.
     * @return the slug value.
     */
    public String getValue() {
        return slugValue;
    }

    /**
     * Get a Slug object from a decoded string value.
     * @param value the raw value of the HTTP header, may be null
     * @return a Slug object with a decoded value or null
     */
    public static Slug valueOf(final String value) {
        return ofNullable(value).flatMap(Slug::decodeSlug).map(Slug::new).orElse(null);
    }

    private static Optional<String> decodeSlug(final String value) {
        try {
            final URLCodec decoder = new URLCodec();
            return of(decoder.decode(value));
        } catch (final DecoderException ex) {
            LOGGER.warn("Error decoding slug value, ignoring header: {}", ex.getMessage());
        }
        return empty();
    }

    private static String replaceChars(final String value) {
        return value.split("#")[0].split("\\?")[0].trim().replaceAll("[\\s/]+", "_");
    }
}
