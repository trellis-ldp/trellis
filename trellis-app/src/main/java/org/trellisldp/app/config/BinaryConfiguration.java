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

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Configuration for the Binary service.
 */
public class BinaryConfiguration {

    @NotEmpty
    private String path;

    private Integer levels = 4;

    private Integer length = 2;

    /**
     * Get a path value for the binary files.
     * @return the path
     */
    @JsonProperty
    public String getPath() {
        return path;
    }

    /**
     * Set a path value for the binary files.
     * @param path the path
     */
    @JsonProperty
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * Get a levels value for the binary files.
     * @return the levels of hierarchy
     */
    @JsonProperty
    public Integer getLevels() {
        return levels;
    }

    /**
     * Set a levels value for the binary files.
     * @param levels the levels of hierarchy
     */
    @JsonProperty
    public void setLevels(final Integer levels) {
        this.levels = levels;
    }

    /**
     * Get a length value for the binary files.
     * @return the length of hierarchy segments
     */
    @JsonProperty
    public Integer getLength() {
        return length;
    }

    /**
     * Set a length value for the binary files.
     * @param length the length of hierarchy segments
     */
    @JsonProperty
    public void setLength(final Integer length) {
        this.length = length;
    }
}
