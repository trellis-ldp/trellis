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
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.RuntimeRepositoryException;
import org.trellisldp.id.UUIDGenerator;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class HttpBasedBinaryServiceTest {

    private static final RDF rdf = new SimpleRDF();

    private static final IRI resource = rdf.createIRI("http://www.trellisldp.org/ns/trellis.ttl");
    private static final IRI sslResource = rdf.createIRI("https://s3.amazonaws.com/www.trellisldp.org/ns/trellis.ttl");
    private static final IdentifierService idService = new UUIDGenerator();

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
        when(mockInvocationBuilder.put(any(Entity.class))).thenReturn(mockResponse);
        when(mockInvocationBuilder.delete()).thenReturn(mockResponse);
        when(mockInvocationBuilder.get()).thenReturn(mockResponse);
        when(mockResponse.getStatusInfo()).thenReturn(mockStatusType);
        when(mockStatusType.getFamily()).thenReturn(SUCCESSFUL);
        when(mockStatusType.toString()).thenReturn("Successful");
    }

    @Test
    public void testExists() {

        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"));

        assertTrue(resolver.exists(resource));
        assertTrue(resolver.exists(sslResource));
        assertFalse(resolver.exists(rdf.createIRI("http://www.trellisldp.org/ns/non-existent.ttl")));
    }

    @Test
    public void testGetIdSupplier() {
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"));
        assertTrue(resolver.getIdentifierSupplier().get().startsWith("http://example.org/"));
    }

    @Test
    public void testBase64Digest() throws IOException {
        final byte[] data = "Some data".getBytes(UTF_8);
        when(mockInputStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException("Expected Error"));

        final BinaryService service = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"));
        assertEquals(of("W4L4v03yv7DmbMqnMG/QJA=="), service.digest("MD5", new ByteArrayInputStream(data)));
        assertEquals(of("jXJFPxAHmvPfx/z8QQmx7VXhg58="), service.digest("SHA", new ByteArrayInputStream(data)));
        assertEquals(of("jXJFPxAHmvPfx/z8QQmx7VXhg58="), service.digest("SHA-1", new ByteArrayInputStream(data)));
        assertFalse(service.digest("MD5", mockInputStream).isPresent());
    }

    @Test
    public void testGetContent() {
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"));

        assertTrue(resolver.getContent(resource).isPresent());
        assertTrue(resolver.getContent(resource).map(this::uncheckedToString).get()
                .contains("owl:Ontology"));
    }

    @Test
    public void testGetSslContent() {
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"));

        assertTrue(resolver.getContent(sslResource).isPresent());
        assertTrue(resolver.getContent(sslResource).map(this::uncheckedToString).get()
                .contains("owl:Ontology"));
    }

    @Test
    public void testSetContent() {
        final String contents = "A new resource";
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"));

        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        assertThrows(RuntimeRepositoryException.class, () -> resolver.setContent(sslResource, inputStream));
    }

    @Test
    public void testMockedClient() throws IOException {
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"),
                mockClient);
        final String contents = "A new resource";
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        resolver.setContent(sslResource, inputStream);

        verify(mockInvocationBuilder).put(any(Entity.class));
    }

    @Test
    public void testMockedDelete() throws IOException {
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"),
                mockClient);
        resolver.purgeContent(sslResource);

        verify(mockInvocationBuilder).delete();
    }

    @Test
    public void testMockedDeleteException() {
        when(mockStatusType.getFamily()).thenReturn(CLIENT_ERROR);
        when(mockStatusType.toString()).thenReturn("BAD REQUEST");
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"),
                mockClient);
        assertThrows(RuntimeRepositoryException.class, () -> resolver.purgeContent(sslResource));
    }

    @Test
    public void testExceptedPut() throws IOException {
        when(mockStatusType.getFamily()).thenReturn(CLIENT_ERROR);
        when(mockStatusType.toString()).thenReturn("BAD REQUEST");
        final String contents = "A new resource";
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"),
                mockClient);
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));

        assertThrows(RuntimeRepositoryException.class, () ->
                resolver.setContent(resource, inputStream));
    }

    @Test
    public void testExceptedDelete() throws IOException {
        when(mockStatusType.getFamily()).thenReturn(CLIENT_ERROR);
        when(mockStatusType.toString()).thenReturn("BAD REQUEST");
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"),
                mockClient);

        assertThrows(RuntimeRepositoryException.class, () ->
            resolver.purgeContent(resource));
    }

    @Test
    public void testGetNoEntity() throws IOException {
        final BinaryService resolver = new HttpBasedBinaryService(idService.getSupplier("http://example.org/"),
                mockClient);
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
