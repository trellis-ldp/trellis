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
import static java.util.stream.Stream.empty;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.vocabulary.RDF.type;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * A file-based Trellis resource.
 */
public class FileResource implements Resource {

    private static final RDF rdf = getInstance();
    private static final Logger LOGGER = getLogger(FileResource.class);

    private final File file;
    private final IRI identifier;
    protected final Graph graph = rdf.createGraph();

    /**
     * Create a resource backed by an NQuads file.
     * @param identifier the resource identifier
     * @param file the file
     */
    public FileResource(final IRI identifier, final File file) {
        this.identifier = identifier;
        this.file = file;
        init();
    }

    private void init() {
        try (final Stream<? extends Triple> triples = stream(Trellis.PreferServerManaged)) {
            triples.forEach(graph::add);
        }
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public IRI getInteractionModel() {
        return getObjectForPredicate(type).orElse(null);
    }

    @Override
    public Instant getModified() {
        return graph.stream(identifier, DC.modified, null).map(triple -> (Literal) triple.getObject())
            .map(Literal::getLexicalForm).map(Instant::parse).findFirst().orElse(null);
    }

    @Override
    public Optional<IRI> getContainer() {
        return getObjectForPredicate(DC.isPartOf);
    }

    @Override
    public Optional<Binary> getBinary() {
        return graph.stream(identifier, DC.hasPart, null).map(Triple::getObject).filter(t -> t instanceof IRI)
                .map(t -> (IRI) t).findFirst().map(id -> new Binary((IRI) id,
                        // Add a date
                        graph.stream(id, DC.modified, null).map(Triple::getObject).map(t -> (Literal) t)
                            .map(Literal::getLexicalForm).map(Instant::parse).findFirst().orElse(null),
                        // Add a MIMEtype
                        graph.stream(id, DC.format, null).map(Triple::getObject)
                            .map(t -> (Literal) t).map(Literal::getLexicalForm).findFirst().orElse(null),
                        // Add a size value
                        graph.stream(id, DC.extent, null).map(Triple::getObject).map(t -> (Literal) t)
                            .map(Literal::getLexicalForm).map(Long::parseLong).findFirst().orElse(null)));
    }

    @Override
    public Optional<IRI> getMembershipResource() {
        return getObjectForPredicate(LDP.membershipResource);
    }

    @Override
    public Optional<IRI> getMemberRelation() {
        return getObjectForPredicate(LDP.hasMemberRelation);
    }

    @Override
    public Optional<IRI> getInsertedContentRelation() {
        return getObjectForPredicate(LDP.insertedContentRelation);
    }

    @Override
    public Optional<IRI> getMemberOfRelation() {
        return getObjectForPredicate(LDP.isMemberOfRelation);
    }

    private Optional<IRI> getObjectForPredicate(final IRI predicate) {
        return graph.stream(identifier, predicate, null).filter(t -> t.getObject() instanceof IRI)
            .map(t -> (IRI) t.getObject()).findFirst();
    }

    @Override
    public Boolean hasAcl() {
        try (final Stream<? extends Triple> triples = stream(Trellis.PreferAccessControl)) {
            return triples.findFirst().isPresent();
        }
    }

    @Override
    public Stream<Quad> stream() {
        LOGGER.trace("Streaming quads for {}", identifier);
        try {
            return lines(file.toPath()).flatMap(FileUtils::parseQuad);
        } catch (final IOException ex) {
            LOGGER.warn("Could not read file at {}: {}", file, ex.getMessage());
        }
        return empty();
    }
}
