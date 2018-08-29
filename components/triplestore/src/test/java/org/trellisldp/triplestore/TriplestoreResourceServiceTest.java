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
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Predicate.isEqual;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.trellisldp.api.Binary;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.id.UUIDGenerator;
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
    private static final String baseUrl = "http://example.com/";
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
    private static final IRI resource2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2");
    private static final IRI members = rdf.createIRI(TRELLIS_DATA_PREFIX + "members");
    private static final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
    private static final IRI child2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2/child");

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
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);
        assertNotEquals(svc.generateIdentifier(), svc.generateIdentifier(), "Not unique identifiers!");
        assertNotEquals(svc.generateIdentifier(), svc.generateIdentifier(), "Not unique identifiers!");
    }

    @Test
    public void testResourceNotFound() {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);
        assertEquals(MISSING_RESOURCE, svc.get(rdf.createIRI(TRELLIS_DATA_PREFIX + "missing")).join(),
                "Not a missing resource!");
    }

    @Test
    public void testInitializeRoot() {
        final Instant early = now();
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Resource res = svc.get(root).join();
        assertAll("Check resource", checkResource(res, root, LDP.BasicContainer, early));
        assertAll("Check resource stream", checkResourceStream(res, 0L, 2L, 5L, 0L, 0L, 0L));
    }

    @Test
    public void testInitializeRoot2() {
        final Instant early = now();
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferServerManaged, root, RDF.type, LDP.BasicContainer);
        dataset.add(Trellis.PreferServerManaged, root, DC.modified, rdf.createLiteral(early.toString(), XSD.dateTime));

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final ResourceService svc = new TriplestoreResourceService(rdfConnection, idService, mockEventService);

        final Resource res = svc.get(root).join();
        assertAll("Check resource", checkResource(res, root, LDP.BasicContainer, early));
        assertAll("Check resource stream", checkResourceStream(res, 0L, 2L, 0L, 0L, 0L, 0L));
    }

    @Test
    public void testUpdateRoot() throws Exception {
        final Instant early = now();
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Resource res1 = svc.get(root).join();
        assertAll("Check resource", checkResource(res1, root, LDP.BasicContainer, early));
        assertAll("Check resource stream", checkResourceStream(res1, 0L, 2L, 5L, 0L, 0L, 0L));

        final Dataset data = rdf.createDataset();
        svc.get(root).thenAccept(res ->
                res.stream().filter(q -> !q.getGraphName().filter(Trellis.PreferServerManaged::equals).isPresent())
                            .forEach(data::add)).join();
        data.add(Trellis.PreferUserManaged, root, RDFS.label, rdf.createLiteral("Resource Label"));
        data.add(Trellis.PreferUserManaged, root, RDFS.seeAlso, rdf.createIRI("http://example.com"));
        data.add(Trellis.PreferUserManaged, root, LDP.inbox, rdf.createIRI("http://ldn.example.com/"));
        data.add(Trellis.PreferUserManaged, root, RDF.type, rdf.createLiteral("Some weird type"));
        data.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Update);

        final Instant later = meanwhile();

        assertNull(svc.replace(root, mockSession, LDP.BasicContainer, data, null, null).join(),
                "Unsuccessful replace operation!");
        final Resource res2 = svc.get(root).join();
        assertAll("Check resource", checkResource(res2, root, LDP.BasicContainer, later));
        assertAll("Check resource stream", checkResourceStream(res2, 4L, 2L, 5L, 1L, 0L, 0L));
    }

    @Test
    public void testRDFConnectionError() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(mockRdfConnection, idService, mockEventService);
        doThrow(new RuntimeException("Expected exception")).when(mockRdfConnection).update(any(UpdateRequest.class));
        doThrow(new RuntimeException("Expected exception")).when(mockRdfConnection)
            .loadDataset(any(org.apache.jena.query.Dataset.class));

        assertThrows(ExecutionException.class, () ->
                svc.create(resource, mockSession, LDP.RDFSource, rdf.createDataset(), root, null).get(),
                "No exception with dropped backend connection!");
        assertThrows(ExecutionException.class, () -> svc.add(resource, mockSession, rdf.createDataset()).get(),
                "No exception with dropped backend connection!");
    }

    @Test
    public void testGetContainer() {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        assertFalse(svc.getContainer(root).isPresent(), "parent found for root container!");
        assertTrue(svc.getContainer(resource).isPresent(), "no parent found for non-root!");
        assertEquals(root, svc.getContainer(resource).get(), "incorrect resource parent!");
        assertTrue(svc.getContainer(child).isPresent(), "no parent found for child!");
        assertEquals(resource, svc.getContainer(child).get(), "incorrect child resource parent!");
    }

    @Test
    public void testPutLdpRs() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        final BlankNode bnode = rdf.createBlankNode();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.RDFSource, dataset, root, null).join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.RDFSource, 1L, 3L, 3L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();
    }

    @Test
    public void testPutLdpRsWithoutBaseUrl() throws Exception {
        when(mockSession.getProperty(TRELLIS_SESSION_BASE_URL)).thenReturn(empty());
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.RDFSource, dataset, root, null).join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.RDFSource, 1L, 3L, 1L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();
    }

    @Test
    public void testPutLdpNr() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final IRI binaryIdentifier = rdf.createIRI("foo:binary");
        final Instant binaryTime = now();
        final Dataset dataset = rdf.createDataset();
        final Binary binary = new Binary(binaryIdentifier, binaryTime, "text/plain", 10L);
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.NonRDFSource, dataset, root, binary).join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.NonRDFSource, 1L, 7L, 1L, 0L)),
            svc.get(resource).thenAccept(res ->
                assertAll("Check binary", checkBinary(res, binaryIdentifier, binaryTime, "text/plain", 10L))),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        final IRI resource3 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/notachild");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, resource3, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertNull(svc.create(resource3, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(resource3).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource3, LDP.RDFSource, evenLater));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 3L, 0L, 1L, 0L, 0L));
                assertFalse(res.getBinary().isPresent(), "Unexpected binary metadata!");
            }),
            svc.get(root).thenAccept(checkRoot(later, 1L)),
            svc.get(root).thenAccept(res -> {
                assertTrue(res.getModified().isBefore(evenLater), "out-of-order modification sequence (1)!");
                assertFalse(res.getBinary().isPresent(), "unexpected binary metadata!");
            }),
            svc.get(resource).thenAccept(checkResource(later, LDP.NonRDFSource, 1L, 7L, 1L, 0L)),
            svc.get(resource).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater), "out-of-order modification sequence (2)!"))).join();

        verify(mockEventService, times(3)).emit(any());
    }

    @Test
    public void testPutLdpC() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        final BlankNode bnode = rdf.createBlankNode();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("resource"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.alternative, rdf.createLiteral("alt title"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("description"));
        dataset.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.Container, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.Container, 3L, 3L, 3L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        // Now add a child resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("child"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater, 1L, 3L, 1L)),
            svc.get(resource).thenAccept(checkResource(evenLater, LDP.Container, 3L, 3L, 3L, 1L)),
            svc.get(root).thenAccept(checkRoot(later, 1L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater), "out-of-order creation sequence!"))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now update that child resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.description, rdf.createLiteral("a description"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.label, rdf.createLiteral("other title"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.seeAlso, rdf.createIRI("http://example.com"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Update);

        final Instant evenLater2 = meanwhile();

        assertNull(svc.replace(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 3L, 3L, 2L)),
            svc.get(resource).thenAccept(checkResource(evenLater, LDP.Container, 3L, 3L, 3L, 1L)),
            svc.get(resource).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "out-of-order create sequence (1)!")),
            svc.get(root).thenAccept(checkRoot(later, 1L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "out-of-order create sequence (2)!"))).join();

        verify(mockEventService, times(5)).emit(any());
    }

    @Test
    public void testAddAuditTriples() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset1 = rdf.createDataset();
        final Dataset dataset2 = rdf.createDataset();
        final BlankNode bnode = rdf.createBlankNode();
        dataset1.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("resource"));
        dataset1.add(Trellis.PreferUserManaged, resource, DC.alternative, rdf.createLiteral("alt title"));
        dataset2.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode);
        dataset2.add(Trellis.PreferAudit, bnode, RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.Container, dataset1, root, null).join(),
                "Unsuccessful create operation!");
        assertNull(svc.add(resource, mockSession, dataset2).join(), "Unsuccessful add operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.Container, 2L, 3L, 2L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());
    }

    @Test
    public void testPutDeleteLdpC() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("resource title"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("resource description"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.Container, dataset, root, null).join(),
                "Unsuccessful create operaion!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.Container, 2L, 3L, 1L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        // Now add a child resource
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("child"));
        dataset.add(Trellis.PreferUserManaged, child, DC.description, rdf.createLiteral("nested resource"));

        final Instant evenLater = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(child).thenAccept(checkChild(evenLater, 2L, 3L, 1L)),
            svc.get(resource).thenAccept(checkResource(evenLater, LDP.Container, 2L, 3L, 1L, 1L)),
            svc.get(root).thenAccept(checkRoot(later, 1L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater), "out-of-order creation sequence!"))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now delete the child resource
        final BlankNode bnode = rdf.createBlankNode();
        dataset.clear();
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, AS.Delete);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferServerManaged, child, RDF.type, LDP.Resource);

        final Instant preDelete = meanwhile();

        assertNull(svc.delete(child, mockSession, LDP.Resource, dataset).join(), "Unsuccessful delete operation!");

        allOf(
            svc.get(child).thenAccept(res -> assertEquals(DELETED_RESOURCE, res, "Incorrect resource object!")),
            svc.get(resource).thenAccept(checkResource(preDelete, LDP.Container, 2L, 3L, 1L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(preDelete), "out-of-order modification dates!"))).join();

        verify(mockEventService, times(6)).emit(any());
    }

    @Test
    public void testPutLdpBc() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.alternative, rdf.createLiteral("alt title"));
        dataset.add(Trellis.PreferUserManaged, resource, RDFS.label, rdf.createLiteral("a label"));

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.BasicContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.BasicContainer, 3L, 3L, 0L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        // Now add a child resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.label, rdf.createLiteral("label"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater, 2L, 3L, 1L)),
            svc.get(resource).thenAccept(checkResource(evenLater, LDP.BasicContainer, 3L, 3L, 0L, 1L)),
            svc.get(root).thenAccept(checkRoot(later, 1L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater), "out-of-order create sequence!"))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now update the child resource
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Update);
        dataset.add(Trellis.PreferUserManaged, child, RDFS.seeAlso, rdf.createIRI("http://www.example.com/"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.label, rdf.createLiteral("a label"));

        final Instant evenLater2 = meanwhile();

        assertNull(svc.replace(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 2L, 3L, 2L)),
            svc.get(resource).thenAccept(checkResource(evenLater, LDP.BasicContainer, 3L, 3L, 0L, 1L)),
            svc.get(resource).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "out-of-order create sequence (1)!")),
            svc.get(root).thenAccept(checkRoot(later, 1L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "out-of-order create sequence (2)!"))).join();

        verify(mockEventService, times(5)).emit(any());
    }

    @Test
    public void testPutLdpDcSelf() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("direct container"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.alternative, rdf.createLiteral("alt title"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("LDP-DC pointing to self"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, resource);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, DC.relation);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.DirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 5L, 7L, 0L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        // Now add the child resources to the ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));

        final Instant evenLater2 = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 1L, 3L, 0L)),
            svc.get(resource).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource, LDP.DirectContainer, evenLater2));
                assertAll("Check resource stream", checkResourceStream(res, 5L, 7L, 0L, 0L, 1L, 1L));
                assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource, LDP.contains, child))), "Missing contains triple!");
                assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(resource, DC.relation, child))), "Missing membership triple!");
            }),
            svc.get(root).thenAccept(checkRoot(later, 1L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "out-of-order creation sequence!"))).join();

        verify(mockEventService, times(4)).emit(any());
    }

    @Test
    public void testPutLdpDc() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("direct container"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("LDP-DC test"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, DC.relation);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.DirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 4L, 7L, 0L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("title"));

        final Instant evenLater = meanwhile();

        assertNull(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(members).thenAccept(checkMember(evenLater, 1L, 3L, 0L, 0L)),
            svc.get(members).thenAccept(res -> assertFalse(res.getBinary().isPresent(), "Unexpected binary metadata!")),
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 4L, 7L, 0L, 0L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.getModified().isBefore(evenLater), "out-of-sequence!")),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now add the child resources to the ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));

        final Instant evenLater2 = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 1L, 3L, 0L)),
            svc.get(resource).thenAccept(checkResource(evenLater2, LDP.DirectContainer, 4L, 7L, 0L, 1L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource, LDP.contains, child))), "Missing contains triple!")),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 3L, 0L, 1L)),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, DC.relation, child))), "Missing membership triple!")),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "out-of-sequence creation date!"))).join();

        verify(mockEventService, times(7)).emit(any());
    }

    @Test
    public void testPutLdpDcMultiple() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("direct container"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("multiple LDP-DC test"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, DC.relation);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.DirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 4L, 7L, 1L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("second LDP-DC"));
        dataset.add(Trellis.PreferUserManaged, resource2, DC.description, rdf.createLiteral("another LDP-DC"));
        dataset.add(Trellis.PreferUserManaged, resource2, RDFS.label, rdf.createLiteral("test multple LDP-DCs"));
        dataset.add(Trellis.PreferUserManaged, resource2, SKOS.prefLabel, rdf.createLiteral("test multple LDP-DCs"));
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.hasMemberRelation, DC.subject);

        final Instant evenLater = meanwhile();

        assertNull(svc.create(resource2, mockSession, LDP.DirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.DirectContainer, evenLater));
                assertAll("Check resource stream", checkResourceStream(res, 6L, 7L, 0L, 1L, 0L, 0L));
            }),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("member resource"));
        dataset.add(Trellis.PreferUserManaged, members, DC.description, rdf.createLiteral("LDP-RS membership test"));

        final Instant evenLater2 = meanwhile();

        assertNull(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(members).thenAccept(checkMember(evenLater2, 2L, 3L, 1L, 0L)),
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 4L, 7L, 1L, 0L)),
            svc.get(resource).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "out-of-sequence creation dates!")),
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource stream", checkResourceStream(res, 6L, 7L, 0L, 1L, 0L, 0L));
                assertTrue(res.getModified().isBefore(evenLater2), "out-of-sequence creation dates (2)!");
            }),
            svc.get(root).thenAccept(checkRoot(evenLater2, 3L))).join();

        verify(mockEventService, times(6)).emit(any());

        // Now add the child resources to the ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater3 = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater3, 1L, 3L, 1L)),
            svc.get(resource).thenAccept(checkResource(evenLater, LDP.DirectContainer, 4L, 7L, 1L, 1L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource, LDP.contains, child))), "Missing contains triple!")),
            svc.get(members).thenAccept(checkMember(evenLater3, 2L, 3L, 1L, 1L)),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, DC.relation, child))), "Missing membership triple!")),
            svc.get(root).thenAccept(checkRoot(evenLater2, 3L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater3), "out-of-sequence creation dates!"))).join();

        verify(mockEventService, times(9)).emit(any());

        // Now add a child resources to the other ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child2, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater4 = meanwhile();

        assertNull(svc.create(child2, mockSession, LDP.RDFSource, dataset, resource2, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, child2, LDP.RDFSource, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 3L, 0L, 1L, 0L, 0L));
            }),
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.DirectContainer, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 6L, 7L, 0L, 1L, 0L, 1L));
                assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource2, LDP.contains, child2))), "Missing contains triple!");
            }),
            svc.get(members).thenAccept(checkMember(evenLater4, 2L, 3L, 1L, 2L)),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, DC.subject, child2))), "Missing membership triple!")),
            svc.get(root).thenAccept(checkRoot(evenLater2, 3L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater4), "out-of-order create sequence!"))).join();

        verify(mockEventService, times(12)).emit(any());
    }

    @Test
    public void testPutLdpDcMultipleInverse() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("direct container inverse"));
        dataset.add(Trellis.PreferUserManaged, resource, RDFS.label, rdf.createLiteral("LDP-DC test"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("LDP-DC inverse test"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.isMemberOfRelation, DC.relation);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.DirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 5L, 7L, 1L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("Second LDP-DC"));
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.isMemberOfRelation, DC.subject);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertNull(svc.create(resource2, mockSession, LDP.DirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.DirectContainer, evenLater));
                assertAll("Check resource stream", checkResourceStream(res, 3L, 7L, 0L, 1L, 0L, 0L));
            }),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Membership resource"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertNull(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 3L, 1L, 0L)),
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 5L, 7L, 1L, 0L)),
            svc.get(resource).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "Out-of-order create sequence (1)!")),
            svc.get(resource2).thenAccept(res -> {
                assertTrue(res.getModified().isBefore(evenLater2), "Out-of-order create sequence (2)!");
                assertAll("Check resource stream", checkResourceStream(res, 3L, 7L, 0L, 1L, 0L, 0L));
            }),
            svc.get(root).thenAccept(checkRoot(evenLater2, 3L))).join();

        verify(mockEventService, times(6)).emit(any());

        // Now add the child resources to the ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("Child resource"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater3 = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, child, LDP.RDFSource, evenLater3));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 3L, 0L, 1L, 1L, 0L));
            }),
            svc.get(resource).thenAccept(checkResource(evenLater3, LDP.DirectContainer, 5L, 7L, 1L, 1L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource, LDP.contains, child))), "Missing contains triple!")),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 3L, 1L, 0L)),
            svc.get(members).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater3), "Out-of-order creation sequence (1)!")),
            svc.get(root).thenAccept(checkRoot(evenLater2, 3L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater3), "Out-of-order creation sequence (2)!"))).join();

        verify(mockEventService, times(8)).emit(any());

        // Now add a child resources to the other ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child2, DC.title, rdf.createLiteral("Second child resource"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater4 = meanwhile();

        assertNull(svc.create(child2, mockSession, LDP.RDFSource, dataset, resource2, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, child2, LDP.RDFSource, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 3L, 0L, 1L, 1L, 0L));
            }),
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.DirectContainer, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 3L, 7L, 0L, 1L, 0L, 1L));
                assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource2, LDP.contains, child2))), "Missing contains triple!");
            }),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 3L, 1L, 0L)),
            svc.get(members).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater4), "Out-of-order create sequence (1)!")),
            svc.get(root).thenAccept(checkRoot(evenLater2, 3L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater4), "Out-of-order create sequence (2)!"))).join();

        verify(mockEventService, times(10)).emit(any());
    }

    @Test
    public void testPutLdpIc() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        final BlankNode bnode0 = rdf.createBlankNode();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("Indirect Container"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("Test LDP-IC"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.subject, rdf.createIRI("http://example.com/subject"));
        dataset.add(Trellis.PreferUserManaged, resource, RDF.type, SKOS.Concept);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode0);
        dataset.add(Trellis.PreferAudit, bnode0, PROV.atTime, rdf.createLiteral(now().toString(), XSD.dateTime));
        dataset.add(Trellis.PreferAudit, bnode0, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferAudit, bnode0, RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.IndirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 7L, 7L, 4L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        // Now add a membership resource
        final BlankNode bnode1 = rdf.createBlankNode();
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Membership resource"));
        dataset.add(Trellis.PreferAudit, members, PROV.wasGeneratedBy, bnode1);
        dataset.add(Trellis.PreferAudit, bnode1, PROV.atTime, rdf.createLiteral(now().toString(), XSD.dateTime));
        dataset.add(Trellis.PreferAudit, bnode1, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferAudit, bnode1, RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertNull(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(members).thenAccept(checkMember(evenLater, 1L, 3L, 4L, 0L)),
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 7L, 7L, 4L, 0L)),
            svc.get(resource).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater), "out-of-order creation sequence!")),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now add the child resources to the ldp-dc
        final BlankNode bnode2 = rdf.createBlankNode();
        final Literal label = rdf.createLiteral("label-1");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label);
        dataset.add(Trellis.PreferAudit, child, PROV.wasGeneratedBy, bnode2);
        dataset.add(Trellis.PreferAudit, bnode2, RDF.type, AS.Create);
        dataset.add(Trellis.PreferAudit, bnode2, RDF.type, PROV.Activity);

        final Instant evenLater2 = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 1L, 3L, 3L)),
            svc.get(resource).thenAccept(checkResource(evenLater2, LDP.IndirectContainer, 7L, 7L, 4L, 1L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource, LDP.contains, child))), "Missing contains triple!")),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 3L, 4L, 1L)),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, RDFS.label, label))), "Missing member triple!")),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "out-of-order creation sequence!"))).join();

        verify(mockEventService, times(7)).emit(any());
    }

    @Test
    public void testPutLdpIcDefaultContent() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("Indirect Container"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description,
                rdf.createLiteral("LDP-IC with default content"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, LDP.MemberSubject);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.IndirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 5L, 7L, 1L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Member resource"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertNull(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(members).thenAccept(checkMember(evenLater, 1L, 3L, 1L, 0L)),
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 5L, 7L, 1L, 0L)),
            svc.get(resource).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater), "Incorrect modification sequence!")),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now add the child resources to the ldp-dc
        final Literal label = rdf.createLiteral("label1");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 1L, 3L, 1L)),
            svc.get(resource).thenAccept(checkResource(evenLater2, LDP.IndirectContainer, 5L, 7L, 1L, 1L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource, LDP.contains, child))), "Missing contains triple!")),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 3L, 1L, 1L)),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, RDFS.label, child))), "Missing membership triple!")),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater2), "Bad event sequence!"))).join();

        verify(mockEventService, times(7)).emit(any());
    }

    @Test
    public void testPutLdpIcMultipleStatements() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        final BlankNode bnode = rdf.createBlankNode();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("LDP-IC with multiple stmts"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.IndirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 4L, 7L, 2L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Membership LDP-RS"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertNull(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(members).thenAccept(checkMember(evenLater, 1L, 3L, 1L, 0L)),
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 4L, 7L, 2L, 0L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.getModified().isBefore(evenLater),
                    "Incorrect modification sequence!")),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now add the child resources to the ldp-dc
        final Literal label1 = rdf.createLiteral("Label", "en");
        final Literal label2 = rdf.createLiteral("Zeichnung", "de");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label1);
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label2);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 2L, 3L, 1L)),
            svc.get(resource).thenAccept(checkResource(evenLater2, LDP.IndirectContainer, 4L, 7L, 2L, 1L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource, LDP.contains, child))), "Missing contains triple!")),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 3L, 1L, 2L)),
            svc.get(members).thenAccept(res -> {
                assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, RDFS.label, label2))), "Missing member triple (1)!");
                assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, RDFS.label, label1))), "Missing member triple (2)!");
            }),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)),
            svc.get(root).thenAccept(res -> assertTrue(res.getModified().isBefore(evenLater2),
                    "Out-of-sequence resource dates!"))).join();

        verify(mockEventService, times(7)).emit(any());
    }

    @Test
    public void testPutLdpIcMultipleResources() throws Exception {
        final ResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())), idService, mockEventService);

        final Dataset dataset = rdf.createDataset();
        final BlankNode bnode = rdf.createBlankNode();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("First LDP-IC"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("Test multiple LDP-ICs"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertNull(svc.create(resource, mockSession, LDP.IndirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 5L, 7L, 3L, 0L)),
            svc.get(root).thenAccept(checkRoot(later, 1L))).join();

        verify(mockEventService, times(2)).emit(any());

        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("Second LDP-IC"));
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertNull(svc.create(resource2, mockSession, LDP.IndirectContainer, dataset, root, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.IndirectContainer, evenLater));
                assertAll("Check resource stream", checkResourceStream(res, 4L, 7L, 0L, 1L, 0L, 0L));
            }),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L))).join();

        verify(mockEventService, times(4)).emit(any());

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Shared member resource"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertNull(svc.create(members, mockSession, LDP.RDFSource, dataset, root, null).join(),
                "Unsuccessfult create operation!");
        allOf(
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 3L, 1L, 0L)),
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 5L, 7L, 3L, 0L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.getModified().isBefore(evenLater2),
                    "Incorrect modification sequence of first resource!")),
            svc.get(resource2).thenAccept(res -> {
                assertTrue(res.getModified().isBefore(evenLater2), "Incorrect modification sequence!");
                assertAll("Check resource stream", checkResourceStream(res, 4L, 7L, 0L, 1L, 0L, 0L));
            }),
            svc.get(root).thenAccept(checkRoot(evenLater, 3L))).join();

        verify(mockEventService, times(6)).emit(any());

        // Now add the child resources to the ldp-ic
        final Literal label = rdf.createLiteral("first label");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater3 = meanwhile();

        assertNull(svc.create(child, mockSession, LDP.RDFSource, dataset, resource, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater3, 1L, 3L, 1L)),
            svc.get(resource).thenAccept(checkResource(evenLater3, LDP.IndirectContainer, 5L, 7L, 3L, 1L)),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource, LDP.contains, child))), "Missing contains triple!")),
            svc.get(members).thenAccept(checkMember(evenLater3, 1L, 3L, 1L, 1L)),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, RDFS.label, label))), "Missing membership triple!")),
            svc.get(root).thenAccept(checkRoot(evenLater, 3L)),
            svc.get(root).thenAccept(res ->
                assertTrue(res.getModified().isBefore(evenLater3), "Incorrect date sequence!"))).join();

        verify(mockEventService, times(9)).emit(any());

        // Now add the child resources to the ldp-ic
        final Literal label2 = rdf.createLiteral("second label");
        final BlankNode bnode2 = rdf.createBlankNode();
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child2, SKOS.prefLabel, label2);
        dataset.add(Trellis.PreferAudit, child2, PROV.wasGeneratedBy, bnode2);
        dataset.add(Trellis.PreferAudit, bnode2, PROV.atTime, rdf.createLiteral(now().toString(), XSD.dateTime));
        dataset.add(Trellis.PreferAudit, bnode2, RDF.type, AS.Create);

        final Instant evenLater4 = meanwhile();

        assertNull(svc.create(child2, mockSession, LDP.RDFSource, dataset, resource2, null).join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, child2, LDP.RDFSource, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 3L, 0L, 3L, 0L, 0L));
            }),
            svc.get(resource).thenAccept(checkResource(evenLater3, LDP.IndirectContainer, 5L, 7L, 3L, 1L)),
            svc.get(resource).thenAccept(res -> {
                assertTrue(res.getModified().isBefore(evenLater4), "Incorrect modification date!");
                assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createTriple(resource, LDP.contains, child))), "Missing containment triple!");
            }),
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.IndirectContainer, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 4L, 7L, 0L, 1L, 0L, 1L));
                assertTrue(res.stream(LDP.PreferContainment).anyMatch(isEqual(rdf.createTriple(resource2, LDP.contains,
                                    child2))), "Missing containment triple!");
            }),
            svc.get(members).thenAccept(checkMember(evenLater4, 1L, 3L, 1L, 2L)),
            svc.get(members).thenAccept(res -> {
                assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, RDFS.label, label))), "Missing member triple (1)!");
                assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createTriple(members, RDFS.label, label2))), "Missing member triple (2)!");
            }),
            svc.get(root).thenAccept(checkRoot(evenLater, 3L)),
            svc.get(root).thenAccept(res ->
                    assertTrue(res.getModified().isBefore(evenLater4), "Incorrect resource date"))).join();

        verify(mockEventService, times(12)).emit(any());
    }

    private static Consumer<Resource> checkChild(final Instant time, final long properties, final long server,
            final long audit) {
        return res -> {
            assertAll("Check resource", checkResource(res, child, LDP.RDFSource, time));
            assertAll("Check resource stream", checkResourceStream(res, properties, server, 0L, audit, 0L, 0L));
        };
    }

    private static Consumer<Resource> checkRoot(final Instant time, final long children) {
        return res -> {
            assertAll("Check resource", checkResource(res, root, LDP.BasicContainer, time));
            assertAll("Check resource stream", checkResourceStream(res, 0L, 2L, 5L, 0L, 0L, children));
        };
    }

    private static Consumer<Resource> checkResource(final Instant time, final IRI ldpType, final long properties,
            final long server, final long audit, final long children) {
        return res -> {
            assertAll("Check resource", checkResource(res, resource, ldpType, time));
            assertAll("Check resource stream", checkResourceStream(res, properties, server, 0L, audit, 0L, children));
        };
    }

    private static Consumer<Resource> checkMember(final Instant time, final long properties, final long server,
            final long audit, final long membership) {
        return res -> {
            assertAll("Check resource", checkResource(res, members, LDP.RDFSource, time));
            assertAll("Check resource stream", checkResourceStream(res, properties, server, 0L, audit, membership, 0L));
        };
    }

    private static Stream<Executable> checkResource(final Resource res, final IRI identifier, final IRI ldpType,
            final Instant time) {
        return Stream.of(
                () -> assertNotNull(res, "Missing resource!"),
                () -> assertEquals(identifier, res.getIdentifier(), "Incorrect identifier!"),
                () -> assertEquals(ldpType, res.getInteractionModel(), "Incorrect interaction model!"),
                () -> assertFalse(res.getModified().isBefore(time), "Non-sequential date!"),
                () -> assertFalse(res.getModified().isAfter(now().plusMillis(5L)), "modification date in the future!"));
    }

    private static Stream<Executable> checkBinary(final Resource res, final IRI identifier, final Instant time,
            final String mimeType, final Long size) {
        return Stream.of(
                () -> assertNotNull(res, "Missing resource!"),
                () -> assertTrue(res.getBinary().isPresent(), "missing binary metadata!"),
                () -> assertEquals(identifier, res.getBinary().get().getIdentifier(), "Incorrect binary identifier!"),
                () -> assertEquals(mimeType, res.getBinary().flatMap(Binary::getMimeType).orElse(null),
                                   "Incorrect binary mimetype!"),
                () -> assertEquals(size, res.getBinary().flatMap(Binary::getSize).orElse(null),
                                   "Incorrect binary size!"),
                () -> assertEquals(time, res.getBinary().get().getModified(), "Incorrect binary modification date!"));
    }

    private static Stream<Executable> checkResourceStream(final Resource res, final long userManaged,
            final long serverManaged, final long accessControl, final long audit, final long membership,
            final long containment) {
        final long total = userManaged + serverManaged + accessControl + audit + membership + containment;
        return Stream.of(
                () -> assertEquals(userManaged, res.stream(Trellis.PreferUserManaged).count(),
                                   "Incorrect user triple count!"),
                () -> assertEquals(serverManaged, res.stream(Trellis.PreferServerManaged).count(),
                                   "Incorrect server triple count!"),
                () -> assertEquals(accessControl, res.stream(Trellis.PreferAccessControl).count(),
                                   "Incorrect ACL triple count!"),
                () -> assertEquals(audit, res.stream(Trellis.PreferAudit).count(), "Incorrect audit triple count!"),
                () -> assertEquals(membership, res.stream(LDP.PreferMembership).count(),
                                   "Incorrect member triple count!"),
                () -> assertEquals(containment, res.stream(LDP.PreferContainment).count(),
                                   "Incorrect containment triple count!"),
                () -> assertEquals(total, res.stream().count(), "Incorrect total triple count!"));
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
