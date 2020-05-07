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
package org.trellisldp.triplestore;

import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.function.Predicate.isEqual;
import static org.apache.jena.query.DatasetFactory.create;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.triplestore.TriplestoreUtils.getInstance;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Resource;
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
class TriplestoreResourceTest {

    private static final JenaRDF rdf = getInstance();
    private static final IRI root = rdf.createIRI("trellis:");
    private static final IRI identifier = rdf.createIRI("trellis:data/resource");
    private static final IRI child1 = rdf.createIRI("trellis:data/resource/child1");
    private static final IRI child2 = rdf.createIRI("trellis:data/resource/child2");
    private static final IRI child3 = rdf.createIRI("trellis:data/resource/child3");
    private static final IRI child4 = rdf.createIRI("trellis:data/resource/child4");
    private static final IRI auditId = rdf.createIRI("trellis:data/resource?ext=audit");
    private static final IRI aclId = rdf.createIRI("trellis:data/resource?ext=acl");
    private static final IRI aclSubject = rdf.createIRI("trellis:data/resource#auth");
    private static final IRI member = rdf.createIRI("trellis:data/member");
    private static final String time = "2018-01-12T14:02:00Z";
    private static final IRI fooGraph = rdf.createIRI("https://example.com/Foo");
    private static final IRI barGraph = rdf.createIRI("https://example.com/Bar");

    private static final AuditService auditService = new DefaultAuditService() {};

    private final Instant created = now();
    private final Map<String, IRI> extensions = singletonMap("acl", Trellis.PreferAccessControl);

