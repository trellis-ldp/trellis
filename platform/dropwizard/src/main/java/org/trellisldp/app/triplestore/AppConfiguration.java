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
package org.trellisldp.app.triplestore;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

import org.trellisldp.dropwizard.config.TrellisConfiguration;

class AppConfiguration extends TrellisConfiguration {

    @NotNull
    private String mementos;

    @NotNull
    private String binaries;

    @NotNull
    private String namespaces;

    private boolean isVersioningEnabled = true;

    private int levels = 3;

    private int length =  2;

    private String resourceLocation;

    /**
     * Get the Memento configuration.
     * @return the Memento resource location
     */
    @JsonProperty
    public String getMementos() {
        return mementos;
    }

    /**
     * Set the Memento resource configuration.
     * @param config the Memento resource location
     */
    @JsonProperty
    public void setMementos(final String config) {
        this.mementos = config;
    }

    /**
     * Get whether versioning is enabled.
     * @return true if memento versioning is enabled; false otherwise
     */
    @JsonProperty
    public boolean getIsVersioningEnabled() {
        return isVersioningEnabled;
    }

    /**
     * Set whether mementos are enabled.
     * @param isVersioningEnabled whether versioning is enabled
     */
    @JsonProperty
    public void setIsVersioningEnabled(final boolean isVersioningEnabled) {
        this.isVersioningEnabled = isVersioningEnabled;
    }

    /**
     * Get the binary configuration.
     * @return the binary configuration
     */
    @JsonProperty
    public String getBinaries() {
        return binaries;
    }

    /**
     * Set the binary configuration.
     * @param config the binary configuration
     */
    @JsonProperty
    public void setBinaries(final String config) {
        this.binaries = config;
    }

    /**
     * Set the namespaces filename.
     * @param namespaces the namespaces filename
     */
    @JsonProperty
    public void setNamespaces(final String namespaces) {
        this.namespaces = namespaces;
    }

    /**
     * Get the namespace filename.
     * @return the namespace filename
     */
    @JsonProperty
    public String getNamespaces() {
        return namespaces;
    }

    /**
     * Set the RDF Connection configuration.
     * @param config the RDF Connection location
     */
    @JsonProperty
    public void setResources(final String config) {
        this.resourceLocation = config;
    }

    /**
     * Get the RDF Connection configuration.
     * @return the RDF Connection location
     */
    @JsonProperty
    public String getResources() {
        return resourceLocation;
    }

    /**
     * Set the character length of intermediate path components for internal binary resource identifiers.
     *
     * @implNote For POSIX filesystems there are performance consideration for placing many
     *           files in a single directory. Using such intermediate directories can significantly
     *           improve performance. Setting this to "2" results in a maximum of 256 subdirectories
     *           in each intermediate segment. Values between 1 and 3 are suitable for most cases.
     * @param length the character length of each hierarchy segment
     */
    @JsonProperty
    public void setBinaryHierarchyLength(final int length) {
        this.length = length;
    }

    /**
     * Get the character length of intermediate path components for internal binary resource identifiers.
     *
     * @implNote For POSIX filesystems there are performance consideration for placing many
     *           files in a single directory. Using such intermediate directories can significantly
     *           improve performance. Setting this to "2" results in a maximum of 256 subdirectories
     *           in each intermediate segment. Values between 1 and 3 are suitable for most cases.
     * @return the character length of each hierarchy segment
     */
    @JsonProperty
    public int getBinaryHierarchyLength() {
        return length;
    }

    /**
     * Set the number of levels of hierarchy for internal binary resource identifiers.
     *
     * @implNote For POSIX filesystems there are performance consideration for placing many
     *           files in a single directory. Using such intermediate directories can significantly
     *           improve performance. Values between 2 and 4 are generally suitable for most uses.
     * @param levels the number of levels of hierarchy.
     */
    @JsonProperty
    public void setBinaryHierarchyLevels(final int levels) {
        this.levels = levels;
    }

    /**
     * Get the number of levels of hierarchy for internal binary resource identifiers.
     *
     * @implNote For POSIX filesystems there are performance consideration for placing many
     *           files in a single directory. Using such intermediate directories can significantly
     *           improve performance. Values between 2 and 4 are generally suitable for most uses.
     * @return the number of levels of hierarchy.
     */
    @JsonProperty
    public int getBinaryHierarchyLevels() {
        return levels;
    }
}
