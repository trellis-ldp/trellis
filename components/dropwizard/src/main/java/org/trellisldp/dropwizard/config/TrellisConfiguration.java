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

    private boolean useRelativeIris;

    private String hubUrl;

    private String baseUrl;

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
     * Set the configuration for relative IRIs.
     * @param useRelativeIris whether to use relative IRIs
     */
    @JsonProperty
    public void setUseRelativeIris(final boolean useRelativeIris) {
        this.useRelativeIris = useRelativeIris;
    }

    /**
     * Get the configuration for relative IRIs.
     * @return true if using relative IRIs; false otherwise
     */
    @JsonProperty
    public boolean getUseRelativeIris() {
        return useRelativeIris;
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
}