    @Mock
    private Session mockSession;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        when(mockSession.getCreated()).thenReturn(created);
        when(mockSession.getDelegatedBy()).thenReturn(empty());
    }

    @Test
    void testEmptyResource() {
        final TriplestoreResource res = new TriplestoreResource(connect(create()), identifier, extensions, false);
        res.fetchData();
        assertFalse(res.exists(), "Unexpected resource!");
    }

    @Test
    void testPartialResource() {
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier, extensions, false);
        res.fetchData();
        assertFalse(res.exists(), "Unexpected resource!");
    }

    @Test
    void testMinimalResource() {
        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier, extensions, false);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, false, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 0L, 0L, 0L, 0L));
    }

    @Test
    void testResourceWithAuditQuads() {
        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier, extensions, false);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, false, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 0L, 5L, 0L, 0L));
    }

    @Test
    void testResourceWithAuditQuads2() {
        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier, extensions, true);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, false, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 1L, 0L, 5L, 0L, 0L));
    }

    @Test
    void testResourceWithAclQuads() {
        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        dataset.add(aclId, aclSubject, ACL.mode, ACL.Read);
        dataset.add(aclId, aclSubject, ACL.agentClass, FOAF.Agent);
        dataset.add(aclId, aclSubject, ACL.accessTo, identifier);
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier, extensions, false);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, true, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 3L, 5L, 0L, 0L));
    }

    @Test
    void testResourceWithExtensionQuads() {
        final IRI fooId = rdf.createIRI(identifier.getIRIString() + "?ext=foo");
        final Map<String, IRI> ext = new HashMap<>();
        ext.put("acl", Trellis.PreferAccessControl);
        ext.put("foo", fooGraph);
        ext.put("bar", barGraph);

        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        dataset.add(fooId, identifier, DC.references, rdf.createIRI("https://example.com/Resource"));
        dataset.add(aclId, aclSubject, ACL.mode, ACL.Read);
        dataset.add(aclId, aclSubject, ACL.agentClass, FOAF.Agent);
        dataset.add(aclId, aclSubject, ACL.accessTo, identifier);
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier, ext, false);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        res.stream().forEach(quad -> System.out.println(quad));
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, true, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream",
                () -> assertEquals(0L, res.stream(singleton(Trellis.PreferServerManaged)).count(),
                                   "Incorrect server managed triple count!"),
                () -> assertEquals(2L, res.stream(singleton(Trellis.PreferUserManaged)).count(),
                                   "Incorrect user managed triple count!"),
                () -> assertEquals(3L, res.stream(singleton(Trellis.PreferAccessControl)).count(),
                                   "Incorrect acl triple count!"),
                () -> assertEquals(5L, res.stream(singleton(Trellis.PreferAudit)).count(),
                                   "Incorrect audit triple count!"),
                () -> assertEquals(1L, res.stream(singleton(fooGraph)).count(),
                                   "Incorrect extension triple count!"),
                () -> assertEquals(11L, res.stream().count(), "Incorrect total triple count!"));
    }

    @Test
    void testBinaryResource() {
        final String mimeType = "image/jpeg";
        final IRI binaryIdentifier = rdf.createIRI("file:///binary");
        final JenaDataset dataset = buildLdpDataset(LDP.NonRDFSource);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.hasPart, binaryIdentifier);
        dataset.add(Trellis.PreferServerManaged, binaryIdentifier, DC.format, rdf.createLiteral(mimeType));
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));

        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier, extensions, true);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        res.getBinaryMetadata().ifPresent(b -> {
            assertEquals(binaryIdentifier, b.getIdentifier(), "Incorrect binary identifier!");
            assertEquals(of(mimeType), b.getMimeType(), "Incorrect binary mime type!");
        });
        assertAll("Check resource", checkResource(res, identifier, LDP.NonRDFSource, true, false, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 1L, 0L, 5L, 0L, 0L));
    }

    @Test
    void testResourceWithChildren() {
        final JenaDataset dataset = buildLdpDataset(LDP.Container);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        getChildIRIs().forEach(c -> {
            dataset.add(Trellis.PreferServerManaged, c, DC.isPartOf, identifier);
            dataset.add(Trellis.PreferServerManaged, c, RDF.type, LDP.RDFSource);
        });

        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier, extensions, false);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.Container, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 0L, 0L, 0L, 4L));
    }

    @Test
    void testResourceWithoutChildren() {
        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        getChildIRIs().forEach(c -> dataset.add(Trellis.PreferServerManaged, c, DC.isPartOf, identifier));

        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier, extensions, true);
        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 1L, 0L, 0L, 0L, 0L));
    }

    @Test
    void testDirectContainer() {
        final JenaDataset dataset = buildLdpDataset(LDP.DirectContainer);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.member, member);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.membershipResource, member);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.hasMemberRelation, DC.subject);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.insertedContentRelation, LDP.MemberSubject);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, member, DC.isPartOf, root);
        getChildIRIs().forEach(c -> {
            dataset.add(Trellis.PreferServerManaged, c, DC.isPartOf, identifier);
            dataset.add(Trellis.PreferServerManaged, c, RDF.type, LDP.RDFSource);
        });

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier, extensions, true);
        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.DirectContainer, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res, member, DC.subject, null, LDP.MemberSubject));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 1L, 0L, 0L, 0L, 4L));

        final TriplestoreResource memberRes = new TriplestoreResource(rdfConnection, member, extensions, false);
        memberRes.fetchData();
        assertTrue(memberRes.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(memberRes, member, LDP.RDFSource, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(memberRes, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(memberRes, 1L, 0L, 0L, 0L, 4L, 0L));
        assertEquals(4L, memberRes.stream(singleton(LDP.PreferMembership)).map(Quad::getPredicate)
                .filter(isEqual(DC.subject)).count(), "Incorrect triple count!");

        assertNotEquals(res.getRevision(), memberRes.getRevision(), "Revisions not unequal");
    }

    @Test
    void testIndirectContainer() {
        final JenaDataset dataset = buildLdpDataset(LDP.IndirectContainer);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.member, member);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.membershipResource, member);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.hasMemberRelation, DC.relation);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.insertedContentRelation, DC.subject);
        dataset.add(Trellis.PreferServerManaged, member, DC.isPartOf, root);
        dataset.add(identifier, identifier, DC.alternative, rdf.createLiteral("An LDP-IC resource"));
        dataset.add(member, member, DC.alternative, rdf.createLiteral("A membership resource"));
        getChildIRIs().forEach(c -> {
            dataset.add(Trellis.PreferServerManaged, c, DC.isPartOf, identifier);
            dataset.add(Trellis.PreferServerManaged, c, RDF.type, LDP.RDFSource);
            dataset.add(c, c, DC.subject, rdf.createIRI("http://example.org/" + randomUUID()));
        });

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier, extensions, false);
        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.IndirectContainer, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res, member, DC.relation, null, DC.subject));
        assertAll("Check RDF stream", checkRdfStream(res, 3L, 0L, 0L, 0L, 0L, 4L));

        final TriplestoreResource res2 = new TriplestoreResource(rdfConnection, member, extensions, false);
        res2.fetchData();
        assertTrue(res2.exists(), "Missing resource (2)!");
        assertAll("Check resource", checkResource(res2, member, LDP.RDFSource, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res2, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res2, 2L, 0L, 0L, 0L, 4L, 0L));
        assertEquals(4L, res2.stream(singleton(LDP.PreferMembership)).map(Quad::getPredicate)
                .filter(isEqual(DC.relation)).count(), "Incorrect triple count!");

        assertNotEquals(res.getRevision(), res2.getRevision(), "Revisions not unequal");
    }

    private static Stream<IRI> getChildIRIs() {
        return Stream.of(child1, child2, child3, child4);
    }

    private static JenaDataset buildLdpDataset(final IRI ldpType) {
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(identifier, identifier, RDF.type, SKOS.Concept);
        dataset.add(identifier, identifier, SKOS.prefLabel, rdf.createLiteral("resource"));
        dataset.add(member, member, SKOS.prefLabel, rdf.createLiteral("member resource"));
        dataset.add(Trellis.PreferServerManaged, identifier, RDF.type, ldpType);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, member, RDF.type, LDP.RDFSource);
        dataset.add(Trellis.PreferServerManaged, member, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        return dataset;
    }

    private static Stream<Executable> checkResource(final Resource res, final IRI identifier, final IRI ldpType,
            final boolean hasBinary, final boolean hasAcl, final boolean hasParent) {
        return Stream.of(
                () -> assertEquals(identifier, res.getIdentifier(), "Incorrect identifier!"),
                () -> assertEquals(ldpType, res.getInteractionModel(), "Incorrect interaction model!"),
                () -> assertEquals(parse(time), res.getModified(), "Incorrect modified date!"),
                () -> assertNotNull(res.getRevision(), "Revision is null!"),
                () -> assertEquals(hasBinary, res.getBinaryMetadata().isPresent(), "Unexpected binary presence!"),
                () -> assertEquals(hasParent, res.getContainer().isPresent(), "Unexpected parent resource!"),
                () -> assertEquals(res.stream(barGraph).findAny().isPresent(), res.hasMetadata(barGraph),
                                   "Unexpected metadata"),
                () -> assertEquals(res.stream(fooGraph).findAny().isPresent(), res.hasMetadata(fooGraph),
                                   "Unexpected metadata"),
                () -> assertEquals(res.getMetadataGraphNames().contains(Trellis.PreferAudit),
                                   res.hasMetadata(Trellis.PreferAudit), "Unexpected Audit quads"),
                () -> assertEquals(res.stream(Trellis.PreferAudit).findAny().isPresent(),
                                   res.hasMetadata(Trellis.PreferAudit), "Missing audit quads"),
                () -> assertEquals(res.getMetadataGraphNames().contains(Trellis.PreferAccessControl),
                                   res.hasMetadata(Trellis.PreferAccessControl), "Unexpected ACL presence!"),
                () -> assertEquals(hasAcl, res.hasMetadata(Trellis.PreferAccessControl), "Unexpected ACL presence!"));
    }

    private static Stream<Executable> checkLdpProperties(final Resource res, final IRI membershipResource,
            final IRI hasMemberRelation, final IRI memberOfRelation, final IRI insertedContentRelation) {
        return Stream.of(
                () -> assertEquals(membershipResource != null, res.getMembershipResource().isPresent(),
                                   "unexpected ldp:membershipResource property!"),
                () -> assertEquals(hasMemberRelation != null, res.getMemberRelation().isPresent(),
                                   "unexpected ldp:hasMemberRelation property!"),
                () -> assertEquals(memberOfRelation != null, res.getMemberOfRelation().isPresent(),
                                   "unexpected ldp:isMemberOfRelation property!"),
                () -> assertEquals(insertedContentRelation != null, res.getInsertedContentRelation().isPresent(),
                                   "unexpected ldp::insertedContentRelation property!"),
                () -> assertEquals(membershipResource, res.getMembershipResource().orElse(null),
                                   "Incorrect ldp:membershipResource!"),
                () -> assertEquals(hasMemberRelation, res.getMemberRelation().orElse(null),
                                   "Incorrect ldp:hasMemeberRelation!"),
                () -> assertEquals(memberOfRelation, res.getMemberOfRelation().orElse(null),
                                   "Incorrect ldp:isMemberOfRelation!"),
                () -> assertEquals(insertedContentRelation, res.getInsertedContentRelation().orElse(null),
                                   "Incorrect ldp:insertedContentRelation!"));
    }

    private static Stream<Executable> checkRdfStream(final Resource res, final long userManaged,
            final long serverManaged, final long acl, final long audit, final long membership, final long containment) {
        final long total = userManaged + acl + audit + membership + containment + serverManaged;
        return Stream.of(
                () -> assertEquals(serverManaged, res.stream(singleton(Trellis.PreferServerManaged)).count(),
                                   "Incorrect server managed triple count!"),
                () -> assertEquals(userManaged, res.stream(singleton(Trellis.PreferUserManaged)).count(),
                                   "Incorrect user managed triple count!"),
                () -> assertEquals(acl, res.stream(singleton(Trellis.PreferAccessControl)).count(),
                                   "Incorrect acl triple count!"),
                () -> assertEquals(audit, res.stream(singleton(Trellis.PreferAudit)).count(),
                                   "Incorrect audit triple count!"),
                () -> assertEquals(membership, res.stream(singleton(LDP.PreferMembership)).count(),
                                   "Incorrect member triple count!"),
                () -> assertEquals(containment, res.stream(singleton(LDP.PreferContainment)).count(),
                                   "Incorrect containment triple count!"),
                () -> assertEquals(total, res.stream().count(), "Incorrect total triple count!"));
    }
}
