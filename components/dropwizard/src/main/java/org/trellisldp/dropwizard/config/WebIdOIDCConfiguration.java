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
 * Configuration for the WebId OIDC authenticator.
 */
public class WebIdOIDCConfiguration {
    private boolean enabled = false;
    private int cacheSize = 50;
    private int cacheExpireDays = 30;

    /**
     * Get whether WebId OIDC support is enabled.
     *
     * @return true if it is enabled
     */
    @JsonProperty
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * Enable or disable the WebId OIDC support.
     *
     * @param enabled true if it should be enabled
     */
    @JsonProperty
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get maximum size of the provider key configuration cache.
     *
     * @return the size of the cache
     */
    @JsonProperty
    public int getCacheSize() {
        return cacheSize;
    }

    /**
     * Set the size of the provider key configuration cache.
     *
     * @param cacheSize the size of the cache
     */
    @JsonProperty
    public void setCacheSize(final int cacheSize) {
        this.cacheSize = cacheSize;
    }

    /**
     * Get the cache expire time in days.
     *
     * @return the number of days after which an element expires
     */
    @JsonProperty
    public int getCacheExpireDays() {
        return cacheExpireDays;
    }

    /**
     * Set the cache expire time in days.
     *
     * @param cacheExpireDays the number of days after which an element expires
     */
    @JsonProperty
    public void setCacheExpireDays(final int cacheExpireDays) {
        this.cacheExpireDays = cacheExpireDays;
    }
}
