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
import static java.util.Collections.emptyMap;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class BinaryServiceTest {

    private static final RDF rdf = new SimpleRDF();

    private final IRI identifier = rdf.createIRI("trellis:repository/resource");
    private final IRI other = rdf.createIRI("trellis:repository/other");
    private final String checksum = "blahblahblah";

    @Mock
    private BinaryService mockBinaryService;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private Session mockSession;

    @Mock
    private Binary mockBinary;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        doCallRealMethod().when(mockBinaryService).calculateDigest(any(), any());
        doCallRealMethod().when(mockBinaryService).setContent(any(), any());
        when(mockBinaryService.exists(eq(identifier))).thenReturn(true);
        when(mockBinaryService.digest(any(), any())).thenReturn(of(checksum));
    }

    @Test
    public void testDefaultMethods() {
        when(mockBinaryService.getContent(any())).thenReturn(of(mockInputStream));
        final Map<String, String> data = emptyMap();
        mockBinaryService.setContent(identifier, mockInputStream);
        assertEquals(of(checksum), mockBinaryService.calculateDigest(other, "md5"));
        verify(mockBinaryService).setContent(eq(identifier), eq(mockInputStream),
                eq(emptyMap()));
        assertEquals(mockInputStream, mockBinaryService.getContent(identifier).get());
    }

    @Test
    public void testGetContent() throws IOException {
        doCallRealMethod().when(mockBinaryService).getContent(eq(identifier));
        when(mockBinaryService.getContent(eq(identifier), any()))
            .thenReturn(of(new ByteArrayInputStream("FooBar".getBytes(UTF_8))));
        final Optional<InputStream> content = mockBinaryService.getContent(identifier);
        assertTrue(content.isPresent());
        assertEquals("FooBar", IOUtils.toString(content.get(), UTF_8));
    }

    @Test
    public void testMultipartUpload() {
        final String baseUrl = "baseurl";
        final String path = "path";
        final BinaryService.MultipartUpload upload = new BinaryService.MultipartUpload(baseUrl, path, mockSession,
                mockBinary);
        assertEquals(baseUrl, upload.getBaseUrl());
        assertEquals(path, upload.getPath());
        assertEquals(mockSession, upload.getSession());
        assertEquals(mockBinary, upload.getBinary());
    }
}
