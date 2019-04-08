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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import javax.enterprise.event.Observes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;

@ServerEndpoint(value = "/")
public class TrellisWebSocket {

    private static final Logger LOGGER = getLogger(TrellisWebSocket.class);
    private static final ConcurrentMap<Session, String> peers = new ConcurrentHashMap<>();

    /**
     * Handle an observed resource update.
     * @param path the path of the updated resource
     */
    public void broadcast(final @Observes @ResourceUpdate String path) {
        LOGGER.debug("Broadcasting event for {}", path);
        peers.entrySet().stream().filter(e -> path.equals(e.getValue()) || path.startsWith(e.getValue() + "/"))
            .map(Entry::getKey).filter(Session::isOpen).map(Session::getAsyncRemote)
            .map(r -> r.sendText("pub " + path)).forEach(future -> {
                try {
                    future.get();
                } catch (final ExecutionException ex) {
                    LOGGER.error("Error sending notification: {}", ex.getCause().getMessage());
                } catch (final InterruptedException ex) {
                    LOGGER.error("Notification delivery interrupted: {}", ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            });
    }

    /**
     * Handle connection initiations.
     * @param session the session
     */
    @OnOpen
    public void open(final Session session) {
        LOGGER.debug("WebSocket connection initiated: {}", session.getId());
    }

    /**
     * Handle connection ends.
     * @param session the session
     */
    @OnClose
    public void close(final Session session) {
        peers.remove(session);
        LOGGER.debug("WebSocket connection closed: {}", session.getId());
    }

    /**
     * Handle socket errors.
     * @param session the session
     * @param throwable the reason
     */
    @OnError
    public void error(final Session session, final Throwable throwable) {
        LOGGER.error("WebSocket error: {}", throwable.getMessage());
    }

    /**
     * Handle subscriptions.
     * @param message the message
     * @param session the session
     * @return the response
     */
    @OnMessage
    public String message(final String message, final Session session) {
        final String[] parts = message.trim().split(" +", 2);
        if (parts.length == 2 && parts[0].equals("sub")) {
            peers.put(session, getUri(parts[1]));
            return "Successfully subscribed to " + parts[1];
        }
        return "Failure";
    }

    private String getUri(final String uri) {
        if (uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
}
