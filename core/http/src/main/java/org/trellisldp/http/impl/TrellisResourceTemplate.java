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
package org.trellisldp.http.impl;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.trellisldp.api.BinaryTemplate;
import org.trellisldp.api.ResourceTemplate;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;
import org.trellisldp.vocabulary.Trellis;

/**
 * A template for generating a {@link org.trellisldp.api.Resource} in a persistence layer.
 */
public class TrellisResourceTemplate implements ResourceTemplate {

    private final IRI identifier;
    private final IRI interactionModel;
    private final IRI container;
    private final BinaryTemplate binary;
    private final Dataset dataset;

    /**
     * Create a template for generating a Trellis Resource.
     * @param identifier the identifier
     * @param interactionModel the LDP interaction model
     * @param dataset the dataset
     * @param container the container, may be {@code null}
     * @param binary the binary template, may be {@code null}
     */
    public TrellisResourceTemplate(final IRI identifier, final IRI interactionModel, final Dataset dataset,
            final IRI container, final BinaryTemplate binary) {
        requireNonNull(identifier, "identifier may not be null!");
        requireNonNull(interactionModel, "interactionModel may not be null!");
        this.identifier = identifier;
        this.interactionModel = interactionModel;
        this.dataset = dataset;
        this.container = container;
        this.binary = binary;
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public IRI getInteractionModel() {
        return interactionModel;
    }

    @Override
    public Optional<IRI> getContainer() {
        return ofNullable(container);
    }

    @Override
    public Optional<IRI> getMembershipResource() {
        return firstIRI(LDP.membershipResource);
    }

    @Override
    public Optional<IRI> getMemberRelation() {
        return firstIRI(LDP.hasMemberRelation);
    }

    @Override
    public Optional<IRI> getMemberOfRelation() {
        return firstIRI(LDP.isMemberOfRelation);
    }

    @Override
    public Optional<IRI> getInsertedContentRelation() {
        return firstIRI(LDP.insertedContentRelation);
    }

    @Override
    public Stream<? extends Quad> stream() {
        return dataset.stream();
    }

    @Override
    public Dataset dataset() {
        return dataset;
    }

    @Override
    public Optional<BinaryTemplate> getBinaryTemplate() {
        return ofNullable(binary);
    }

    @Override
    public Stream<Entry<String, String>> getExtraLinkRelations() {
        final Stream.Builder<Entry<String, String>> builder = Stream.builder();
        dataset.stream(of(Trellis.PreferUserManaged), identifier, OA.annotationService, null)
            .map(Quad::getObject).filter(IRI.class::isInstance).map(IRI.class::cast).map(IRI::getIRIString)
            .map(x -> new SimpleImmutableEntry<>(x, OA.annotationService.getIRIString())).forEach(builder::accept);
        dataset.stream(of(Trellis.PreferUserManaged), identifier, LDP.inbox, null)
            .map(Quad::getObject).filter(IRI.class::isInstance).map(IRI.class::cast).map(IRI::getIRIString)
            .map(x -> new SimpleImmutableEntry<>(x, LDP.inbox.getIRIString())).forEach(builder::accept);
        return builder.build();
    }

    private Optional<IRI> firstIRI(final IRI predicate) {
        return dataset.stream(of(Trellis.PreferUserManaged), identifier, predicate, null)
            .map(Quad::getObject).filter(IRI.class::isInstance).map(IRI.class::cast).findFirst();
    }
}
