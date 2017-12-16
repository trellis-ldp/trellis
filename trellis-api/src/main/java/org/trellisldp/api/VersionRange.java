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
package org.trellisldp.api;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

/**
 * This class provides a mechanism for representing date ranges,
 * which are used by Memento resources and included in either TimeMap
 * or Link headers.
 *
 * @see <a href="http://tools.ietf.org/html/rfc7089">IETF RFC 7089</a>
 *
 * @author acoburn
 */
public class VersionRange {

    private final Instant from;
    private final Instant until;

    /**
     * Create a VersionRange object.
     * @param from the starting time
     * @param until the ending time
     */
    public VersionRange(final Instant from, final Instant until) {
        requireNonNull(from, "from may not be null!");
        requireNonNull(until, "until may not be null!");

        this.from = from;
        this.until = until;
    }

    /**
     * Get the datetime corresponding to when the temporal interval covered by this Memento begins
     * @return the from value for this Memento
     */
    public Instant getFrom() {
        return from;
    }

    /**
     * Get the datetime corresponding to when the temporal interval covered by this Memento ends
     * @return the until value for this Memento
     */
    public Instant getUntil() {
        return until;
    }
}
