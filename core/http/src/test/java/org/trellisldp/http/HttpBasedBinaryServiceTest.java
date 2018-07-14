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
package org.trellisldp.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.of;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.id.UUIDGenerator;

/**
 * @author acoburn
 */
public class HttpBasedBinaryServiceTest {

    private static final RDF rdf = new SimpleRDF();

    private static final IRI resource = rdf.createIRI("https://www.trellisldp.org/ns/trellis.ttl");
    private static final IdentifierService idService = new UUIDGenerator("http://example.org/");

    @Mock
    private Client mockClient;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private Response mockResponse;

    @Mock
    private WebTarget mockWebTarget;

    @Mock
    private Invocation.Builder mockInvocationBuilder;

    @Mock
    private Response.StatusType mockStatusType;

    @BeforeEach
    public void setUp() throws IOException {
        initMocks(this);
        when(mockClient.target(anyString())).thenReturn(mockWebTarget);
        when(mockWebTarget.request()).thenReturn(mockInvocationBuilder);
        when(mockInvocationBuilder.header(anyString(), anyString())).thenReturn(mockInvocationBuilder);
        when(mockInvocationBuilder.put(any(Entity.class))).thenReturn(mockResponse);
        when(mockInvocationBuilder.delete()).thenReturn(mockResponse);
        when(mockInvocationBuilder.get()).thenReturn(mockResponse);
        when(mockResponse.getStatusInfo()).thenReturn(mockStatusType);
        when(mockStatusType.getFamily()).thenReturn(SUCCESSFUL);
        when(mockStatusType.toString()).thenReturn("Successful");
    }

    @Test
    public void testExists() {

        final BinaryService resolver = new HttpBasedBinaryService(idService);

        assertTrue(resolver.exists(resource));
        assertFalse(resolver.exists(rdf.createIRI("http://www.trellisldp.org/ns/non-existent.ttl")));
    }

    @Test
    public void testGetIdSupplier() {
        final BinaryService resolver = new HttpBasedBinaryService(idService);
        assertTrue(resolver.generateIdentifier().startsWith("http://example.org/"));
    }

    @Test
    public void testBase64Digest() throws IOException {
        final byte[] data = "Some data".getBytes(UTF_8);
        when(mockInputStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException("Expected Error"));

        final BinaryService service = new HttpBasedBinaryService(idService);
        assertEquals(of("W4L4v03yv7DmbMqnMG/QJA=="), service.digest("MD5", new ByteArrayInputStream(data)));
        assertEquals(of("jXJFPxAHmvPfx/z8QQmx7VXhg58="), service.digest("SHA", new ByteArrayInputStream(data)));
        assertEquals(of("jXJFPxAHmvPfx/z8QQmx7VXhg58="), service.digest("SHA-1", new ByteArrayInputStream(data)));
        assertFalse(service.digest("MD5", mockInputStream).isPresent());
    }

    @Test
    public void testGetContent() {
        final BinaryService resolver = new HttpBasedBinaryService(idService);

        assertTrue(resolver.getContent(resource).isPresent());
        assertTrue(resolver.getContent(resource).map(this::uncheckedToString).get()
                .contains("owl:Ontology"));
    }

    @Test
    public void testGetContentSegment() {
        final BinaryService resolver = new HttpBasedBinaryService(idService);

        final Optional<InputStream> res = resolver.getContent(resource, 5, 20);
        assertTrue(res.isPresent());
        final String str = res.map(this::uncheckedToString).get();

        assertFalse(str.contains("owl:Ontology"));
        assertEquals(16, str.length());
    }

    @Test
    public void testGetSslContent() {
        final BinaryService resolver = new HttpBasedBinaryService(idService);

        assertTrue(resolver.getContent(resource).isPresent());
        assertTrue(resolver.getContent(resource).map(this::uncheckedToString).get()
                .contains("owl:Ontology"));
    }

    @Test
    public void testSetContent() {
        final String contents = "A new resource";
        final BinaryService resolver = new HttpBasedBinaryService(idService);

        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        assertThrows(RuntimeTrellisException.class, () -> resolver.setContent(resource, inputStream));
    }

    @Test
    public void testMockedClient() throws IOException {
        final BinaryService resolver = new HttpBasedBinaryService(idService, mockClient);
        final String contents = "A new resource";
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        resolver.setContent(resource, inputStream);

        verify(mockInvocationBuilder).put(any(Entity.class));
    }

    @Test
    public void testMockedDelete() throws IOException {
        final BinaryService resolver = new HttpBasedBinaryService(idService, mockClient);
        resolver.purgeContent(resource);

        verify(mockInvocationBuilder).delete();
    }

    @Test
    public void testMockedDeleteException() {
        when(mockStatusType.getFamily()).thenReturn(CLIENT_ERROR);
        when(mockStatusType.toString()).thenReturn("BAD REQUEST");
        final BinaryService resolver = new HttpBasedBinaryService(idService, mockClient);
        assertThrows(RuntimeTrellisException.class, () -> resolver.purgeContent(resource));
    }

    @Test
    public void testExceptedPut() throws IOException {
        when(mockStatusType.getFamily()).thenReturn(CLIENT_ERROR);
        when(mockStatusType.toString()).thenReturn("BAD REQUEST");
        final String contents = "A new resource";
        final BinaryService resolver = new HttpBasedBinaryService(idService, mockClient);
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));

        assertThrows(RuntimeTrellisException.class, () ->
                resolver.setContent(resource, inputStream));
    }

    @Test
    public void testExceptedDelete() throws IOException {
        when(mockStatusType.getFamily()).thenReturn(CLIENT_ERROR);
        when(mockStatusType.toString()).thenReturn("BAD REQUEST");
        final BinaryService resolver = new HttpBasedBinaryService(idService, mockClient);

        assertThrows(RuntimeTrellisException.class, () ->
            resolver.purgeContent(resource));
    }

    @Test
    public void testGetNoEntity() throws IOException {
        final BinaryService resolver = new HttpBasedBinaryService(idService, mockClient);
        assertFalse(resolver.getContent(resource).isPresent());
    }

    private String uncheckedToString(final InputStream is) {
        try {
            return IOUtils.toString(is, UTF_8);
        } catch (final IOException ex) {
            return null;
        }
    }
}
