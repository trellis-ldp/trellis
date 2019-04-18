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
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final Set<IRI> propertiesWithInDomainRange = singleton(LDP.membershipResource);

    // Properties that need to be used with objects that are IRIs
    private static final List<IRI> propertiesWithUriRange = unmodifiableList(
                    Arrays.asList(LDP.membershipResource, LDP.hasMemberRelation, LDP.isMemberOfRelation, LDP.inbox,
                                    LDP.insertedContentRelation, OA.annotationService));

    // Properties that cannot be used as dynamic Membership properties
    private static final List<IRI> restrictedMemberProperties = unmodifiableList(Arrays.asList(ACL.accessControl,
                    LDP.contains, RDF.type, LDP.membershipResource, LDP.hasMemberRelation, LDP.inbox,
                    LDP.insertedContentRelation, OA.annotationService));

    private static final Map<IRI, Predicate<Triple>> typeMap;

    static {

        final Map<IRI, Predicate<Triple>> typeMapElements = new HashMap<>();

        typeMapElements.put(LDP.BasicContainer, LdpConstraints::basicConstraints);
        typeMapElements.put(LDP.Container, LdpConstraints::basicConstraints);
        typeMapElements.put(LDP.DirectContainer, LdpConstraints::memberContainerConstraints);
        typeMapElements.put(LDP.IndirectContainer, LdpConstraints::memberContainerConstraints);
        typeMapElements.put(LDP.NonRDFSource, LdpConstraints::basicConstraints);
        typeMapElements.put(LDP.RDFSource, LdpConstraints::basicConstraints);

        typeMap = unmodifiableMap(typeMapElements);
    }

    // Identify those predicates that are prohibited in the given ixn model
    private static boolean memberContainerConstraints(final Triple triple) {
        return triple.getPredicate().equals(ACL.accessControl) || triple.getPredicate().equals(LDP.contains);
    }

    // Identify those predicates that are prohibited in the given ixn model
    private static boolean basicConstraints(final Triple triple) {
        return memberContainerConstraints(triple) || triple.getPredicate().equals(LDP.insertedContentRelation)
                        || triple.getPredicate().equals(LDP.membershipResource)
                        || triple.getPredicate().equals(LDP.hasMemberRelation)
                        || triple.getPredicate().equals(LDP.isMemberOfRelation);
    }

    // Verify that the object of a triple whose predicate is either ldp:hasMemberRelation or ldp:isMemberOfRelation
    // is not equal to ldp:contains or any of the other cardinality-restricted IRIs
    private static boolean invalidMembershipProperty(final Triple triple) {
        return (LDP.hasMemberRelation.equals(triple.getPredicate())
                        || LDP.isMemberOfRelation.equals(triple.getPredicate()))
                        && restrictedMemberProperties.contains(triple.getObject());
    }

    // Verify that the range of the property is an IRI (if the property is in the above set)
    private static boolean uriRangeFilter(final Triple triple) {
        return invalidMembershipProperty(triple)
                        || (propertiesWithUriRange.contains(triple.getPredicate())
                                        && !(triple.getObject() instanceof IRI))
                        || (RDF.type.equals(triple.getPredicate()) && !(triple.getObject() instanceof IRI));
    }

    // Ensure that any LDP properties are appropriate for the interaction model
    private static boolean propertyFilter(final Triple t, final IRI ixnModel) {
        return typeMap.getOrDefault(ixnModel, LdpConstraints::basicConstraints).test(t);
    }

    // Verify that the range of the property is in the server's domain
    private static boolean inDomainRangeFilter(final Triple triple, final String domain) {
        return propertiesWithInDomainRange.contains(triple.getPredicate())
                        && !triple.getObject().ntriplesString().startsWith("<" + domain);
    }

    // Verify that ldp:membershipResource and one of ldp:hasMemberRelation or ldp:isMemberOfRelation is present
    private static boolean hasMembershipProps(final Graph graph) {
        return graph.contains(null, LDP.membershipResource, null)
                        && (graph.stream(null, LDP.hasMemberRelation, null).count()
                                        + graph.stream(null, LDP.isMemberOfRelation, null).count() == 1L);
    }

    // Verify that the cardinality of the `propertiesWithUriRange` properties. Keep any whose cardinality is > 1
    private static boolean violatesCardinality(final Graph graph, final IRI model) {
        final boolean isIndirect = LDP.IndirectContainer.equals(model);
        return isIndirect && (!graph.contains(null, LDP.insertedContentRelation, null) || !hasMembershipProps(graph))
                        || !isIndirect && LDP.DirectContainer.equals(model) && !hasMembershipProps(graph)
                        || propertiesWithUriRange.stream().anyMatch(p -> graph.stream(null, p, null).count() > 1);
    }

    private List<ConstraintViolation> violatesModelConstraints(final Triple triple, final IRI model,
                    final String domain) {
        requireNonNull(model, "The interaction model must not be null!");
        final List<ConstraintViolation> violations = new ArrayList<>();
        if (propertyFilter(triple, model)) {
            violations.add(new ConstraintViolation(Trellis.InvalidProperty, triple));
        }
        if (uriRangeFilter(triple)) {
            violations.add(new ConstraintViolation(Trellis.InvalidRange, triple));
        }
        if (inDomainRangeFilter(triple, domain)) {
            violations.add(new ConstraintViolation(Trellis.InvalidRange, triple));
        }
        return violations;
    };

    @Override
    public Stream<ConstraintViolation> constrainedBy(final IRI model, final Graph graph, final String domain) {
        Stream<ConstraintViolation> violations = graph.stream()
                        .flatMap(t -> violatesModelConstraints(t, model, domain).stream());
        if (violatesCardinality(graph, model)) {
            final ConstraintViolation cardinalityViolation = new ConstraintViolation(Trellis.InvalidCardinality,
                            graph.stream().collect(toList()));
            violations = concat(violations, Stream.of(cardinalityViolation));
        }
        return violations;
    }
}
