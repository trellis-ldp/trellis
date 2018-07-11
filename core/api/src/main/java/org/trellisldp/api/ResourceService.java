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

import static java.util.Optional.of;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;

/**
 * The ResourceService provides methods for creating, retrieving and manipulating
 * Trellis resources.
 *
 * @author acoburn
 */
public interface ResourceService extends MutableDataService<Resource>, ImmutableDataService<Resource> {

    /**
     * Get the identifier for the structurally-logical container for the resource.
     *
     * <p>Note: The returned identifier is not guaranteed to exist.
     *
     * @param identifier the identifier
     * @return an identifier for the structurally-logical container
     *
     */
    default Optional<IRI> getContainer(final IRI identifier) {
        final String path = identifier.getIRIString().substring(TRELLIS_DATA_PREFIX.length());
        return of(path).filter(p -> !p.isEmpty()).map(x -> x.lastIndexOf('/')).map(idx -> idx < 0 ? 0 : idx)
                    .map(idx -> TRELLIS_DATA_PREFIX + path.substring(0, idx)).map(getInstance()::createIRI);
    }

    /**
     * Scan the resources.
     *
     * @return a stream of RDF Triples, containing the resource and its LDP type
     */
    Stream<? extends Triple> scan();

    /**
     * Skolemize a blank node.
     *
     * @param term the RDF term
     * @return a skolemized node, if a blank node; otherwise the original term
     */
    default RDFTerm skolemize(final RDFTerm term) {
        if (term instanceof BlankNode) {
            return getInstance().createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) term).uniqueReference());
        }
        return term;
    }

    /**
     * Un-skolemize a blank node.
     *
     * @param term the RDF term
     * @return a blank node, if a previously-skolemized node; otherwise the original term
     */
    default RDFTerm unskolemize(final RDFTerm term) {
        if (term instanceof IRI) {
            final String iri = ((IRI) term).getIRIString();
            if (iri.startsWith(TRELLIS_BNODE_PREFIX)) {
                return getInstance().createBlankNode(iri.substring(TRELLIS_BNODE_PREFIX.length()));
            }
        }
        return term;
    }

    /**
     * Return an "internal" representation of an RDF term.
     *
     * @param <T> the type of RDF term
     * @param term the RDF term
     * @param baseUrl the base URL of the domain
     * @return the "internal" RDF term
     */
    default <T extends RDFTerm> T toInternal(final T term, final String baseUrl) {
        if (term instanceof IRI) {
            final String iri = ((IRI) term).getIRIString();
            if (iri.startsWith(baseUrl)) {
                @SuppressWarnings("unchecked")
                final T t = (T) getInstance().createIRI(TRELLIS_DATA_PREFIX + iri.substring(baseUrl.length()));
                return t;
            }
        }
        return term;
    }

    /**
     * Return an "external" representation of an RDF term.
     *
     * @param <T> the type of RDF term
     * @param term the RDF term
     * @param baseUrl the base URL of the domain
     * @return the "external" RDF term
     */
    default <T extends RDFTerm> T toExternal(final T term, final String baseUrl) {
        if (term instanceof IRI) {
            final String iri = ((IRI) term).getIRIString();
            if (iri.startsWith(TRELLIS_DATA_PREFIX)) {
                @SuppressWarnings("unchecked")
                final T t = (T) getInstance().createIRI(baseUrl + iri.substring(TRELLIS_DATA_PREFIX.length()));
                return t;
            }
        }
        return term;
    }

    /**
     * Export the server's resources as a stream of {@link Quad}s.
     *
     * @param graphNames the graph names to export
     * @return a stream of quads, where each named graph refers to the resource identifier
     */
    default Stream<? extends Quad> export(final Collection<IRI> graphNames) {
        return scan().map(Triple::getSubject).filter(x -> x instanceof IRI).map(x -> (IRI) x)
            // TODO - JDK9 optional to stream
            .flatMap(id -> get(id).map(Stream::of).orElseGet(Stream::empty))
            .flatMap(resource -> resource.stream(graphNames).map(q ->
                getInstance().createQuad(resource.getIdentifier(), q.getSubject(), q.getPredicate(), q.getObject())));
    }

    /**
     * Return a collection of interaction models supported by this Resource Service.
     *
     * @return a set of supported interaction models
     */
    Set<IRI> supportedInteractionModels();

    /**
     * An identifier generator.
     *
     * @return a new identifier
     */
    String generateIdentifier();
}
