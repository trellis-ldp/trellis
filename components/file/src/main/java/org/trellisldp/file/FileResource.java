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

import static java.nio.file.Files.lines;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.empty;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.vocabulary.RDF.type;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFTerm;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Time;
import org.trellisldp.vocabulary.Trellis;

/**
 * A file-based Trellis resource.
 */
public class FileResource implements Resource {

    private static final Logger LOGGER = getLogger(FileResource.class);
    private static final Set<IRI> IGNORE = ignoreMetadataGraphs();

    private final File file;
    private final IRI identifier;
    private final Map<IRI, RDFTerm> data;
    private final Set<IRI> metadataGraphs;

    /**
     * Create a resource backed by an NQuads file.
     * @param identifier the resource identifier
     * @param file the file
     */
    public FileResource(final IRI identifier, final File file) {
        this.identifier = requireNonNull(identifier, "identifier may not be null!");
        this.file = file;
        try (final Stream<Quad> quads = fetchContent(identifier, file)) {
            final Map<IRI, RDFTerm> serverManaged = new HashMap<>();
            final Set<IRI> graphs = new HashSet<>();
            quads.forEach(quad -> quad.getGraphName().filter(IRI.class::isInstance).map(IRI.class::cast)
                    .ifPresent(graphName -> {
                if (Trellis.PreferServerManaged.equals(graphName)) {
                    final boolean binaryModified = !identifier.equals(quad.getSubject()) &&
                        DC.modified.equals(quad.getPredicate());
                    serverManaged.put(binaryModified ? Time.hasTime : quad.getPredicate(), quad.getObject());
                } else if (!IGNORE.contains(graphName)) {
                    graphs.add(graphName);
                }
            }));
            data = unmodifiableMap(serverManaged);
            metadataGraphs = unmodifiableSet(graphs);
        }
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public IRI getInteractionModel() {
        return asIRI(type).orElse(null);
    }

    @Override
    public Instant getModified() {
        return asLiteral(DC.modified).map(Instant::parse).orElse(null);
    }

    @Override
    public Optional<IRI> getContainer() {
        return asIRI(DC.isPartOf);
    }

    @Override
    public Optional<BinaryMetadata> getBinaryMetadata() {
        return asIRI(DC.hasPart).map(id ->
                BinaryMetadata.builder(id).mimeType(asLiteral(DC.format).orElse(null)).build());
    }

    @Override
    public Optional<IRI> getMembershipResource() {
        return asIRI(LDP.membershipResource);
    }

    @Override
    public Optional<IRI> getMemberRelation() {
        return asIRI(LDP.hasMemberRelation);
    }

    @Override
    public Optional<IRI> getInsertedContentRelation() {
        return asIRI(LDP.insertedContentRelation);
    }

    @Override
    public Optional<IRI> getMemberOfRelation() {
        return asIRI(LDP.isMemberOfRelation);
    }

    @Override
    public Set<IRI> getMetadataGraphNames() {
        return metadataGraphs;
    }

    @Override
    public Stream<Quad> stream() {
        return fetchContent(identifier, file).filter(FileUtils::filterServerManagedQuads);
    }

    private Optional<IRI> asIRI(final IRI predicate) {
        return ofNullable(data.get(predicate)).filter(IRI.class::isInstance).map(IRI.class::cast);
    }

    private Optional<String> asLiteral(final IRI predicate) {
        return ofNullable(data.get(predicate)).filter(Literal.class::isInstance).map(Literal.class::cast)
            .map(Literal::getLexicalForm);
    }

    static Set<IRI> ignoreMetadataGraphs() {
        final Set<IRI> ignore = new HashSet<>();
        ignore.add(Trellis.PreferUserManaged);
        ignore.add(Trellis.PreferServerManaged);
        ignore.add(LDP.PreferContainment);
        ignore.add(LDP.PreferMembership);
        return unmodifiableSet(ignore);
    }

    static Stream<Quad> fetchContent(final IRI identifier, final File file) {
        LOGGER.trace("Streaming quads for {}", identifier);
        try {
            return lines(file.toPath()).flatMap(FileUtils::parseQuad);
        } catch (final IOException ex) {
            LOGGER.warn("Could not read file at {}: {}", file, ex.getMessage());
        }
        return empty();
    }
}
