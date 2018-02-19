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
package org.trellisldp.triplestore;

import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.jena.query.DatasetFactory.create;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.triplestore.TriplestoreUtils.getInstance;

import java.time.Instant;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Session;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.vocabulary.XSD;

/**
 * Test the TriplestoreResource class.
 */
@RunWith(JUnitPlatform.class)
public class TriplestoreResourceTest {

    private static final JenaRDF rdf = getInstance();
    private static final IRI root = rdf.createIRI("trellis:");
    private static final IRI identifier = rdf.createIRI("trellis:resource");
    private static final IRI child1 = rdf.createIRI("trellis:resource/child1");
    private static final IRI child2 = rdf.createIRI("trellis:resource/child2");
    private static final IRI child3 = rdf.createIRI("trellis:resource/child3");
    private static final IRI child4 = rdf.createIRI("trellis:resource/child4");
    private static final IRI auditId = rdf.createIRI("trellis:resource?ext=audit");
    private static final IRI aclId = rdf.createIRI("trellis:resource?ext=acl");
    private static final IRI aclSubject = rdf.createIRI("trellis:resource#auth");
    private static final IRI other = rdf.createIRI("trellis:other");

    private static final AuditService auditService = new DefaultAuditService() {};

    private final Instant created = now();

    @Mock
    private Session mockSession;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        when(mockSession.getCreated()).thenReturn(created);
        when(mockSession.getDelegatedBy()).thenReturn(empty());
    }

    @Test
    public void testEmptyResource() {
        final RDFConnection rdfConnection = connect(create());
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertFalse(res.exists());
    }

    @Test
    public void testPartialResource() {
        final String time = "2018-01-12T14:02:00Z";
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertFalse(res.exists());
    }

    @Test
    public void testMinimalResource() {
        final String time = "2018-01-12T14:02:00Z";
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(identifier, identifier, RDF.type, SKOS.Concept);
        dataset.add(identifier, identifier, SKOS.prefLabel, rdf.createLiteral("resource"));
        dataset.add(other, other, SKOS.prefLabel, rdf.createLiteral("other"));
        dataset.add(Trellis.PreferServerManaged, identifier, RDF.type, LDP.RDFSource);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists());
        assertEquals(identifier, res.getIdentifier());
        assertEquals(LDP.RDFSource, res.getInteractionModel());
        assertEquals(parse(time), res.getModified());
        assertFalse(res.getMembershipResource().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.hasAcl());
        assertFalse(res.isDeleted());
        assertEquals(2L, res.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(2L, res.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(4L, res.stream().count());
    }

    @Test
    public void testResourceWithAuditQuads() {
        final String time = "2018-01-12T14:02:00Z";
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(identifier, identifier, RDF.type, SKOS.Concept);
        dataset.add(identifier, identifier, SKOS.prefLabel, rdf.createLiteral("resource"));
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));
        dataset.add(Trellis.PreferServerManaged, identifier, RDF.type, LDP.RDFSource);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists());
        assertEquals(identifier, res.getIdentifier());
        assertEquals(LDP.RDFSource, res.getInteractionModel());
        assertEquals(parse(time), res.getModified());
        assertFalse(res.getMembershipResource().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.hasAcl());
        assertFalse(res.isDeleted());
        assertEquals(2L, res.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(2L, res.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(5L, res.stream(singleton(Trellis.PreferAudit)).count());
        assertEquals(9L, res.stream().count());
    }

    @Test
    public void testResourceWithAclQuads() {
        final String time = "2018-01-12T14:02:00Z";
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(identifier, identifier, RDF.type, SKOS.Concept);
        dataset.add(identifier, identifier, SKOS.prefLabel, rdf.createLiteral("resource"));
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));
        dataset.add(Trellis.PreferServerManaged, identifier, RDF.type, LDP.RDFSource);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(aclId, aclSubject, ACL.mode, ACL.Read);
        dataset.add(aclId, aclSubject, ACL.agentClass, FOAF.Agent);
        dataset.add(aclId, aclSubject, ACL.accessTo, identifier);

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists());
        assertEquals(identifier, res.getIdentifier());
        assertEquals(LDP.RDFSource, res.getInteractionModel());
        assertEquals(parse(time), res.getModified());
        assertFalse(res.getMembershipResource().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertFalse(res.getBinary().isPresent());
        assertTrue(res.hasAcl());
        assertFalse(res.isDeleted());
        assertEquals(2L, res.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(2L, res.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(5L, res.stream(singleton(Trellis.PreferAudit)).count());
        assertEquals(3L, res.stream(singleton(Trellis.PreferAccessControl)).count());
        assertEquals(12L, res.stream().count());
    }

    @Test
    public void testBinaryResource() {
        final String time = "2018-01-12T14:02:00Z";
        final String binaryTime = "2018-01-10T14:02:00Z";
        final String size = "2560";
        final String mimeType = "image/jpeg";
        final IRI binaryIdentifier = rdf.createIRI("file:binary");

        final JenaDataset dataset = rdf.createDataset();
        dataset.add(identifier, identifier, RDF.type, SKOS.Concept);
        dataset.add(identifier, identifier, SKOS.prefLabel, rdf.createLiteral("resource"));
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));
        dataset.add(Trellis.PreferServerManaged, identifier, RDF.type, LDP.NonRDFSource);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, identifier, DC.hasPart, binaryIdentifier);
        dataset.add(Trellis.PreferServerManaged, binaryIdentifier, DC.modified,
                rdf.createLiteral(binaryTime, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, binaryIdentifier, DC.extent, rdf.createLiteral(size, XSD.long_));
        dataset.add(Trellis.PreferServerManaged, binaryIdentifier, DC.format, rdf.createLiteral(mimeType));

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists());
        assertEquals(identifier, res.getIdentifier());
        assertEquals(LDP.NonRDFSource, res.getInteractionModel());
        assertEquals(parse(time), res.getModified());
        assertFalse(res.getMembershipResource().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertTrue(res.getBinary().isPresent());
        res.getBinary().ifPresent(b -> {
            assertEquals(binaryIdentifier, b.getIdentifier());
            assertEquals(parse(binaryTime), b.getModified());
            assertEquals(of(Long.parseLong(size)), b.getSize());
            assertEquals(of(mimeType), b.getMimeType());
        });
        assertFalse(res.hasAcl());
        assertFalse(res.isDeleted());
        assertEquals(6L, res.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(2L, res.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(5L, res.stream(singleton(Trellis.PreferAudit)).count());
        assertEquals(13L, res.stream().count());
    }

    @Test
    public void testResourceWithChildren() {
        final String time = "2018-01-12T14:02:00Z";
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(identifier, identifier, RDF.type, SKOS.Concept);
        dataset.add(identifier, identifier, SKOS.prefLabel, rdf.createLiteral("resource"));
        dataset.add(other, other, SKOS.prefLabel, rdf.createLiteral("other"));
        dataset.add(Trellis.PreferServerManaged, identifier, RDF.type, LDP.Container);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, child1, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child2, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child3, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child4, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists());
        assertEquals(identifier, res.getIdentifier());
        assertEquals(LDP.Container, res.getInteractionModel());
        assertEquals(parse(time), res.getModified());
        assertFalse(res.getMembershipResource().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.hasAcl());
        assertFalse(res.isDeleted());
        assertEquals(3L, res.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(2L, res.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(4L, res.stream(singleton(LDP.PreferContainment)).count());
        assertEquals(9L, res.stream().count());
    }

    @Test
    public void testResourceWithoutChildren() {
        final String time = "2018-01-12T14:02:00Z";
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(identifier, identifier, RDF.type, SKOS.Concept);
        dataset.add(identifier, identifier, SKOS.prefLabel, rdf.createLiteral("resource"));
        dataset.add(other, other, SKOS.prefLabel, rdf.createLiteral("other"));
        dataset.add(Trellis.PreferServerManaged, identifier, RDF.type, LDP.RDFSource);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, child1, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child2, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child3, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child4, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists());
        assertEquals(identifier, res.getIdentifier());
        assertEquals(LDP.RDFSource, res.getInteractionModel());
        assertEquals(parse(time), res.getModified());
        assertFalse(res.getMembershipResource().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.hasAcl());
        assertFalse(res.isDeleted());
        assertEquals(3L, res.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(2L, res.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(0L, res.stream(singleton(LDP.PreferContainment)).count());
        assertEquals(5L, res.stream().count());
    }

    @Test
    public void testDirectContainer() {
        final String time = "2018-01-12T14:02:00Z";
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(identifier, identifier, RDF.type, SKOS.Concept);
        dataset.add(identifier, identifier, SKOS.prefLabel, rdf.createLiteral("resource"));
        dataset.add(other, other, SKOS.prefLabel, rdf.createLiteral("other"));
        dataset.add(Trellis.PreferServerManaged, other, DC.isPartOf, root);
        dataset.add(Trellis.PreferServerManaged, other, RDF.type, LDP.RDFSource);
        dataset.add(Trellis.PreferServerManaged, other, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        dataset.add(Trellis.PreferServerManaged, identifier, RDF.type, LDP.DirectContainer);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.member, other);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.membershipResource, other);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.hasMemberRelation, DC.subject);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.insertedContentRelation, LDP.MemberSubject);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, child1, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child2, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child3, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child4, DC.isPartOf, identifier);

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists());
        assertEquals(identifier, res.getIdentifier());
        assertEquals(LDP.DirectContainer, res.getInteractionModel());
        assertEquals(parse(time), res.getModified());
        assertTrue(res.getMembershipResource().isPresent());
        assertTrue(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertTrue(res.getInsertedContentRelation().isPresent());
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.hasAcl());
        assertFalse(res.isDeleted());
        assertEquals(7L, res.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(2L, res.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(4L, res.stream(singleton(LDP.PreferContainment)).count());
        assertEquals(13L, res.stream().count());

        final TriplestoreResource res2 = new TriplestoreResource(rdfConnection, other);
        res2.fetchData();
        assertTrue(res2.exists());
        assertEquals(other, res2.getIdentifier());
        assertEquals(LDP.RDFSource, res2.getInteractionModel());
        assertEquals(parse(time), res2.getModified());
        assertFalse(res2.getMembershipResource().isPresent());
        assertFalse(res2.getMemberRelation().isPresent());
        assertFalse(res2.getMemberOfRelation().isPresent());
        assertFalse(res2.getInsertedContentRelation().isPresent());
        assertFalse(res2.getBinary().isPresent());
        assertFalse(res2.hasAcl());
        assertFalse(res2.isDeleted());
        assertEquals(3L, res2.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(1L, res2.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(0L, res2.stream(singleton(LDP.PreferContainment)).count());
        assertEquals(4L, res2.stream(singleton(LDP.PreferMembership)).count());
        assertEquals(4L, res2.stream(singleton(LDP.PreferMembership))
                .filter(t -> DC.subject.equals(t.getPredicate())).count());
        assertEquals(8L, res2.stream().count());
    }

    @Test
    public void testIndirectContainer() {
        final String time = "2018-01-12T14:02:00Z";
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(identifier, identifier, RDF.type, SKOS.Concept);
        dataset.add(identifier, identifier, SKOS.prefLabel, rdf.createLiteral("resource"));
        dataset.add(other, other, SKOS.prefLabel, rdf.createLiteral("other"));
        dataset.add(Trellis.PreferServerManaged, other, DC.isPartOf, root);
        dataset.add(Trellis.PreferServerManaged, other, RDF.type, LDP.RDFSource);
        dataset.add(Trellis.PreferServerManaged, other, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        dataset.add(Trellis.PreferServerManaged, identifier, RDF.type, LDP.IndirectContainer);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.member, other);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.membershipResource, other);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.hasMemberRelation, DC.relation);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.insertedContentRelation, DC.subject);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, child1, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child2, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child3, DC.isPartOf, identifier);
        dataset.add(Trellis.PreferServerManaged, child4, DC.isPartOf, identifier);
        dataset.add(child1, child1, DC.subject, rdf.createIRI("http://example.org/1"));
        dataset.add(child2, child2, DC.subject, rdf.createIRI("http://example.org/2"));
        dataset.add(child3, child3, DC.subject, rdf.createIRI("http://example.org/3"));
        dataset.add(child4, child4, DC.subject, rdf.createIRI("http://example.org/4"));

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists());
        assertEquals(identifier, res.getIdentifier());
        assertEquals(LDP.IndirectContainer, res.getInteractionModel());
        assertEquals(parse(time), res.getModified());
        assertTrue(res.getMembershipResource().isPresent());
        assertTrue(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertTrue(res.getInsertedContentRelation().isPresent());
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.hasAcl());
        assertFalse(res.isDeleted());
        assertEquals(7L, res.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(2L, res.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(4L, res.stream(singleton(LDP.PreferContainment)).count());
        assertEquals(13L, res.stream().count());

        final TriplestoreResource res2 = new TriplestoreResource(rdfConnection, other);
        res2.fetchData();
        assertTrue(res2.exists());
        assertEquals(other, res2.getIdentifier());
        assertEquals(LDP.RDFSource, res2.getInteractionModel());
        assertEquals(parse(time), res2.getModified());
        assertFalse(res2.getMembershipResource().isPresent());
        assertFalse(res2.getMemberRelation().isPresent());
        assertFalse(res2.getMemberOfRelation().isPresent());
        assertFalse(res2.getInsertedContentRelation().isPresent());
        assertFalse(res2.getBinary().isPresent());
        assertFalse(res2.hasAcl());
        assertFalse(res2.isDeleted());
        assertEquals(3L, res2.stream(singleton(Trellis.PreferServerManaged)).count());
        assertEquals(1L, res2.stream(singleton(Trellis.PreferUserManaged)).count());
        assertEquals(0L, res2.stream(singleton(LDP.PreferContainment)).count());
        assertEquals(4L, res2.stream(singleton(LDP.PreferMembership)).count());
        assertEquals(4L, res2.stream(singleton(LDP.PreferMembership))
                .filter(t -> DC.relation.equals(t.getPredicate())).count());
        assertEquals(8L, res2.stream().count());
    }
}
