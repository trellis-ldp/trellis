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
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.JRE.JAVA_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.mockito.Mock;
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

    @Mock
    private InputStream mockInputStream;

    @BeforeAll
    public static void setUpEverything() {
        System.getProperties().setProperty(FileBinaryService.BINARY_BASE_PATH, directory);
    }

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testFileExists() {
        final BinaryService resolver = new FileBinaryService(idService);
        assertTrue(resolver.exists(file));
        assertFalse(resolver.exists(rdf.createIRI("file:///fake.txt")));
    }

    @Test
    public void testFilePurge() {
        final BinaryService resolver = new FileBinaryService(idService);
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream("Some data".getBytes(UTF_8));
        resolver.setContent(fileIRI, inputStream);
        assertTrue(resolver.exists(fileIRI));
        resolver.purgeContent(fileIRI);
        assertFalse(resolver.exists(fileIRI));
    }

    @Test
    public void testIdSupplier() {
        final BinaryService resolver = new FileBinaryService(idService);
        assertTrue(resolver.generateIdentifier().startsWith("file:///"));
        assertNotEquals(resolver.generateIdentifier(), resolver.generateIdentifier());
    }

    @Test
    public void testFileContent() {
        final BinaryService resolver = new FileBinaryService(idService);
        assertTrue(resolver.getContent(file).isPresent());
        assertEquals("A test document.\n", resolver.getContent(file).map(this::uncheckedToString).get());
    }

    @Test
    public void testFileContentSegment() {
        final BinaryService resolver = new FileBinaryService(idService);
        assertTrue(resolver.getContent(file, 1, 5).isPresent());
        assertEquals(" tes", resolver.getContent(file, 1, 5).map(this::uncheckedToString).get());
    }

    @Test
    public void testFileContentSegments() {
        final BinaryService resolver = new FileBinaryService(idService);

        assertTrue(resolver.getContent(file, 8, 10).isPresent());
        assertEquals("oc", resolver.getContent(file, 8, 10).map(this::uncheckedToString).get());
    }

    @Test
    public void testFileContentSegmentBeyond() {
        final BinaryService resolver = new FileBinaryService(idService);
        assertTrue(resolver.getContent(file, 1000, 1005).isPresent());
        assertEquals("", resolver.getContent(file, 1000, 1005).map(this::uncheckedToString).get());
    }

    @Test
    public void testSetFileContent() {
        final String contents = "A new file";
        final BinaryService resolver = new FileBinaryService(idService);
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        resolver.setContent(fileIRI, inputStream);
        assertTrue(resolver.getContent(fileIRI).isPresent());
        assertEquals(contents, resolver.getContent(fileIRI).map(this::uncheckedToString).get());
    }

    @Test
    public void testGetFileContentError() throws IOException {
        final BinaryService resolver = new FileBinaryService(idService);
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        assertThrows(UncheckedIOException.class, () -> resolver.getContent(fileIRI));
        assertThrows(UncheckedIOException.class, () -> resolver.getContent(fileIRI, 0, 4));
    }

    @Test
    public void testSetFileContentError() throws IOException {
        final InputStream throwingMockInputStream = mock(InputStream.class, inv -> {
                throw new IOException("Expected error");
        });
        final BinaryService resolver = new FileBinaryService(idService);
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        assertThrows(UncheckedIOException.class, () -> resolver.setContent(fileIRI, throwingMockInputStream));
    }

    @Test
    public void testBase64Digest() throws IOException {
        final byte[] data = "Some data".getBytes(UTF_8);
        when(mockInputStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException("Expected Error"));

        final BinaryService service = new FileBinaryService(idService);
        assertEquals(of("W4L4v03yv7DmbMqnMG/QJA=="), service.digest("MD5", new ByteArrayInputStream(data)));
        assertEquals(of("jXJFPxAHmvPfx/z8QQmx7VXhg58="), service.digest("SHA", new ByteArrayInputStream(data)));
        assertEquals(of("jXJFPxAHmvPfx/z8QQmx7VXhg58="), service.digest("SHA-1", new ByteArrayInputStream(data)));
        assertFalse(service.digest("MD5", mockInputStream).isPresent());
    }

    @Test
    @DisabledOnJre(JAVA_8)
    public void testJdk9Digests() throws IOException {
        final byte[] data = "Some data".getBytes(UTF_8);
        final BinaryService service = new FileBinaryService(idService);
        assertEquals(of("hrhkhljRY6RyA8cQHDJ+uENNdBqksUsbP/nAi6cjvNE="),
                service.digest("SHA3-256", new ByteArrayInputStream(data)));
        assertEquals(of("Nn5wxem8PkrYujCxY3IJZQFqUNrMZRYh0J3gvePGOoWp3N95DPM8IfNTlBLStVyC"),
                service.digest("SHA3-384", new ByteArrayInputStream(data)));
        assertEquals(of("cSRKDMnTgyFVNoK2yc/PMNf+4tDkcQMmwvc5GJ12IqbMo/d2xTIAn9H+0WOnkWWtpDoaO0JAY+xPP8zG/RwfZg=="),
                service.digest("SHA3-512", new ByteArrayInputStream(data)));
    }

    @Test
    @EnabledOnJre(JAVA_8)
    public void testJdk9DigestsOnJdk8() throws IOException {
        final byte[] data = "Some data".getBytes(UTF_8);
        final BinaryService service = new FileBinaryService(idService);
        assertFalse(service.digest("SHA3-256", new ByteArrayInputStream(data)).isPresent());
        assertFalse(service.digest("SHA3-384", new ByteArrayInputStream(data)).isPresent());
        assertFalse(service.digest("SHA3-512", new ByteArrayInputStream(data)).isPresent());
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
