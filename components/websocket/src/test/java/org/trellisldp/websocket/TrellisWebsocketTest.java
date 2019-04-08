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
package org.trellisldp.websocket;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.RuntimeTrellisException;

public class TrellisWebsocketTest {

    private static final String SUBSCRIPTION = "Successfully subscribed to ";

    @Mock
    private Session mockSession, mockSession2;

    @Mock
    private RemoteEndpoint.Async mockEndpoint, mockEndpoint2;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockSession.getId()).thenReturn("Session-id");
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession.getAsyncRemote()).thenReturn(mockEndpoint);
        when(mockEndpoint.sendText(anyString())).thenReturn(completedFuture(null));

        when(mockSession2.getId()).thenReturn("Session-id2");
        when(mockSession2.isOpen()).thenReturn(true);
        when(mockSession2.getAsyncRemote()).thenReturn(mockEndpoint2);
        when(mockEndpoint2.sendText(anyString())).thenReturn(completedFuture(null));
    }

    @Test
    public void testOpen() {
        final TrellisWebSocket socket = new TrellisWebSocket();
        socket.open(mockSession);
        socket.broadcast("/path");
        verify(mockSession, never()).getAsyncRemote();
    }

    @Test
    public void testPathMessage() {
        final TrellisWebSocket socket = new TrellisWebSocket();
        socket.open(mockSession);
        socket.open(mockSession2);
        assertEquals(SUBSCRIPTION + "/path", socket.message("sub /path", mockSession));
        assertEquals(SUBSCRIPTION + "/other", socket.message("sub /other", mockSession2));
        socket.broadcast("/path");

        verify(mockSession).getAsyncRemote();
        verify(mockEndpoint).sendText(eq("pub /path"));

        verify(mockSession2, never()).getAsyncRemote();
        verify(mockEndpoint2, never()).sendText(eq("pub /path"));
    }

    @Test
    public void testChildMessage() {
        final TrellisWebSocket socket = new TrellisWebSocket();
        socket.open(mockSession);
        socket.open(mockSession2);
        assertEquals(SUBSCRIPTION + "/path", socket.message("sub /path", mockSession));
        assertEquals(SUBSCRIPTION + "/path", socket.message("sub /path", mockSession2));
        socket.broadcast("/path/child");

        verify(mockSession).getAsyncRemote();
        verify(mockEndpoint).sendText(eq("pub /path/child"));

        verify(mockSession2).getAsyncRemote();
        verify(mockEndpoint2).sendText(eq("pub /path/child"));
    }

    @Test
    public void testNearMatchMessage() {
        final TrellisWebSocket socket = new TrellisWebSocket();
        socket.open(mockSession);
        socket.open(mockSession2);
        assertEquals(SUBSCRIPTION + "/pathlong", socket.message("sub /pathlong", mockSession));
        assertEquals(SUBSCRIPTION + "/path", socket.message("sub /path", mockSession2));
        socket.broadcast("/path");

        verify(mockSession, never()).getAsyncRemote();
        verify(mockEndpoint, never()).sendText(eq("pub /path"));

        verify(mockSession2).getAsyncRemote();
        verify(mockEndpoint2).sendText(eq("pub /path"));
    }

    @Test
    public void testClosedSocketMessage() {
        final TrellisWebSocket socket = new TrellisWebSocket();
        socket.open(mockSession);
        socket.open(mockSession2);
        assertEquals(SUBSCRIPTION + "/path", socket.message("sub    /path", mockSession));
        assertEquals(SUBSCRIPTION + "/path", socket.message("sub  /path", mockSession2));
        socket.broadcast("/path/child");
        socket.close(mockSession2);
        socket.broadcast("/path/child");

        verify(mockSession, times(2)).getAsyncRemote();
        verify(mockEndpoint, times(2)).sendText(eq("pub /path/child"));

        verify(mockSession2, times(1)).getAsyncRemote();
        verify(mockEndpoint2, times(1)).sendText(eq("pub /path/child"));
    }

    @Test
    public void testErrorHandler() {
        final TrellisWebSocket socket = new TrellisWebSocket();
        assertDoesNotThrow(() -> socket.error(mockSession, new RuntimeTrellisException("Expected Exception")));
    }

    @Test
    public void testFailedAsync() {
        when(mockEndpoint.sendText(anyString())).thenAnswer(inv -> runAsync(() -> {
            throw new RuntimeTrellisException("Expected async exception");
        }));
        final TrellisWebSocket socket = new TrellisWebSocket();
        socket.open(mockSession);
        assertEquals(SUBSCRIPTION + "/path", socket.message("sub /path", mockSession));
        assertDoesNotThrow(() -> socket.broadcast("/path"));

        verify(mockSession).getAsyncRemote();
        verify(mockEndpoint).sendText(eq("pub /path"));
    }

    @Test
    public void testNoSubscription() {
        final TrellisWebSocket socket = new TrellisWebSocket();
        assertEquals("Failure", socket.message("blah blah blah", mockSession));
        assertEquals("Failure", socket.message("sub", mockSession));
        assertEquals("Failure", socket.message("sub ", mockSession));
    }
}
