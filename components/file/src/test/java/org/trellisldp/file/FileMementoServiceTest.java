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
package org.trellisldp.file;

import static java.time.Instant.MAX;
import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Test a file-based memento service.
 */
class FileMementoServiceTest {

    private static final RDF rdf = new JenaRDF();

    @AfterAll
    static void cleanUp() throws IOException {
        final File dir = new File(FileMementoServiceTest.class.getResource("/versions").getFile()).getParentFile();
        final File vDir = new File(dir, "versions2");
        if (vDir.exists()) {
            deleteDirectory(vDir);
        }
    }

    @Test
    void testPutThenDelete() {
        final File dir = new File(getClass().getResource("/versions").getFile());
        final FileMementoService svc = new FileMementoService(dir.getAbsolutePath(), true);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "another-resource");
        final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
        final Instant time = parse("2019-08-16T14:21:01Z");

        final Resource mockResource = mock(Resource.class);

        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.getBinaryMetadata()).thenReturn(empty());
        when(mockResource.getMembershipResource()).thenReturn(empty());
        when(mockResource.getMemberOfRelation()).thenReturn(empty());
        when(mockResource.getMemberRelation()).thenReturn(empty());
        when(mockResource.getInsertedContentRelation()).thenReturn(empty());
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                    rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("Title"))));

        svc.put(mockResource).toCompletableFuture().join();

        final Resource res = svc.get(identifier, time).toCompletableFuture().join();
        assertEquals(identifier, res.getIdentifier());
        assertEquals(time, res.getModified());

        // Delete a different time, memento should still be present
        svc.delete(identifier, time.plusSeconds(100)).toCompletableFuture().join();
        assertEquals(identifier, svc.get(identifier, time).toCompletableFuture().join().getIdentifier());

        // Delete the actual memento, memento should now be gone
        svc.delete(identifier, time).toCompletableFuture().join();
        assertEquals(MISSING_RESOURCE, svc.get(identifier, time).toCompletableFuture().join());
    }

    @Test
    void testPutDisabled() {
        final File dir = new File(getClass().getResource("/versions").getFile());
        final FileMementoService svc = new FileMementoService(dir.getAbsolutePath(), false);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "another-resource");
        final Instant time = parse("2019-08-16T14:21:01Z");

        final Resource mockResource = mock(Resource.class);

        when(mockResource.getModified()).thenReturn(time);

        svc.put(mockResource).toCompletableFuture().join();

        final Resource res = svc.get(identifier, time).toCompletableFuture().join();
        assertEquals(MISSING_RESOURCE, res);
        assertDoesNotThrow(() -> svc.delete(identifier, time).toCompletableFuture().join());
    }

    @Test
    void testListDisabled() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/versions").getFile());
        assertTrue(dir.exists(), "Resource directory doesn't exist!");
        assertTrue(dir.isDirectory(), "Resource directory isn't a valid directory");

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH, dir.getAbsolutePath());
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO, "false");

            final MementoService svc = new FileMementoService();

            assertEquals(0L, svc.mementos(identifier).toCompletableFuture().join().size(),
                    "Incorrect count of Mementos!");
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH);
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO);
        }
    }

    @Test
    void testList() {
        final Instant time = parse("2017-02-16T11:15:01Z");
        final Instant time2 = parse("2017-02-16T11:15:11Z");
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/versions").getFile());
        assertTrue(dir.exists(), "Resource directory doesn't exist!");
        assertTrue(dir.isDirectory(), "Resource directory isn't a valid directory");

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH, dir.getAbsolutePath());

            final MementoService svc = new FileMementoService();

            assertEquals(2L, svc.mementos(identifier).toCompletableFuture().join().size(),
                    "Incorrect count of Mementos!");
            svc.get(identifier, now()).thenAccept(res -> assertEquals(time2, res.getModified(), "Incorrect date!"))
                .toCompletableFuture().join();
            svc.get(identifier, time).thenAccept(res -> assertEquals(time, res.getModified(), "Incorrect date!"))
                .toCompletableFuture().join();
            svc.get(identifier, parse("2015-02-16T10:00:00Z"))
                .thenAccept(res -> assertEquals(time, res.getModified(), "Incorrect date!"))
                .toCompletableFuture().join();
            svc.get(identifier, time2).thenAccept(res -> assertEquals(time2, res.getModified(), "Incorrect date!"))
                .toCompletableFuture().join();
            svc.get(identifier, MAX).thenAccept(res -> assertEquals(time2, res.getModified(), "Incorrect date!"))
                .toCompletableFuture().join();
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH);
        }
    }

    @Test
    void testPutBinary() {
        final File dir = new File(getClass().getResource("/versions").getFile());
        final MementoService svc = new FileMementoService(dir.getAbsolutePath(), true);
        final IRI identifier = rdf.createIRI("trellis:data/a-binary");
        final IRI binaryId = rdf.createIRI("file:binary");
        final IRI root = rdf.createIRI("trellis:data/");
        final Instant time = now();
        final String mimeType = "text/plain";

        final Resource mockResource = mock(Resource.class);

        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                    rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("Title")),
                    rdf.createQuad(Trellis.PreferServerManaged, identifier, DC.isPartOf, root)));
        when(mockResource.getBinaryMetadata())
            .thenReturn(of(BinaryMetadata.builder(binaryId).mimeType(mimeType).build()));
        when(mockResource.getMemberOfRelation()).thenReturn(empty());
        when(mockResource.getMemberRelation()).thenReturn(empty());
        when(mockResource.getMembershipResource()).thenReturn(empty());
        when(mockResource.getInsertedContentRelation()).thenReturn(empty());

        svc.put(mockResource).toCompletableFuture().join();

        final Resource res = svc.get(identifier, time).toCompletableFuture().join();
        assertEquals(identifier, res.getIdentifier());
        assertEquals(time, res.getModified());
        assertEquals(LDP.NonRDFSource, res.getInteractionModel());
        assertEquals(of(root), res.getContainer());
        assertTrue(res.getBinaryMetadata().isPresent());
        res.getBinaryMetadata().ifPresent(b -> {
            assertEquals(binaryId, b.getIdentifier());
            assertEquals(of(mimeType), b.getMimeType());
        });
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMembershipResource().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
    }

    @Test
    void testPutIndirectContainer() {
        final File dir = new File(getClass().getResource("/versions").getFile());
        final MementoService svc = new FileMementoService(dir.getAbsolutePath(), true);
        final IRI identifier = rdf.createIRI("trellis:data/a-resource");
        final IRI member = rdf.createIRI("trellis:data/membership-resource");
        final IRI root = rdf.createIRI("trellis:data/");
        final Instant time = now();

        final Resource mockResource = mock(Resource.class);

        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                    rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("Title")),
                    rdf.createQuad(Trellis.PreferServerManaged, identifier, DC.isPartOf, root)));
        when(mockResource.getBinaryMetadata()).thenReturn(empty());
        when(mockResource.getMemberOfRelation()).thenReturn(of(DC.isPartOf));
        when(mockResource.getMemberRelation()).thenReturn(of(LDP.member));
        when(mockResource.getMembershipResource()).thenReturn(of(member));
        when(mockResource.getInsertedContentRelation()).thenReturn(of(FOAF.primaryTopic));

        svc.put(mockResource).toCompletableFuture().join();

        final Resource res = svc.get(identifier, time).toCompletableFuture().join();
        assertEquals(identifier, res.getIdentifier());
        assertEquals(time, res.getModified());
        assertEquals(LDP.IndirectContainer, res.getInteractionModel());
        assertEquals(of(DC.isPartOf), res.getMemberOfRelation());
        assertEquals(of(LDP.member), res.getMemberRelation());
        assertEquals(of(member), res.getMembershipResource());
        assertEquals(of(FOAF.primaryTopic), res.getInsertedContentRelation());
        assertEquals(of(root), res.getContainer());
        assertFalse(res.getBinaryMetadata().isPresent());
        assertEquals(1L, res.stream(Trellis.PreferUserManaged).count());
    }

    @Test
    void testListNonExistent() {
        final File dir = new File(getClass().getResource("/versions").getFile());

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH, dir.getAbsolutePath());

            final MementoService svc = new FileMementoService();
            final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "nonexistent");

            assertTrue(dir.exists(), "Resource directory doesn't exist!");
            assertTrue(dir.isDirectory(), "Invalid resource directory!");
            assertTrue(svc.mementos(identifier).toCompletableFuture().join().isEmpty(),
                    "Resource directory isn't empty!");
            assertEquals(MISSING_RESOURCE, svc.get(identifier, now()).toCompletableFuture().join(),
                    "Wrong response for missing resource!");
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH);
        }
    }

    @Test
    void testListNone() {
        final File dir = new File(getClass().getResource("/versions").getFile());

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH, dir.getAbsolutePath());

            final MementoService svc = new FileMementoService();
            final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "empty");

            assertTrue(dir.exists(), "Resource directory doesn't exist!");
            assertTrue(dir.isDirectory(), "Invalid resource directory!");
            assertEquals(MISSING_RESOURCE, svc.get(identifier, now()).toCompletableFuture().join(),
                    "Wrong response for missing resource!");
            assertTrue(svc.mementos(identifier).toCompletableFuture().join().isEmpty(),
                    "Memento list isn't empty!");
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH);
        }
    }

    @Test
    void testNewVersionSystem() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/versions").getFile()).getParentFile();
        assertTrue(dir.exists(), "Resource directory doesn't exist!");
        assertTrue(dir.isDirectory(), "Invalid resource directory!");
        final File versionDir = new File(dir, "versions2");
        assertFalse(versionDir.exists(), "Version directory already exists!");

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH, versionDir.getAbsolutePath());

            final FileMementoService svc = new FileMementoService();

            assertTrue(svc.mementos(identifier).toCompletableFuture().join().isEmpty(),
                    "Memento list isn't empty!");
            final File file = new File(getClass().getResource("/resource.nq").getFile());
            assertTrue(file.exists(), "Memento resource doesn't exist!");
            final Resource res = new FileResource(identifier, file);
            svc.put(res).toCompletableFuture().join();

            assertEquals(1L, svc.mementos(identifier).toCompletableFuture().join().size(),
                    "Incorrect count of Mementos!");
            svc.put(res, res.getModified().plusSeconds(10)).toCompletableFuture().join();
            assertEquals(2L, svc.mementos(identifier).toCompletableFuture().join().size(),
                    "Incorrect count of Mementos!");
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_PATH);
        }
    }
}
