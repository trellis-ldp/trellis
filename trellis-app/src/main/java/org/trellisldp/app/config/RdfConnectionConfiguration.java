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
package org.trellisldp.app.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for an RDF Connection.
 */
public class RdfConnectionConfiguration {

    private String location = null;

    /**
     * Set the location of the RDF store.
     * @param location the location of the RDF store
     */
    @JsonProperty
    public void setLocation(final String location) {
        this.location = location;
    }

    /**
     * Get the location of the RDF store.
     * @return the RDF store locations
     */
    @JsonProperty
    public String getLocation() {
        return location;
    }
}

