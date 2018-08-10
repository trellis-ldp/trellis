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
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;

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
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());

        System.getProperties().setProperty(FileMementoService.MEMENTO_BASE_PATH, dir.getAbsolutePath());

        final MementoService svc = new FileMementoService();

        assertEquals(2L, svc.list(identifier).join().size());
        svc.get(identifier, now()).thenAccept(res -> assertEquals(time2, res.getModified())).join();
        svc.get(identifier, time).thenAccept(res -> assertEquals(time, res.getModified())).join();
        assertEquals(MISSING_RESOURCE, svc.get(identifier, parse("2015-02-16T10:00:00Z")).join());
        svc.get(identifier, time2).thenAccept(res -> assertEquals(time2, res.getModified())).join();
        svc.get(identifier, MAX).thenAccept(res -> assertEquals(time2, res.getModified())).join();
    }

    @Test
    public void testListNonExistent() {
        final File dir = new File(getClass().getResource("/versions").getFile());

        System.getProperties().setProperty(FileMementoService.MEMENTO_BASE_PATH, dir.getAbsolutePath());

        final MementoService svc = new FileMementoService();
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "nonexistent");

        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertTrue(svc.list(identifier).join().isEmpty());
        assertEquals(MISSING_RESOURCE, svc.get(identifier, now()).join());
    }

    @Test
    public void testListNone() {
        final File dir = new File(getClass().getResource("/versions").getFile());

        System.getProperties().setProperty(FileMementoService.MEMENTO_BASE_PATH, dir.getAbsolutePath());

        final MementoService svc = new FileMementoService();
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "empty");

        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertEquals(MISSING_RESOURCE, svc.get(identifier, now()).join());
        assertTrue(svc.list(identifier).join().isEmpty());
    }

    @Test
    public void testNewVersionSystem() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/versions").getFile()).getParentFile();
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        final File versionDir = new File(dir, "versions2");
        assertFalse(versionDir.exists());

        System.getProperties().setProperty(FileMementoService.MEMENTO_BASE_PATH, versionDir.getAbsolutePath());

        final MementoService svc = new FileMementoService();

        assertTrue(svc.list(identifier).join().isEmpty());
        final File file = new File(getClass().getResource("/resource.nq").getFile());
        assertTrue(file.exists());
        final Resource res = new FileResource(identifier, file);
        final Instant time = now();
        svc.put(identifier, time, res.stream()).join();

        assertEquals(1L, svc.list(identifier).join().size());
        svc.put(identifier, time.plusSeconds(10), res.stream()).join();
        assertEquals(2L, svc.list(identifier).join().size());
        assertNull(svc.delete(identifier, time.plusSeconds(15)).join());
        assertNull(svc.delete(identifier, time.plusSeconds(10)).join());
        assertEquals(1L, svc.list(identifier).join().size());
    }

    @Test
    public void testUnwritableVersionSystem() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/readonly").getFile());

        final File readonly = new File(getClass().getResource(
                    "/readonly/35/97/1a/f68d4d5afced3770fc13fb8e560dc253/").getFile());
        assumeTrue(readonly.setReadOnly());

        System.getProperties().setProperty(FileMementoService.MEMENTO_BASE_PATH, dir.getAbsolutePath());

        final MementoService svc = new FileMementoService();
        assertEquals(2L, svc.list(identifier).join().size());
        final File file = new File(getClass().getResource("/resource.nq").getFile());
        assertTrue(file.exists());
        final Resource res = new FileResource(identifier, file);

        final Instant time = parse("2017-02-16T11:15:01Z");
        svc.put(identifier, time.plusSeconds(10), res.stream()).handle(this::assertError).join();

        assertEquals(2L, svc.list(identifier).join().size());
        assertNull(svc.delete(identifier, time).handle(this::assertError).join());
        assertNull(svc.delete(identifier, time.plusSeconds(10)).join());
        assertEquals(2L, svc.list(identifier).join().size());
    }

    @Test
    public void testAccessUnreadable() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");

        final File dir = new File(getClass().getResource("/unreadable").getFile());
        final File unreadable = new File(getClass().getResource(
                    "/unreadable/35/97/1a/f68d4d5afced3770fc13fb8e560dc253/").getFile());
        assumeTrue(unreadable.setReadable(false));


        System.getProperties().setProperty(FileMementoService.MEMENTO_BASE_PATH, dir.getAbsolutePath());

        final MementoService svc = new FileMementoService();

        assertTrue(svc.list(identifier).handle((m, err) -> {
            assertNull(m);
            assertNotNull(err);
            return emptyList();
        }).join().isEmpty());
    }

    private Void assertError(final Object o, final Throwable err) {
        assertNull(o);
        assertNotNull(err);
        return null;
    }
}
