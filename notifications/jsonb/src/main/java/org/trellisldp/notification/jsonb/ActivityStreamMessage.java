/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
package org.trellisldp.notification.jsonb;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.api.Notification;
import org.trellisldp.vocabulary.AS;

/**
 * A structure used for serializing an Notification into an ActivityStream 2.0 JSON object.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-core/">Activity Streams 2.0</a>
 *
 * @author acoburn
 */
@JsonbPropertyOrder({"@context","id", "type", "actor", "object", "published"})
public class ActivityStreamMessage {

    private String identifier;
    private String published;
    private List<Object> contexts = List.of("https://www.w3.org/ns/activitystreams",
            Map.of("state", Map.of("@id", "http://www.w3.org/2011/http-headers#etag")));
    private List<String> type;
    private List<String> actor;
    private NotificationResource object;

    /**
     * The resource that is the object of this message.
     */
    public static class NotificationResource {
        private final String identifier;
        private final String state;
        private final List<String> type;

        /**
         * Create a new notification resource.
         *
         * @param id the identifier
         * @param type the types
         * @param state the state
         */
        public NotificationResource(final String id, final List<String> type, final String state) {
            this.identifier = id;
            this.type = type.isEmpty() ? null : type;
            this.state = state;
        }

        /**
         * @return the identifier
         */
        public String getId() {
            return identifier;
        }

        /**
         * @return the resource types
         */
        public List<String> getType() {
            return type;
        }

        /**
         * @return the resource state
         */
        public String getState() {
            return state;
        }
    }

    /**
     * @return the notification identifier
     */
    public String getId() {
        return identifier;
    }

    /**
     * @return the created date
     */
    @JsonbProperty("published")
    public String getPublished() {
        return published;
    }

    /**
     * @return the JSON-LD context
     */
    @JsonbProperty("@context")
    public List<Object> getContext() {
        return contexts;
    }

    /**
     * @return the notification types
     */
    public List<String> getType() {
        return type;
    }

    /**
     * @return the actors associated with this notification
     */
    public List<String> getActor() {
        return actor;
    }

    /**
     * @return the resource that is the object of this message
     */
    public NotificationResource getObject() {
        return object;
    }

    /**
     * Populate a ActivityStreamMessage from an Notification.
     *
     * @param notification The notification
     * @return an ActivityStreamMessage
     */
    public static ActivityStreamMessage from(final Notification notification) {

        final ActivityStreamMessage msg = new ActivityStreamMessage();

        msg.identifier = notification.getIdentifier().getIRIString();
        msg.type = notification.getTypes().stream().map(IRI::getIRIString)
            .map(type -> type.startsWith(AS.getNamespace()) ? type.substring(AS.getNamespace().length()) : type)
            .collect(toList());

        msg.published = notification.getCreated().toString();

        final List<String> actors = notification.getAgents().stream().map(IRI::getIRIString).collect(toList());
        if (!actors.isEmpty()) {
            msg.actor = actors;
        }

        notification.getObject().map(IRI::getIRIString).ifPresent(object ->
            msg.object = new NotificationResource(object,
                notification.getObjectTypes().stream().map(IRI::getIRIString).collect(toList()),
                notification.getObjectState().orElse(null)));

        return msg;
    }
}
