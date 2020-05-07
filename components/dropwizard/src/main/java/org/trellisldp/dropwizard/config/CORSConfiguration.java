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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Configuration for CORS headers.
 */
public class CORSConfiguration {

    private boolean enabled;

    private List<String> origins = singletonList("*");

    private List<String> allowMethods = asList("PUT", "DELETE", "PATCH",
            "GET", "HEAD", "OPTIONS", "POST");

    private List<String> allowHeaders = asList("Content-Type", "Link", "Accept",
            "Accept-Datetime", "Authorization", "Prefer", "Slug", "Origin");

    private List<String> exposeHeaders = asList("Content-Type", "Link",
            "Memento-Datetime", "Preference-Applied", "Location", "WWW-Authenticate",
            "Accept-Patch", "Accept-Post", "Accept-Ranges", "ETag", "Vary");

    private boolean allowCredentials = true;

    private int maxAge = 180;

    /**
     * Get whether CORS has been enabled.
     * @return true if CORS is enabled; false otherwise
     */
    @JsonProperty
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * Enable or disable CORS.
     * @param enabled true if CORS is enabled; false otherwise
     */
    @JsonProperty
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get a list of allowed origins.
     * @return the Allow-Origin values
     */
    @JsonProperty
    public List<String> getAllowOrigin() {
        return origins;
    }

    /**
     * Set the allowed origins.
     * @param origins the origins
     */
    @JsonProperty
    public void setAllowOrigin(final List<String> origins) {
        this.origins = origins;
    }

    /**
     * Get a list of allowed methods.
     * @return the Allow-Methods values
     */
    @JsonProperty
    public List<String> getAllowMethods() {
        return allowMethods;
    }

    /**
     * Set the allowed methods.
     * @param methods the methods
     */
    @JsonProperty
    public void setAllowMethods(final List<String> methods) {
        this.allowMethods = methods;
    }

    /**
     * Get a list of allowed headers.
     * @return the Allow-Headers values
     */
    @JsonProperty
    public List<String> getAllowHeaders() {
        return allowHeaders;
    }

    /**
     * Set the allowed headers.
     * @param headers the allowed headers
     */
    @JsonProperty
    public void setAllowHeaders(final List<String> headers) {
        this.allowHeaders = headers;
    }

    /**
     * Get a list of exposed headers.
     * @return the Expose-Header values
     */
    @JsonProperty
    public List<String> getExposeHeaders() {
        return exposeHeaders;
    }

    /**
     * Set the exposed headers.
     * @param headers the exposed headers
     */
    @JsonProperty
    public void setExposeHeaders(final List<String> headers) {
        this.exposeHeaders = headers;
    }

    /**
     * Get the Max-Age header.
     * @return the Max-Age header
     */
    @JsonProperty
    public int getMaxAge() {
        return maxAge;
    }

    /**
     * Set the Max-Age header.
     * @param maxAge the max age
     */
    @JsonProperty
    public void setMaxAge(final int maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Get the value of Allow-Credentials.
     * @return true if Allow-Credentials is set; false otherwise
     */
    @JsonProperty
    public boolean getAllowCredentials() {
        return allowCredentials;
    }

    /**
     * Control whether Allow-Credentials should be displayed.
     * @param allowCredentials true if Allow-Credentials should be included; false otherwise
     */
    @JsonProperty
    public void setAllowCredentials(final boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}
