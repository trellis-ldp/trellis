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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Optional;

import org.apache.commons.rdf.api.IRI;

/**
 * Metadata values used for resource composition.
 */
public class Metadata {

    private final IRI identifier;
    private final IRI ixnModel;
    private final IRI container;
    private final IRI memberRelation;
    private final IRI membershipResource;
    private final IRI memberOfRelation;
    private final IRI insertedContentRelation;
    private final Binary binary;

    /**
     * A Metadata-bearing data structure for use with resource manipulation.
     *
     * @param identifier the identifier
     * @param ixnModel the interaction model
     * @param container a container identifier, may be {@code null}
     * @param membershipResource an LDP membershipResource, may be {@code null}
     * @param memberRelation an LDP hasMemberRelation predicate, may be {@code null}
     * @param memberOfRelation an LDP isMemberOfRelation predicate, may be {@code null}
     * @param insertedContentRelation an LDP insertedContentRelation, may be {@code null}
     * @param binary metadata about a Binary, may be {@code null}
     */
    private Metadata(final IRI identifier, final IRI ixnModel, final IRI container, final IRI membershipResource,
            final IRI memberRelation, final IRI memberOfRelation, final IRI insertedContentRelation,
            final Binary binary) {
        this.identifier = requireNonNull(identifier, "Identifier cannot be null!");
        this.ixnModel = requireNonNull(ixnModel, "Interaction model cannot be null!");
        this.container = container;
        this.membershipResource = membershipResource;
        this.memberRelation = memberRelation;
        this.memberOfRelation = memberOfRelation;
        this.insertedContentRelation = insertedContentRelation;
        this.binary = binary;
    }

    /**
     * A Metadata-bearing data structure for use with resource manipulation.
     *
     * @param r a resource
     */
    public Metadata(final Resource r) {
        this(r.getIdentifier(), r.getInteractionModel(), r.getContainer().orElse(null),
                        r.getMemberRelation().orElse(null), r.getMembershipResource().orElse(null),
                        r.getMemberOfRelation().orElse(null), r.getInsertedContentRelation().orElse(null),
                        r.getBinary().orElse(null));
    }

    /**
     * A mutable builder for a {@link Metadata} object.
     *
     * @param identifier the resource identifier
     * @return a builder for a {@link Metadata} object
     */
    public static Builder builder(final IRI identifier) {
        return new Builder(identifier);
    }

    /**
     * A mutable builder for a {@link Metadata} object.
     *
     * @param r the resource
     * @return a builder for a {@link Metadata} object
     */
    public static Builder builder(final Resource r) {
        return builder(r.getIdentifier()).interactionModel(r.getInteractionModel())
                        .container(r.getContainer().orElse(null)).memberRelation(r.getMemberRelation().orElse(null))
                        .membershipResource(r.getMembershipResource().orElse(null))
                        .memberOfRelation(r.getMemberOfRelation().orElse(null))
                        .insertedContentRelation(r.getInsertedContentRelation().orElse(null));
    }

    /**
     * Get an identifier for this metadata.
     *
     * @return the identifier
     */
    public IRI getIdentifier() {
        return identifier;
    }

    /**
     * Get the LDP interaction model for this metadata.
     *
     * @return the interaction model
     */
    public IRI getInteractionModel() {
        return ixnModel;
    }

    /**
     * Get the container for this resource.
     *
     * @apiNote returning an empty Optional should indicate here that the resource is not contained by any parent
     *          resource. This may be because it is a root resource and therefore not contained by any other resource.
     *          Alternatively, it could mean that a PUT operation was used to create the resource.
     * @return the identifier for a container, if one exists.
     */
    public Optional<IRI> getContainer() {
        return ofNullable(container);
    }

    /**
     * Retrieve the membership resource if this is an LDP Direct or Indirect container.
     *
     * @implSpec Other LDP resource types will always return an empty {@link Optional} value
     * @return the membership resource
     */
    public Optional<IRI> getMembershipResource() {
        return ofNullable(membershipResource);
    }

    /**
     * Retrieve the member relation if this is an LDP Direct or Indirect container.
     *
     * @implSpec Other LDP resource types will always return an empty {@link Optional} value
     * @return the ldp:hasMemberRelation IRI
     */
    public Optional<IRI> getMemberRelation() {
        return ofNullable(memberRelation);
    }

    /**
     * Retrieve the member of relation IRI.
     *
     * @implSpec Other LDP resource types will always return an empty {@link Optional} value
     * @return the ldp:isMemberOfRelation IRI
     */
    public Optional<IRI> getMemberOfRelation() {
        return ofNullable(memberOfRelation);
    }

    /**
     * Retrieve the inserted content relation if this is an LDP Indirect container.
     *
     * @implSpec Other LDP resource types will always return an empty {@link Optional} value
     * @return the inserted content relation
     */
    public Optional<IRI> getInsertedContentRelation() {
        return ofNullable(insertedContentRelation);
    }

    /**
     * Retrieve the binary metadata if this is an LDP NonRDFSource.
     * @implSpec Otheer LDP resource types will always return an empty {@link Optional} value
     * @return the binary metadata
     */
    public Optional<Binary> getBinary() {
        return ofNullable(binary);
    }

    /**
     * A mutable builder for a {@link Metadata} object.
     */
    public static class Builder {
        private final IRI identifier;
        private IRI ixnModel;
        private IRI container;
        private IRI memberRelation;
        private IRI membershipResource;
        private IRI memberOfRelation;
        private IRI insertedContentRelation;
        private Binary binary;

        /**
         * Create a Metadata builder with the provided identifier.
         * @param identifier the identifier
         */
        public Builder(final IRI identifier) {
            this.identifier = requireNonNull(identifier, "Identifier cannot be null!");
        }

        /**
         * Set the LDP interaction model.
         * @param ixnModel the interaction model
         * @return this builder
         */
        public Builder interactionModel(final IRI ixnModel) {
            this.ixnModel = ixnModel;
            return this;
        }

        /**
         * Set the container value.
         * @param container the container identifier
         * @return this builder
         */
        public Builder container(final IRI container) {
            this.container = container;
            return this;
        }

        /**
         * Set the member relation value.
         * @param memberRelation the member relation predicate
         * @return this builder
         */
        public Builder memberRelation(final IRI memberRelation) {
            this.memberRelation = memberRelation;
            return this;
        }

        /**
         * Set the membership resource value.
         * @param membershipResource the member resource identifier
         * @return this builder
         */
        public Builder membershipResource(final IRI membershipResource) {
            this.membershipResource = membershipResource;
            return this;
        }

        /**
         * Set the member of relation value.
         * @param memberOfRelation the member of relation predicate
         * @return this builder
         */
        public Builder memberOfRelation(final IRI memberOfRelation) {
            this.memberOfRelation = memberOfRelation;
            return this;
        }

        /**
         * Set the inserted content relation value.
         * @param insertedContentRelation the inserted content relation predicate
         * @return this builder
         */
        public Builder insertedContentRelation(final IRI insertedContentRelation) {
            this.insertedContentRelation = insertedContentRelation;
            return this;
        }

        /**
         * Set the binary metadata.
         * @param binary the binary metadata
         * @return this builder
         */
        public Builder binary(final Binary binary) {
            this.binary = binary;
            return this;
        }

        /**
         * Build the Metadata object, transitioning this builder to the built state.
         * @return the built stream
         */
        public Metadata build() {
            return new Metadata(identifier, ixnModel, container, memberRelation, membershipResource, memberOfRelation,
                            insertedContentRelation, binary);
        }
    }
}
