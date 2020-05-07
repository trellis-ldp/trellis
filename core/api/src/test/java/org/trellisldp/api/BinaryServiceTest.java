/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
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
package org.trellisldp.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
class BinaryServiceTest {

    private static final RDF rdf = new SimpleRDF();

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private Binary mockBinary;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void testGetContent() throws IOException {
        final IRI identifier = rdf.createIRI("trellis:data/resource");
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("FooBar".getBytes(UTF_8));
        when(mockBinaryService.get(eq(identifier))).thenAnswer(inv -> completedFuture(mockBinary));
        when(mockBinary.getContent(anyInt(), anyInt())).thenReturn(inputStream);
        try (final InputStream content = mockBinaryService.get(identifier)
                .thenApply(b -> b.getContent(0, 6)).toCompletableFuture().join()) {
            assertEquals("FooBar", IOUtils.toString(content, UTF_8), "Binary content did not match");
        }
    }
}
