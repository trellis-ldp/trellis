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
package org.trellisldp.file;

import static java.time.Instant.MAX;
import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.util.Collections.emptySortedSet;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;

/**
 * Test a file-based memento service.
 */
public class FileMementoServiceTest {

    private static final RDF rdf = new JenaRDF();

    @AfterAll
    public static void cleanUp() throws IOException {
        final File dir = new File(FileMementoServiceTest.class.getResource("/versions").getFile()).getParentFile();
        final File vDir = new File(dir, "versions2");
        if (vDir.exists()) {
            deleteDirectory(vDir);
        }

        final File readonly = new File(FileMementoServiceTest.class.getResource(
                    "/readonly/35/97/1a/f68d4d5afced3770fc13fb8e560dc253/").getFile());
        readonly.setWritable(true);

        final File unreadable = new File(FileMementoServiceTest.class.getResource(
                    "/unreadable/35/97/1a/f68d4d5afced3770fc13fb8e560dc253/").getFile());
        unreadable.setReadable(true);
    }

    @Test
    public void testList() {
        final Instant time = parse("2017-02-16T11:15:01Z");
        final Instant time2 = parse("2017-02-16T11:15:11Z");
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/versions").getFile());
        assertTrue(dir.exists(), "Resource directory doesn't exist!");
        assertTrue(dir.isDirectory(), "Resource directory isn't a valid directory");

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH, dir.getAbsolutePath());

            final MementoService svc = new FileMementoService();

            assertEquals(2L, svc.mementos(identifier).join().size(), "Incorrect count of Mementos!");
            svc.get(identifier, now()).thenAccept(res -> assertEquals(time2, res.getModified(), "Incorrect date!"))
                .join();
            svc.get(identifier, time).thenAccept(res -> assertEquals(time, res.getModified(), "Incorrect date!"))
                .join();
            assertEquals(MISSING_RESOURCE, svc.get(identifier, parse("2015-02-16T10:00:00Z")).join(),
                    "Wrong response for a missing resource!");
            svc.get(identifier, time2).thenAccept(res -> assertEquals(time2, res.getModified(), "Incorrect date!"))
                .join();
            svc.get(identifier, MAX).thenAccept(res -> assertEquals(time2, res.getModified(), "Incorrect date!"))
                .join();
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH);
        }
    }

    @Test
    public void testListNonExistent() {
        final File dir = new File(getClass().getResource("/versions").getFile());

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH, dir.getAbsolutePath());

            final MementoService svc = new FileMementoService();
            final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "nonexistent");

            assertTrue(dir.exists(), "Resource directory doesn't exist!");
            assertTrue(dir.isDirectory(), "Invalid resource directory!");
            assertTrue(svc.mementos(identifier).join().isEmpty(), "Resource directory isn't empty!");
            assertEquals(MISSING_RESOURCE, svc.get(identifier, now()).join(), "Wrong response for missing resource!");
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH);
        }
    }

    @Test
    public void testListNone() {
        final File dir = new File(getClass().getResource("/versions").getFile());

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH, dir.getAbsolutePath());

            final MementoService svc = new FileMementoService();
            final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "empty");

            assertTrue(dir.exists(), "Resource directory doesn't exist!");
            assertTrue(dir.isDirectory(), "Invalid resource directory!");
            assertEquals(MISSING_RESOURCE, svc.get(identifier, now()).join(), "Wrong response for missing resource!");
            assertTrue(svc.mementos(identifier).join().isEmpty(), "Memento list isn't empty!");
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH);
        }
    }

    @Test
    public void testNewVersionSystem() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/versions").getFile()).getParentFile();
        assertTrue(dir.exists(), "Resource directory doesn't exist!");
        assertTrue(dir.isDirectory(), "Invalid resource directory!");
        final File versionDir = new File(dir, "versions2");
        assertFalse(versionDir.exists(), "Version directory already exists!");

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH, versionDir.getAbsolutePath());

            final MementoService svc = new FileMementoService();

            assertTrue(svc.mementos(identifier).join().isEmpty(), "Memento list isn't empty!");
            final File file = new File(getClass().getResource("/resource.nq").getFile());
            assertTrue(file.exists(), "Memento resource doesn't exist!");
            final Resource res = new FileResource(identifier, file);
            final Instant time = now();
            svc.put(identifier, time, res.stream()).join();

            assertEquals(1L, svc.mementos(identifier).join().size(), "Incorrect count of Mementos!");
            svc.put(identifier, time.plusSeconds(10), res.stream()).join();
            assertEquals(2L, svc.mementos(identifier).join().size(), "Incorrect count of Mementos!");
            assertNull(svc.delete(identifier, time.plusSeconds(15)).join(), "Error with Memento deletion (+15s)!");
            assertNull(svc.delete(identifier, time.plusSeconds(10)).join(), "Error with Memento deletion (+10s)!");
            assertEquals(1L, svc.mementos(identifier).join().size(), "Incorrect count of Mementos!");
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH);
        }
    }

    @Test
    public void testUnwritableVersionSystem() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/readonly").getFile());

        final File readonly = new File(getClass().getResource(
                    "/readonly/35/97/1a/f68d4d5afced3770fc13fb8e560dc253/").getFile());
        assumeTrue(readonly.setReadOnly(), "Unable to set directory read-only, skipping test!");

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH, dir.getAbsolutePath());

            final MementoService svc = new FileMementoService();
            assertEquals(2L, svc.mementos(identifier).join().size(), "Incorrect count of Mementos!");
            final File file = new File(getClass().getResource("/resource.nq").getFile());
            assertTrue(file.exists(), "Memento resource doesn't exist!");
            final Resource res = new FileResource(identifier, file);

            final Instant time = parse("2017-02-16T11:15:01Z");
            svc.put(identifier, time.plusSeconds(10), res.stream()).handle(this::assertError).join();

            assertEquals(2L, svc.mementos(identifier).join().size(), "Incorrect count of Mementos!");
            assertNull(svc.delete(identifier, time).handle(this::assertError).join(),
                    "Completion error deleting Memento!");
            assertNull(svc.delete(identifier, time.plusSeconds(10)).join(),
                    "Error deleting non-existent Memento (+10s)!");
            assertEquals(2L, svc.mementos(identifier).join().size(), "Incorrect count of Mementos!");
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH);
        }
    }

    @Test
    public void testAccessUnreadable() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");

        final File dir = new File(getClass().getResource("/unreadable").getFile());
        final File unreadable = new File(getClass().getResource(
                    "/unreadable/35/97/1a/f68d4d5afced3770fc13fb8e560dc253/").getFile());
        assumeTrue(unreadable.setReadable(false), "Couldn't set directory as unreadable, skipping test!");

        try {
            System.setProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH, dir.getAbsolutePath());

            final MementoService svc = new FileMementoService();

            assertTrue(svc.mementos(identifier).handle((m, err) -> {
                assertNull(m, "There shouldn't be a value for an unreadable directory!");
                assertNotNull(err, "There should have been an error with an unreadable directory!");
                return emptySortedSet();
            }).join().isEmpty(), "Memento list wasn't empty!");
        } finally {
            System.clearProperty(FileMementoService.CONFIG_FILE_MEMENTO_BASE_PATH);
        }
    }

    private Void assertError(final Object o, final Throwable err) {
        assertNull(o, "There shouldn't be a value when an error is expected!");
        assertNotNull(err, "There should have been an expected error!");
        return null;
    }
}
