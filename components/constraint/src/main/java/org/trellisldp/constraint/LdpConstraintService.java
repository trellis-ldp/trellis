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
package org.trellisldp.constraint;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.trellisldp.vocabulary.RDF.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;
import org.trellisldp.vocabulary.RDFS;
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
@ApplicationScoped
public class LdpConstraintService implements ConstraintService {

    private static final String EN = "en";
    private static final RDF rdf = RDFFactory.getInstance();
    private static final Set<IRI> propertiesWithInDomainRange = singleton(LDP.membershipResource);

    // Properties that need to be used with objects that are IRIs
    private static final List<IRI> propertiesWithUriRange = unmodifiableList(
                    Arrays.asList(LDP.membershipResource, LDP.hasMemberRelation, LDP.isMemberOfRelation, LDP.inbox,
                                    LDP.insertedContentRelation, OA.annotationService));

    // Properties that cannot be used as dynamic Membership properties
    private static final List<IRI> restrictedMemberProperties = unmodifiableList(Arrays.asList(ACL.accessControl,
                    LDP.contains, type, LDP.membershipResource, LDP.hasMemberRelation, LDP.inbox,
                    LDP.insertedContentRelation, OA.annotationService));

    private static final Map<IRI, Predicate<Triple>> typeMap;

    static {

        final Map<IRI, Predicate<Triple>> typeMapElements = new HashMap<>();

        typeMapElements.put(LDP.BasicContainer, LdpConstraintService::basicConstraints);
        typeMapElements.put(LDP.Container, LdpConstraintService::basicConstraints);
        typeMapElements.put(LDP.DirectContainer, LdpConstraintService::memberContainerConstraints);
        typeMapElements.put(LDP.IndirectContainer, LdpConstraintService::memberContainerConstraints);
        typeMapElements.put(LDP.NonRDFSource, LdpConstraintService::basicConstraints);
        typeMapElements.put(LDP.RDFSource, LdpConstraintService::basicConstraints);

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
                        || (type.equals(triple.getPredicate()) && !(triple.getObject() instanceof IRI));
    }

    // Ensure that any LDP properties are appropriate for the interaction model
    private static boolean propertyFilter(final Triple t, final IRI ixnModel) {
        return typeMap.getOrDefault(ixnModel, LdpConstraintService::basicConstraints).test(t);
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
        final BlankNode bnode = rdf.createBlankNode();
        final List<ConstraintViolation> violations = new ArrayList<>();
        final List<Triple> triples = new ArrayList<>();
        triples.add(triple);
        if (propertyFilter(triple, model)) {
            triples.add(rdf.createTriple(bnode, RDFS.label, rdf.createLiteral("Invalid property", EN)));
            triples.add(rdf.createTriple(bnode, RDFS.comment, rdf.createLiteral(
                            "The supplied RDF triple contained a property that was not accepted "
                            + "for the provided interaction model.", EN)));
            violations.add(new ConstraintViolation(Trellis.InvalidProperty, triples));
        }
        if (uriRangeFilter(triple)) {
            triples.add(rdf.createTriple(bnode, RDFS.label, rdf.createLiteral("Invalid object value", EN)));
            triples.add(rdf.createTriple(bnode, RDFS.comment, rdf.createLiteral(
                            "The supplied RDF triple contained object value that was not accepted. "
                            + "Typically, this means that a literal was provided where an IRI is required.", EN)));
            violations.add(new ConstraintViolation(Trellis.InvalidRange, triples));
        }
        if (inDomainRangeFilter(triple, domain)) {
            triples.add(rdf.createTriple(bnode, RDFS.label, rdf.createLiteral("Invalid domain", EN)));
            triples.add(rdf.createTriple(bnode, RDFS.comment, rdf.createLiteral(
                            "The supplied RDF triple contained an out-of-domain IRI value where an "
                            + "in-domain IRI value was expected.", EN)));
            violations.add(new ConstraintViolation(Trellis.InvalidRange, triples));
        }
        return violations;
    }

    @Override
    public Stream<ConstraintViolation> constrainedBy(final IRI identifier, final IRI model, final Graph graph,
            final String domain) {
        final Stream<ConstraintViolation> violations = graph.stream()
                        .flatMap(t -> violatesModelConstraints(t, model, domain).stream());
        if (violatesCardinality(graph, model)) {
            final List<Triple> triples = new ArrayList<>(graph.stream().collect(toList()));
            final BlankNode bnode = rdf.createBlankNode();
            triples.add(rdf.createTriple(bnode, RDFS.label, rdf.createLiteral("Invalid cardinality", EN)));
            triples.add(rdf.createTriple(bnode, RDFS.comment, rdf.createLiteral(
                        "The supplied graph contains at least one property with an invalid cardinality "
                        + "for the provided interaction model", EN)));
            final ConstraintViolation cardinalityViolation = new ConstraintViolation(Trellis.InvalidCardinality,
                    triples);
            return concat(violations, Stream.of(cardinalityViolation));
        }
        return violations;
    }
}
