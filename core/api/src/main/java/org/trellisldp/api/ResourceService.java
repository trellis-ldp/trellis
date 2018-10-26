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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;

/**
 * The ResourceService provides methods for creating, retrieving and manipulating Trellis resources.
 *
 * @implSpec Implementations should take care to provide any initialization needed, in a constructor or methods
 *           managed by an external framework (e.g. using {@code PostConstruct}). This may or may not include
 *           actions in external systems like databases, which may be better managed elsewhere.
 */
public interface ResourceService extends MutableDataService<Resource>, ImmutableDataService<Resource> {

    /**
     * Get the identifier for the structurally-logical container for the resource.
     *
     * @apiNote The returned identifier is not guaranteed to exist.
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
     * Update the modification date of the provided resource.
     *
     * @apiNote In general, when this method is called, it can be expected that the target resource
     *          will already exist. In cases where that resource does not already exist, an implementation
     *          may choose to throw an exception, log an error or simply do nothing.
     * @param identifier the identifier of the resource
     * @return a new completion stage that, when the stage completes normally, indicates that the
     *         identified resource has been updated with a new modification date.
     */
    CompletableFuture<Void> touch(IRI identifier);

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
