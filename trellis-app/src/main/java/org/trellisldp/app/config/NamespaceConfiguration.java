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
 * Configuration for the namespace service.
 */
public class NamespaceConfiguration {

    @NotNull
    private String file;

    /**
     * Set the filename for use with the namespace service.
     * @param file the namespace file
     */
    @JsonProperty
    public void setFile(final String file) {
        this.file = file;
    }

    /**
     * Get the filename used with the namespace service.
     * @return the filename
     */
    @JsonProperty
    public String getFile() {
        return file;
    }
}
