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
package org.trellisldp.event.jsonb;

import static java.util.stream.Collectors.toList;

import java.util.List;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.api.Event;
import org.trellisldp.vocabulary.AS;

/**
 * A structure used for serializing an Event into an ActivityStream 2.0 JSON object.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-core/">Activity Streams 2.0</a>
 *
 * @author acoburn
 */
@JsonbPropertyOrder({"@context","id", "type", "inbox", "actor", "object", "published"})
public class ActivityStreamMessage {

    private String identifier;
    private String inbox;
    private String published;
    private String context = "https://www.w3.org/ns/activitystreams";
    private List<String> type;
    private List<String> actor;
    private EventResource object;

    /**
     * The resource that is the object of this message.
     */
    public static class EventResource {
        private final String identifier;
        private final List<String> type;

        /**
         * Create a new event resource.
         *
         * @param id the identifier
         * @param type the types
         */
        public EventResource(final String id, final List<String> type) {
            this.identifier = id;
            this.type = type.isEmpty() ? null : type;
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
    }

    /**
     * @return the event identifier
     */
    public String getId() {
        return identifier;
    }

    /**
     * @return the inbox assocated with the resource
     */
    public String getInbox() {
        return inbox;
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
    public String getContext() {
        return context;
    }

    /**
     * @return the event types
     */
    public List<String> getType() {
        return type;
    }

    /**
     * @return the actors associated with this event
     */
    public List<String> getActor() {
        return actor;
    }

    /**
     * @return the resource that is the object of this message
     */
    public EventResource getObject() {
        return object;
    }

    /**
     * Populate a ActivityStreamMessage from an Event.
     *
     * @param event The event
     * @return an ActivityStreamMessage
     */
    public static ActivityStreamMessage from(final Event event) {

        final ActivityStreamMessage msg = new ActivityStreamMessage();

        msg.identifier = event.getIdentifier().getIRIString();
        msg.type = event.getTypes().stream().map(IRI::getIRIString)
            .map(type -> type.startsWith(AS.getNamespace()) ? type.substring(AS.getNamespace().length()) : type)
            .collect(toList());

        msg.published = event.getCreated().toString();

        final List<String> actors = event.getAgents().stream().map(IRI::getIRIString).collect(toList());
        if (!actors.isEmpty()) {
            msg.actor = actors;
        }

        event.getInbox().map(IRI::getIRIString).ifPresent(inbox -> msg.inbox = inbox);
        event.getObject().map(IRI::getIRIString).ifPresent(object ->
            msg.object = new EventResource(object,
                    event.getObjectTypes().stream().map(IRI::getIRIString).collect(toList())));

        return msg;
    }
}
