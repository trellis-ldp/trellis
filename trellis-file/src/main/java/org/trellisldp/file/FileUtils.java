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
package org.trellisldp.file;

import static java.io.File.separator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.IntStream.range;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.jena.riot.Lang.NQUADS;
import static org.apache.jena.riot.RDFParser.fromString;
import static org.apache.jena.sparql.core.DatasetGraphFactory.create;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.Iterator;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.zip.CRC32;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;

/**
 * File-based utilities.
 */
public final class FileUtils {

    private static final Logger LOGGER = getLogger(FileUtils.class);
    private static final JenaRDF rdf = new JenaRDF();
    private static final String SEP = " ";

    // The length of the CRC directory partition
    public static final int LENGTH = 2;
    public static final int MAX = 3;

    /**
     * Get a directory for a given resource identifier.
     * @param baseDirectory the base directory
     * @param identifier a resource identifier
     * @return a directory
     */
    public static File getResourceDirectory(final File baseDirectory, final IRI identifier) {
        requireNonNull(baseDirectory, "The baseDirectory may not be null!");
        requireNonNull(identifier, "The identifier may not be null!");
        final String id = identifier.getIRIString();
        final StringJoiner joiner = new StringJoiner(separator);
        final CRC32 hasher = new CRC32();
        hasher.update(id.getBytes(UTF_8));
        final String intermediate = Long.toHexString(hasher.getValue());

        range(0, intermediate.length() / LENGTH).limit(MAX)
            .forEach(i -> joiner.add(intermediate.substring(i * LENGTH, (i + 1) * LENGTH)));

        joiner.add(md5Hex(id));
        return new File(baseDirectory, joiner.toString());
    }

    /**
     * Parse a string into a Quad.
     * @param line the line of text
     * @return the Quad
     */
    public static Optional<Quad> parseQuad(final String line) {
        final DatasetGraph dataset = create();
        try {
            fromString(line).lang(NQUADS).parse(dataset);
        } catch (final RiotException ex) {
            LOGGER.warn("Skipping invalid data value: {}", line);
            return empty();
        }
        return of(dataset.find()).filter(Iterator::hasNext).map(Iterator::next).map(rdf::asQuad);
    }

    /**
     * Serialize an RDF Quad.
     * @param quad the quad
     * @return a serialization of the quad
     */
    public static String serializeQuad(final Quad quad) {
        return quad.getSubject() + SEP + quad.getPredicate() + SEP + quad.getObject() + SEP
            + quad.getGraphName().map(g -> g + " .").orElse(".");
    }

    private FileUtils() {
        // prevent instantiation
    }
}
