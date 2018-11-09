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

import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;

/**
 * A template for generating a {@link Resource} in a persistence layer.
 */
public interface ResourceTemplate {

    /**
     * Get the identifier for the resource.
     * @return an identifier
     */
    IRI getIdentifier();

    /**
     * Get the interaction model for the resource.
     * @return the LDP interaction model
     */
    IRI getInteractionModel();

    /**
     * Get the container for this resource, if one exists.
     * @return the parent container, if present
     */
    Optional<IRI> getContainer();

    /**
     * Get the LDP membership resource, if present.
     * @return the ldp:membershipResource value, if present
     */
    Optional<IRI> getMembershipResource();

    /**
     * Get the LDP member relation, if present.
     * @return the ldp:hasMemberRelation value, if present
     */
    Optional<IRI> getMemberRelation();

    /**
     * Get the LDP member of relation, if present.
     * @return the ldp:isMemberOfRelation value, if present
     */
    Optional<IRI> getMemberOfRelation();

    /**
     * Get the LDP inserted content relation, if present.
     * @return the ldp:insertedContentRelation value, if present
     */
    Optional<IRI> getInsertedContentRelation();

    /**
     * Get the supplied RDF data as a stream.
     *
     * @apiNote the data available through this method will only include user-managed and ACL quads.
     *          It will not include containment, membership or audit quads.
     * @return a stream of user-supplied RDF quads
     */
    Stream<? extends Quad> stream();

    /**
     * Get the supplied RDF data as a {@link Dataset}.
     *
     * @apiNote the data available through this method will only include user-managed and ACL quads.
     *          It will not include containment, membership or audit quads.
     * @return a dataset containing user-supplied RDF quads
     */
    Dataset dataset();

    /**
     * Get the BinaryTemplate, if one is present.
     *
     * @return the binary template
     */
    Optional<BinaryTemplate> getBinaryTemplate();

    /**
     * Get any extra data for link relations.
     * @apiNote Each entry can be used to create a link header, such that the key refers
     *          to the URI and the value is the "rel" portion. For example, an item with
     *          {@code key="http://example.com/author001"} and {@code value="author"} will result
     *          in the header {@code Link: <http://example.com/author001>; rel="author"}.
     *          An implementation may choose not to store these values.
     * @return a stream of relation types
     */
    Stream<Entry<String, String>> getExtraLinkRelations();
}
