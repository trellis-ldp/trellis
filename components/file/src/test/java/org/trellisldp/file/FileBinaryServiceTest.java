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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.JRE.JAVA_8;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.CompletionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.id.UUIDGenerator;

/**
 * Test the file-based binary service.
 */
public class FileBinaryServiceTest {

    private static final String testDoc = "test.txt";

    private static final RDF rdf = new SimpleRDF();

    private static final String directory = new File(FileBinaryService.class.getResource("/" + testDoc).getPath())
        .getParent();

    private static final IRI file = rdf.createIRI("file:///" + testDoc);
    private final IdentifierService idService = new UUIDGenerator();

    @BeforeAll
    public static void setUpEverything() {
        System.getProperties().setProperty(FileBinaryService.BINARY_BASE_PATH, directory);
    }

    @Test
    public void testFilePurge() {
        final BinaryService service = new FileBinaryService(idService);
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream("Some data".getBytes(UTF_8));
        assertNull(service.setContent(fileIRI, inputStream).join());
        assertEquals("Some data", uncheckedToString(service.getContent(fileIRI).join()));
        assertNull(service.purgeContent(fileIRI).join());
        assertNull(service.purgeContent(fileIRI).join());
    }

    @Test
    public void testIdSupplier() {
        final BinaryService service = new FileBinaryService(idService);
        assertTrue(service.generateIdentifier().startsWith("file:///"));
        assertNotEquals(service.generateIdentifier(), service.generateIdentifier());
    }

    @Test
    public void testFileContent() {
        final BinaryService service = new FileBinaryService(idService);
        assertEquals("A test document.\n", service.getContent(file).thenApply(this::uncheckedToString).join());
    }

    @Test
    public void testFileContentSegment() {
        final BinaryService service = new FileBinaryService(idService);
        assertEquals(" tes", service.getContent(file, 1, 5).thenApply(this::uncheckedToString).join());
    }

    @Test
    public void testFileContentSegments() {
        final BinaryService service = new FileBinaryService(idService);
        assertEquals("oc", service.getContent(file, 8, 10).thenApply(this::uncheckedToString).join());
    }

    @Test
    public void testFileContentSegmentBeyond() {
        final BinaryService service = new FileBinaryService(idService);
        assertEquals("", service.getContent(file, 1000, 1005).thenApply(this::uncheckedToString).join());
    }

    @Test
    public void testSetFileContent() {
        final String contents = "A new file";
        final BinaryService service = new FileBinaryService(idService);
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        service.setContent(fileIRI, inputStream).join();
        assertEquals(contents, service.getContent(fileIRI).thenApply(this::uncheckedToString).join());
    }

    @Test
    public void testGetFileContentError() throws IOException {
        final BinaryService service = new FileBinaryService(idService);
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        assertThrows(CompletionException.class, () -> service.getContent(fileIRI).join());
        assertThrows(CompletionException.class, () -> service.getContent(fileIRI, 0, 4).join());
    }

    @Test
    public void testBadAlgorithm() throws Exception {
        final BinaryService service = new FileBinaryService(idService);
        assertNull(service.calculateDigest(file, "BLAHBLAH").join());
    }

    @Test
    public void testSetFileContentError() throws Exception {
        final InputStream throwingMockInputStream = mock(InputStream.class, inv -> {
                throw new IOException("Expected error");
        });
        final BinaryService service = new FileBinaryService(idService);
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        assertAll(() -> service.setContent(fileIRI, throwingMockInputStream).handle((val, err) -> {
                assertNotNull(err);
                return null;
            }).join());
    }

    @Test
    public void testBase64Digest() throws IOException {
        final BinaryService service = new FileBinaryService(idService);
        assertEquals("oZ1Y1O/8vs39RH31fh9lrA==", service.calculateDigest(file, "MD5").join());
        assertEquals("QJuYLse9SK/As177lt+rSfixyH0=", service.calculateDigest(file, "SHA").join());
        assertEquals("QJuYLse9SK/As177lt+rSfixyH0=", service.calculateDigest(file, "SHA-1").join());
        assertThrows(CompletionException.class, () ->
                service.calculateDigest(rdf.createIRI("file:///" + randomFilename()), "MD5").join());
    }

    @Test
    @DisabledOnJre(JAVA_8)
    public void testJdk9Digests() throws IOException {
        final BinaryService service = new FileBinaryService(idService);
        assertEquals("FQgyH2yU2NhyMTZ7YDDKwV5vcWUBM1zq0uoIYUiHH+4=",
                service.calculateDigest(file, "SHA3-256").join());
        assertEquals("746UDLrFXM61gzI0FnoVT2S0Z7EmQUfhHnoSYwkR2MHzbBe6j9rMigQBfR8ApZUA",
                service.calculateDigest(file, "SHA3-384").join());
        assertEquals("Ecu/R0kV4eL0J/VOpyVA2Lz0T6qsJj9ioQ+QorJDztJeMj6uhf6zqyhZnu9zMYiwrkX8U4oWiZMDT/0fWjOyYg==",
                service.calculateDigest(file, "SHA3-512").join());
    }

    @Test
    @EnabledOnJre(JAVA_8)
    public void testJdk9DigestsOnJdk8() throws IOException {
        final BinaryService service = new FileBinaryService(idService);
        assertFalse(service.calculateDigest(file, "SHA3-256").handle(this::checkError).join());
        assertFalse(service.calculateDigest(file, "SHA3-384").handle(this::checkError).join());
        assertFalse(service.calculateDigest(file, "SHA3-512").handle(this::checkError).join());
    }

    @Test
    public void testBadIdentifier() {
        final BinaryService service = new FileBinaryService(idService);
        assertFalse(service.getContent(rdf.createIRI("http://example.com/")).handle(this::checkError).join());
    }

    private Boolean checkError(final Object asyncValue, final Throwable err) {
        assertNull(asyncValue);
        assertNotNull(err);
        return false;
    }

    private String uncheckedToString(final InputStream is) {
        try {
            return IOUtils.toString(is, UTF_8);
        } catch (final IOException ex) {
            return null;
        }
    }

    private static String randomFilename() {
        final SecureRandom random = new SecureRandom();
        final String filename = new BigInteger(50, random).toString(32);
        return filename + ".json";
    }
}
