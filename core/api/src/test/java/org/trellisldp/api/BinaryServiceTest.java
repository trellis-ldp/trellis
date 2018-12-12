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
package org.trellisldp.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.io.IOUtils.readLines;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * @author acoburn
 */
public class BinaryServiceTest {

    private static final RDF rdf = new SimpleRDF();
    private static final Map<String, List<String>> hints = emptyMap();

    private final IRI identifier = rdf.createIRI("trellis:data/resource");

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private Binary mockBinary;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        doCallRealMethod().when(mockBinaryService).setContent(any(BinaryMetadata.class),
                any(InputStream.class), any(MessageDigest.class), eq(hints));
    }

    @Test
    public void testGetContent() throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("FooBar".getBytes(UTF_8));
        when(mockBinaryService.get(eq(identifier))).thenAnswer(inv -> completedFuture(mockBinary));
        when(mockBinary.getContent(anyInt(), anyInt())).thenReturn(inputStream);
        try (final InputStream content = mockBinaryService.get(identifier).thenApply(b -> b.getContent(0, 6)).join()) {
            assertEquals("FooBar", IOUtils.toString(content, UTF_8), "Binary content did not match");
        }
    }

    @Test
    public void testSetContent() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("FooBar".getBytes(UTF_8));
        when(mockBinaryService.setContent(any(BinaryMetadata.class), any(InputStream.class), eq(hints)))
            .thenAnswer(inv -> {
                readLines((InputStream) inv.getArguments()[1], UTF_8);
                return completedFuture(null);
            });
        assertDoesNotThrow(mockBinaryService.setContent(BinaryMetadata.builder(identifier).build(),
                inputStream, MessageDigest.getInstance("MD5"), hints).thenApply(getEncoder()::encodeToString)
                .thenAccept(digest -> assertEquals("8yom4qOoqjOM13tuEmPFNQ==", digest))::join);
    }
}
