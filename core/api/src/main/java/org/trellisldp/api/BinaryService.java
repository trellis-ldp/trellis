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
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
     * @return the new completion stage with the binary content
     */
    CompletableFuture<InputStream> getContent(IRI identifier, Integer from, Integer to);

    /**
     * Get the content of the binary object.
     *
     * @param identifier an identifier used for locating the binary object
     * @return the new completion stage with the binary content
     */
    CompletableFuture<InputStream> getContent(IRI identifier);

    /**
     * Set the content for a binary object.
     *
     * @param identifier the binary object identifier
     * @param stream the content
     * @return the new completion stage
     */
    default CompletableFuture<Void> setContent(IRI identifier, InputStream stream) {
        return setContent(identifier, stream, emptyMap());
    }

    /**
     * Set the content for a binary object.
     *
     * @param identifier the binary object identifier
     * @param stream the content
     * @param metadata any user metadata
     * @return a new completion stage that, when the stage completes normally, indicates that the binary
     * data were successfully stored in the corresponding persistence layer. In the case of an unsuccessful
     * operation, the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    CompletableFuture<Void> setContent(IRI identifier, InputStream stream, Map<String, String> metadata);

    /**
     * Purge the content from its corresponding datastore.
     *
     * @param identifier the binary object identifier
     * @return a new completion stage that, when the stage completes normally, indicates that the binary
     * data were successfully deleted from the corresponding persistence layer. In the case of an unsuccessful
     * operation, the {@link CompletableFuture} will complete exceptionally and can be handled with
     * {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    CompletableFuture<Void> purgeContent(IRI identifier);

    /**
     * Calculate the digest for a binary object.
     *
     * <p>Note: as per RFC 3230, the digest value is calculated over the entire resource,
     * not just the HTTP payload.
     *
     * @param identifier the identifier
     * @param algorithm the algorithm
     * @return the new completion stage containing a computed digest for the binary resource
     */
    CompletableFuture<String> calculateDigest(IRI identifier, String algorithm);

    /**
     * Get a list of supported algorithms.
     *
     * @return the supported digest algorithms
     */
    Set<String> supportedAlgorithms();

    /**
     * Get a new identifier.
     *
     * @return a new identifier
     */
    String generateIdentifier();
}
