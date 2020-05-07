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

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Map;
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
 * <p>The {@link BinaryMetadata} class also provides access methods for the MIME Type of the resource.
 *
 * @author acoburn
 */
public final class BinaryMetadata {

    private final IRI identifier;
    private final String mimeType;
    private final Map<String, List<String>> hints;

    /**
     * A simple BinaryMetadata object.
     *
     * @param identifier the identifier
     * @param mimeType the mimeType, may be {@code null}
     * @param hints hints for persistence, may not be {@code null}
     */
    private BinaryMetadata(final IRI identifier, final String mimeType, final Map<String, List<String>> hints) {
        this.identifier = requireNonNull(identifier, "Identifier may not be null!");
        this.mimeType = mimeType;
        this.hints = requireNonNull(hints, "Hints may not be null!");
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
     * Retrieve any hints for persistence.
     *
     * @return the hints
     */
    public Map<String, List<String>> getHints() {
        return hints;
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
        private Map<String, List<String>> hints;

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
         * Set the hints for persistence.
         * @param hints the hints, may not be {@code null}
         * @return this builder
         */
        public Builder hints(final Map<String, List<String>> hints) {
            this.hints = requireNonNull(hints, "Hints cannot be null!");
            return this;
        }

        /**
         * Build the BinaryMetadata object.
         * @return the built BinaryMetadata
         */
        public BinaryMetadata build() {
            return new BinaryMetadata(identifier, mimeType, hints == null ? emptyMap() : hints);
        }
    }
}
