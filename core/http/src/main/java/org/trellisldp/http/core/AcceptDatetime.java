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

import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.DateTimeException;
import java.time.Instant;

import org.slf4j.Logger;

/**
 * A class representing an HTTP Accept-Datetime header.
 *
 * @author acoburn
 */
public class AcceptDatetime {

    private static final Logger LOGGER = getLogger(AcceptDatetime.class);

    private final Instant datetime;

    /**
     * Create an Accept-Datetime header object.
     *
     * @param datetime the date time in RFC 1123 format
     */
    public AcceptDatetime(final Instant datetime) {
        this.datetime = datetime;
    }

    /**
     * Retrieve the corresponding instant.
     *
     * @return the instant
     */
    public Instant getInstant() {
        return datetime;
    }

    @Override
    public String toString() {
        return datetime.toString();
    }

    /**
     * Create an Accept-Datetime header object from a string.
     *
     * @param value the header value
     * @return an AcceptDatetime object or null if the value is not parseable
     */
    public static AcceptDatetime valueOf(final String value) {
        if (value != null) {
            final Instant time = parseDatetime(value);
            if (time != null) {
                return new AcceptDatetime(time);
            }
        }
        return null;
    }

    private static Instant parseDatetime(final String datetime) {
        try {
            return parse(datetime.trim(), RFC_1123_DATE_TIME).toInstant();
        } catch (final DateTimeException ex) {
            LOGGER.warn("Invalid date supplied ({}): {}", datetime, ex.getMessage());
        }
        return null;
    }
}
