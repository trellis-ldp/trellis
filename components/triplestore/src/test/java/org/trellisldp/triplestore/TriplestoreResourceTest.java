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
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.function.Predicate.isEqual;
import static org.apache.jena.query.DatasetFactory.create;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.triplestore.TriplestoreUtils.getInstance;

import java.time.Instant;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
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
public class TriplestoreResourceTest {

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
        final TriplestoreResource res = new TriplestoreResource(connect(create()), identifier);
        res.fetchData();
        assertFalse(res.exists(), "Unexpected resource!");
    }

    @Test
    public void testPartialResource() {
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier);
        res.fetchData();
        assertFalse(res.exists(), "Unexpected resource!");
    }

    @Test
    public void testMinimalResource() {
        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, false, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 0L, 0L, 0L));
    }

    @Test
    public void testResourceWithAuditQuads() {
        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, false, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 5L, 0L, 0L));
    }

    @Test
    public void testResourceWithAclQuads() {
        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        dataset.add(aclId, aclSubject, ACL.mode, ACL.Read);
        dataset.add(aclId, aclSubject, ACL.agentClass, FOAF.Agent);
        dataset.add(aclId, aclSubject, ACL.accessTo, identifier);
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));
        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, true, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 3L, 5L, 0L, 0L));
    }

    @Test
    public void testBinaryResource() {
        final String size = "2560";
        final String mimeType = "image/jpeg";
        final IRI binaryIdentifier = rdf.createIRI("file:///binary");
        final JenaDataset dataset = buildLdpDataset(LDP.NonRDFSource);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.hasPart, binaryIdentifier);
        dataset.add(Trellis.PreferServerManaged, binaryIdentifier, DC.extent, rdf.createLiteral(size, XSD.long_));
        dataset.add(Trellis.PreferServerManaged, binaryIdentifier, DC.format, rdf.createLiteral(mimeType));
        auditService.creation(identifier, mockSession).forEach(q ->
                dataset.add(auditId, q.getSubject(), q.getPredicate(), q.getObject()));

        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        res.getBinaryMetadata().ifPresent(b -> {
            assertEquals(binaryIdentifier, b.getIdentifier(), "Incorrect binary identifier!");
            assertEquals(of(Long.parseLong(size)), b.getSize(), "Incorrect binary size!");
            assertEquals(of(mimeType), b.getMimeType(), "Incorrect binary mime type!");
        });
        assertAll("Check resource", checkResource(res, identifier, LDP.NonRDFSource, true, false, false));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 5L, 0L, 0L));
    }

    @Test
    public void testResourceWithChildren() {
        final JenaDataset dataset = buildLdpDataset(LDP.Container);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        getChildIRIs().forEach(c -> dataset.add(Trellis.PreferServerManaged, c, DC.isPartOf, identifier));

        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier);

        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.Container, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 0L, 0L, 4L));
    }

    @Test
    public void testResourceWithoutChildren() {
        final JenaDataset dataset = buildLdpDataset(LDP.RDFSource);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        getChildIRIs().forEach(c -> dataset.add(Trellis.PreferServerManaged, c, DC.isPartOf, identifier));

        final TriplestoreResource res = new TriplestoreResource(connect(wrap(dataset.asJenaDatasetGraph())),
                identifier);
        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.RDFSource, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 0L, 0L, 0L));
    }

    @Test
    public void testDirectContainer() {
        final JenaDataset dataset = buildLdpDataset(LDP.DirectContainer);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.isPartOf, root);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.member, member);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.membershipResource, member);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.hasMemberRelation, DC.subject);
        dataset.add(Trellis.PreferServerManaged, identifier, LDP.insertedContentRelation, LDP.MemberSubject);
        dataset.add(Trellis.PreferServerManaged, identifier, DC.modified, rdf.createLiteral(time, XSD.dateTime));
        dataset.add(Trellis.PreferServerManaged, member, DC.isPartOf, root);
        getChildIRIs().forEach(c -> dataset.add(Trellis.PreferServerManaged, c, DC.isPartOf, identifier));

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.DirectContainer, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res, member, DC.subject, null, LDP.MemberSubject));
        assertAll("Check RDF stream", checkRdfStream(res, 2L, 0L, 0L, 0L, 4L));

        final TriplestoreResource memberRes = new TriplestoreResource(rdfConnection, member);
        memberRes.fetchData();
        assertTrue(memberRes.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(memberRes, member, LDP.RDFSource, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(memberRes, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(memberRes, 1L, 0L, 0L, 4L, 0L));
        assertEquals(4L, memberRes.stream(singleton(LDP.PreferMembership)).map(Triple::getPredicate)
                .filter(isEqual(DC.subject)).count(), "Incorrect triple count!");
    }

    @Test
    public void testIndirectContainer() {
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
            dataset.add(c, c, DC.subject, rdf.createIRI("http://example.org/" + randomUUID()));
        });

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        assertTrue(res.exists(), "Missing resource!");
        assertAll("Check resource", checkResource(res, identifier, LDP.IndirectContainer, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res, member, DC.relation, null, DC.subject));
        assertAll("Check RDF stream", checkRdfStream(res, 3L, 0L, 0L, 0L, 4L));

        final TriplestoreResource res2 = new TriplestoreResource(rdfConnection, member);
        res2.fetchData();
        assertTrue(res2.exists(), "Missing resource (2)!");
        assertAll("Check resource", checkResource(res2, member, LDP.RDFSource, false, false, true));
        assertAll("Check LDP properties", checkLdpProperties(res2, null, null, null, null));
        assertAll("Check RDF stream", checkRdfStream(res2, 2L, 0L, 0L, 4L, 0L));
        assertEquals(4L, res2.stream(singleton(LDP.PreferMembership)).map(Triple::getPredicate)
                .filter(isEqual(DC.relation)).count(), "Incorrect triple count!");
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
            final Boolean hasBinary, final Boolean hasAcl, final Boolean hasParent) {
        return Stream.of(
                () -> assertEquals(identifier, res.getIdentifier(), "Incorrect identifier!"),
                () -> assertEquals(ldpType, res.getInteractionModel(), "Incorrect interaction model!"),
                () -> assertEquals(parse(time), res.getModified(), "Incorrect modified date!"),
                () -> assertEquals(hasBinary, res.getBinaryMetadata().isPresent(), "Unexpected binary presence!"),
                () -> assertEquals(hasParent, res.getContainer().isPresent(), "Unexpected parent resource!"),
                () -> assertEquals(hasAcl, res.hasAcl(), "Unexpected ACL presence!"));
    }

    private static Stream<Executable> checkLdpProperties(final Resource res, final IRI membershipResource,
            final IRI hasMemberRelation, final IRI memberOfRelation, final IRI insertedContentRelation) {
        return Stream.of(
                () -> assertEquals(nonNull(membershipResource), res.getMembershipResource().isPresent(),
                                   "unexpected ldp:membershipResource property!"),
                () -> assertEquals(nonNull(hasMemberRelation), res.getMemberRelation().isPresent(),
                                   "unexpected ldp:hasMemberRelation property!"),
                () -> assertEquals(nonNull(memberOfRelation), res.getMemberOfRelation().isPresent(),
                                   "unexpected ldp:isMemberOfRelation property!"),
                () -> assertEquals(nonNull(insertedContentRelation), res.getInsertedContentRelation().isPresent(),
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
            final long acl, final long audit, final long membership, final long containment) {
        final long total = userManaged + acl + audit + membership + containment;
        return Stream.of(
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
