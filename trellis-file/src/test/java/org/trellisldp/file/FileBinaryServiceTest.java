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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.DefaultIdentifierService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.RDFFactory;

/**
 * Test the file-based binary service.
 */
class FileBinaryServiceTest {

    private static final String testDoc = "test.txt";

    private static final RDF rdf = RDFFactory.getInstance();

    private final String directory = new File(FileBinaryService.class.getResource("/" + testDoc).getPath())
        .getParent();

    private final IRI file = rdf.createIRI("file:///" + testDoc);

    private final IdentifierService idService = new DefaultIdentifierService();

    private FileBinaryService service;

    @BeforeEach
    void setUp() {
        service = new FileBinaryService();
        service.basePath = directory;
        service.hierarchy = 3;
        service.length = 2;
        service.idService = idService;
        service.init();
    }

    @Test
    void testFilePurge() {
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream("Some data".getBytes(UTF_8));
        assertNull(service.setContent(BinaryMetadata.builder(fileIRI).build(), inputStream)
                .toCompletableFuture().join(), "setContent didn't complete cleanly!");
        assertEquals("Some data", uncheckedToString(service.get(fileIRI).thenApply(Binary::getContent)
                .toCompletableFuture().join()), "incorrect value for getContent!");
        assertNull(service.purgeContent(fileIRI).toCompletableFuture().join(),
                "purgeContent didn't complete cleanly!");
        assertNull(service.purgeContent(fileIRI).toCompletableFuture().join(),
                "purgeContent (2) didn't complete cleanly!");
    }

    @Test
    void testIdSupplier() {
        final IRI identifier = rdf.createIRI("https://example.com/resource");
        assertTrue(service.generateIdentifier(identifier).startsWith("file:///"), "Identifier has incorrect prefix!");
        assertNotEquals(service.generateIdentifier(identifier), service.generateIdentifier(identifier),
                "Identifiers are not unique!");
    }

    @Test
    void testFileContent() throws Exception {
        try (final InputStream input = FileBinaryService.class.getResourceAsStream("/test.txt")) {
            final String doc = uncheckedToString(input);
            assertNotNull(doc);
            assertEquals(doc, service.get(file).thenApply(Binary::getContent)
                    .thenApply(FileBinaryServiceTest::uncheckedToString).toCompletableFuture().join(),
                            "Incorrect content when fetching from a file!");
        }
    }

    @Test
    void testFileContentSegment() {
        assertEquals(" tes", service.get(file).thenApply(b -> b.getContent(1, 5))
                        .thenApply(FileBinaryServiceTest::uncheckedToString)
                        .toCompletableFuture().join(), "Incorrect segment when fetching from a file!");
        assertEquals("oc", service.get(file).thenApply(b -> b.getContent(8, 10))
                        .thenApply(FileBinaryServiceTest::uncheckedToString)
                        .toCompletableFuture().join(), "Incorrect segment when fetching from a file!");
    }

    @Test
    void testFileContentSegmentBeyond() {
        assertEquals("", service.get(file).thenApply(b -> b.getContent(1000, 1005))
                .thenApply(FileBinaryServiceTest::uncheckedToString).toCompletableFuture().join(),
                "Incorrect out-of-range segment when fetching from a file!");
    }

    @Test
    void testSetFileContent() {
        final String contents = "A new file";
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        assertNull(service.setContent(BinaryMetadata.builder(fileIRI).build(), inputStream)
                .toCompletableFuture().join(), "Setting content didn't complete cleanly!");
        assertEquals(contents, service.get(fileIRI).thenApply(Binary::getContent)
                        .thenApply(FileBinaryServiceTest::uncheckedToString)
                        .toCompletableFuture().join(), "Fetching new content returned incorrect value!");
    }

    @Test
    void testGetFileContentError() {
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());

        final CompletableFuture<InputStream> future = service.get(fileIRI).thenApply(Binary::getContent)
            .toCompletableFuture();
        assertThrows(CompletionException.class, () -> future.join(),
                "Fetching from invalid file should have thrown an exception!");
    }

    @Test
    void testGetFileSegmentError() {
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());

        final CompletableFuture<InputStream> future = service.get(fileIRI).thenApply(b -> b.getContent(0, 4))
            .toCompletableFuture();
        assertThrows(CompletionException.class, () -> future.join(),
                "Fetching binary segment from invalid file should have thrown an exception!");
    }

    @Test
    void testSetFileContentError() {
        final InputStream throwingMockInputStream = mock(InputStream.class, inv -> {
                throw new IOException("Expected error");
        });
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        assertAll(() -> service.setContent(BinaryMetadata.builder(fileIRI).build(), throwingMockInputStream)
            .handle((val, err) -> {
                assertNotNull(err, "There should have been an error with the input stream!");
                return null;
            }).toCompletableFuture().join());
    }

    @Test
    void testGetFileSkipContentError() {
        final IRI fileIRI = rdf.createIRI("file:///" + randomFilename());
        assertAll(() -> service.get(fileIRI).thenApply(binary -> binary.getContent(10, 20)).handle((val, err) -> {
                assertNotNull(err, "There should have been an error with the input stream!");
                return null;
            }).toCompletableFuture().join());
    }

    @Test
    void testBadIdentifier() {
        assertFalse(service.get(rdf.createIRI("http://example.com/")).thenApply(Binary::getContent)
                        .handle(FileBinaryServiceTest::checkError).toCompletableFuture().join(),
                        "Shouldn't be able to fetch content from a bad IRI!");
    }

    static boolean checkError(final Object asyncValue, final Throwable err) {
        assertNull(asyncValue, "The async value should be null!");
        assertNotNull(err, "There should be an async error!");
        return false;
    }

    static String uncheckedToString(final InputStream is) {
        try {
            return IOUtils.toString(is, UTF_8);
        } catch (final IOException ex) {
            return null;
        }
    }

    static String randomFilename() {
        final SecureRandom random = new SecureRandom();
        final String filename = new BigInteger(50, random).toString(32);
        return filename + ".json";
    }
}
