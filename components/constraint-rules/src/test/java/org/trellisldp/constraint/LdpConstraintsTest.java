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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.Lang.TURTLE;
import static org.apache.jena.riot.RDFDataMgr.read;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class LdpConstraintsTest {

    private static final JenaRDF rdf = new JenaRDF();

    private final String domain = "trellis:repository/";

    private final ConstraintService svc = new LdpConstraints();

    private final List<IRI> models = asList(LDP.RDFSource, LDP.NonRDFSource, LDP.Container, LDP.Resource,
            LDP.DirectContainer, LDP.IndirectContainer);

    @Test
    public void testInvalidAccessControlProperty() {
        assertTrue(models.stream()
                .map(type -> svc.constrainedBy(type, asGraph("/hasAccessControlTriples.ttl", domain + "foo"), domain)
                    .filter(v -> v.getConstraint().equals(Trellis.InvalidProperty)).findFirst().isPresent())
                .reduce(true, (acc, x) -> acc && x));

        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type,
                    asGraph("/hasAccessControlTriples.ttl", subject), domain)
                .filter(v -> v.getConstraint().equals(Trellis.InvalidProperty)).findFirst();
            assertTrue(res.isPresent());
            res.ifPresent(violation -> {
                assertEquals(Trellis.InvalidProperty, violation.getConstraint());
                assertTrue(violation.getTriples().contains(
                        rdf.createTriple(rdf.createIRI(subject), ACL.accessControl, rdf.createIRI(domain + "bar"))));
            });
        });
    }

    @Test
    public void testInvalidContainsProperty() {
        assertTrue(models.stream()
                .map(type -> svc.constrainedBy(type, asGraph("/hasLdpContainsTriples.ttl", domain + "foo"), domain)
                    .filter(v -> v.getConstraint().equals(Trellis.InvalidProperty)).findFirst().isPresent())
                .reduce(true, (acc, x) -> acc && x));

        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type,
                    asGraph("/hasLdpContainsTriples.ttl", subject), domain)
                .filter(v -> v.getConstraint().equals(Trellis.InvalidProperty)).findFirst();
            assertTrue(res.isPresent());
            res.ifPresent(violation -> {
                assertEquals(Trellis.InvalidProperty, violation.getConstraint());
                assertTrue(violation.getTriples().contains(
                            rdf.createTriple(rdf.createIRI(subject), LDP.contains, rdf.createIRI(domain + "bar"))));
            });
        });
    }

    @Test
    public void testInvalidInsertedContentRelation() {
        final List<IRI> found = models.stream().filter(type ->
                svc.constrainedBy(type, asGraph("/hasInsertedContent.ttl", domain + "foo"), domain)
                .findFirst().isPresent()).collect(toList());

        assertTrue(found.contains(LDP.Container));
        assertFalse(found.contains(LDP.DirectContainer));
        assertFalse(found.contains(LDP.IndirectContainer));

        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type, asGraph("/hasInsertedContent.ttl",
                        subject), domain).findFirst();
            if (type.equals(LDP.IndirectContainer) || type.equals(LDP.DirectContainer)) {
                assertFalse(res.isPresent());
            } else {
                assertTrue(res.isPresent());
                res.ifPresent(violation -> {
                    assertEquals(Trellis.InvalidProperty, violation.getConstraint());
                    assertTrue(violation.getTriples().contains(
                            rdf.createTriple(rdf.createIRI(subject), LDP.hasMemberRelation, DC.isPartOf)));
                });
            }
        });
    }

    @Test
    public void testInvalidLdpProps() {
        final List<IRI> found = models.stream().filter(type ->
                svc.constrainedBy(type, asGraph("/basicContainer.ttl", domain + "foo"), domain).findFirst().isPresent())
            .collect(toList());

        assertTrue(found.contains(LDP.Container));
        assertFalse(found.contains(LDP.DirectContainer));
        assertFalse(found.contains(LDP.IndirectContainer));

        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type, asGraph("/basicContainer.ttl",
                        subject), domain).findFirst();
            if (type.equals(LDP.DirectContainer) || type.equals(LDP.IndirectContainer)) {
                assertFalse(res.isPresent());
            } else {
                assertTrue(res.isPresent());
                res.ifPresent(violation -> {
                    assertEquals(Trellis.InvalidProperty, violation.getConstraint());
                    assertTrue(violation.getTriples().contains(
                            rdf.createTriple(rdf.createIRI(subject), LDP.hasMemberRelation, DC.isPartOf)));
                });
            }
        });
    }

    @Test
    public void testLdpType() {
        assertFalse(models.stream().map(ldpType ->
            svc.constrainedBy(ldpType, asGraph("/withLdpType.ttl", domain + "foo"), domain).filter(v ->
                        Trellis.InvalidType.equals(v.getConstraint())).findFirst().isPresent())
                .reduce(false, (acc, x) -> acc || x));
    }

    @Test
    public void testInvalidDomain() {
        assertEquals(0L, models.stream()
                .filter(type -> !svc.constrainedBy(type, asGraph("/invalidDomain.ttl", domain + "foo"), domain)
                    .findAny().isPresent())
                .count());

        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final List<ConstraintViolation> res = svc.constrainedBy(type, asGraph("/invalidDomain.ttl",
                        subject), domain).collect(toList());
            if (type.equals(LDP.DirectContainer) || type.equals(LDP.IndirectContainer)) {
                final Optional<ConstraintViolation> violation = res.stream()
                    .filter(v -> v.getConstraint().equals(Trellis.InvalidRange)).findFirst();
                assertTrue(violation.isPresent());
                assertEquals(Trellis.InvalidRange, violation.get().getConstraint());
                assertTrue(violation.get().getTriples().contains(rdf.createTriple(rdf.createIRI(subject),
                            LDP.membershipResource, DC.subject)));
            } else {
                final Optional<ConstraintViolation> violation = res.stream()
                    .filter(v -> v.getConstraint().equals(Trellis.InvalidProperty)).findFirst();
                assertTrue(violation.isPresent());
                assertEquals(Trellis.InvalidProperty, violation.get().getConstraint());
                assertTrue(violation.get().getTriples().contains(rdf.createTriple(rdf.createIRI(subject),
                            LDP.hasMemberRelation, DC.creator)));
            }
        });
    }

    @Test
    public void testInvalidInbox() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.RDFSource,
                asGraph("/invalidInbox.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.inbox, rdf.createLiteral("invalid range", "en"))));
        });
    }

    @Test
    public void testBasicConstraints1() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.Container,
                asGraph("/invalidContainer1.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidProperty, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.isMemberOfRelation, rdf.createIRI(domain + "resource"))));
        });
    }

    @Test
    public void testBasicConstraints2() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.Container,
                asGraph("/invalidContainer2.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidProperty, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.insertedContentRelation, rdf.createIRI(domain + "resource"))));
        });
    }

    @Test
    public void testBasicConstraints3() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.Container,
                asGraph("/invalidContainer3.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidProperty, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.membershipResource, rdf.createIRI(domain + "resource"))));
        });
    }

    @Test
    public void testMembershipTriples1() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.IndirectContainer,
                asGraph("/invalidMembershipTriple.ttl", domain + "foo"), domain)
            .filter(v -> v.getConstraint().equals(Trellis.InvalidRange)).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.hasMemberRelation, LDP.contains)));
        });
    }

    @Test
    public void testMembershipTriples2() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.DirectContainer,
                asGraph("/invalidMembershipTriple2.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.isMemberOfRelation, LDP.contains)));
        });
    }

    @Test
    public void testCardinality() {
        assertEquals(0L, models.stream()
                .filter(type -> !svc.constrainedBy(type, asGraph("/invalidCardinality.ttl", domain + "foo"), domain)
                    .findFirst().isPresent())
                .count());

        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type, asGraph("/invalidCardinality.ttl",
                        subject), domain).findFirst();
            assertTrue(res.isPresent());
            res.ifPresent(violation -> {
                assertEquals(Trellis.InvalidCardinality, violation.getConstraint());
            });
        });
    }

    @Test
    public void testInvalidType2() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.RDFSource,
                asGraph("/invalidType.ttl", domain + "foo"), domain)
            .filter(v -> v.getConstraint().equals(Trellis.InvalidRange)).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint());
        });
    }


    @Test
    public void testTooManyMembershipTriples() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.IndirectContainer,
                asGraph("/tooManyMembershipTriples.ttl", domain + "foo"), domain).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidCardinality, violation.getConstraint());
        });
    }

    private Graph asGraph(final String resource, final String context) {
        final Model model = createDefaultModel();
        read(model, getClass().getResourceAsStream(resource), context, TURTLE);
        return rdf.asGraph(model);
    }
}
