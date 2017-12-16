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
 * <p>The LDP specification divides resources into two categories: RDF resources and
 * non-RDF resources. Non-RDF resources may also have a corresponding RDF description.
 * These interfaces assume it is the case that Non-RDF resources have an RDF description.</p>
 *
 * <p>For those resources that are non-RDF resources (LDP-NR), the base {@link Resource} interface
 * will make a {@link Binary} object available. The binary content is not accessed directly
 * through the {@link Binary} class, but rather an identifier is returned, which may
 * be resolved by an external system.</p>
 *
 * <p>The {@link Binary} class also provides access methods for the MIME Type and size of the
 * resource.</p>
 *
 * @author acoburn
 */
public class Binary {

    private final IRI identifier;
    private final String mimeType;
    private final Long size;
    private final Instant modified;

    /**
     * A simple Binary object
     * @param identifier the identifier
     * @param modified the modified date
     * @param mimeType the mimeType
     * @param size the size
     */
    public Binary(final IRI identifier, final Instant modified, final String mimeType, final Long size) {
        requireNonNull(identifier);
        requireNonNull(modified);

        this.identifier = identifier;
        this.modified = modified;
        this.mimeType = mimeType;
        this.size = size;
    }

    /**
     * Retrieve an IRI identifying the location of the binary
     * @return the resource content
     */
    public IRI getIdentifier() {
        return identifier;
    }

    /**
     * Retrieve the mime-type of the resource, if one was specified
     * @return the mime-type
     */
    public Optional<String> getMimeType() {
        return ofNullable(mimeType);
    }

    /**
     * Retrieve the size of the binary, if known
     * @return the binary size
     */
    public Optional<Long> getSize() {
        return ofNullable(size);
    }

    /**
     * Retrieve the last-modified date of the binary
     * @return the last-modified date
     */
    public Instant getModified() {
        return modified;
    }
}
