/*
 * Copyright (c) Aaron Coburn and individual contributors
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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.JSONLD;

/**
 * @author acoburn
 */
class FileNamespaceServiceTest {

    private static final String nsDoc = "/testNamespaces.json";

    @Test
    void testReadFromJson() {
        final URL res = FileNamespaceService.class.getResource(nsDoc);
        final FileNamespaceService svc = new FileNamespaceService();
        svc.filePath = res.getPath();
        svc.init();
        assertEquals(2, svc.getNamespaces().size(), "Namespace mapping count is incorrect!");
    }

    @Test
    void testReadError() {
        final URL res = FileNamespaceService.class.getResource("/thisIsNot.json");
        final FileNamespaceService svc = new FileNamespaceService();
        svc.filePath = res.getPath();
        assertThrows(UncheckedIOException.class, svc::init, "Loaded namespaces from invalid file!");
    }

    @Test
    void testNonObjectNamespaceFile() {
        final URL res = FileNamespaceService.class.getResource("/invalidNamespaces.json");
        assertTrue(FileNamespaceService.read(res.getPath()).isEmpty());
    }

    @Test
    void testWriteNonexistent() {
        final File file = new File(getClass().getResource(nsDoc).getFile());
        final File nonexistent = new File(file.getParentFile(), "nonexistent2/dir/file.json");
        final FileNamespaceService svc = new FileNamespaceService();
        svc.filePath = nonexistent.getAbsolutePath();
        assertDoesNotThrow(svc::init, "Loaded namespaces from nonexistent directory!");
    }

    @Test
    void testWriteCreateDirectories() {
        final File file = new File(getClass().getResource(nsDoc).getFile());
        final File nonexistent = new File(file.getParentFile(), "nonexistent3/dir/file.json");
        final Map<String, String> data = singletonMap("dcterms", "http://purl.org/dc/terms/");
        assertDoesNotThrow(() -> FileNamespaceService.write(nonexistent, data, true));
    }

    @Test
    void testCreateHierarchy() {
        final File dir = new File(getClass().getResource(nsDoc).getFile()).getParentFile();
        assertFalse(FileNamespaceService.shouldCreateDirectories(null));
        assertFalse(FileNamespaceService.shouldCreateDirectories(dir));
        assertTrue(FileNamespaceService.shouldCreateDirectories(new File("nonexistent2/other")));
    }

    @Test
    void testWriteBadFile() throws IOException {
        final File mockFile = mock(File.class, inv -> {
            throw new IOException("Expected exception.");
        });
        final Map<String, String> namespaces = emptyMap();
        assertThrows(UncheckedIOException.class, () -> FileNamespaceService.write(mockFile, namespaces, false));
    }

    @Test
    void testWriteToJson() {
        final File file = new File(FileNamespaceService.class.getResource(nsDoc).getPath());
        final String filename = file.getParent() + "/" + randomFilename();

        final FileNamespaceService svc1 = new FileNamespaceService();
        svc1.filePath = filename;
        svc1.init();
        assertEquals(15, svc1.getNamespaces().size(), "Incorrect namespace mapping count!");
        assertFalse(svc1.getNamespaces().containsKey("jsonld"), "jsonld prefix unexpectedly found!");
        assertTrue(svc1.setPrefix("jsonld", JSONLD.getNamespace()), "unable to set jsonld mapping!");
        assertEquals(16, svc1.getNamespaces().size(), "Namespace count was not incremented!");
        assertTrue(svc1.getNamespaces().containsKey("jsonld"), "jsonld prefix not found in mapping!");

        final FileNamespaceService svc2 = new FileNamespaceService();
        svc2.filePath = filename;
        svc2.init();
        assertEquals(16, svc2.getNamespaces().size(), "Incorrect namespace count when reloading from file!");
        assertFalse(svc2.setPrefix("jsonld", JSONLD.getNamespace()),
                "unexpected response when trying to re-set jsonld mapping!");
    }

    static String randomFilename() {
        final SecureRandom random = new SecureRandom();
        final String filename = new BigInteger(50, random).toString(32);
        return filename + ".json";
    }
}
