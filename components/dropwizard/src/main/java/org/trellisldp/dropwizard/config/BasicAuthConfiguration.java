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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for Basic AuthN.
 */
public class BasicAuthConfiguration {

    private boolean enabled = true;

    private String usersFile;

    /**
     * Get whether basic authentication has been enabled.
     * @return true if basic auth is enabled; false otherwise
     */
    @JsonProperty
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * Enable or disable basic authentication.
     * @param enabled true if basic auth is enabled; false otherwise
     */
    @JsonProperty
    public void setEnabled(final boolean enabled) {
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
}
