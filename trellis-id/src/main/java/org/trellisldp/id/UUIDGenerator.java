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
package org.trellisldp.id;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.IntStream.rangeClosed;

import java.util.StringJoiner;
import java.util.function.Supplier;

import org.trellisldp.api.IdentifierService;

/**
 * The IdentifierService provides a mechanism for creating new identifiers.
 *
 * @author acoburn
 */
public class UUIDGenerator implements IdentifierService {

    @Override
    public Supplier<String> getSupplier(final String prefix, final Integer hierarchy, final Integer length) {

        requireNonNull(prefix, "The Id prefix may not be null!");
        requireNonNull(hierarchy, "The hierarchy value may not be null!");
        requireNonNull(length, "The length value may not be null!");
        return () -> getId(prefix, hierarchy, length);
    }

    @Override
    public Supplier<String> getSupplier(final String prefix) {
        return getSupplier(prefix, 0, 0);
    }

    @Override
    public Supplier<String> getSupplier() {
        return getSupplier("");
    }

    private static String getId(final String prefix, final Integer hierarchy, final Integer length) {
        final String id = randomUUID().toString();
        final String nodash = id.replaceAll("-", "");
        final StringJoiner joiner = new StringJoiner("/");
        rangeClosed(0, hierarchy - 1).forEach(x -> joiner.add(nodash.substring(x * length, (x + 1) * length)));
        joiner.add(id);

        return prefix + joiner;
    }
}
