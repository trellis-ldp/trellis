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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.Lang.TURTLE;
import static org.apache.jena.riot.RDFDataMgr.read;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
class LdpConstraintServiceTest {

    private static final JenaRDF rdf = new JenaRDF();

    private final String domain = "trellis:data/";
    private final IRI identifier = rdf.createIRI(domain + "resource");
    private final ConstraintService svc = new LdpConstraintService();
    private final List<IRI> models = asList(LDP.RDFSource, LDP.NonRDFSource, LDP.Container, LDP.Resource,
            LDP.DirectContainer, LDP.IndirectContainer);

    @Test
    void testInvalidAccessControlProperty() {
        assertTrue(models.stream()
                .map(type ->
                    svc.constrainedBy(identifier, type, asGraph("/hasAccessControlTriples.ttl", domain + "foo"), domain)
                       .anyMatch(v -> v.getConstraint().equals(Trellis.InvalidProperty)))
                .reduce(true, (acc, x) -> acc && x), "InvalidProperty constraint not found!");

        models.forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, type,
                    asGraph("/hasAccessControlTriples.ttl", subject), domain)
                .filter(v -> v.getConstraint().equals(Trellis.InvalidProperty)).findFirst();
            assertTrue(res.isPresent(), "Constraint violation not found!");
            res.ifPresent(violation -> {
                assertEquals(Trellis.InvalidProperty, violation.getConstraint(), "no InvalidProperty violation!");
                assertTrue(violation.getTriples().contains(
                        rdf.createTriple(rdf.createIRI(subject), ACL.accessControl, rdf.createIRI(domain + "bar"))),
                        "Triple missing from violation!");
            });
        });
    }

    @Test
    void testInvalidContainsProperty() {
        assertTrue(models.stream()
                .map(type -> svc.constrainedBy(identifier, type, asGraph("/hasLdpContainsTriples.ttl", domain + "foo"),
                        domain).anyMatch(v -> v.getConstraint().equals(Trellis.InvalidProperty)))
                .reduce(true, (acc, x) -> acc && x), "InvalidProperty constring not found!");

        models.forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, type,
                    asGraph("/hasLdpContainsTriples.ttl", subject), domain)
                .filter(v -> v.getConstraint().equals(Trellis.InvalidProperty)).findFirst();
            assertTrue(res.isPresent(), "Constraint violation not found!");
            res.ifPresent(violation -> {
                assertEquals(Trellis.InvalidProperty, violation.getConstraint(), "no InvalidProperty violation!");
                assertTrue(violation.getTriples().contains(
                        rdf.createTriple(rdf.createIRI(subject), LDP.contains, rdf.createIRI(domain + "bar"))),
                        "Triple missing from violation!");
            });
        });
    }

    @Test
    void testInvalidInsertedContentRelation() {
        final List<IRI> found = models.stream()
            .filter(type -> svc.constrainedBy(identifier, type, asGraph("/hasInsertedContent.ttl", domain + "foo"),
                        domain).findFirst().isPresent())
            .collect(toList());

        assertTrue(found.contains(LDP.Container), "ldp:Container not present!");
        assertFalse(found.contains(LDP.DirectContainer), "ldp:DirectContainer not expected!");
        assertFalse(found.contains(LDP.IndirectContainer), "ldp:IndirectContainer not expected!");

        models.forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, type,
                    asGraph("/hasInsertedContent.ttl", subject), domain).findFirst();
            if (type.equals(LDP.IndirectContainer) || type.equals(LDP.DirectContainer)) {
                assertFalse(res.isPresent(), "constraint violation not expected for " + type);
            } else {
                assertTrue(res.isPresent(), "constraint violation not found for " + type);
                res.ifPresent(violation -> {
                    assertEquals(Trellis.InvalidProperty, violation.getConstraint(), "no InvalidProperty violation!");
                    assertTrue(violation.getTriples().contains(
                            rdf.createTriple(rdf.createIRI(subject), LDP.hasMemberRelation, DC.isPartOf)),
                            "Triple missing from violation!");
                });
            }
        });
    }

    @Test
    void testInvalidLdpProps() {
        final List<IRI> found = models.stream()
            .filter(type -> svc.constrainedBy(identifier, type, asGraph("/basicContainer.ttl", domain + "foo"), domain)
                    .findFirst().isPresent())
            .collect(toList());

        assertTrue(found.contains(LDP.Container), "ldp:Container not present!");
        assertFalse(found.contains(LDP.DirectContainer), "ldp:DirectContainer not expected!");
        assertFalse(found.contains(LDP.IndirectContainer), "ldp:IndirectContainer not expected!");

        models.forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, type, asGraph("/basicContainer.ttl",
                        subject), domain).findFirst();
            if (type.equals(LDP.DirectContainer) || type.equals(LDP.IndirectContainer)) {
                assertFalse(res.isPresent(), "constraint violation not expected for " + type);
            } else {
                assertTrue(res.isPresent(), "constraint violation not found for " + type);
                res.ifPresent(violation -> {
                    assertEquals(Trellis.InvalidProperty, violation.getConstraint(), "no InvalidProperty violation!");
                    assertTrue(violation.getTriples().contains(
                            rdf.createTriple(rdf.createIRI(subject), LDP.hasMemberRelation, DC.isPartOf)),
                            "Triple missing from violation!");
                });
            }
        });
    }

    @Test
    void testLdpType() {
        assertFalse(models.stream().map(ldpType ->
            svc.constrainedBy(identifier, ldpType, asGraph("/withLdpType.ttl", domain + "foo"), domain).anyMatch(v ->
                        Trellis.InvalidType.equals(v.getConstraint())))
                .reduce(false, (acc, x) -> acc || x), "Unexpected constraint violation!");
    }

    @Test
    void testInvalidDomain() {
        assertEquals(0L, models.stream()
                .filter(type -> !svc.constrainedBy(identifier, type, asGraph("/invalidDomain.ttl", domain + "foo"),
                        domain).findAny().isPresent())
                .count(), "Unexpected InvalidDomain violation!");

        models.forEach(type -> {
            final String subject = domain + "foo";
            final List<ConstraintViolation> res = svc.constrainedBy(identifier, type, asGraph("/invalidDomain.ttl",
                        subject), domain).collect(toList());
            if (type.equals(LDP.DirectContainer) || type.equals(LDP.IndirectContainer)) {
                final Optional<ConstraintViolation> violation = res.stream()
                    .filter(v -> v.getConstraint().equals(Trellis.InvalidRange)).findFirst();
                assertTrue(violation.isPresent(), "no constraint violation for " + type);
                assertEquals(Trellis.InvalidRange, violation.get().getConstraint(), "no InvalidRange violation!");
                assertTrue(violation.get().getTriples().contains(rdf.createTriple(rdf.createIRI(subject),
                            LDP.membershipResource, DC.subject)), "Triple missing from violation!");
            } else {
                final Optional<ConstraintViolation> violation = res.stream()
                    .filter(v -> v.getConstraint().equals(Trellis.InvalidProperty)).findFirst();
                assertTrue(violation.isPresent(), "no constraint violation for " + type);
                assertEquals(Trellis.InvalidProperty, violation.get().getConstraint(), "no InvalidProperty violation!");
                assertTrue(violation.get().getTriples().contains(rdf.createTriple(rdf.createIRI(subject),
                            LDP.hasMemberRelation, DC.creator)), "Triple missing from violation!");
            }
        });
    }

    @Test
    void testInvalidInbox() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, LDP.RDFSource,
                asGraph("/invalidInbox.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent(), "no constraint violation found!");
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint(), "no InvalidRange constraint!");
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.inbox, rdf.createLiteral("invalid range", "en"))),
                    "Triple not found in constraint violation!");
        });
    }

    @Test
    void testBasicConstraints1() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, LDP.Container,
                asGraph("/invalidContainer1.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent(), "no constraint violation found!");
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidProperty, violation.getConstraint(), "no InvalidProperty violation!");
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.isMemberOfRelation, rdf.createIRI(domain + "resource"))),
                    "Triple not found in constraint violation!");
        });
    }

    @Test
    void testBasicConstraints2() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, LDP.Container,
                asGraph("/invalidContainer2.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent(), "no constraint violation found!");
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidProperty, violation.getConstraint(), "no InvalidProperty violation!");
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.insertedContentRelation, rdf.createIRI(domain + "resource"))),
                    "Expected triple not found in violation!");
        });
    }

    @Test
    void testBasicConstraints3() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, LDP.Container,
                asGraph("/invalidContainer3.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent(), "no constraint violation found!");
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidProperty, violation.getConstraint(), "no InvalidProperty violation!");
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.membershipResource, rdf.createIRI(domain + "resource"))),
                    "Expected triple not found in violation!");
        });
    }

    @Test
    void testMembershipTriples1() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, LDP.IndirectContainer,
                asGraph("/invalidMembershipTriple.ttl", domain + "foo"), domain)
            .filter(v -> v.getConstraint().equals(Trellis.InvalidRange)).findFirst();
        assertTrue(res.isPresent(), "no constraint violation found!");
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint(), "no InvalidRange violation!");
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.hasMemberRelation, LDP.contains)), "Expected triple not found in violation!");
        });
    }

    @Test
    void testMembershipTriples2() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, LDP.DirectContainer,
                asGraph("/invalidMembershipTriple2.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent(), "no constraint violation found!");
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint(), "no InvalidRange violation found!");
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.isMemberOfRelation, LDP.contains)), "Expected triple not found in violation!");
        });
    }

    @Test
    void testCardinality() {
        assertEquals(0L, models.stream()
                .filter(type -> !svc.constrainedBy(identifier, type, asGraph("/invalidCardinality.ttl", domain + "foo"),
                        domain).findFirst().isPresent())
                .count(), "unexpected constraint violation found!");

        models.forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, type,
                    asGraph("/invalidCardinality.ttl", subject), domain).findFirst();
            assertTrue(res.isPresent(), "no constraint violation found for " + type);
            res.ifPresent(violation ->
                assertEquals(Trellis.InvalidCardinality, violation.getConstraint(), "no InvalidCardinality violation"));
        });
    }

    @Test
    void testInvalidType2() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, LDP.RDFSource,
                asGraph("/invalidType.ttl", domain + "foo"), domain)
            .filter(v -> v.getConstraint().equals(Trellis.InvalidRange)).findFirst();
        assertTrue(res.isPresent(), "no constraint violation found!");
        res.ifPresent(violation ->
            assertEquals(Trellis.InvalidRange, violation.getConstraint(), "no InvalidRange violation!"));
    }


    @Test
    void testTooManyMembershipTriples() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(identifier, LDP.IndirectContainer,
                asGraph("/tooManyMembershipTriples.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent(), "no constraint violation found!");
        res.ifPresent(violation ->
            assertEquals(Trellis.InvalidCardinality, violation.getConstraint(), "no InvalidCardinality violation!"));
    }

    private Graph asGraph(final String resource, final String context) {
        final Model model = createDefaultModel();
        read(model, getClass().getResourceAsStream(resource), context, TURTLE);
        return rdf.asGraph(model);
    }
}
