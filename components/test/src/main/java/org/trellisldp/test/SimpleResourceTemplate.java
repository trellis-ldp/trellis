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
package org.trellisldp.test;

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
public class SimpleResourceTemplate implements ResourceTemplate {

    private final IRI identifier;
    private final IRI interactionModel;
    private final IRI container;
    private final Dataset dataset;
    private final BinaryTemplate binary;

    /**
     * Create a template for generating a Trellis Resource.
     * @param identifier the identifier
     * @param interactionModel the LDP interaction model
     * @param dataset the dataset
     * @param container the container, may be {@code null}
     * @param binary the binary template, may be {@code null}
     */
    public SimpleResourceTemplate(final IRI identifier, final IRI interactionModel, final Dataset dataset,
            final IRI container, final BinaryTemplate binary) {
        requireNonNull(identifier, "identifier may not be null!");
        requireNonNull(interactionModel, "interactionModel may not be null!");
        this.identifier = identifier;
        this.interactionModel = interactionModel;
        this.container = container;
        this.dataset = dataset;
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
    public Optional<IRI> getMembershipResource() {
        return firstValue(LDP.membershipResource);
    }

    @Override
    public Optional<IRI> getMemberOfRelation() {
        return firstValue(LDP.isMemberOfRelation);
    }

    @Override
    public Optional<IRI> getMemberRelation() {
        return firstValue(LDP.hasMemberRelation);
    }

    @Override
    public Optional<IRI> getInsertedContentRelation() {
        return firstValue(LDP.insertedContentRelation);
    }

    @Override
    public Optional<IRI> getContainer() {
        return ofNullable(container);
    }

    @Override
    public Optional<BinaryTemplate> getBinaryTemplate() {
        return ofNullable(binary);
    }

    @Override
    public Dataset dataset() {
        return dataset;
    }

    @Override
    public Stream<? extends Quad> stream() {
        return dataset.stream();
    }

    @Override
    public Stream<Entry<String, String>> getExtraLinkRelations() {
        final Stream.Builder<Entry<String, String>> builder = Stream.builder();
        firstValue(OA.annotationService).map(IRI::getIRIString)
            .map(x -> new SimpleImmutableEntry<>(x, OA.annotationService.getIRIString())).ifPresent(builder::accept);
        firstValue(LDP.inbox).map(IRI::getIRIString)
            .map(x -> new SimpleImmutableEntry<>(x, LDP.inbox.getIRIString())).ifPresent(builder::accept);
        return builder.build();
    }

    private Optional<IRI> firstValue(final IRI predicate) {
        return dataset.stream(of(Trellis.PreferUserManaged), identifier, predicate, null)
            .map(Quad::getObject).filter(IRI.class::isInstance).map(IRI.class::cast).findFirst();
    }
}
