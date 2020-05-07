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
package org.trellisldp.event.jackson;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

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
@JsonInclude(NON_ABSENT)
@JsonPropertyOrder({"@context","id", "type", "inbox", "actor", "object", "published"})
class ActivityStreamMessage {

    private String context = "https://www.w3.org/ns/activitystreams";
    private String id;
    private List<String> type;
    private String inbox;
    private List<String> actor;
    private EventResource object;
    private String published;

    /**
     * The resource that is the object of a message.
     */
    @JsonInclude(NON_ABSENT)
    static class EventResource {
        private final String id;
        private final List<String> type;

        /**
         * Create a new resource for the event.
         *
         * @param id the identifier
         * @param type the types
         */
        public EventResource(final String id, final List<String> type) {
            this.id = id;
            this.type = type.isEmpty() ? null : type;
        }

        /**
         * @return the identifier
         */
        public String getId() {
            return id;
        }

        /**
         * @return the resource types
         */
        public List<String> getType() {
            return type;
        }
    }

    /**
     * @return the JSON-LD context
     */
    @JsonProperty("@context")
    public String getContext() {
        return context;
    }

    /**
     * @return the event identifier
     */
    public String getId() {
        return id;
    }

    /**
     * @return the event types
     */
    public List<String> getType() {
        return type;
    }

    /**
     * @return the inbox assocated with the resource
     */
    public String getInbox() {
        return inbox;
    }

    /**
     * @return the actors associated with this event
     */
    public List<String> getActor() {
        return actor;
    }

    /**
     * @return the resource that is the object of this event
     */
    public EventResource getObject() {
        return object;
    }

    /**
     * @return the created date
     */
    @JsonProperty("published")
    public String getPublished() {
        return published;
    }

    /**
     * Populate a ActivityStreamMessage from an Event.
     *
     * @param event The event
     * @return an ActivityStreamMessage
     */
    public static ActivityStreamMessage from(final Event event) {

        final ActivityStreamMessage msg = new ActivityStreamMessage();

        msg.id = event.getIdentifier().getIRIString();
        msg.type = event.getTypes().stream().map(IRI::getIRIString)
            .map(type -> type.startsWith(AS.getNamespace()) ? type.substring(AS.getNamespace().length()) : type)
            .collect(toList());

        msg.published = event.getCreated().toString();

        final List<String> actors = event.getAgents().stream().map(IRI::getIRIString).collect(toList());
        msg.actor = actors.isEmpty() ? null : actors;

        event.getInbox().map(IRI::getIRIString).ifPresent(inbox -> msg.inbox = inbox);
        event.getObject().map(IRI::getIRIString).ifPresent(object ->
            msg.object = new EventResource(object,
                    event.getObjectTypes().stream().map(IRI::getIRIString).collect(toList())));

        return msg;
    }
}
