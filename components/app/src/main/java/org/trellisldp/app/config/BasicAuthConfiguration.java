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

import javax.validation.constraints.NotNull;

/**
 * Configuration for Basic AuthN.
 */
public class BasicAuthConfiguration {

    @NotNull
    private String realm = "trellis";

    private Boolean enabled = true;

    private String usersFile;

    /**
     * Get whether basic authentication has been enabled.
     * @return true if basic auth is enabled; false otherwise
     */
    @JsonProperty
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Enable or disable basic authentication.
     * @param enabled true if basic auth is enabled; false otherwise
     */
    @JsonProperty
    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the username file.
     * @return the username file
     */
    @JsonProperty
    public String getUsersFile() {
        return usersFile;
    }

    /**
     * Set the username file.
     * @param usersFile the username file
     */
    @JsonProperty
    public void setUsersFile(final String usersFile) {
        this.usersFile = usersFile;
    }

    /**
     * Get the security realm.
     * @return the realm; by default, this is 'trellis'
     */
    @JsonProperty
    public String getRealm() {
        return realm;
    }

    /**
     * Set the security realm.
     * @param realm the security realm
     */
    @JsonProperty
    public void setRealm(final String realm) {
        this.realm = realm;
    }
}
