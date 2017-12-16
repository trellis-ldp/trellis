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
package org.trellisldp.event;

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
 * A structure used for serializing an Event into an ActivityStream 2.0 JSON object
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-core/">Activity Streams 2.0</a>
 *
 * @author acoburn
 */
@JsonInclude(NON_ABSENT)
@JsonPropertyOrder({"@context","id", "type", "inbox", "actor", "object", "published"})
class ActivityStreamMessage {

    /**
     * The target resource of a message
     */
    @JsonInclude(NON_ABSENT)
    static class EventResource {
        private String id;
        private List<String> type;

        /**
         * Create a new event resource target
         * @param id the identifier
         * @param type the types
         */
        public EventResource(final String id, final List<String> type) {
            this.id = id;
            this.type = type.isEmpty() ? null : type;
        }

        /**
         * Get the identifier
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * Get the resource types
         * @return the types
         */
        public List<String> getType() {
            return type;
        }
    }

    private String id;
    private List<String> type;
    private String inbox;
    private List<String> actor;
    private EventResource object;
    private String published;

    /**
     * Get the event identifier
     * @return the event identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Get the event types
     * @return the event types
     */
    public List<String> getType() {
        return type;
    }

    /**
     * The inbox assocated with the resource
     */
    public String getInbox() {
        return inbox;
    }

    /**
     * The actors associated with this event
     */
    public List<String> getActor() {
        return actor;
    }

    /**
     * The target resource
     */
    public EventResource getObject() {
        return object;
    }

    /**
     * The created date
     */
    @JsonProperty("published")
    public String getPublished() {
        return published;
    }

    /**
     * The JSON-LD context
     */
    @JsonProperty("@context")
    public String context = "https://www.w3.org/ns/activitystreams";

    /**
     * Populate a ActivityStreamMessage from an Event
     * @param event The event
     * @return an ActivityStreamMessage
     */
    public static ActivityStreamMessage from(final Event event) {

        final ActivityStreamMessage msg = new ActivityStreamMessage();

        msg.id = event.getIdentifier().getIRIString();
        msg.type = event.getTypes().stream().map(IRI::getIRIString)
            .map(type -> type.startsWith(AS.URI) ? type.substring(AS.URI.length()) : type)
            .collect(toList());

        msg.published = event.getCreated().toString();

        final List<String> actors = event.getAgents().stream().map(IRI::getIRIString).collect(toList());
        msg.actor = actors.isEmpty() ? null : actors;

        event.getInbox().map(IRI::getIRIString).ifPresent(inbox -> msg.inbox = inbox);
        event.getTarget().map(IRI::getIRIString).ifPresent(target ->
            msg.object = new EventResource(target,
                    event.getTargetTypes().stream().map(IRI::getIRIString).collect(toList())));

        return msg;
    }
}
