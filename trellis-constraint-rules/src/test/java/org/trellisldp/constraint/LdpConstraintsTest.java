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
import static org.trellisldp.vocabulary.RDF.type;

import java.util.List;
import java.util.Optional;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdf.model.Model;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class LdpConstraintsTest {

    private static final JenaRDF rdf = new JenaRDF();

    private final String domain = "trellis:repository/";

    private final ConstraintService svc = new LdpConstraints();

    private final List<IRI> models = asList(LDP.RDFSource, LDP.NonRDFSource, LDP.Container, LDP.Resource,
            LDP.DirectContainer, LDP.IndirectContainer);

    @Test
    public void testInvalidAccessControlProperty() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type, domain,
                    asGraph("/hasAccessControlTriples.ttl", subject))
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
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type, domain,
                    asGraph("/hasLdpContainsTriples.ttl", subject))
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
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type, domain, asGraph("/hasInsertedContent.ttl",
                        subject)).findFirst();
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
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type, domain, asGraph("/basicContainer.ttl",
                        subject)).findFirst();
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
    public void testInvalidType() {
        models.stream().forEach(ldpType -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(ldpType, domain, asGraph("/withLdpType.ttl",
                        subject)).filter(v -> Trellis.InvalidType.equals(v.getConstraint())).findFirst();
            assertTrue(res.isPresent());
            res.ifPresent(violation -> {
                assertEquals(Trellis.InvalidType, violation.getConstraint());
                assertEquals(1L, violation.getTriples().size());
                assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(subject), type,
                                LDP.Resource)));
            });
        });
    }

    @Test
    public void testInvalidDomain() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final List<ConstraintViolation> res = svc.constrainedBy(type, domain, asGraph("/invalidDomain.ttl",
                        subject)).collect(toList());
            assertFalse(res.isEmpty());
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
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.RDFSource, domain,
                asGraph("/invalidInbox.ttl", domain + "foo")).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.inbox, rdf.createLiteral("invalid range", "en"))));
        });
    }

    @Test
    public void testBasicConstraints1() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.Container, domain,
                asGraph("/invalidContainer1.ttl", domain + "foo")).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidProperty, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.isMemberOfRelation, rdf.createIRI(domain + "resource"))));
        });
    }

    @Test
    public void testBasicConstraints2() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.Container, domain,
                asGraph("/invalidContainer2.ttl", domain + "foo")).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidProperty, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.insertedContentRelation, rdf.createIRI(domain + "resource"))));
        });
    }

    @Test
    public void testBasicConstraints3() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.Container, domain,
                asGraph("/invalidContainer3.ttl", domain + "foo")).findFirst();
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
                domain, asGraph("/invalidMembershipTriple.ttl", domain + "foo"))
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
                domain, asGraph("/invalidMembershipTriple2.ttl", domain + "foo")).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint());
            assertTrue(violation.getTriples().contains(rdf.createTriple(rdf.createIRI(domain + "foo"),
                            LDP.isMemberOfRelation, LDP.contains)));
        });
    }

    @Test
    public void testCardinality() {
        models.stream().forEach(type -> {
            final String subject = domain + "foo";
            final Optional<ConstraintViolation> res = svc.constrainedBy(type, domain, asGraph("/invalidCardinality.ttl",
                        subject)).findFirst();
            assertTrue(res.isPresent());
            res.ifPresent(violation -> {
                assertEquals(Trellis.InvalidCardinality, violation.getConstraint());
            });
        });
    }

    @Test
    public void testInvalidType2() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.RDFSource, domain,
                asGraph("/invalidType.ttl", domain + "foo"))
            .filter(v -> v.getConstraint().equals(Trellis.InvalidRange)).findFirst();
        assertTrue(res.isPresent());
        res.ifPresent(violation -> {
            assertEquals(Trellis.InvalidRange, violation.getConstraint());
        });
    }


    @Test
    public void testTooManyMembershipTriples() {
        final Optional<ConstraintViolation> res = svc.constrainedBy(LDP.IndirectContainer, domain,
                asGraph("/tooManyMembershipTriples.ttl", domain + "foo")).findFirst();
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
