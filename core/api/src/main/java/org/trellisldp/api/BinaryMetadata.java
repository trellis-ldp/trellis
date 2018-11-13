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

import java.util.Optional;

import org.apache.commons.rdf.api.IRI;

/**
 * The LDP specification divides resources into two categories: RDF resources and
 * non-RDF resources. Non-RDF resources may also have a corresponding RDF description.
 * These interfaces assume it is the case that Non-RDF resources have an RDF description.
 *
 * <p>For those resources that are non-RDF resources (LDP-NR), the base {@link Resource} interface
 * will make a {@link BinaryMetadata} object available. The binary content is not accessed directly
 * through the {@link BinaryMetadata} class, but rather an identifier is returned, which may
 * be resolved by an external system.
 *
 * <p>The {@link BinaryMetadata} class also provides access methods for the MIME Type and size of the
 * resource.
 *
 * @author acoburn
 */
public final class BinaryMetadata {

    private final IRI identifier;
    private final String mimeType;
    private final Long size;

    /**
     * A simple BinaryMetadata object.
     *
     * @param identifier the identifier
     * @param mimeType the mimeType, may be {@code null}
     * @param size the size, may be {@code null}
     */
    private BinaryMetadata(final IRI identifier, final String mimeType, final Long size) {
        this.identifier = requireNonNull(identifier, "identifier may not be null!");
        this.mimeType = mimeType;
        this.size = size;
    }

    /**
     * Retrieve an IRI identifying the location of the binary.
     *
     * @return the resource content
     */
    public IRI getIdentifier() {
        return identifier;
    }

    /**
     * Retrieve the mime-type of the resource, if one was specified.
     *
     * @return the mime-type
     */
    public Optional<String> getMimeType() {
        return ofNullable(mimeType);
    }

    /**
     * Retrieve the size of the binary, if known.
     *
     * @return the binary size
     */
    public Optional<Long> getSize() {
        return ofNullable(size);
    }

    /**
     * Get a mutable builder for a {@link BinaryMetadata}.
     * @param identifier the identifier
     * @return a builder for a {@link BinaryMetadata}
     */
    public static Builder builder(final IRI identifier) {
        return new Builder(identifier);
    }

    /**
     * A mutable buillder for a {@link BinaryMetadata}.
     */
    public static final class Builder {
        private final IRI identifier;
        private String mimeType;
        private Long size;

        /**
         * Create a BinaryMetadata builder with the provided identifier.
         * @param identifier the identifier
         */
        private Builder(final IRI identifier) {
            this.identifier = requireNonNull(identifier, "Identifier cannot be null!");
        }

        /**
         * Set the binary MIME type.
         * @param mimeType the MIME type
         * @return this builder
         */
        public Builder mimeType(final String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        /**
         * Set the binary size.
         * @param size the binary size
         * @return this builder
         */
        public Builder size(final Long size) {
            this.size = size;
            return this;
        }

        /**
         * Build the BinaryMetadata object, transitioning this builder to the built state.
         * @return the built BinaryMetadata
         */
        public BinaryMetadata build() {
            return new BinaryMetadata(identifier, mimeType, size);
        }
    }
}
