/*
 * Copyright (c) Aaron Coburn and individual contributors
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
package org.trellisldp.common;

import static java.util.Objects.requireNonNull;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.common.HttpConstants.*;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
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

    private static final String UNWISE_CHARS = getConfig()
        .getOptionalValue(CONFIG_HTTP_PATH_CHARACTERS_ENCODE, String.class).orElse(UNWISE_CHARACTERS);
    private static final String INVALID_CHARS = getConfig()
        .getOptionalValue(CONFIG_HTTP_PATH_CHARACTERS_DISALLOW, String.class).orElse(INVALID_CHARACTERS);
    private static final Logger LOGGER = getLogger(Slug.class);
    private static final URLCodec DECODER = new URLCodec();

    private final String slugValue;

    /**
     * Create a new Slug object.
     * @param value the value of the Slug header.
     */
    public Slug(final String value) {
        this.slugValue = cleanSlugString(requireNonNull(value, "Must be a non-null value!"));
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
        if (value != null) {
            final String decoded = decodeSlug(value);
            if (decoded != null) {
                return new Slug(decoded);
            }
        }
        return null;
    }

    private static String decodeSlug(final String value) {
        try {
            return DECODER.decode(value);
        } catch (final DecoderException ex) {
            LOGGER.debug("Error decoding slug value, ignoring header: {}", ex.getMessage());
        }
        return null;
    }

    private static String cleanSlugString(final String value) {
        // Remove any fragment URIs, query parameters and whitespace
        final String base = StringUtils.deleteWhitespace(value.split("#")[0].split("\\?")[0]);
        // Remove any "unwise" characters plus '/'
        return StringUtils.replaceChars(base, UNWISE_CHARS + INVALID_CHARS + "/", "");
    }
}
