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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.trellisldp.api.BinaryService;

/**
 * Test the file-based binary service.
 */
public class FileBinaryServiceTest {

    private static final String testDoc = "test.txt";

    private static final RDF rdf = new SimpleRDF();

    private static final String directory = new File(FileBinaryService.class.getResource("/" + testDoc).getPath())
        .getParent();

    private static final IRI file = rdf.createIRI("file:///" + testDoc);

    @BeforeAll
    public static void setUpEverything() {
        System.setProperty(FileBinaryService.CONFIG_FILE_BINARY_BASE_PATH, directory);
    }

    @AfterAll
    public static void cleanUp() {
        System.clearProperty(FileBinaryService.CONFIG_FILE_BINARY_BASE_PATH);
    }

    @Test
    public void testFilePurge() {
        final BinaryService service = new FileBinaryService();
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream("Some data".getBytes(UTF_8));
        assertNull(service.setContent(fileIRI, inputStream).join(), "setContent didn't complete cleanly!");
        assertEquals("Some data", uncheckedToString(service.getContent(fileIRI).join()),
                "incorrect value for getContent!");
        assertNull(service.purgeContent(fileIRI).join(), "purgeContent didn't complete cleanly!");
        assertNull(service.purgeContent(fileIRI).join(), "purgeContent (2) didn't complete cleanly!");
    }

    @Test
    public void testIdSupplier() {
        final BinaryService service = new FileBinaryService();
        assertTrue(service.generateIdentifier().startsWith("file:///"), "Identifier has incorrect prefix!");
        assertNotEquals(service.generateIdentifier(), service.generateIdentifier(), "Identifiers are not unique!");
    }

    @Test
    public void testFileContent() {
        final BinaryService service = new FileBinaryService();
        assertEquals("A test document.\n", service.getContent(file).thenApply(this::uncheckedToString).join(),
                "Incorrect content when fetching from a file!");
    }

    @Test
    public void testFileContentSegment() {
        final BinaryService service = new FileBinaryService();
        assertEquals(" tes", service.getContent(file, 1, 5).thenApply(this::uncheckedToString).join(),
                "Incorrect segment when fetching from a file!");
        assertEquals("oc", service.getContent(file, 8, 10).thenApply(this::uncheckedToString).join(),
                "Incorrect segment when fetching from a file!");
    }

    @Test
    public void testFileContentSegmentBeyond() {
        final BinaryService service = new FileBinaryService();
        assertEquals("", service.getContent(file, 1000, 1005).thenApply(this::uncheckedToString).join(),
                "Incorrect out-of-range segment when fetching from a file!");
    }

    @Test
    public void testSetFileContent() {
        final String contents = "A new file";
        final BinaryService service = new FileBinaryService();
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        assertNull(service.setContent(fileIRI, inputStream).join(), "Setting content didn't complete cleanly!");
        assertEquals(contents, service.getContent(fileIRI).thenApply(this::uncheckedToString).join(),
                "Fetching new content returned incorrect value!");
    }

    @Test
    public void testGetFileContentError() throws IOException {
        final BinaryService service = new FileBinaryService();
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        assertThrows(CompletionException.class, () -> service.getContent(fileIRI).join(),
                "Fetching from invalid file should have thrown an exception!");
        assertThrows(CompletionException.class, () -> service.getContent(fileIRI, 0, 4).join(),
                "Fetching binary segment from invalid file should have thrown an exception!");
    }

    @Test
    public void testBadAlgorithm() throws Exception {
        final BinaryService service = new FileBinaryService();
        assertNull(service.calculateDigest(file, "BLAHBLAH").join(), "Unsupported algorithm should return null!");
    }

    @Test
    public void testSetFileContentError() throws Exception {
        final InputStream throwingMockInputStream = mock(InputStream.class, inv -> {
                throw new IOException("Expected error");
        });
        final BinaryService service = new FileBinaryService();
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        assertAll(() -> service.setContent(fileIRI, throwingMockInputStream).handle((val, err) -> {
                assertNotNull(err, "There should have been an error with the input stream!");
                return null;
            }).join());
    }

    @Test
    public void testBase64Digest() throws IOException {
        final BinaryService service = new FileBinaryService();
        assertEquals("oZ1Y1O/8vs39RH31fh9lrA==", service.calculateDigest(file, "MD5").join(), "Bad MD5 digest!");
        assertEquals("QJuYLse9SK/As177lt+rSfixyH0=", service.calculateDigest(file, "SHA").join(), "Bad SHA digest!");
        assertEquals("QJuYLse9SK/As177lt+rSfixyH0=", service.calculateDigest(file, "SHA-1").join(), "Bad SHA1 digest!");
        assertThrows(CompletionException.class, () ->
                service.calculateDigest(rdf.createIRI("file:///" + randomFilename()), "MD5").join(),
                "Computing digest on invalid file should throw an exception!");
    }

    @Test
    @DisabledOnJre(JAVA_8)
    public void testJdk9Digests() throws IOException {
        final BinaryService service = new FileBinaryService();
        assertEquals("FQgyH2yU2NhyMTZ7YDDKwV5vcWUBM1zq0uoIYUiHH+4=",
                service.calculateDigest(file, "SHA3-256").join(), "Bad SHA3-256 digest!");
        assertEquals("746UDLrFXM61gzI0FnoVT2S0Z7EmQUfhHnoSYwkR2MHzbBe6j9rMigQBfR8ApZUA",
                service.calculateDigest(file, "SHA3-384").join(), "Bad SHA3-384 digest!");
        assertEquals("Ecu/R0kV4eL0J/VOpyVA2Lz0T6qsJj9ioQ+QorJDztJeMj6uhf6zqyhZnu9zMYiwrkX8U4oWiZMDT/0fWjOyYg==",
                service.calculateDigest(file, "SHA3-512").join(), "Bad SHA3-512 digest!");
    }

    @Test
    @EnabledOnJre(JAVA_8)
    public void testJdk9DigestsOnJdk8() throws IOException {
        final BinaryService service = new FileBinaryService();
        assertFalse(service.calculateDigest(file, "SHA3-256").handle(this::checkError).join(),
                "SHA3-256 digest shouldn't be supported on JDK8!");
        assertFalse(service.calculateDigest(file, "SHA3-384").handle(this::checkError).join(),
                "SHA3-384 digest shouldn't be supported on JDK8!");
        assertFalse(service.calculateDigest(file, "SHA3-512").handle(this::checkError).join(),
                "SHA3-512 digest shouldn't be supported on JDK8!");
    }

    @Test
    public void testBadIdentifier() {
        final BinaryService service = new FileBinaryService();
        assertFalse(service.getContent(rdf.createIRI("http://example.com/")).handle(this::checkError).join(),
                "Shouldn't be able to fetch content from a bad IRI!");
    }

    private Boolean checkError(final Object asyncValue, final Throwable err) {
        assertNull(asyncValue, "The async value should be null!");
        assertNotNull(err, "There should be an async error!");
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
