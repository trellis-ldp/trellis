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
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Predicate.isEqual;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Metadata.builder;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import java.io.File;
import java.time.Instant;
import java.util.Map;
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
import org.apache.jena.rdfconnection.RDFConnectionLocal;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.update.UpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
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
class TriplestoreResourceServiceTest {

    private static final JenaRDF rdf = new JenaRDF();
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
    private static final IRI resource2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2");
    private static final IRI members = rdf.createIRI(TRELLIS_DATA_PREFIX + "members");
    private static final IRI child = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/child");
    private static final IRI child2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource2/child");

    static {
        setDefaultPollInterval(100L, MILLISECONDS);
    }

    @Mock
    private RDFConnection mockRdfConnection;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void testIdentifierService() {
        final ResourceService svc = new TriplestoreResourceService();
        assertNotEquals(svc.generateIdentifier(), svc.generateIdentifier(), "Not unique identifiers!");
        assertNotEquals(svc.generateIdentifier(), svc.generateIdentifier(), "Not unique identifiers!");
    }

    @Test
    void testResourceNotFound() {
        final ResourceService svc = new TriplestoreResourceService();
        assertEquals(MISSING_RESOURCE, svc.get(rdf.createIRI(TRELLIS_DATA_PREFIX + "missing")).toCompletableFuture()
                .join(), "Not a missing resource!");
    }

    @Test
    void testNoRoot() {
        final ResourceService svc = new TriplestoreResourceService();

        assertEquals(MISSING_RESOURCE, svc.get(rdf.createIRI(TRELLIS_DATA_PREFIX)).toCompletableFuture().join(),
                "Not a missing resource!");
    }

    @Test
    void testInitializeRoot() {
        final Instant early = now();
        final TriplestoreResourceService svc = new TriplestoreResourceService();
        svc.initialize();

        final Resource res = svc.get(root).toCompletableFuture().join();
        assertAll("Check resource", checkResource(res, root, LDP.BasicContainer, early));
        assertAll("Check resource stream", checkResourceStream(res, 0L, 5L, 0L, 0L, 0L));
    }

    @Test
    void testInitializeRoot2() {
        final Instant early = now();
        final JenaDataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferServerManaged, root, RDF.type, LDP.BasicContainer);
        dataset.add(Trellis.PreferServerManaged, root, DC.modified, rdf.createLiteral(early.toString(), XSD.dateTime));

        final RDFConnection rdfConnection = connect(wrap(dataset.asJenaDatasetGraph()));
        final TriplestoreResourceService svc = new TriplestoreResourceService(rdfConnection);
        svc.initialize();

