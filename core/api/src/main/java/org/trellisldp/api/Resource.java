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

import static java.util.Collections.singleton;
import static java.util.Optional.empty;

import java.time.Instant;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.Triple;

/**
 * The central resource abstraction for a Trellis-based linked data server.
 *
 * @see <a href="https://www.w3.org/TR/ldp/">Linked Data Platform Specification</a>
 *
 * @author acoburn
 */
public interface Resource {

    enum SpecialResources implements Resource {
        /**
         * A non-existent resource: one that does not
         * exist at a given IRI.
         */
        MISSING_RESOURCE {
            @Override
            public IRI getIdentifier() {
                return null;
            }

            @Override
            public IRI getInteractionModel() {
                return null;
            }

            @Override
            public Instant getModified() {
                return null;
            }

            @Override
            public Stream<? extends Quad> stream() {
                return Stream.empty();
            }

            @Override
            public String toString() {
                return "A non-existent resource";
            }
        },

        /**
         * A resource that previously existed but which
         * no longer exists.
         */
        DELETED_RESOURCE {
            @Override
            public IRI getIdentifier() {
                return null;
            }

            @Override
            public IRI getInteractionModel() {
                return null;
            }

            @Override
            public Instant getModified() {
                return null;
            }

            @Override
            public Stream<? extends Quad> stream() {
                return Stream.empty();
            }

            @Override
            public String toString() {
                return "A deleted resource";
            }
        }
    }

    /**
     * Get an identifier for this resource.
     *
     * @return the identifier
     */
    IRI getIdentifier();

    /**
     * Get the LDP interaction model for this resource.
     *
     * @return the interaction model
     */
    IRI getInteractionModel();

    /**
     * Get the last modified date.
     *
     * @return the last-modified date
     */
    Instant getModified();

    /**
     * Retrieve the membership resource if this is an LDP Direct or Indirect container.
     *
     * <p>Note: Other resource types will always return an empty {@link Optional} value.</p>
     *
     * @return the membership resource
     */
    default Optional<IRI> getMembershipResource() {
        return empty();
    }

    /**
     * Retrieve the member relation if this is an LDP Direct or Indirect container.
     *
     * <p>Note: Other resource types will always return an empty {@link Optional} value.
     *
     * @return the ldp:hasMemberRelation IRI
     */
    default Optional<IRI> getMemberRelation() {
        return empty();
    }

    /**
     * Retrieve the member of relation IRI.
     *
     * <p>Note: Other resource types will always return an empty {@link Optional} value.
     *
     * @return the ldp:isMemberOfRelation IRI
     */
    default Optional<IRI> getMemberOfRelation() {
        return empty();
    }

    /**
     * Retrieve the inserted content relation if this is an LDP Indirect container.
     *
     * <p>Note: Other resource types will always return an empty {@link Optional} value.
     *
     * @return the inserted content relation
     */
    default Optional<IRI> getInsertedContentRelation() {
        return empty();
    }

    /**
     * Retrieve the RDF Quads for a resource.
     *
     * @return the RDF quads
     */
    Stream<? extends Quad> stream();

    /**
     * Retrieve the RDF Quads for a resource.
     *
     * @return the RDF quads
     */
    default Dataset dataset() {
        return stream().collect(RDFUtils.toDataset().concurrent());
    }

    /**
     * Retrieve the RDF Triples for a given named graph.
     *
     * @param graphName the named graph
     * @return the RDF triples
     */
    default Stream<? extends Triple> stream(IRI graphName) {
        return stream(singleton(graphName));
    }

    /**
     * Retrieve the RDF Triples for a set of named graphs.
     *
     * @param graphNames the named graphs
     * @return the RDF triples
     */
    default Stream<? extends Triple> stream(Collection<IRI> graphNames) {
        return stream().filter(quad -> quad.getGraphName().filter(graphNames::contains).isPresent())
            .map(Quad::asTriple);
    }

    /**
     * Retrieve a Binary for this resouce, if it is a LDP-NR.
     *
     * <p>Note: Other resource types will always return an empty {@link Optional} value.
     *
     * @return the binary object
     */
    default Optional<Binary> getBinary() {
        return empty();
    }

    /**
     * Test whether this resource is a Memento resource. {@code Resource}s retrieved
     * from {@link ResourceService#get(IRI)} should return {@code false} here, and
     * {@code Resource}s retrieved from {@link MementoService#get(IRI, Instant)}
     * should return {@code true}.
     *
     * @return {@code true} if this is a Memento resource; {@code false} otherwise
     *
     * @see ResourceService#get(IRI)
     * @see MementoService#get(IRI, Instant)
     */
    default Boolean isMemento() {
        return false;
    }

    /**
     * Test whether this resource has an ACL resource.
     *
     * @return true if this resource has and ACL resource; false otherwise
     */
    default Boolean hasAcl() {
        return false;
    }

    /**
     * Get any extra implementation-defined link relations for this resource.
     *
     * <p>Each entry will be used to create a link header, such that the key refers
     * to the URI and the value is the "rel" portion. For example, an item with
     * {@code key="http://example.com/author001"} and {@code value="author"} will result in
     * the header {@code Link: <http://example.com/author001>; rel="author"}.
     *
     * @return a stream of relation types
     */
    default Stream<Entry<String, String>> getExtraLinkRelations() {
        return Stream.empty();
    }

    /**
     * Test whether this resource has been marked as deleted.
     *
     * <p>By default this method returns false.
     *
     * <p>Note: this can be used to distinguish between resources that don't exist (e.g. HTTP 404)
     * and resources that are no longer available (e.g. HTTP 410) but which still have historical
     * versions available.
     *
     * @return true if this resource has been deleted; false otherwise
     */
    default Boolean isDeleted() {
        return false;
    }
}
