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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class NamespacesJsonContextTest {

    private static final String nsDoc = "/testNamespaces.json";
    private static final String JSONLD = "http://www.w3.org/ns/json-ld#";
    private static final String LDP = "http://www.w3.org/ns/ldp#";

    @AfterAll
    public static void cleanUp() throws IOException {
        final File readonly = new File(NamespacesJsonContext.class.getResource("/readonly.json").getFile());
        readonly.setWritable(true);
    }

    @Test
    public void testReadFromJson() {
        final URL res = NamespacesJsonContext.class.getResource(nsDoc);
        System.getProperties().setProperty(NamespacesJsonContext.NAMESPACES_PATH, res.getPath());
        final NamespacesJsonContext svc = new NamespacesJsonContext();
        assertEquals(2, svc.getNamespaces().size());
        assertEquals(LDP, svc.getNamespace("ldp").get());
        assertEquals("ldp", svc.getPrefix(LDP).get());
        System.getProperties().remove(NamespacesJsonContext.NAMESPACES_PATH);
    }

    @Test
    public void testReadError() {
        final URL res = NamespacesJsonContext.class.getResource("/thisIsNot.json");
        System.getProperties().setProperty(NamespacesJsonContext.NAMESPACES_PATH, res.getPath());

        assertThrows(UncheckedIOException.class, () -> new NamespacesJsonContext());
        System.getProperties().remove(NamespacesJsonContext.NAMESPACES_PATH);
    }

    @Test
    public void testWriteError() throws Exception {
        final URL res = NamespacesJsonContext.class.getResource("/readonly.json");

        System.getProperties().setProperty(NamespacesJsonContext.NAMESPACES_PATH, res.getPath());

        final NamespacesJsonContext svc = new NamespacesJsonContext();
        final File file = new File(res.toURI());
        assumeTrue(file.setWritable(false));
        assertThrows(UncheckedIOException.class, () -> svc.setPrefix("ex", "http://example.com/"));
        System.getProperties().remove(NamespacesJsonContext.NAMESPACES_PATH);
    }

    @Test
    public void testWriteToJson() {
        final File file = new File(NamespacesJsonContext.class.getResource(nsDoc).getPath());
        final String filename = file.getParent() + "/" + randomFilename();

        System.getProperties().setProperty(NamespacesJsonContext.NAMESPACES_PATH, filename);

        final NamespacesJsonContext svc1 = new NamespacesJsonContext();
        assertEquals(15, svc1.getNamespaces().size());
        assertFalse(svc1.getNamespace("jsonld").isPresent());
        assertFalse(svc1.getPrefix(JSONLD).isPresent());
        assertTrue(svc1.setPrefix("jsonld", JSONLD));
        assertEquals(16, svc1.getNamespaces().size());
        assertEquals(JSONLD, svc1.getNamespace("jsonld").get());
        assertEquals("jsonld", svc1.getPrefix(JSONLD).get());

        final NamespacesJsonContext svc2 = new NamespacesJsonContext();
        assertEquals(16, svc2.getNamespaces().size());
        assertEquals(JSONLD, svc2.getNamespace("jsonld").get());
        assertFalse(svc2.setPrefix("jsonld", JSONLD));
        System.getProperties().remove(NamespacesJsonContext.NAMESPACES_PATH);
    }

    private static String randomFilename() {
        final SecureRandom random = new SecureRandom();
        final String filename = new BigInteger(50, random).toString(32);
        return filename + ".json";
    }
}
