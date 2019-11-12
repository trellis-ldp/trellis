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
package org.trellisldp.namespaces;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.vocabulary.JSONLD;

/**
 * @author acoburn
 */
class JsonNamespaceServiceTest {

    private static final String nsDoc = "/testNamespaces.json";

    @Test
    void testReadFromJson() {
        final URL res = JsonNamespaceService.class.getResource(nsDoc);
        try {
            System.setProperty(JsonNamespaceService.CONFIG_NAMESPACES_PATH, res.getPath());
            final NamespaceService svc = new JsonNamespaceService();
            assertEquals(2, svc.getNamespaces().size(), "Namespace mapping count is incorrect!");
        } finally {
            System.clearProperty(JsonNamespaceService.CONFIG_NAMESPACES_PATH);
        }
    }

    @Test
    void testReadError() {
        final URL res = JsonNamespaceService.class.getResource("/thisIsNot.json");
        try {
            System.setProperty(JsonNamespaceService.CONFIG_NAMESPACES_PATH, res.getPath());
            assertThrows(UncheckedIOException.class, JsonNamespaceService::new,
                    "Loaded namespaces from invalid file!");
        } finally {
            System.clearProperty(JsonNamespaceService.CONFIG_NAMESPACES_PATH);
        }
    }

    @Test
    void testWriteNonexistent() {
        final File file = new File(getClass().getResource(nsDoc).getFile());
        final File nonexistent = new File(file.getParentFile(), "nonexistent/dir/file.json");
        assertDoesNotThrow(() -> new JsonNamespaceService(nonexistent.getAbsolutePath()),
                    "Loaded namespaces from nonexistent directory!");
    }

    @Test
    void testCreateHierarchy() {
        final File dir = new File(getClass().getResource(nsDoc).getFile()).getParentFile();
        assertFalse(JsonNamespaceService.shouldCreateDirectories(null));
        assertFalse(JsonNamespaceService.shouldCreateDirectories(dir));
        assertTrue(JsonNamespaceService.shouldCreateDirectories(new File("nonexistent/other")));
    }

    @Test
    void testWriteBadFile() throws IOException {
        final File mockFile = mock(File.class, inv -> {
            throw new IOException("Expected exception.");
        });
        assertThrows(UncheckedIOException.class, () -> JsonNamespaceService.write(mockFile, emptyMap()));
    }

    @Test
    void testWriteToJson() {
        final File file = new File(JsonNamespaceService.class.getResource(nsDoc).getPath());
        final String filename = file.getParent() + "/" + randomFilename();

        try {
            System.setProperty(JsonNamespaceService.CONFIG_NAMESPACES_PATH, filename);

            final NamespaceService svc1 = new JsonNamespaceService();
            assertEquals(15, svc1.getNamespaces().size(), "Incorrect namespace mapping count!");
            assertFalse(svc1.getNamespaces().containsKey("jsonld"), "jsonld prefix unexpectedly found!");
            assertTrue(svc1.setPrefix("jsonld", JSONLD.getNamespace()), "unable to set jsonld mapping!");
            assertEquals(16, svc1.getNamespaces().size(), "Namespace count was not incremented!");
            assertTrue(svc1.getNamespaces().containsKey("jsonld"), "jsonld prefix not found in mapping!");

            final NamespaceService svc2 = new JsonNamespaceService();
            assertEquals(16, svc2.getNamespaces().size(), "Incorrect namespace count when reloading from file!");
            assertFalse(svc2.setPrefix("jsonld", JSONLD.getNamespace()),
                    "unexpected response when trying to re-set jsonld mapping!");
        } finally {
            System.getProperties().remove(JsonNamespaceService.CONFIG_NAMESPACES_PATH);
        }
    }

    private static String randomFilename() {
        final SecureRandom random = new SecureRandom();
        final String filename = new BigInteger(50, random).toString(32);
        return filename + ".json";
    }
}
