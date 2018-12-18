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
package org.trellisldp.constraint;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.Trellis;

/**
 * A set of constraints applied to user-provided graphs.
 *
 * <p>This class includes the following restrictions on a provided {@link Graph}:
 * <ul>
 * <li>Prevent LDP types to be set explicitly (Link headers should be used).</li>
 * <li>Direct Containers require certain defined predicates (as defined by LDP).</li>
 * <li>Indirect Containers require certain defined predicates (as defined by LDP).</li>
 * <li>ldp:contains triples may not be set directly (as defined by LDP).</li>
 * <li>ldp:contains may not be used as a membership property.</li>
 * <li>rdf:type may not be used as a membership property.</li>
 * <li>any rdf:type {@link Triple} requires an {@link IRI} as object.</li>
 * <li>any ldp:inbox {@link Triple} requires an {@link IRI} as object.</li>
 * <li>any oa:annotationService {@link Triple} requires an {@link IRI} as object.</li>
 * </ul>
 *
 * @author acoburn
 */
public class LdpConstraints implements ConstraintService {

    // Identify those predicates that are prohibited in the given ixn model
    private static final Predicate<Triple> memberContainerConstraints = triple ->
        triple.getPredicate().equals(ACL.accessControl) || triple.getPredicate().equals(LDP.contains);

    // Identify those predicates that are prohibited in the given ixn model
    private static final Predicate<Triple> basicConstraints = memberContainerConstraints.or(triple ->
        triple.getPredicate().equals(LDP.insertedContentRelation) ||
        triple.getPredicate().equals(LDP.membershipResource) ||
        triple.getPredicate().equals(LDP.hasMemberRelation) ||
        triple.getPredicate().equals(LDP.isMemberOfRelation));

    private static final Set<IRI> propertiesWithInDomainRange = singleton(LDP.membershipResource);

    private static final Map<IRI, Predicate<Triple>> typeMap = unmodifiableMap(Stream.of(
                new SimpleEntry<>(LDP.BasicContainer, basicConstraints),
                new SimpleEntry<>(LDP.Container, basicConstraints),
                new SimpleEntry<>(LDP.DirectContainer, memberContainerConstraints),
                new SimpleEntry<>(LDP.IndirectContainer, memberContainerConstraints),
                new SimpleEntry<>(LDP.NonRDFSource, basicConstraints),
                new SimpleEntry<>(LDP.RDFSource, basicConstraints))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

    // Properties that need to be used with objects that are IRIs
    private static final Set<IRI> propertiesWithUriRange = unmodifiableSet(Stream.of(
                LDP.membershipResource, LDP.hasMemberRelation, LDP.isMemberOfRelation, LDP.inbox,
                LDP.insertedContentRelation, OA.annotationService).collect(toSet()));

    // Properties that cannot be used as dynamic Membership properties
    private static final Set<IRI> restrictedMemberProperties = unmodifiableSet(Stream.of(
                ACL.accessControl, LDP.contains, RDF.type, LDP.membershipResource, LDP.hasMemberRelation,
                LDP.inbox, LDP.insertedContentRelation, OA.annotationService).collect(toSet()));

    // Verify that the object of a triple whose predicate is either ldp:hasMemberRelation or ldp:isMemberOfRelation
    // is not equal to ldp:contains or any of the other cardinality-restricted IRIs
    private static Predicate<Triple> invalidMembershipProperty = triple ->
        (LDP.hasMemberRelation.equals(triple.getPredicate()) || LDP.isMemberOfRelation.equals(triple.getPredicate())) &&
        restrictedMemberProperties.contains(triple.getObject());

    // Verify that the range of the property is an IRI (if the property is in the above set)
    private static Predicate<Triple> uriRangeFilter = invalidMembershipProperty.or(triple ->
        propertiesWithUriRange.contains(triple.getPredicate()) && !(triple.getObject() instanceof IRI))
        .or(triple -> RDF.type.equals(triple.getPredicate()) && !(triple.getObject() instanceof IRI));

    // Ensure that any LDP properties are appropriate for the interaction model
    private static Predicate<Triple> propertyFilter(final IRI model) {
        return of(model).filter(typeMap::containsKey).map(typeMap::get).orElse(basicConstraints);
    }

    // Verify that the range of the property is in the server's domain
    private static Predicate<Triple> inDomainRangeFilter(final String domain) {
        return triple -> propertiesWithInDomainRange.contains(triple.getPredicate()) &&
            !triple.getObject().ntriplesString().startsWith("<" + domain);
    }

    // Verify that ldp:membershipResource and one of ldp:hasMemberRelation or ldp:isMemberOfRelation is present
    private static boolean hasMembershipProps(final Graph graph) {
        return graph.contains(null, LDP.membershipResource, null)
                && (graph.stream(null, LDP.hasMemberRelation, null).count()
                    + graph.stream(null, LDP.isMemberOfRelation, null).count() == 1L);
    }

    // Verify that the cardinality of the `propertiesWithUriRange` properties. Keep any whose cardinality is > 1
    private static Predicate<Graph> checkCardinality(final IRI model) {
        return graph -> {
            if (LDP.IndirectContainer.equals(model)) {
                if (!graph.contains(null, LDP.insertedContentRelation, null) || !hasMembershipProps(graph)) {
                    return true;
                }
            } else if (LDP.DirectContainer.equals(model) && !hasMembershipProps(graph)) {
                return true;
            }

            return propertiesWithUriRange.stream().anyMatch(p -> graph.stream(null, p, null).count() > 1);
        };
    }

    private Function<Triple, Stream<ConstraintViolation>> checkModelConstraints(final IRI model, final String domain) {
        requireNonNull(model, "The interaction model must not be null!");

        return triple -> {
            final Stream.Builder<ConstraintViolation> builder = Stream.builder();
            of(triple).filter(propertyFilter(model)).map(t -> new ConstraintViolation(Trellis.InvalidProperty, t))
                .ifPresent(builder::accept);

            of(triple).filter(uriRangeFilter).map(t -> new ConstraintViolation(Trellis.InvalidRange, t))
                .ifPresent(builder::accept);

            of(triple).filter(inDomainRangeFilter(domain)).map(t -> new ConstraintViolation(Trellis.InvalidRange, t))
                .ifPresent(builder::accept);

            return builder.build();
        };
    }

    @Override
    public Stream<ConstraintViolation> constrainedBy(final IRI model, final Graph graph, final String domain) {
        return concat(graph.stream().flatMap(checkModelConstraints(model, domain)),
                Stream.of(graph).filter(checkCardinality(model))
                    .map(g -> new ConstraintViolation(Trellis.InvalidCardinality, g.stream().collect(toList()))));
    }
}
