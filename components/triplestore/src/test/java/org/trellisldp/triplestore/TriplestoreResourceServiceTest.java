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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_SESSION_BASE_URL;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.update.UpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.Binary;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.id.UUIDGenerator;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.RDFS;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.vocabulary.XSD;

/**
 * Test the TriplestoreResourceService class.
 */
public class TriplestoreResourceServiceTest {

    private static final JenaRDF rdf = new JenaRDF();
    private static final IdentifierService idService = new UUIDGenerator();
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final String baseUrl = "http://example.com/";

    private final Instant created = now();

    static {
        setDefaultPollInterval(100L, MILLISECONDS);
    }

    @Mock
    private Session mockSession;

    @Mock
    private EventService mockEventService;

    @Mock
    private RDFConnection mockRdfConnection;

    @Mock
    private MementoService mockMementoService;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        when(mockSession.getCreated()).thenReturn(created);
        when(mockSession.getDelegatedBy()).thenReturn(empty());
        when(mockSession.getProperty(TRELLIS_SESSION_BASE_URL)).thenReturn(of(baseUrl));
    }

    @Test
    public void testIdentifierService() {
        final JenaDataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);
        assertNotEquals(svc.generateIdentifier(), svc.generateIdentifier());
        assertNotEquals(svc.generateIdentifier(), svc.generateIdentifier());
    }

    @Test
    public void testResourceNotFound() {
        final JenaDataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);
        assertFalse(svc.get(rdf.createIRI(TRELLIS_DATA_PREFIX + "missing")).isPresent());
    }

    @Test
    public void testScan() {
        final IRI one = rdf.createIRI(TRELLIS_DATA_PREFIX + "1");
        final IRI two = rdf.createIRI(TRELLIS_DATA_PREFIX + "2");
        final IRI three = rdf.createIRI(TRELLIS_DATA_PREFIX + "3");
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferServerManaged, one, RDF.type, LDP.Container);
        dataset.add(Trellis.PreferServerManaged, two, RDF.type, LDP.NonRDFSource);
        dataset.add(Trellis.PreferServerManaged, three, RDF.type, LDP.RDFSource);

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);
        assertEquals(4L, svc.scan().count());
        assertTrue(svc.scan().anyMatch(t -> t.getSubject().equals(root) && t.getObject().equals(LDP.BasicContainer)));
        assertTrue(svc.scan().anyMatch(t -> t.getSubject().equals(one) && t.getObject().equals(LDP.Container)));
        assertTrue(svc.scan().anyMatch(t -> t.getSubject().equals(two) && t.getObject().equals(LDP.NonRDFSource)));
        assertTrue(svc.scan().anyMatch(t -> t.getSubject().equals(three) && t.getObject().equals(LDP.RDFSource)));
    }

    @Test
    public void testInitializeRoot() {
        final Instant early = now();
        final JenaDataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(LDP.BasicContainer, res.getInteractionModel());
            assertEquals(root, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertEquals(0L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(2L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(5L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(7L, res.stream().count());
        });
    }

    @Test
    public void testInitializeRoot2() {
        final Instant early = now();
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferServerManaged, root, RDF.type, LDP.BasicContainer);
        dataset.add(Trellis.PreferServerManaged, root, DC.modified, rdf.createLiteral(early.toString(), XSD.dateTime));

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(LDP.BasicContainer, res.getInteractionModel());
            assertEquals(root, res.getIdentifier());
            assertEquals(early, res.getModified());
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertEquals(0L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(2L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(0L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(2L, res.stream().count());
        });
    }

    @Test
    public void testUpdateRoot() throws Exception {
        final Instant early = now();
        final JenaDataset dataset = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(LDP.BasicContainer, res.getInteractionModel());
            assertEquals(root, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertEquals(0L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(2L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(5L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(7L, res.stream().count());
        });

        final Dataset data = rdf.createDataset();
        svc.get(root).ifPresent(res ->
                res.stream().filter(q -> !q.getGraphName().filter(Trellis.PreferServerManaged::equals).isPresent())
                            .forEach(data::add));
        data.add(Trellis.PreferUserManaged, root, RDFS.label, rdf.createLiteral("Resource Label"));
        data.add(Trellis.PreferUserManaged, root, RDFS.seeAlso, rdf.createIRI("http://example.com"));
        data.add(Trellis.PreferUserManaged, root, LDP.inbox, rdf.createIRI("http://ldn.example.com/"));
        data.add(Trellis.PreferUserManaged, root, RDF.type, rdf.createLiteral("Some weird type"));
        data.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Update);

        final Instant later = meanwhile();

        assertTrue(svc.replace(root, mockSession, LDP.BasicContainer, data, null, null).get());
        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(LDP.BasicContainer, res.getInteractionModel());
            assertEquals(root, res.getIdentifier());
            assertFalse(res.getModified().isBefore(later));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getBinary().isPresent());
            assertEquals(4L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(2L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(5L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(12L, res.stream().count());
        });
    }


    @Test
    public void testRDFConnectionError() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(mockRdfConnection, idService,
                mockMementoService, mockEventService);
        doThrow(new RuntimeException("Expected exception")).when(mockRdfConnection).update(any(UpdateRequest.class));
        doThrow(new RuntimeException("Expected exception")).when(mockRdfConnection)
            .loadDataset(any(org.apache.jena.query.Dataset.class));

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        assertThrows(ExecutionException.class, () ->
                svc.create(resource, mockSession, LDP.RDFSource, rdf.createDataset(), root, null).get());
        assertThrows(ExecutionException.class, () -> svc.add(resource, mockSession, rdf.createDataset()).get());
    }

    @Test
    public void testGetContainer() {
        final RDFConnection rdfConnection = connect(wrap(rdf.createDataset().asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        assertFalse(svc.getContainer(root).isPresent());
        assertTrue(svc.getContainer(resource).isPresent());
        assertEquals(root, svc.getContainer(resource).get());
        assertTrue(svc.getContainer(child).isPresent());
        assertEquals(resource, svc.getContainer(child).get());
    }

    @Test
    public void testPutLdpRs() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final BlankNode acl = rdf.createBlankNode();
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferAccessControl, acl, RDF.type, ACL.Authorization);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.RDFSource, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(1L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(6L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertFalse(res.getModified().isBefore(later));
        });
    }

    @Test
    public void testPutLdpRsWithoutBaseUrl() throws Exception {
        when(mockSession.getProperty(TRELLIS_SESSION_BASE_URL)).thenReturn(empty());
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.RDFSource, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertFalse(res.getModified().isBefore(later));
        });
    }

    @Test
    public void testPutLdpNr() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI binaryIdentifier = rdf.createIRI("foo:binary");
        final Instant binaryTime = now();
        final Dataset dataset = rdf.createDataset();
        final Binary binary = new Binary(binaryIdentifier, binaryTime, "text/plain", 10L);
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.NonRDFSource, dataset, root, binary).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.NonRDFSource, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertTrue(res.getBinary().isPresent());
            res.getBinary().ifPresent(b -> {
                assertEquals(binaryIdentifier, b.getIdentifier());
                assertEquals(of("text/plain"), b.getMimeType());
                assertEquals(of(10L), b.getSize());
                assertEquals(binaryTime, b.getModified());
            });
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(9L, res.stream().count());
        });
        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertFalse(res.getModified().isBefore(later));
        });
        verify(mockEventService, times(2)).emit(any());

        final IRI resource2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/notachild");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(resource2, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(resource2, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater));
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
        });

        verify(mockEventService, times(3)).emit(any());
    }

    @Test
    public void testPutLdpC() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.Container, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.Container, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(5L, res.stream().count());
            assertFalse(res.getModified().isBefore(later));
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(later));
        });

        verify(mockEventService, times(2)).emit(any());

        // Now add a child resource
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater));
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        svc.get(resource).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater));
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(4)).emit(any());

        // Now update that child resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, RDFS.label, rdf.createLiteral("other title"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.seeAlso, rdf.createIRI("http://example.com"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Update);

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.replace(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(2L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(2L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(7L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(evenLater2));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(evenLater2));
        });

        verify(mockEventService, times(5)).emit(any());
    }

    @Test
    public void testAddAuditTriples() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Dataset dataset1 = rdf.createDataset();
        final Dataset dataset2 = rdf.createDataset();
        dataset1.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset2.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.Container, dataset1, root, null).get());
        assertTrue(svc.add(resource, mockSession, dataset2).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.Container, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(5L, res.stream().count());
            assertFalse(res.getModified().isBefore(later));
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(later));
        });

        verify(mockEventService, times(2)).emit(any());
    }

    @Test
    public void testPutDeleteLdpC() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.Container, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.Container, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });
        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(later));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(later));
        });

        verify(mockEventService, times(2)).emit(any());

        // Now add a child resource
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });
        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(evenLater));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(evenLater));
        });

        verify(mockEventService, times(4)).emit(any());

        // Now delete the child resource
        final BlankNode bnode = rdf.createBlankNode();
        final BlankNode acl = rdf.createBlankNode();
        dataset.clear();
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, AS.Delete);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferServerManaged, child, RDF.type, LDP.Resource);
        dataset.add(Trellis.PreferAccessControl, acl, RDF.type, ACL.Authorization);

        final Instant preDelete = meanwhile();

        assertTrue(svc.delete(child, mockSession, LDP.Resource, dataset).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertTrue(res.isDeleted());
            assertEquals(child, res.getIdentifier());
            assertEquals(LDP.Resource, res.getInteractionModel());
            assertFalse(res.getModified().isBefore(preDelete));
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(preDelete));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(preDelete));
        });

        // TODO -- verify that this is correct (should it be 6?)
        // yes it should --ajs6f
        verify(mockEventService, times(6)).emit(any());
    }

    @Test
    public void testPutLdpBc() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.BasicContainer, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.BasicContainer, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });
        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(later));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(later));
        });

        verify(mockEventService, times(2)).emit(any());

        // Now add a child resource
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });
        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(evenLater));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(evenLater));
        });

        verify(mockEventService, times(4)).emit(any());

        // Now update the child resource
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Update);
        dataset.add(Trellis.PreferUserManaged, child, RDFS.seeAlso, rdf.createIRI("http://www.example.com/"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.label, rdf.createLiteral("a label"));

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.replace(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(2L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(2L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(7L, res.stream().count());
        });
        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(evenLater));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(evenLater2));
        });

        verify(mockEventService, times(5)).emit(any());
    }

    @Test
    public void testPutLdpDcSelf() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, resource);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, DC.relation);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.DirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.DirectContainer, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(later));
            assertFalse(res.getBinary().isPresent());
            assertEquals(3L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(11L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(later));
        });

        verify(mockEventService, times(2)).emit(any());

        // Now add the child resources to the ldp-dc
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(evenLater2));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child)).findFirst().isPresent());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(DC.relation)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        verify(mockEventService, times(4)).emit(any());
    }

    @Test
    public void testPutLdpDc() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI members = rdf.createIRI(TRELLIS_DATA_PREFIX + "members");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, DC.relation);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.DirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.DirectContainer, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(later));
            assertFalse(res.getBinary().isPresent());
            assertEquals(3L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(11L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(later));
        });

        verify(mockEventService, times(2)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("title"));

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).get());
        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(members, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(5L, res.stream().count());
        });
        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertFalse(res.getModified().isBefore(evenLater));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(evenLater));
        });

        verify(mockEventService, times(4)).emit(any());

        // Now add the child resources to the ldp-dc
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.getModified().isBefore(evenLater2));
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(DC.relation)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        verify(mockEventService, times(7)).emit(any());
    }

    @Test
    public void testPutLdpDcMultiple() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI members = rdf.createIRI(TRELLIS_DATA_PREFIX + "members");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, DC.relation);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.DirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.DirectContainer, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(later));
            assertFalse(res.getBinary().isPresent());
            assertEquals(3L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(11L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(later));
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(2)).emit(any());

        final IRI resource2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2");
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.hasMemberRelation, DC.subject);

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(resource2, mockSession, LDP.DirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertEquals(LDP.DirectContainer, res.getInteractionModel());
            assertEquals(resource2, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(3L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(11L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater));
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(4)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("title"));

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).get());
        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(members, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(3L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(6)).emit(any());

        // Now add the child resources to the ldp-dc
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater3 = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater3));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater3));
            assertEquals(3L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater3));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater3));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(DC.relation)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        verify(mockEventService, times(9)).emit(any());

        // Now add a child resources to the other ldp-dc
        final IRI child2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2/child");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child2, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater4 = meanwhile();

        assertTrue(svc.create(child2, mockSession, LDP.RDFSource, dataset, resource2, null).get());
        assertTrue(svc.get(child2).isPresent());
        svc.get(child2).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child2, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater4));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater4));
            assertEquals(3L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater4));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource2) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child2)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater4));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(2L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(DC.subject)
                        && t.getObject().equals(child2)).findFirst().isPresent());
        });

        verify(mockEventService, times(12)).emit(any());
    }

    @Test
    public void testPutLdpDcMultipleInverse() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI members = rdf.createIRI(TRELLIS_DATA_PREFIX + "members");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.isMemberOfRelation, DC.relation);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.DirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.DirectContainer, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(later));
            assertFalse(res.getBinary().isPresent());
            assertEquals(of(DC.relation), res.getMemberOfRelation());
            assertEquals(3L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(11L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(later));
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(2)).emit(any());

        final IRI resource2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.isMemberOfRelation, DC.subject);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(resource2, mockSession, LDP.DirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertEquals(LDP.DirectContainer, res.getInteractionModel());
            assertEquals(resource2, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(3L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(11L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater));
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(4)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).get());
        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(members, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(3L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(6)).emit(any());

        // Now add the child resources to the ldp-dc
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater3 = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater3));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertEquals(6L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater3));
            assertEquals(3L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater3));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater3));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(8)).emit(any());

        // Now add a child resources to the other ldp-dc
        final IRI child2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2/child");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child2, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater4 = meanwhile();

        assertTrue(svc.create(child2, mockSession, LDP.RDFSource, dataset, resource2, null).get());
        assertTrue(svc.get(child2).isPresent());
        svc.get(child2).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child2, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater4));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertEquals(6L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater4));
            assertEquals(3L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater4));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource2) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child2)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater4));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(10)).emit(any());
    }

    @Test
    public void testPutLdpIc() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI members = rdf.createIRI(TRELLIS_DATA_PREFIX + "members");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.IndirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.IndirectContainer, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(later));
            assertFalse(res.getBinary().isPresent());
            assertEquals(4L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(12L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(2)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).get());
        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(members, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater));
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(4)).emit(any());

        // Now add the child resources to the ldp-dc
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        final Literal label = rdf.createLiteral("label1");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater2));
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(RDFS.label)
                        && t.getObject().equals(label)).findFirst().isPresent());
        });

        verify(mockEventService, times(7)).emit(any());
    }

    @Test
    public void testPutLdpIcDefaultContent() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI members = rdf.createIRI(TRELLIS_DATA_PREFIX + "members");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, LDP.MemberSubject);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.IndirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.IndirectContainer, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(later));
            assertFalse(res.getBinary().isPresent());
            assertEquals(4L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(12L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(2)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).get());
        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(members, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater));
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(4)).emit(any());

        // Now add the child resources to the ldp-dc
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        final Literal label = rdf.createLiteral("label1");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater2));
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(RDFS.label)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        verify(mockEventService, times(7)).emit(any());
    }

    @Test
    public void testPutLdpIcMultipleStatements() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI members = rdf.createIRI(TRELLIS_DATA_PREFIX + "members");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.IndirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.IndirectContainer, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isBefore(later));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getBinary().isPresent());
            assertEquals(4L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(12L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(later));
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(2)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).get());
        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(members, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater));
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(4)).emit(any());

        // Now add the child resources to the ldp-dc
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        final Literal label1 = rdf.createLiteral("Label", "en");
        final Literal label2 = rdf.createLiteral("Zeichnung", "de");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label1);
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label2);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(2L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(6L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater2));
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(2L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(RDFS.label)
                        && t.getObject().equals(label2)).findFirst().isPresent());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(RDFS.label)
                        && t.getObject().equals(label1)).findFirst().isPresent());
        });

        verify(mockEventService, times(7)).emit(any());
    }

    @Test
    public void testPutLdpIcMultipleResources() throws Exception {
        final Instant early = now();
        final JenaDataset d = rdf.createDataset();
        final RDFConnection rdfConnection = connect(wrap(d.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService,
                mockMementoService, mockEventService);

        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI members = rdf.createIRI(TRELLIS_DATA_PREFIX + "members");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertTrue(svc.create(resource, mockSession, LDP.IndirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertEquals(LDP.IndirectContainer, res.getInteractionModel());
            assertEquals(resource, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(later));
            assertFalse(res.getBinary().isPresent());
            assertEquals(4L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(12L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(later));
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(2)).emit(any());

        final IRI resource2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertTrue(svc.create(resource2, mockSession, LDP.IndirectContainer, dataset, root, null).get());
        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertEquals(LDP.IndirectContainer, res.getInteractionModel());
            assertEquals(resource2, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater));
            assertFalse(res.getBinary().isPresent());
            assertEquals(4L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(7L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(12L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater));
            assertEquals(2L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(4)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertTrue(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).get());
        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(members, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater2));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater2));
            assertEquals(3L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater2));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        verify(mockEventService, times(6)).emit(any());

        // Now add the child resources to the ldp-ic
        final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
        final Literal label = rdf.createLiteral("label1");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater3 = meanwhile();

        assertTrue(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).get());
        assertTrue(svc.get(child).isPresent());
        svc.get(child).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater3));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater3));
            assertEquals(3L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater3));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater3));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(RDFS.label)
                        && t.getObject().equals(label)).findFirst().isPresent());
        });

        verify(mockEventService, times(9)).emit(any());

        // Now add the child resources to the ldp-ic
        final IRI child2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2/child");
        final Literal label2 = rdf.createLiteral("label2");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child2, SKOS.prefLabel, label2);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater4 = meanwhile();

        assertTrue(svc.create(child2, mockSession, LDP.RDFSource, dataset, resource2, null).get());
        assertTrue(svc.get(child2).isPresent());
        svc.get(child2).ifPresent(res -> {
            assertEquals(LDP.RDFSource, res.getInteractionModel());
            assertEquals(child2, res.getIdentifier());
            assertFalse(res.getModified().isBefore(early));
            assertFalse(res.getModified().isAfter(now().plusMillis(5L)));
            assertFalse(res.getModified().isBefore(evenLater4));
            assertFalse(res.getBinary().isPresent());
            assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(3L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferAudit).count());
            assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
            assertEquals(5L, res.stream().count());
        });

        assertTrue(svc.get(root).isPresent());
        svc.get(root).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater4));
            assertEquals(3L, res.stream(LDP.PreferContainment).count());
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
        });

        assertTrue(svc.get(resource).isPresent());
        svc.get(resource).ifPresent(res -> {
            assertTrue(res.getModified().isBefore(evenLater4));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child)).findFirst().isPresent());
        });

        assertTrue(svc.get(resource2).isPresent());
        svc.get(resource2).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater4));
            assertEquals(0L, res.stream(LDP.PreferMembership).count());
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertTrue(res.stream(LDP.PreferContainment).filter(t ->
                        t.getSubject().equals(resource2) && t.getPredicate().equals(LDP.contains)
                        && t.getObject().equals(child2)).findFirst().isPresent());
        });

        assertTrue(svc.get(members).isPresent());
        svc.get(members).ifPresent(res -> {
            assertFalse(res.getModified().isBefore(evenLater4));
            assertEquals(0L, res.stream(LDP.PreferContainment).count());
            assertEquals(2L, res.stream(LDP.PreferMembership).count());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(RDFS.label)
                        && t.getObject().equals(label)).findFirst().isPresent());
            assertTrue(res.stream(LDP.PreferMembership).filter(t ->
                        t.getSubject().equals(members) && t.getPredicate().equals(RDFS.label)
                        && t.getObject().equals(label2)).findFirst().isPresent());
        });

        verify(mockEventService, times(12)).emit(any());
    }

    private static Instant meanwhile() {
        final Instant t1 = now();
        await().until(() -> isReallyLaterThan(t1));
        final Instant t2 = now();
        await().until(() -> isReallyLaterThan(t2));
        return t2;
    }

    private static Boolean isReallyLaterThan(final Instant time) {
        final Instant t = now();
        return t.isAfter(time) && (t.toEpochMilli() > time.toEpochMilli() || t.getNano() > time.getNano());
    }
}
