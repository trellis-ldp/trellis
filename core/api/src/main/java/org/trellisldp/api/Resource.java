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
package org.trellisldp.api;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;

import java.time.Instant;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;

/**
 * The central resource abstraction for a Trellis-based linked data server.
 *
 * @see <a href="https://www.w3.org/TR/ldp/">Linked Data Platform Specification</a>
 *
 * @apiNote A common implementation pattern for this interface is to initialize the {@link Resource} object
 *          with metadata such as {@link #getModified}, {@link #getInteractionModel} and other data fetched
 *          by the accessors. The resource content, on the other hand, is often loaded on-demand via
 *          the {@link #stream} method. This pattern tends to reduce the number of requests to the
 *          persistence layer while also limiting the data held in memory for a given {@link Resource} object.
 * @author acoburn
 */
public interface Resource {

    enum SpecialResources implements Resource {
        /**
         * A non-existent resource: one that does not exist at a given IRI.
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
            public String getRevision() {
                return null;
            }

            @Override
            public Instant getModified() {
                return null;
            }

            @Override
            public Optional<IRI> getContainer() {
                return empty();
            }

            @Override
            public Stream<Quad> stream() {
                return Stream.empty();
            }

            @Override
            public String toString() {
                return "A non-existent resource";
            }
        },

        /**
         * A resource that previously existed but which no longer exists.
         */
        DELETED_RESOURCE {
            @Override
            public IRI getIdentifier() {
                return null;
            }

            @Override
            public Instant getModified() {
                return null;
            }

            @Override
            public String getRevision() {
                return null;
            }

            @Override
            public Optional<IRI> getContainer() {
                return empty();
            }

            @Override
            public IRI getInteractionModel() {
                return null;
            }

            @Override
            public Stream<Quad> stream() {
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
     * Get the revision or state tag of the resource.
     *
     * @apiNote the return value of this method should be unique to the state of the resource.
     *          That is, it should be unique across all resources on the server, and whenever
     *          a given resource changes, this revision tag should also change. This value will
     *          be used as the raw material for generating an ETag in the HTTP layer, and it
     *          should not return {@code null}.
     * @implSpec the default implementation returns a concatenation of the resource identifier
     *           and the modification date.
     * @return the revision tag of the resource
     */
    default String getRevision() {
        return getModified() + "::" + getIdentifier();
    }

    /**
     * Get the container for this resource.
     *
     * @apiNote returning an empty Optional should indicate here that the resource is not
     *          contained by any parent resource. This may be because it is a root resource
     *          and therefore not contained by any other resource. Alternatively, it could
     *          mean that a PUT operation was used to create the resource.
     * @return the identifier for a container, if one exists.
     */
    Optional<IRI> getContainer();

    /**
     * Retrieve the membership resource if this is an LDP Direct or Indirect container.
     *
     * @implSpec Other LDP resource types will always return an empty {@link Optional} value
     * @return the membership resource
     */
    default Optional<IRI> getMembershipResource() {
        return empty();
    }

    /**
     * Retrieve the member relation if this is an LDP Direct or Indirect container.
     *
     * @implSpec Other LDP resource types will always return an empty {@link Optional} value
     * @return the ldp:hasMemberRelation IRI
     */
    default Optional<IRI> getMemberRelation() {
        return empty();
    }

    /**
     * Retrieve the member of relation IRI.
     *
     * @implSpec Other LDP resource types will always return an empty {@link Optional} value
     * @return the ldp:isMemberOfRelation IRI
     */
    default Optional<IRI> getMemberOfRelation() {
        return empty();
    }

    /**
     * Retrieve the inserted content relation if this is an LDP Indirect container.
     *
     * @implSpec Other LDP resource types will always return an empty {@link Optional} value
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
    Stream<Quad> stream();

    /**
     * Retrieve the RDF Quads for a resource.
     *
     * @return the RDF quads
     */
    default Dataset dataset() {
        try (final Stream<Quad> quads = stream()) {
            return quads.collect(TrellisUtils.toDataset());
        }
    }

    /**
     * Retrieve the RDF Quads for a given named graph.
     *
     * @param graphName the named graph
     * @return the RDF quads
     */
    default Stream<Quad> stream(IRI graphName) {
        return stream(singleton(graphName));
    }

    /**
     * Retrieve the RDF Quads for a set of named graphs.
     *
     * @param graphNames the named graphs
     * @return the RDF quads
     */
    default Stream<Quad> stream(Collection<IRI> graphNames) {
        return stream().filter(quad -> quad.getGraphName().filter(graphNames::contains).isPresent());
    }

    /**
     * Retrieve a BinaryMetadata for this resouce, if it is a LDP-NR.
     *
     * @implSpec Other LDP resource types will always return an empty {@link Optional} value
     * @return the binary metadata
     */
    default Optional<BinaryMetadata> getBinaryMetadata() {
        return empty();
    }

    /**
     * Test whether this resource has an attached metadata resource.
     *
     * @param graphName the name of the metadata graph
     * @return true if this resource has an attached metadata resource; false otherwise
     */
    default boolean hasMetadata(IRI graphName) {
        return getMetadataGraphNames().contains(graphName);
    }

    /**
     * Retrieve a collection of attached metadata graph names.
     *
     * @return a set of metadata resource graph names
     */
    default Set<IRI> getMetadataGraphNames() {
        return emptySet();
    }

    /**
     * Get any extra implementation-defined link relations for this resource.
     *
     * @apiNote Each entry will be used to create a link header, such that the key refers
     *          to the URI and the value is the "rel" portion. For example, an item with
     *          {@code key="http://example.com/author001"} and {@code value="author"} will result
     *          in the header {@code Link: <http://example.com/author001>; rel="author"}.
     * @return a stream of relation types
     */
    default Stream<Entry<String, String>> getExtraLinkRelations() {
        return Stream.empty();
    }
}
