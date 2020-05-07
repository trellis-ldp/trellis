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
package org.trellisldp.api;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.IntStream.rangeClosed;

import java.util.StringJoiner;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;

/**
 * The IdentifierService provides a mechanism for creating new identifiers.
 *
 * @author acoburn
 */
@ApplicationScoped
public class DefaultIdentifierService implements IdentifierService {

    private final String defaultPrefix;

    /**
     * Create a UUID-based IdentifierService.
     */
    public DefaultIdentifierService() {
        this("");
    }

    /**
     * Create a UUID-based IdentifierService with a default prefix value.
     * @param prefix the prefix
     */
    public DefaultIdentifierService(final String prefix) {
        this.defaultPrefix = prefix;
    }

    @Override
    public Supplier<String> getSupplier(final String prefix, final int hierarchy, final int length) {
        requireNonNull(prefix, "The Id prefix may not be null!");
        return () -> getId(prefix, hierarchy, length);
    }

    @Override
    public Supplier<String> getSupplier(final String prefix) {
        return getSupplier(prefix, 0, 0);
    }

    @Override
    public Supplier<String> getSupplier() {
        return getSupplier(defaultPrefix);
    }

    private static String getId(final String prefix, final int hierarchy, final int length) {
        final String id = randomUUID().toString();
        final String nodash = id.replace("-", "");
        final StringJoiner joiner = new StringJoiner("/");
        rangeClosed(0, hierarchy - 1).forEach(x -> joiner.add(nodash.substring(x * length, (x + 1) * length)));
        joiner.add(id);

        return prefix + joiner;
    }
}
