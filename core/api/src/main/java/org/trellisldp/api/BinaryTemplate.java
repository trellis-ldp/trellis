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
import static java.util.Optional.ofNullable;

import java.time.Instant;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;

/**
 * A template for generating a {@link Binary} in a persistence layer.
 */
public class BinaryTemplate {

    private final IRI identifier;
    private final String mimeType;
    private final Long size;
    private final Instant modified;

    /**
     * Create a new BinaryTemplate object.
     * @param identifier the identifier
     * @param mimeType the mime type, may be {@code null}
     * @param size the size, may be {@code null}
     */
    public BinaryTemplate(final IRI identifier, final String mimeType, final Long size) {
        this(identifier, mimeType, size, null);
    }

    /**
     * Create a new BinaryTemplate object.
     *
     * @apiNote the modified parameter can be used with an existing binary resource if an earlier modification
     *          date it to be used. This may be useful in cases where a LDP-NR description is being updated
     *          and the corresponding binary is not changed.
     * @param identifier the identifier
     * @param mimeType the mime type, may be {@code null}
     * @param size the size, may be {@code null}
     * @param modified a provisional modification date, may be {@code null}
     */
    public BinaryTemplate(final IRI identifier, final String mimeType, final Long size, final Instant modified) {
        requireNonNull(identifier, "Identifier may not be null!");
        this.identifier = identifier;
        this.mimeType = mimeType;
        this.size = size;
        this.modified = modified;
    }

    /**
     * Get the identifier for the binary.
     * @return the identifier
     */
    public IRI getIdentifier() {
        return identifier;
    }

    /**
     * Get the size of the binary in bytes, if available.
     * @return the size in bytes of the binary.
     */
    public Optional<Long> getSize() {
        return ofNullable(size);
    }

    /**
     * Get the MIME Type of the binary, if available.
     * @return a MIME Type of the binary
     */
    public Optional<String> getMimeType() {
        return ofNullable(mimeType);
    }

    /**
     * Get the modification date of the binary, if available.
     * @return the modification date of the binary
     */
    public Optional<Instant> getModified() {
        return ofNullable(modified);
    }

    /**
     * Create a Binary template from existing binary metadata.
     *
     * @param binary the binary metadata
     * @return a new binary template containing the original Binary modification date
     */
    public static BinaryTemplate fromBinary(final Binary binary) {
        return new BinaryTemplate(binary.getIdentifier(), binary.getMimeType().orElse(null),
                binary.getSize().orElse(null), binary.getModified());
    }
}
