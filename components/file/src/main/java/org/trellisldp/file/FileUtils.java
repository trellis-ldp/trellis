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
package org.trellisldp.file;

import static java.io.File.separator;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.jena.riot.tokens.TokenizerFactory.makeTokenizerString;
import static org.apache.jena.sparql.core.Quad.create;
import static org.apache.jena.sparql.core.Quad.defaultGraphIRI;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.tokens.Token;
import org.slf4j.Logger;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.XSD;

/**
 * File-based utilities.
 */
public final class FileUtils {

    /** The configuration key for controlling LDP type triples. */
    public static final String CONFIG_FILE_LDP_TYPE = "trellis.file.ldp-type";
    private static final Logger LOGGER = getLogger(FileUtils.class);
    private static final JenaRDF rdf = new JenaRDF();
    private static final String SEP = " ";

    /**
     * The length of the CRC directory partition.
     */
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
     * Parse a string into a stream of Quads.
     * @param line the line of text
     * @return the Quad
     */
    public static Stream<Quad> parseQuad(final String line) {
        final List<Token> tokens = new ArrayList<>();
        makeTokenizerString(line).forEachRemaining(tokens::add);

        final List<Node> nodes = tokens.stream().filter(Token::isNode).map(Token::asNode).filter(Objects::nonNull)
            .collect(toList());

        if (nodes.size() == 3) {
            return of(rdf.asQuad(create(defaultGraphIRI, nodes.get(0), nodes.get(1), nodes.get(2))));
        } else if (nodes.size() == 4) {
            return of(rdf.asQuad(create(nodes.get(3), nodes.get(0), nodes.get(1), nodes.get(2))));
        } else {
            LOGGER.warn("Skipping invalid data value: {}", line);
            return empty();
        }
    }

    /**
     * Filter any server-managed triples from the resource stream.
     * @param quad the quad
     * @return true if the quad should be kept, false otherwise
     */
    public static boolean filterServerManagedQuads(final Quad quad) {
        if (quad.getGraphName().equals(Optional.of(PreferServerManaged))) {
            return quad.getPredicate().equals(type) && getConfig()
                .getOptionalValue(CONFIG_FILE_LDP_TYPE, Boolean.class).orElse(Boolean.TRUE);
        }
        return true;
    }

    /**
     * Try to delete a file if it exists or throw an unchecked exception.
     * @param path the file path
     * @return true if the file existed and was deleted; false otherwise
     */
    public static boolean uncheckedDeleteIfExists(final Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (final IOException ex) {
            throw new UncheckedIOException("Error deleting file", ex);
        }
    }

    /**
     * Fetch a stream of files in the provided directory path.
     * @param path the directory path
     * @return a stream of filenames
     */
    public static Stream<Path> uncheckedList(final Path path) {
        try {
            return Files.list(path);
        } catch (final IOException ex) {
            throw new UncheckedIOException("Error fetching file list", ex);
        }
    }

    /**
     * Write a Memento to a particular resource directory.
     * @param resourceDir the resource directory
     * @param resource the resource
     * @param time the time for the memento
     */
    public static void writeMemento(final File resourceDir, final Resource resource,
            final Instant time) {
        try (final BufferedWriter writer = newBufferedWriter(
                        getNquadsFile(resourceDir, time).toPath(), UTF_8, CREATE, WRITE,
                        TRUNCATE_EXISTING)) {

            try (final Stream<String> quads = generateServerManaged(resource).map(FileUtils::serializeQuad)) {
                final Iterator<String> lineIter = quads.iterator();
                while (lineIter.hasNext()) {
                    writer.write(lineIter.next() + lineSeparator());
                }
            }

            try (final Stream<String> quads = resource.stream().filter(FileUtils::notServerManaged)
                    .map(FileUtils::serializeQuad)) {
                final Iterator<String> lineiter = quads.iterator();
                while (lineiter.hasNext()) {
                    writer.write(lineiter.next() + lineSeparator());
                }
            }
        } catch (final IOException ex) {
            throw new UncheckedIOException(
                            "Error writing resource version for " + resource.getIdentifier().getIRIString(), ex);
        }
    }

    /**
     * Get a bounded inputstream.
     * @param stream the input stream
     * @param from the byte from which to start
     * @param to the byte to which to read
     * @throws IOException if an error occurs when skipping forward
     * @return the bounded inputstream
     */
    public static InputStream getBoundedStream(final InputStream stream, final int from, final int to)
            throws IOException {
        final long skipped = stream.skip(from);
        LOGGER.debug("Skipped {} bytes", skipped);
        return new BoundedInputStream(stream, (long) to - from);
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

    /**
     * Get the nquads file for a given moment in time.
     * @param dir the directory
     * @param time the time
     * @return the file
     */
    public static File getNquadsFile(final File dir, final Instant time) {
        return new File(dir, time.getEpochSecond() + ".nq");
    }

    private static Stream<Quad> generateServerManaged(final Resource resource) {
        final List<Quad> quads = new ArrayList<>();

        quads.add(rdf.createQuad(PreferServerManaged, resource.getIdentifier(), type, resource.getInteractionModel()));
        quads.add(rdf.createQuad(PreferServerManaged, resource.getIdentifier(), DC.modified,
                    rdf.createLiteral(resource.getModified().toString(), XSD.dateTime)));

        resource.getBinaryMetadata().ifPresent(b -> {
            quads.add(rdf.createQuad(PreferServerManaged, resource.getIdentifier(), DC.hasPart, b.getIdentifier()));
            b.getMimeType().map(mimeType -> rdf.createQuad(PreferServerManaged, b.getIdentifier(), DC.format,
                rdf.createLiteral(mimeType))).ifPresent(quads::add);
        });

        resource.getContainer()
            .map(iri -> rdf.createQuad(PreferServerManaged, resource.getIdentifier(), DC.isPartOf, iri))
            .ifPresent(quads::add);

        resource.getMemberOfRelation()
            .map(iri -> rdf.createQuad(PreferServerManaged, resource.getIdentifier(), LDP.isMemberOfRelation, iri))
            .ifPresent(quads::add);

        resource.getMemberRelation()
            .map(iri -> rdf.createQuad(PreferServerManaged, resource.getIdentifier(), LDP.hasMemberRelation, iri))
            .ifPresent(quads::add);

        resource.getMembershipResource()
            .map(iri -> rdf.createQuad(PreferServerManaged, resource.getIdentifier(), LDP.membershipResource, iri))
            .ifPresent(quads::add);

        resource.getInsertedContentRelation()
            .map(iri -> rdf.createQuad(PreferServerManaged, resource.getIdentifier(), LDP.insertedContentRelation, iri))
            .ifPresent(quads::add);

        return quads.stream();
    }

    private static boolean notServerManaged(final Quad quad) {
        return !quad.getGraphName().filter(isEqual(PreferServerManaged)).isPresent();
    }

    private FileUtils() {
        // prevent instantiation
    }
}
