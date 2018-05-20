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

import static java.util.Collections.synchronizedMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

/**
 * @author acoburn
 */
public class TrellisConfiguration extends Configuration {

    @NotNull
    private String defaultName = "Trellis";

    @NotNull
    private AuthConfiguration auth = new AuthConfiguration();

    @NotNull
    private CacheConfiguration cache = new CacheConfiguration();

    @NotNull
    private AssetConfiguration assets = new AssetConfiguration();

    @NotNull
    private CORSConfiguration cors = new CORSConfiguration();

    @NotNull
    private JsonLdConfiguration jsonld = new JsonLdConfiguration();

    @NotNull
    private NotificationsConfiguration notifications = new NotificationsConfiguration();

    @NotNull
    private String mementos;

    @NotNull
    private String binaries;

    @NotNull
    private String namespaces;

    @NotNull
    private Integer levels = 3;

    @NotNull
    private Integer length =  2;

    private String hubUrl = null;

    private String baseUrl = null;

    private ResourceConfiguration resources = new ResourceConfiguration();

    private final Map<String, Object> extras = synchronizedMap(new HashMap<>());

    /**
     * Get the base URL.
     * @return the baseURL
     */
    @JsonProperty
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Set the base URL.
     * @param baseUrl the baseURL
     */
    @JsonProperty
    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Get the websub hub URL.
     * @return the websub hub URL
     */
    @JsonProperty
    public String getHubUrl() {
        return hubUrl;
    }

    /**
     * Set the websub hub URL.
     * @param hubUrl the hub URL
     */
    @JsonProperty
    public void setHubUrl(final String hubUrl) {
        this.hubUrl = hubUrl;
    }

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
     * Set the asset configuration.
     * @param assets the asset config
     */
    @JsonProperty
    public void setAssets(final AssetConfiguration assets) {
        this.assets = assets;
    }

    /**
     * Get the asset configuration.
     * @return the asset config
     */
    @JsonProperty
    public AssetConfiguration getAssets() {
        return assets;
    }

    /**
     * Get the application name.
     * @return the name
     */
    @JsonProperty
    public String getDefaultName() {
        return defaultName;
    }

    /**
     * Set the application name.
     * @param name the name
     */
    @JsonProperty
    public void setDefaultName(final String name) {
        this.defaultName = name;
    }

    /**
     * Set the RDF Connection configuration.
     * @param config the RDF Connection location
     */
    @JsonProperty
    public void setResources(final ResourceConfiguration config) {
        this.resources = config;
    }

    /**
     * Get the RDF Connection configuration.
     * @return the RDF Connection location
     */
    @JsonProperty
    public ResourceConfiguration getResources() {
        return resources;
    }

    /**
     * Set an extra configuration value.
     * @param name the name of this config value
     * @param value the value to set
     * @return this config for chaining
     */
    @JsonAnySetter
    public TrellisConfiguration setAdditionalConfig(final String name, final Object value) {
        extras.put(name, value);
        return this;
    }

    /**
     * Get any extra metadata.
     * @return a {@link Map} of any extra metadata
     */
    @JsonAnyGetter
    public Map<String, Object> any() {
        return extras;
    }

    /**
     * Set the cache configuration.
     * @param cache the cache configuration
     */
    @JsonProperty
    public void setCache(final CacheConfiguration cache) {
        this.cache = cache;
    }

    /**
     * Get the cache configuration.
     * @return the cache configuration
     */
    @JsonProperty
    public CacheConfiguration getCache() {
        return cache;
    }

    /**
     * Set the CORS configuration.
     * @param cors the CORS configuration
     */
    @JsonProperty
    public void setCors(final CORSConfiguration cors) {
        this.cors = cors;
    }

    /**
     * Get the CORS configuration.
     * @return the CORS configuration
     */
    @JsonProperty
    public CORSConfiguration getCors() {
        return cors;
    }

    /**
     * Set the Auth configuration.
     * @param auth the Auth configuration
     */
    @JsonProperty
    public void setAuth(final AuthConfiguration auth) {
        this.auth = auth;
    }

    /**
     * Get the Auth configuration.
     * @return the Auth configuration
     */
    @JsonProperty
    public AuthConfiguration getAuth() {
        return auth;
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
     * Set the json-ld configuration.
     * @param jsonld the jsond-ld configuration
     */
    @JsonProperty
    public void setJsonld(final JsonLdConfiguration jsonld) {
        this.jsonld = jsonld;
    }

    /**
     * Get the namespace filename.
     * @return the json-ld configuration
     */
    @JsonProperty
    public JsonLdConfiguration getJsonld() {
        return jsonld;
    }

    /**
     * Set the notifications configuration.
     * @param notifications the notifications configuration
     */
    @JsonProperty
    public void setNotifications(final NotificationsConfiguration notifications) {
        this.notifications = notifications;
    }

    /**
     * Get the notifications configuration.
     * @return the notifications configuration
     */
    @JsonProperty
    public NotificationsConfiguration getNotifications() {
        return notifications;
    }

    /**
     * Set the character length of intermediate path components for internal binary resource identifiers.
     *
     * <p>Note: for POSIX filesystems there are performance consideration for placing many
     * files in a single directory. Using such intermediate directories can significantly improve
     * performance. Setting this to "2" results in a maximum of 256 subdirectories in each intermediate segment.
     * Values between 1 and 3 are suitable for most cases.
     *
     * @param length the character length of each hierarchy segment
     */
    @JsonProperty
    public void setBinaryHierarchyLength(final Integer length) {
        this.length = length;
    }

    /**
     * Get the character length of intermediate path components for internal binary resource identifiers.
     *
     * <p>Note: for POSIX filesystems there are performance consideration for placing many
     * files in a single directory. Using such intermediate directories can significantly improve
     * performance. Setting this to "2" results in a maximum of 256 subdirectories in each intermediate segment.
     * Values between 1 and 3 are suitable for most cases.
     *
     * @return the character length of each hierarchy segment
     */
    @JsonProperty
    public Integer getBinaryHierarchyLength() {
        return length;
    }

    /**
     * Set the number of levels of hierarchy for internal binary resource identifiers.
     *
     * <p>Note: for POSIX filesystems there are performance consideration for placing many
     * files in a single directory. Using such intermediate directories can significantly improve
     * performance. Values between 2 and 4 are generally suitable for most uses.
     *
     * @param levels the number of levels of hierarchy.
     */
    @JsonProperty
    public void setBinaryHierarchyLevels(final Integer levels) {
        this.levels = levels;
    }

    /**
     * Get the number of levels of hierarchy for internal binary resource identifiers.
     *
     * <p>Note: for POSIX filesystems there are performance consideration for placing many
     * files in a single directory. Using such intermediate directories can significantly improve
     * performance. Values between 2 and 4 are generally suitable for most uses.
     *
     * @return the number of levels of hierarchy.
     */
    @JsonProperty
    public Integer getBinaryHierarchyLevels() {
        return levels;
    }
}
