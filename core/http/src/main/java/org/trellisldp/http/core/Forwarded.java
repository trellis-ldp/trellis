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
import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.slf4j.Logger;

/**
 * A class representing an HTTP Forwarded header.
 *
 * @author acoburn
 */
public class Forwarded {

    private static final Logger LOGGER = getLogger(Forwarded.class);

    private static final String BY = "by";
    private static final String FOR = "for";
    private static final String HOST = "host";
    private static final String PROTO = "proto";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    private final String forwardedBy;
    private final String forwardedFor;
    private final String forwardedHost;
    private final String forwardedProto;
    private final String hostname;
    private final String port;

    /**
     * Create a Forwarded object.
     * @param forwardedBy the interface where the request came in to the proxy server
     * @param forwardedFor the client that initiated the request
     * @param forwardedHost the host request header field, as received by the proxy
     * @param forwardedProto the protocol used to make the request
     */
    public Forwarded(final String forwardedBy, final String forwardedFor, final String forwardedHost,
            final String forwardedProto) {
        this.forwardedBy = forwardedBy;
        this.forwardedFor = forwardedFor;
        this.forwardedHost = forwardedHost;
        this.forwardedProto = checkProto(forwardedProto);
        if (forwardedHost != null) {
            final String[] parts = forwardedHost.split(":", 2);
            this.hostname = parts[0];
            this.port = parts.length == 2 && !parts[1].isEmpty() ? parts[1] : "-1";
        } else {
            this.hostname = forwardedHost;
            this.port = forwardedHost;
        }
    }

    /** @return the by parameter, if present */
    public Optional<String> getBy() {
        return ofNullable(forwardedBy);
    }

    /** @return the for parameter, if present */
    public Optional<String> getFor() {
        return ofNullable(forwardedFor);
    }

    /** @return the host parameter, if present */
    public Optional<String> getHost() {
        return ofNullable(forwardedHost);
    }

    /** @return the proto parameter, if present */
    public Optional<String> getProto() {
        return ofNullable(forwardedProto);
    }

    /** @return the port, if present */
    public OptionalInt getPort() {
        if (port != null) {
            try {
                return OptionalInt.of(Integer.parseInt(port));
            } catch (final NumberFormatException ex) {
                LOGGER.warn("Could not parse port number: {}", ex.getMessage());
            }
        }
        return OptionalInt.empty();
    }

    /** @return the hostname, if present */
    public Optional<String> getHostname() {
        return ofNullable(hostname);
    }

    /**
     * Get a Forwarded object from a header value.
     *
     * @param value the header value
     * @return the Forwarded object or null, if not present;
     */
    public static Forwarded valueOf(final String value) {
        if (value != null) {
            final Map<String, String> data = intoMap(value);
            return new Forwarded(data.get(BY), data.get(FOR), data.get(HOST), data.get(PROTO));
        }
        return null;
    }

    static String checkProto(final String proto) {
        if (HTTP.equals(proto) || HTTPS.equals(proto)) {
            return proto;
        }
        return null;
    }

    static Map<String, String> intoMap(final String data) {
        return stream(data.split(";")).map(item -> item.split("=")).filter(kv -> kv.length == 2)
            .filter(kv -> !kv[0].trim().isEmpty() && !kv[1].trim().isEmpty())
            .collect(toMap(kv -> kv[0].trim().toLowerCase(ENGLISH), kv -> stripQuotes(kv[1].trim())));
    }

    static String stripQuotes(final String value) {
        final String trimmed = value.startsWith("\"") ? value.substring(1) : value;
        return trimmed.endsWith("\"") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
