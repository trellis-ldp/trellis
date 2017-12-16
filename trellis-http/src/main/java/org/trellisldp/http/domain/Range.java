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

import static java.lang.Integer.parseInt;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import org.slf4j.Logger;

/**
 * A class representing an HTTP Range header
 *
 * @author acoburn
 */
public class Range {

    private static final Logger LOGGER = getLogger(Range.class);

    private final Integer from;

    private final Integer to;

    /**
     * Create a Range object
     * @param from the from value
     * @param to the to value
     */
    public Range(final Integer from, final Integer to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Get the from value
     * @return the byte offset
     */
    public Integer getFrom() {
        return from;
    }

    /**
     * Get the to value
     * @return the byte end
     */
    public Integer getTo() {
        return to;
    }

    /**
     * Get a Range object from a header value
     * @param value the header value
     * @return the Range object or null if the value is not parseable
     */
    public static Range valueOf(final String value) {
        final Optional<Integer[]> vals = parse(value);
        if (vals.isPresent()) {
            return new Range(vals.get()[0], vals.get()[1]);
        }
        return null;
    }

    private static Optional<Integer[]> parse(final String range) {
        if (nonNull(range) && range.startsWith("bytes=")) {
            final String[] parts = range.substring("bytes=".length()).split("-");
            if (parts.length == 2) {
                try {
                    final Integer[] ints = new Integer[2];
                    ints[0] = parseInt(parts[0]);
                    ints[1] = parseInt(parts[1]);
                    if (ints[0] >= 0 && ints[1] > ints[0]) {
                        return of(ints);
                    }
                    LOGGER.warn("Ignoring range request: {}", range);
                } catch (final NumberFormatException ex) {
                    LOGGER.warn("Invalid Range request ({}): {}", range, ex.getMessage());
                }
            } else {
                LOGGER.warn("Only simple range requests are supported! {}", range);
            }
        }
        return empty();
    }
}
