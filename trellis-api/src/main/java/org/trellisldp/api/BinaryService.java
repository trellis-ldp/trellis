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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;

/**
 * The BinaryService provides methods for retrieving, modifying and checking
 * the validity of binary content.
 *
 * @author acoburn
 */
public interface BinaryService {

    /**
     * A multipart upload container.
     */
    class MultipartUpload {
        private final Binary binary;
        private final String baseUrl;
        private final String path;
        private final Session session;

        /**
         * Create a Multipart Upload object.
         *
         * @param baseUrl the base URL
         * @param path the path
         * @param session the session
         * @param binary the binary
         */
        public MultipartUpload(final String baseUrl, final String path, final Session session, final Binary binary) {
            this.baseUrl = baseUrl;
            this.path = path;
            this.session = session;
            this.binary = binary;
        }

        /**
         * The binary object.
         *
         * @return the binary
         */
        public Binary getBinary() {
            return binary;
        }

        /**
         * The path.
         *
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * The base URL.
         *
         * @return the base URL
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * The Session.
         *
         * @return the session
         */
        public Session getSession() {
            return session;
        }
    }

    /**
     * A multipart upload-capable interface.
     */
    interface MultipartCapable {
        /**
         * Initiate a multi-part upload.
         *
         * @param identifier the object identifier
         * @param mimeType the mimeType of the object
         * @return an upload session identifier
         */
        String initiateUpload(IRI identifier, String mimeType);

        /**
         * Upload a part.
         *
         * @param identifier the upload identifier
         * @param partNumber the part number
         * @param content the content to upload
         * @return a digest value returned for each part; this value is used later wich completeUpload()
         */
        String uploadPart(String identifier, Integer partNumber, InputStream content);

        /**
         * Complete a multi-part upload.
         *
         * @param identifier the upload identifier
         * @param partDigests digest values for each part
         * @return a multipart upload object
         */
        MultipartUpload completeUpload(String identifier, Map<Integer, String> partDigests);

        /**
         * Abort the upload for the given identifier.
         *
         * @param identifier the upload identifier
         */
        void abortUpload(String identifier);

        /**
         * Test whether the provided identifier exists.
         *
         * @param identifier the upload identifier
         * @return true if the session exists; false otherwise
         */
        Boolean uploadSessionExists(String identifier);

        /**
         * List the uploaded parts.
         *
         * @param identifier the upload identifier
         * @return a list of uploaded parts and their digests
         */
        Stream<Map.Entry<Integer, String>> listParts(String identifier);
    }

    /**
     * Get the content of the binary object.
     *
     * @param identifier an identifier used for locating the binary object
     * @param ranges any segment ranges requested
     * @return the content
     */
    Optional<InputStream> getContent(IRI identifier, List<Range<Integer>> ranges);

    /**
     * Get the content of the binary object.
     *
     * @param identifier an identifier used for locating the binary object
     * @return the content
     */
    default Optional<InputStream> getContent(IRI identifier) {
        return getContent(identifier, emptyList());
    }

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
     * Get a supplier of identifiers.
     *
     * @return an identifier supplier for this service
     */
    Supplier<String> getIdentifierSupplier();
}
