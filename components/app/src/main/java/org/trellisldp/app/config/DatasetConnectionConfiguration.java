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

import java.util.Optional;

public class DatasetConnectionConfiguration {

    private String datasetLocation;

    private String userName;

    private String password;

    /**
     * Set the Dataset Location.
     * @param datasetLocation the Dataset Location
     */
    @JsonProperty
    public void setDatasetLocation(final String datasetLocation) {
        this.datasetLocation = datasetLocation;
    }

    /**
     * Get the Dataset Location.
     * @return the Dataset Location
     */
    @JsonProperty
    public Optional<String> getDatasetLocation() {
        return Optional.ofNullable(datasetLocation);
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
    public Optional<String> getUserName() {
        return Optional.ofNullable(userName);
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
    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }
}
