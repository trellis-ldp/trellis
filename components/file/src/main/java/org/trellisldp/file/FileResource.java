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

import static java.nio.file.Files.lines;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.empty;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
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

    private final File file;
    private final IRI identifier;
    private final Map<IRI, RDFTerm> data;

    /**
     * Create a resource backed by an NQuads file.
     * @param identifier the resource identifier
     * @param file the file
     */
    public FileResource(final IRI identifier, final File file) {
        this.identifier = identifier;
        this.file = file;
        this.data = init(identifier, file);
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
    public boolean hasAcl() {
        try (final Stream<Quad> quads = stream(Trellis.PreferAccessControl)) {
            return quads.findFirst().isPresent();
        }
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

    private static Map<IRI, RDFTerm> init(final IRI identifier, final File file) {
        try (final Stream<Triple> triples = fetchContent(identifier, file).filter(q ->
                    q.getGraphName().filter(isEqual(PreferServerManaged)).isPresent()).map(Quad::asTriple)) {
            return triples.collect(toMap(t -> !t.getSubject().equals(identifier) && DC.modified.equals(t.getPredicate())
                        ? Time.hasTime : t.getPredicate(), Triple::getObject));
        }
    }

    private static Stream<Quad> fetchContent(final IRI identifier, final File file) {
        LOGGER.trace("Streaming quads for {}", identifier);
        try {
            return lines(file.toPath()).flatMap(FileUtils::parseQuad);
        } catch (final IOException ex) {
            LOGGER.warn("Could not read file at {}: {}", file, ex.getMessage());
        }
        return empty();
    }
}