        final Resource res = svc.get(root).toCompletableFuture().join();
        assertAll("Check resource", checkResource(res, root, LDP.BasicContainer, early));
        assertAll("Check resource stream", checkResourceStream(res, 0L, 0L, 0L, 0L, 0L));
    }

    @Test
    void testUpdateRoot() {
        final Instant early = now();
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Resource res1 = svc.get(root).toCompletableFuture().join();
        assertAll("Check resource", checkResource(res1, root, LDP.BasicContainer, early));
        assertAll("Check resource stream", checkResourceStream(res1, 0L, 5L, 0L, 0L, 0L));

        final Dataset data = rdf.createDataset();
        svc.get(root).thenAccept(res ->
                res.stream().filter(q -> !q.getGraphName().filter(Trellis.PreferServerManaged::equals).isPresent())
                        .filter(q -> !q.getPredicate().equals(RDF.type) && !q.getObject().equals(LDP.BasicContainer))
                        .forEach(data::add)).toCompletableFuture().join();
        data.add(Trellis.PreferUserManaged, root, RDFS.label, rdf.createLiteral("Resource Label"));
        data.add(Trellis.PreferUserManaged, root, RDFS.seeAlso, rdf.createIRI("http://example.com"));
        data.add(Trellis.PreferUserManaged, root, LDP.inbox, rdf.createIRI("http://ldn.example.com/"));
        data.add(Trellis.PreferUserManaged, root, RDF.type, rdf.createLiteral("Some weird type"));
        data.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Update);

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> svc.replace(builder(root).interactionModel(LDP.BasicContainer).build(), data)
                .toCompletableFuture().join(), "Unsuccessful replace operation!");
        final Resource res2 = svc.get(root).toCompletableFuture().join();
        assertAll("Check resource", checkResource(res2, root, LDP.BasicContainer, later));
        assertAll("Check resource stream", checkResourceStream(res2, 4L, 5L, 1L, 0L, 0L));
    }

    @Test
    void testRDFConnectionError() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(mockRdfConnection);
        svc.initialize();
        doThrow(new RuntimeException("Expected exception")).when(mockRdfConnection).update(any(UpdateRequest.class));
        doThrow(new RuntimeException("Expected exception")).when(mockRdfConnection)
            .loadDataset(any(org.apache.jena.query.Dataset.class));

        assertThrows(ExecutionException.class, () ->
                svc.create(builder(resource).interactionModel(LDP.RDFSource).container(root).build(),
                    rdf.createDataset()).toCompletableFuture().get(),
                "No (create) exception with dropped backend connection!");
        assertThrows(ExecutionException.class, () -> svc.add(resource, rdf.createDataset()).toCompletableFuture().get(),
                "No (add) exception with dropped backend connection!");
        assertThrows(ExecutionException.class, () ->
                svc.delete(builder(resource).interactionModel(LDP.RDFSource).container(root).build())
                .toCompletableFuture().get(),
                "No (delete) exception with dropped backend connection!");
        assertThrows(ExecutionException.class, () -> svc.touch(resource).toCompletableFuture().get(),
                "No (touch) exception with dropped backend connection!");
    }

    @Test
    void testPutLdpRs() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Dataset dataset = rdf.createDataset();
        final BlankNode bnode = rdf.createBlankNode();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> allOf(
            svc.create(builder(resource).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                .toCompletableFuture(),
            svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.RDFSource, 1L, 3L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpRsWithoutBaseUrl() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> allOf(
            svc.create(builder(resource).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                .toCompletableFuture(),
            svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.RDFSource, 1L, 1L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpNr() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final IRI binaryIdentifier = rdf.createIRI("foo:binary");
        final Dataset dataset = rdf.createDataset();
        final BinaryMetadata binary = BinaryMetadata.builder(binaryIdentifier).mimeType("text/plain").build();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> allOf(
              svc.create(builder(resource).interactionModel(LDP.NonRDFSource).container(root).binary(binary).build(),
                  dataset).toCompletableFuture(),
              svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.NonRDFSource, 1L, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(res ->
                assertAll("Check binary", checkBinary(res, binaryIdentifier, "text/plain"))).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        final IRI resource3 = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/notachild");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, resource3, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() ->
                svc.create(builder(resource3).interactionModel(LDP.RDFSource).build(), dataset).toCompletableFuture()
                .join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource3).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource3, LDP.RDFSource, evenLater));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 0L, 1L, 0L, 0L));
                assertFalse(res.getBinaryMetadata().isPresent(), "Unexpected binary metadata!");
            }).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater)).toCompletableFuture(),
            svc.get(root).thenAccept(res ->
                assertFalse(res.getBinaryMetadata().isPresent(), "unexpected binary metadata!")).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later, LDP.NonRDFSource, 1L, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(evenLater)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpC() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Dataset dataset = rdf.createDataset();
        final BlankNode bnode = rdf.createBlankNode();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("resource"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.alternative, rdf.createLiteral("alt title"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("description"));
        dataset.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> allOf(
                svc.create(builder(resource).interactionModel(LDP.Container).container(root).build(), dataset)
                    .toCompletableFuture(),
                svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.Container, 3L, 3L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        // Now add a child resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("child"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() -> allOf(
            svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                .toCompletableFuture(),
            svc.touch(resource).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(child).thenAccept(checkChild(evenLater, 1L, 1L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater, LDP.Container, 3L, 3L, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater)).toCompletableFuture()).join();

        // Now update that child resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.description, rdf.createLiteral("a description"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.label, rdf.createLiteral("other title"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.seeAlso, rdf.createIRI("http://example.com"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Update);

        final Instant evenLater2 = meanwhile();

        assertDoesNotThrow(() -> svc.replace(builder(child).interactionModel(LDP.RDFSource).container(resource).build(),
                    dataset).toCompletableFuture().join(),
                "Unsuccessful create operation!");
        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 3L, 2L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater, LDP.Container, 3L, 3L, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater2)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(evenLater2)).toCompletableFuture()).join();
    }

    @Test
    void testAddAuditTriples() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Dataset dataset1 = rdf.createDataset();
        final Dataset dataset2 = rdf.createDataset();
        final BlankNode bnode = rdf.createBlankNode();
        dataset1.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("resource"));
        dataset1.add(Trellis.PreferUserManaged, resource, DC.alternative, rdf.createLiteral("alt title"));
        dataset2.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode);
        dataset2.add(Trellis.PreferAudit, bnode, RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> allOf(
              svc.create(builder(resource).interactionModel(LDP.Container).container(root).build(), dataset1)
                .toCompletableFuture(),
              svc.add(resource, dataset2).toCompletableFuture(),
              svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.Container, 2L, 2L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();
    }

    @Test
    void testPutDeleteLdpC() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("resource title"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("resource description"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> allOf(
              svc.create(builder(resource).interactionModel(LDP.Container).container(root).build(), dataset)
                  .toCompletableFuture(),
              svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.Container, 2L, 1L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        // Now add a child resource
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("child"));
        dataset.add(Trellis.PreferUserManaged, child, DC.description, rdf.createLiteral("nested resource"));

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() -> allOf(
              svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                .toCompletableFuture(),
              svc.touch(resource).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(child).thenAccept(checkChild(evenLater, 2L, 1L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater, LDP.Container, 2L, 1L, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater)).toCompletableFuture()).join();

        // Now delete the child resource
        final BlankNode bnode = rdf.createBlankNode();
        dataset.clear();
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, AS.Delete);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferServerManaged, child, RDF.type, LDP.Resource);

        final Instant preDelete = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.delete(builder(child).interactionModel(LDP.RDFSource).container(resource).build())
                        .toCompletableFuture(),
                      svc.touch(resource).toCompletableFuture()).join(), "Unsuccessful delete operation!");

        allOf(
            svc.get(child).thenAccept(res -> assertEquals(DELETED_RESOURCE, res, "Incorrect resource object!"))
                .toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(preDelete, LDP.Container, 2L, 1L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(preDelete)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpBc() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.alternative, rdf.createLiteral("alt title"));
        dataset.add(Trellis.PreferUserManaged, resource, RDFS.label, rdf.createLiteral("a label"));

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> allOf(
              svc.create(builder(resource).interactionModel(LDP.BasicContainer).container(root).build(), dataset)
                .toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.BasicContainer, 3L, 0L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        // Now add a child resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("title"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.label, rdf.createLiteral("label"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later2 = meanwhile();

        assertDoesNotThrow(() -> allOf(svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource)
                        .build(), dataset).toCompletableFuture(), svc.touch(resource).toCompletableFuture()).join(),
                "Unsuccessful create operation (basic containment)!");

        allOf(
            svc.get(child).thenAccept(checkChild(later2, 2L, 1L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later2, LDP.BasicContainer, 3L, 0L, 1L))
                .toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(later2)).toCompletableFuture()).join();

        // Now update the child resource
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Update);
        dataset.add(Trellis.PreferUserManaged, child, RDFS.seeAlso, rdf.createIRI("http://www.example.com/"));
        dataset.add(Trellis.PreferUserManaged, child, RDFS.label, rdf.createLiteral("a label"));

        final Instant later3 = meanwhile();

        assertDoesNotThrow(() -> svc.replace(builder(child).interactionModel(LDP.RDFSource).container(resource).build(),
                    dataset).toCompletableFuture().join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(child).thenAccept(checkChild(later3, 2L, 2L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later2, LDP.BasicContainer, 3L, 0L, 1L))
                .toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(later3)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(later3)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpDcSelf() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final IRI container = rdf.createIRI(resource.getIRIString() + "/");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("direct container"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.alternative, rdf.createLiteral("alt title"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("LDP-DC pointing to self"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, container);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, DC.relation);

        final Instant later = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(resource).interactionModel(LDP.DirectContainer).membershipResource(container)
                        .memberRelation(DC.relation).container(root).build(), dataset).toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 5L, 0L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        // Now add the child resources to the ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("ldp-dc (self) child resource"));

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(resource).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(child).thenAccept(checkChild(evenLater, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource, LDP.DirectContainer, evenLater));
                assertAll("Check resource stream", checkResourceStream(res, 5L, 0L, 0L, 1L, 1L));
                assertTrue(res.stream(LDP.PreferContainment).anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment,
                                    container, LDP.contains, child))), "Missing contains triple!");
                assertTrue(res.stream(LDP.PreferMembership).anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership,
                                    container, DC.relation, child))), "Missing membership triple!");
            }).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpDc() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("direct container"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("LDP-DC test"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, DC.relation);

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> allOf(
                svc.create(builder(resource).interactionModel(LDP.DirectContainer).container(root)
                    .memberRelation(DC.relation).membershipResource(members).build(), dataset).toCompletableFuture(),
                svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 4L, 0L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("member resource"));

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(members).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(members).thenAccept(checkMember(evenLater, 1L, 0L, 0L)).toCompletableFuture(),
            svc.get(members).thenAccept(res -> assertFalse(res.getBinaryMetadata().isPresent(),
                    "Unexpected binary metadata!")).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 4L, 0L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(evenLater)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture()).join();

        // Now add the child resources to the ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("ldp-dc child resource"));

        final Instant evenLater2 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(members).toCompletableFuture(), svc.touch(resource).toCompletableFuture()).join(),
                "Unsuccessful create operation!");

        final IRI container = rdf.createIRI(resource.getIRIString() + "/");

        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater2, LDP.DirectContainer, 4L, 0L, 1L))
                .toCompletableFuture(),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment, container, LDP.contains, child))),
                    "Missing contains triple!")).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 0L, 1L)).toCompletableFuture(),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership, members, DC.relation, child))),
                    "Missing membership triple!")).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater2)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpDcMultiple() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("direct container"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("multiple LDP-DC test"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, DC.relation);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(resource).interactionModel(LDP.DirectContainer).container(root)
                    .membershipResource(members).memberRelation(DC.relation).build(), dataset).toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 4L, 1L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("second LDP-DC"));
        dataset.add(Trellis.PreferUserManaged, resource2, DC.description, rdf.createLiteral("another LDP-DC"));
        dataset.add(Trellis.PreferUserManaged, resource2, RDFS.label, rdf.createLiteral("test multple LDP-DCs"));
        dataset.add(Trellis.PreferUserManaged, resource2, SKOS.prefLabel, rdf.createLiteral("test multple LDP-DCs"));
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.hasMemberRelation, DC.subject);

        final Instant later2 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(resource2).interactionModel(LDP.DirectContainer).container(root)
                        .membershipResource(members).memberRelation(DC.subject).build(), dataset).toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.DirectContainer, later2));
                assertAll("Check resource stream", checkResourceStream(res, 6L, 0L, 1L, 0L, 0L));
            }).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later2, 2L)).toCompletableFuture()).join();

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("member resource"));
        dataset.add(Trellis.PreferUserManaged, members, DC.description, rdf.createLiteral("LDP-RS membership test"));

        final Instant later3 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(members).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(members).thenAccept(checkMember(later3, 2L, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 4L, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(later3)).toCompletableFuture(),
            svc.get(resource2).thenAccept(checkPredates(later3)).toCompletableFuture(),
            svc.get(resource2).thenAccept(res ->
                assertAll("Check resource stream", checkResourceStream(res, 6L, 0L, 1L, 0L, 0L))).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later3, 3L)).toCompletableFuture()).join();

        // Now add the child resources to the ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("ldp-dc (1) child resource"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later4 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(resource).toCompletableFuture(), svc.touch(members).toCompletableFuture()).join(),
                "Unsuccessful create operation (multiple/primary direct containment)!");

        final IRI container = rdf.createIRI(resource.getIRIString() + "/");

        allOf(
            svc.get(child).thenAccept(checkChild(later4, 1L, 1L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later2, LDP.DirectContainer, 4L, 1L, 1L)).toCompletableFuture(),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment, container, LDP.contains, child))),
                    "Missing contains triple!")).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(later4, 2L, 1L, 1L)).toCompletableFuture(),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership, members, DC.relation, child))),
                    "Missing membership triple!")).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later3, 3L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(later4)).toCompletableFuture()).join();

        // Now add a child resources to the other ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, child2, DC.title, rdf.createLiteral("ldp-dc (2) child resource"));

        final Instant later5 = meanwhile();
        final IRI container2 = rdf.createIRI(resource2.getIRIString() + "/");

        assertDoesNotThrow(() -> allOf(svc.create(builder(child2).interactionModel(LDP.RDFSource).container(resource2)
                        .build(), dataset).toCompletableFuture(),
                      svc.touch(members).toCompletableFuture(), svc.touch(resource2).toCompletableFuture()).join(),
                "Unsuccessful create operation (Multiple/secondary direct containment)!");

        allOf(
            svc.get(child2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, child2, LDP.RDFSource, later5));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 0L, 1L, 0L, 0L));
            }).toCompletableFuture(),
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.DirectContainer, later5));
                assertAll("Check resource stream", checkResourceStream(res, 6L, 0L, 1L, 0L, 1L));
                assertTrue(res.stream(LDP.PreferContainment).anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment,
                                    container2, LDP.contains, child2))), "Missing contains triple!");
            }).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(later5, 2L, 1L, 2L)).toCompletableFuture(),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership, members, DC.subject, child2))),
                    "Missing membership triple!")).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later3, 3L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(later5)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpDcMultipleInverse() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final IRI container = rdf.createIRI(resource.getIRIString() + "/");
        final IRI container2 = rdf.createIRI(resource2.getIRIString() + "/");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("direct container inverse"));
        dataset.add(Trellis.PreferUserManaged, resource, RDFS.label, rdf.createLiteral("LDP-DC test"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description, rdf.createLiteral("LDP-DC inverse test"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.isMemberOfRelation, DC.relation);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(resource).interactionModel(LDP.DirectContainer).container(root)
                        .membershipResource(members).memberOfRelation(DC.relation).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 5L, 1L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("Second LDP-DC"));
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.isMemberOfRelation, DC.subject);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(resource2).interactionModel(LDP.DirectContainer).container(root)
                        .membershipResource(members).memberOfRelation(DC.subject).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.DirectContainer, evenLater));
                assertAll("Check resource stream", checkResourceStream(res, 3L, 0L, 1L, 0L, 0L));
            }).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture()).join();

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Membership resource"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertDoesNotThrow(() -> allOf(
                svc.create(builder(members).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                    .toCompletableFuture(),
                svc.touch(root).toCompletableFuture()).join(), "Unsuccessful membership resource create operation!");

        allOf(
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later, LDP.DirectContainer, 5L, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(evenLater2)).toCompletableFuture(),
            svc.get(resource2).thenAccept(checkPredates(evenLater2)).toCompletableFuture(),
            svc.get(resource2).thenAccept(res ->
                assertAll("Check resource stream", checkResourceStream(res, 3L, 0L, 1L, 0L, 0L))).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater2, 3L)).toCompletableFuture()).join();

        // Now add the child resources to the ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, DC.title, rdf.createLiteral("Child resource"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater3 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(resource).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(child).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, child, LDP.RDFSource, evenLater3));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 0L, 1L, 1L, 0L));
            }).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater3, LDP.DirectContainer, 5L, 1L, 1L))
                .toCompletableFuture(),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment, container, LDP.contains, child))),
                    "Missing contains triple!")).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 1L, 0L)).toCompletableFuture(),
            svc.get(members).thenAccept(checkPredates(evenLater3)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater2, 3L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater3)).toCompletableFuture()).join();

        // Now add a child resources to the other ldp-dc
        dataset.clear();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);
        dataset.add(Trellis.PreferUserManaged, child2, DC.title, rdf.createLiteral("Second child resource"));

        final Instant evenLater4 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child2).interactionModel(LDP.RDFSource).container(resource2).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(resource2).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(child2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, child2, LDP.RDFSource, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 0L, 1L, 1L, 0L));
            }).toCompletableFuture(),
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.DirectContainer, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 3L, 0L, 1L, 0L, 1L));
                assertTrue(res.stream(LDP.PreferContainment).anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment,
                                    container2, LDP.contains, child2))), "Missing contains triple!");
            }).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 1L, 0L)).toCompletableFuture(),
            svc.get(members).thenAccept(checkPredates(evenLater4)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater2, 3L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater4)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpIc() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final Dataset dataset = rdf.createDataset();
        final BlankNode bnode0 = rdf.createBlankNode();
        final IRI container = rdf.createIRI(resource.getIRIString() + "/");
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

        assertDoesNotThrow(() -> allOf(svc.create(builder(resource).interactionModel(LDP.IndirectContainer)
                .container(root).memberRelation(RDFS.label).membershipResource(members)
                .insertedContentRelation(SKOS.prefLabel).build(), dataset).toCompletableFuture(),
            svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation (indirect containment)!");

        allOf(svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 7L, 4L, 0L))
                .toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        // Now add a membership resource
        final BlankNode bnode1 = rdf.createBlankNode();
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Membership resource"));
        dataset.add(Trellis.PreferAudit, members, PROV.wasGeneratedBy, bnode1);
        dataset.add(Trellis.PreferAudit, bnode1, PROV.atTime, rdf.createLiteral(now().toString(), XSD.dateTime));
        dataset.add(Trellis.PreferAudit, bnode1, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferAudit, bnode1, RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(members).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                        .toCompletableFuture(), svc.touch(root).toCompletableFuture()).join(),
                "Unsuccessful create operation (indirect containment)!");

        allOf(
            svc.get(members).thenAccept(checkMember(evenLater, 1L, 4L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 7L, 4L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(evenLater)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture()).join();

        // Now add the child resources to the ldp-dc
        final BlankNode bnode2 = rdf.createBlankNode();
        final Literal label = rdf.createLiteral("label-1");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label);
        dataset.add(Trellis.PreferAudit, child, PROV.wasGeneratedBy, bnode2);
        dataset.add(Trellis.PreferAudit, bnode2, RDF.type, AS.Create);
        dataset.add(Trellis.PreferAudit, bnode2, RDF.type, PROV.Activity);

        final Instant evenLater2 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(members).toCompletableFuture(), svc.touch(resource).toCompletableFuture()).join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 1L, 3L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater2, LDP.IndirectContainer, 7L, 4L, 1L))
                    .toCompletableFuture(),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment, container, LDP.contains, child))),
                    "Missing contains triple!")).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 4L, 1L)).toCompletableFuture(),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership, members, RDFS.label, label))),
                    "Missing member triple!")).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater2)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpIcDefaultContent() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final IRI container = rdf.createIRI(resource.getIRIString() + "/");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("Indirect Container"));
        dataset.add(Trellis.PreferUserManaged, resource, DC.description,
                rdf.createLiteral("LDP-IC with default content"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, LDP.MemberSubject);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant later = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(resource).interactionModel(LDP.IndirectContainer).container(root)
                        .membershipResource(members).memberRelation(RDFS.label)
                        .insertedContentRelation(LDP.MemberSubject).build(), dataset).toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 5L, 1L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        // Now add a membership resource
        final BlankNode bnode = rdf.createBlankNode();
        dataset.clear();
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, AS.Create);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Member resource"));

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(members).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 5L, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(evenLater)).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(evenLater, 1L, 2L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture()).join();

        // Now add the child resources to the ldp-dc
        final Literal label = rdf.createLiteral("label1");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(members).toCompletableFuture(), svc.touch(resource).toCompletableFuture()).join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 1L, 1L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater2, LDP.IndirectContainer, 5L, 1L, 1L))
                .toCompletableFuture(),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment, container, LDP.contains, child))),
                    "Missing contains triple!")).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 2L, 1L)).toCompletableFuture(),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership, members, RDFS.label, child))),
                    "Missing membership triple!")).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater2)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpIcMultipleStatements() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final IRI container = rdf.createIRI(resource.getIRIString() + "/");
        final Dataset dataset = rdf.createDataset();
        final BlankNode bnode = rdf.createBlankNode();
        dataset.add(Trellis.PreferUserManaged, resource, DC.title, rdf.createLiteral("LDP-IC with multiple stmts"));
        dataset.add(Trellis.PreferUserManaged, resource, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferUserManaged, resource, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferAudit, resource, PROV.wasGeneratedBy, bnode);
        dataset.add(Trellis.PreferAudit, bnode, RDF.type, PROV.Activity);

        final Instant later = meanwhile();

        assertDoesNotThrow(() -> allOf(
                svc.create(builder(resource).interactionModel(LDP.IndirectContainer).container(root)
                    .membershipResource(members).memberRelation(RDFS.label)
                    .insertedContentRelation(SKOS.prefLabel).build(), dataset).toCompletableFuture(),
                svc.touch(root).toCompletableFuture()).join(),
                "Unsuccessful create operation (multiple indirect containment)!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 4L, 2L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        // Now add a membership resource
        dataset.clear();
        final BlankNode bnode2 = rdf.createBlankNode();
        dataset.add(Trellis.PreferAudit, bnode2, RDF.type, AS.Create);
        dataset.add(Trellis.PreferAudit, bnode2, RDF.type, PROV.Activity);
        dataset.add(Trellis.PreferAudit, members, PROV.wasGeneratedBy, bnode2);
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Membership LDP-RS"));

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(members).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(members).thenAccept(checkMember(evenLater, 1L, 3L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 4L, 2L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(evenLater)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture()).join();

        // Now add the child resources to the ldp-dc
        final Literal label1 = rdf.createLiteral("Label", "en");
        final Literal label2 = rdf.createLiteral("Zeichnung", "de");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label1);
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label2);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(members).toCompletableFuture(), svc.touch(resource).toCompletableFuture()).join(),
                "Unsuccessful create operation (indirect container child)!");

        allOf(
            svc.get(child).thenAccept(checkChild(evenLater2, 2L, 1L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater2, LDP.IndirectContainer, 4L, 2L, 1L))
                    .toCompletableFuture(),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment, container, LDP.contains, child))),
                    "Missing contains triple!")).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 3L, 2L)).toCompletableFuture(),
            svc.get(members).thenAccept(res -> {
                assertTrue(res.stream(LDP.PreferMembership).anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership,
                                    members, RDFS.label, label2))), "Missing member triple (1)!");
                assertTrue(res.stream(LDP.PreferMembership).anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership,
                                    members, RDFS.label, label1))), "Missing member triple (2)!");
            }).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater2)).toCompletableFuture()).join();
    }

    @Test
    void testPutLdpIcMultipleResources() {
        final TriplestoreResourceService svc = new TriplestoreResourceService(
                connect(wrap(rdf.createDataset().asJenaDatasetGraph())));
        svc.initialize();

        final IRI container = rdf.createIRI(resource.getIRIString() + "/");
        final IRI container2 = rdf.createIRI(resource2.getIRIString() + "/");
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

        assertDoesNotThrow(() -> allOf(svc.create(builder(resource).interactionModel(LDP.IndirectContainer)
                        .container(root).membershipResource(members).memberRelation(RDFS.label)
                        .insertedContentRelation(SKOS.prefLabel).build(), dataset).toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(),
                "Unsuccessful create operation (multiple indirect containment 2)!");

        allOf(
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 5L, 3L, 0L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(later, 1L)).toCompletableFuture()).join();

        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, resource2, DC.title, rdf.createLiteral("Second LDP-IC"));
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.membershipResource, members);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.insertedContentRelation, SKOS.prefLabel);
        dataset.add(Trellis.PreferUserManaged, resource2, LDP.hasMemberRelation, RDFS.label);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(resource2).interactionModel(LDP.IndirectContainer).container(root)
                        .membershipResource(members).memberRelation(RDFS.label)
                        .insertedContentRelation(SKOS.prefLabel).build(), dataset).toCompletableFuture(),
                      svc.touch(root).toCompletableFuture()).join(), "Unsuccessful create operation!");

        allOf(
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.IndirectContainer, evenLater));
                assertAll("Check resource stream", checkResourceStream(res, 4L, 0L, 1L, 0L, 0L));
            }).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 2L)).toCompletableFuture()).join();

        // Now add a membership resource
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, members, DC.title, rdf.createLiteral("Shared member resource"));
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater2 = meanwhile();

        assertDoesNotThrow(() ->
            allOf(svc.create(builder(members).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                    .toCompletableFuture(),
                  svc.touch(root).toCompletableFuture()).join(), "Unsuccessful member resource creation operation!");

        allOf(
            svc.get(members).thenAccept(checkMember(evenLater2, 1L, 1L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(later, LDP.IndirectContainer, 5L, 3L, 0L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(evenLater2)).toCompletableFuture(),
            svc.get(resource2).thenAccept(checkPredates(evenLater2)).toCompletableFuture(),
            svc.get(resource2).thenAccept(res ->
                assertAll("Check resource stream", checkResourceStream(res, 4L, 0L, 1L, 0L, 0L))).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 3L)).toCompletableFuture()).join();

        // Now add the child resources to the ldp-ic
        final Literal label = rdf.createLiteral("first label");
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child, SKOS.prefLabel, label);
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), RDF.type, AS.Create);

        final Instant evenLater3 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child).interactionModel(LDP.RDFSource).container(resource).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(members).toCompletableFuture(), svc.touch(resource).toCompletableFuture()).join(),
                "Unsuccessful child creation operation!");

        allOf(
            svc.get(child).thenAccept(checkChild(evenLater3, 1L, 1L)).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater3, LDP.IndirectContainer, 5L, 3L, 1L))
                    .toCompletableFuture(),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment, container, LDP.contains, child))),
                    "Missing contains triple!")).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(evenLater3, 1L, 1L, 1L)).toCompletableFuture(),
            svc.get(members).thenAccept(res -> assertTrue(res.stream(LDP.PreferMembership)
                    .anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership, members, RDFS.label, label))),
                    "Missing membership triple!")).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 3L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater3)).toCompletableFuture()).join();

        // Now add the child resources to the ldp-ic
        final Literal label2 = rdf.createLiteral("second label");
        final BlankNode bnode2 = rdf.createBlankNode();
        dataset.clear();
        dataset.add(Trellis.PreferUserManaged, child2, SKOS.prefLabel, label2);
        dataset.add(Trellis.PreferAudit, child2, PROV.wasGeneratedBy, bnode2);
        dataset.add(Trellis.PreferAudit, bnode2, RDF.type, AS.Create);
        dataset.add(Trellis.PreferAudit, bnode2, PROV.atTime, rdf.createLiteral(now().toString(), XSD.dateTime));

        final Instant evenLater4 = meanwhile();

        assertDoesNotThrow(() ->
                allOf(svc.create(builder(child2).interactionModel(LDP.RDFSource).container(resource2).build(), dataset)
                        .toCompletableFuture(),
                      svc.touch(members).toCompletableFuture(), svc.touch(resource2).toCompletableFuture()).join(),
                "Unsuccessful create operation!");

        allOf(
            svc.get(child2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, child2, LDP.RDFSource, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 1L, 0L, 3L, 0L, 0L));
            }).toCompletableFuture(),
            svc.get(resource).thenAccept(checkResource(evenLater3, LDP.IndirectContainer, 5L, 3L, 1L))
                .toCompletableFuture(),
            svc.get(resource).thenAccept(checkPredates(evenLater4)).toCompletableFuture(),
            svc.get(resource).thenAccept(res -> assertTrue(res.stream(LDP.PreferContainment)
                .anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment, container, LDP.contains, child))),
                "Missing containment triple!")).toCompletableFuture(),
            svc.get(resource2).thenAccept(res -> {
                assertAll("Check resource", checkResource(res, resource2, LDP.IndirectContainer, evenLater4));
                assertAll("Check resource stream", checkResourceStream(res, 4L, 0L, 1L, 0L, 1L));
                assertTrue(res.stream(LDP.PreferContainment).anyMatch(isEqual(rdf.createQuad(LDP.PreferContainment,
                                    container2, LDP.contains, child2))), "Missing containment triple!");
            }).toCompletableFuture(),
            svc.get(members).thenAccept(checkMember(evenLater4, 1L, 1L, 2L)).toCompletableFuture(),
            svc.get(members).thenAccept(res -> {
                assertTrue(res.stream(LDP.PreferMembership).anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership,
                                    members, RDFS.label, label))), "Missing member triple (1)!");
                assertTrue(res.stream(LDP.PreferMembership).anyMatch(isEqual(rdf.createQuad(LDP.PreferMembership,
                                    members, RDFS.label, label2))), "Missing member triple (2)!");
            }).toCompletableFuture(),
            svc.get(root).thenAccept(checkRoot(evenLater, 3L)).toCompletableFuture(),
            svc.get(root).thenAccept(checkPredates(evenLater4)).toCompletableFuture()).join();
    }

    @Test
    void testBuildRDFConnectionMemory() {

        final RDFConnection rdfConnection = TriplestoreResourceService.buildRDFConnection(null);
        assertNotNull(rdfConnection, "Missing RDFConnection, using in-memory dataset!");
        assertFalse(rdfConnection.isClosed(), "RDFConnection has been closed!");
        assertTrue(rdfConnection instanceof RDFConnectionLocal, "Incorrect type");
    }

    @Test
    void testBuildRDFConnectionTDB() throws Exception {
        final File dir = new File(new File(getClass().getResource("/logback-test.xml").toURI()).getParent(), "data");
        final RDFConnection rdfConnection = TriplestoreResourceService.buildRDFConnection(dir.getAbsolutePath());
        assertNotNull(rdfConnection, "Missing RDFConnection, using local file!");
        assertFalse(rdfConnection.isClosed(), "RDFConnection has been closed!");
        assertTrue(rdfConnection instanceof RDFConnectionLocal, "Incorrect type");
    }

    @Test
    void testBuildRDFConnectionRemote() {
        final RDFConnection rdfConnection = TriplestoreResourceService.buildRDFConnection("http://localhost/sparql");
        assertNotNull(rdfConnection, "Missing RDFConnection, using local HTTP!");
        assertFalse(rdfConnection.isClosed(), "RDFConnection has been closed!");
        assertTrue(rdfConnection instanceof RDFConnectionRemote, "Incorrect type");
    }

    @Test
    void testBuildRDFConnectionRemoteHTTPS() {
        final RDFConnection rdfConnection = TriplestoreResourceService.buildRDFConnection("https://localhost/sparql");
        assertNotNull(rdfConnection, "Missing RDFConnection, using local HTTP!");
        assertFalse(rdfConnection.isClosed(), "RDFConnection has been closed!");
        assertTrue(rdfConnection instanceof RDFConnectionRemote, "Incorrect type");
    }

    @Test
    void testBuildExtensionMap() {
        final Map<String, IRI> extensions = TriplestoreResourceService.buildExtensionMap(
                "foo=http://example.com/Foo,bar=http://example.com/Bar");
        assertEquals(2, extensions.size());
        assertEquals(rdf.createIRI("http://example.com/Foo"), extensions.get("foo"));
        assertEquals(rdf.createIRI("http://example.com/Bar"), extensions.get("bar"));
    }

    @Test
    void testBuildExtensionMapOddities() {
        final Map<String, IRI> extensions = TriplestoreResourceService.buildExtensionMap(
                "foo, ,bar=http://example.com/Bar, baz = , = baz ");
        assertEquals(1, extensions.size());
        assertEquals(rdf.createIRI("http://example.com/Bar"), extensions.get("bar"));
    }

    private static Consumer<Resource> checkChild(final Instant time, final long properties, final long audit) {
        return res -> {
            assertAll("Check resource", checkResource(res, child, LDP.RDFSource, time));
            assertAll("Check resource stream", checkResourceStream(res, properties, 0L, audit, 0L, 0L));
        };
    }

    private static Consumer<Resource> checkRoot(final Instant time, final long children) {
        return res -> {
            assertAll("Check resource", checkResource(res, root, LDP.BasicContainer, time));
            assertAll("Check resource stream", checkResourceStream(res, 0L, 5L, 0L, 0L, children));
        };
    }

    private static Consumer<Resource> checkPredates(final Instant time) {
        return res -> assertTrue(res.getModified().isBefore(time), "Resource " + res.getIdentifier()
                + " has an unexpected lastModified date: " + res.getModified() + " !< " + time);
    }

    private static Consumer<Resource> checkResource(final Instant time, final IRI ldpType, final long properties,
            final long audit, final long children) {
        return res -> {
            assertAll("Check resource", checkResource(res, resource, ldpType, time));
            assertAll("Check resource stream", checkResourceStream(res, properties, 0L, audit, 0L, children));
        };
    }

    private static Consumer<Resource> checkMember(final Instant time, final long properties, final long audit,
            final long membership) {
        return res -> {
            assertAll("Check resource", checkResource(res, members, LDP.RDFSource, time));
            assertAll("Check resource stream", checkResourceStream(res, properties, 0L, audit, membership, 0L));
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

    private static Stream<Executable> checkBinary(final Resource res, final IRI identifier, final String mimeType) {
        return Stream.of(
                () -> assertNotNull(res, "Missing resource!"),
                () -> assertTrue(res.getBinaryMetadata().isPresent(), "missing binary metadata!"),
                () -> assertEquals(of(identifier), res.getBinaryMetadata().map(BinaryMetadata::getIdentifier),
                                   "Incorrect binary identifier!"),
                () -> assertEquals(mimeType, res.getBinaryMetadata().flatMap(BinaryMetadata::getMimeType).orElse(null),
                                   "Incorrect binary mimetype!"));
    }

    private static Stream<Executable> checkResourceStream(final Resource res, final long userManaged,
            final long accessControl, final long audit, final long membership, final long containment) {
        final long serverManaged = 1; // LDP type triple
        final long total = userManaged + accessControl + audit + membership + containment + serverManaged;
        return Stream.of(
                () -> assertEquals(serverManaged, res.stream(Trellis.PreferServerManaged).count(),
                                   "Incorrect server triple count!"),
                () -> assertEquals(userManaged, res.stream(Trellis.PreferUserManaged).count(),
                                   "Incorrect user triple count!"),
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

    private static boolean isReallyLaterThan(final Instant time) {
        final Instant t = now();
        return t.isAfter(time) && (t.toEpochMilli() > time.toEpochMilli() || t.getNano() > time.getNano());
    }
}
