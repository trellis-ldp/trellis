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
package org.trellisldp.dropwizard.config;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author acoburn
 */
public class NotificationsConfiguration {

    public enum Type {
        NONE, JMS, KAFKA
    }

    private final Map<String, String> other = new HashMap<>();

    private String topic = "trellis";
    private boolean enabled;
    private Type type = Type.NONE;
    private String connectionString;

    /**
     * Get whether notifications have been enabled.
     * @return true if notifications are enabled; false otherwise
     */
    @JsonProperty
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * Enable or disable notifications.
     * @param enabled true if notifications are enabled; false otherwise
     */
    @JsonProperty
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the topic/queue name.
     * @return the name of the topic/queue
     */
    @JsonProperty
    public String getTopicName() {
        return topic;
    }

    /**
     * Set the queue/topic name.
     * @param topic the name of the topic/queue
     */
    @JsonProperty
    public void setTopicName(final String topic) {
        this.topic = topic;
    }

    /**
     * Get the notification component type.
     * @return the notification component
     */
    @JsonProperty
    public Type getType() {
        return type;
    }

    /**
     * Set the notification component type.
     * @param type the component type
     */
    @JsonProperty
    public void setType(final Type type) {
        this.type = type;
    }

    /**
     * Get the connection string.
     * @return the connection string
     */
    @JsonProperty
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * Set the connection string.
     * @param connectionString the connection string
     */
    @JsonProperty
    public void setConnectionString(final String connectionString) {
        this.connectionString = connectionString;
    }

    /**
     * An extension point for other configuration values.
     * @param name the configuration name
     * @param value the configuration value
     */
    @JsonAnySetter
    public void set(final String name, final String value) {
        other.put(name, value);
    }

    /**
     * Get other properties.
     * @return all other properties as a Map
     */
    @JsonAnyGetter
    public Map<String, String> any() {
        return other;
    }
}
