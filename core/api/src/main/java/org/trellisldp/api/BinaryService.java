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

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
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
public interface BinaryService extends RetrievalService<Binary> {

    /**
     * Set the content for a binary object.
     *
     * @param metadata the binary metadata
     * @param stream the content
     * @param hints any hints for storing the binary data
     * @return the new completion stage
     */
    CompletableFuture<Void> setContent(BinaryMetadata metadata, InputStream stream, Map<String, List<String>> hints);

    /**
     * Set the content for a binary object using a digest algorithm.
     *
     * @implSpec The default implementation will compute a digest while processing the {@code InputStream}.
     * @param metadata the binary metadata
     * @param stream the context
     * @param algorithm the digest algorithm
     * @param hints any hints for storing the binary data
     * @return the new completion stage containing the server-computed digest.
     */
    default CompletableFuture<byte[]> setContent(final BinaryMetadata metadata, final InputStream stream,
            final MessageDigest algorithm, final Map<String, List<String>> hints) {
        final DigestInputStream input = new DigestInputStream(stream, algorithm);
        return setContent(metadata, input, hints).thenApply(future -> input.getMessageDigest().digest());
    }

    /**
     * Purge the content from its corresponding datastore.
     *
     * @param identifier the binary object identifier
     * @return a new completion stage that, when the stage completes normally, indicates that the binary data
     *         were successfully deleted from the corresponding persistence layer. In the case of an unsuccessful
     *         operation, the {@link CompletableFuture} will complete exceptionally and can be handled with
     *         {@link CompletableFuture#handle}, {@link CompletableFuture#exceptionally} or similar methods.
     */
    CompletableFuture<Void> purgeContent(IRI identifier);

    /**
     * Calculate the digest for a binary object.
     *
     * @apiNote As per RFC 3230, the digest value is calculated over the entire resource,
     *          not just the HTTP payload.
     * @param identifier the identifier
     * @param algorithm the algorithm
     * @return the new completion stage containing a computed digest for the binary resource
     */
    CompletableFuture<byte[]> calculateDigest(IRI identifier, MessageDigest algorithm);

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
