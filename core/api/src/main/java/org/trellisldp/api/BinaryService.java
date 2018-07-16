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

import static java.util.Collections.emptyMap;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.rdf.api.IRI;

/**
 * The BinaryService provides methods for retrieving, modifying and checking
 * the validity of binary content.
 *
 * @author acoburn
 */
public interface BinaryService {

    /**
     * Get the content of the binary object.
     *
     * @param identifier an identifier used for locating the binary object
     * @param from the starting point of a range request
     * @param to the ending point of a range request
     * @return the content
     */
    Optional<InputStream> getContent(IRI identifier, Integer from, Integer to);

    /**
     * Get the content of the binary object.
     *
     * @param identifier an identifier used for locating the binary object
     * @return the content
     */
    Optional<InputStream> getContent(IRI identifier);

    /**
     * Test whether a binary object exists at the given URI.
     *
     * @param identifier the binary object identifier
     * @return whether the binary object exists
     */
    Boolean exists(IRI identifier);

    /**
     * Set the content for a binary object.
     *
     * @param identifier the binary object identifier
     * @param stream the content
     */
    default void setContent(IRI identifier, InputStream stream) {
        setContent(identifier, stream, emptyMap());
    }

    /**
     * Set the content for a binary object.
     *
     * @param identifier the binary object identifier
     * @param stream the content
     * @param metadata any user metadata
     */
    void setContent(IRI identifier, InputStream stream, Map<String, String> metadata);

    /**
     * Purge the content from its corresponding datastore.
     *
     * @param identifier the binary object identifier
     */
    void purgeContent(IRI identifier);

    /**
     * Calculate the digest for a binary object.
     *
     * <p>Note: as per RFC 3230, the digest value is calculated over the entire resource,
     * not just the HTTP payload.
     *
     * @param identifier the identifier
     * @param algorithm the algorithm
     * @return the digest
     */
    default Optional<String> calculateDigest(IRI identifier, String algorithm) {
        return getContent(identifier).flatMap(stream -> digest(algorithm, stream));
    }

    /**
     * Get a list of supported algorithms.
     *
     * @return the supported digest algorithms
     */
    Set<String> supportedAlgorithms();

    /**
     * Get the digest for an input stream.
     *
     * <p>Note: the digest likely uses the base64 encoding, but the specific encoding is defined
     * for each algorithm at https://www.iana.org/assignments/http-dig-alg/http-dig-alg.xhtml
     *
     * @param algorithm the algorithm to use
     * @param stream the input stream
     * @return a string representation of the digest
     */
    Optional<String> digest(String algorithm, InputStream stream);

    /**
     * Get a new identifier.
     *
     * @return a new identifier
     */
    String generateIdentifier();
}
