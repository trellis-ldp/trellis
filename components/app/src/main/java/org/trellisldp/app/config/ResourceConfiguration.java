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

public class ResourceConfiguration {

    private String resourceLocation;

    private String userName;

    private String password;

    /**
     * Set the RDF Connection configuration.
     * @param config the RDF Connection location
     */
    @JsonProperty
    public void setResourceLocation(final String config) {
        this.resourceLocation = config;
    }

    /**
     * Get the RDF Connection configuration.
     * @return the RDF Connection location
     */
    @JsonProperty
    public String getResourceLocation() {
        return resourceLocation;
    }

    /**
     * Set a BasicAuth userName.
     * @param userName a BasicAuth userName
     */
    @JsonProperty
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /**
     * Get a BasicAuth userName.
     * @return a BasicAuth userName
     */
    @JsonProperty
    public String getUserName() {
        return userName;
    }

    /**
     * Set a BasicAuth password.
     * @param password a BasicAuth password
     */
    @JsonProperty
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * Get a BasicAuth password.
     * @return a BasicAuth password
     */
    @JsonProperty
    public String getPassword() {
        return password;
    }
}
