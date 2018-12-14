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

/**
 * @author acoburn
 */
public class WebacConfiguration {

    private boolean enabled = true;
    private Long cacheSize = 1000L;
    private Long cacheExpireSeconds = 600L;

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
     * Get the maximum size of the cache.
     * @return the maximum size of the cache (default=1000)
     */
    @JsonProperty
    public Long getCacheSize() {
        return cacheSize;
    }

    /**
     * Set the maxiumum size of the cache.
     * @param cacheSize the size of the cache
     */
    @JsonProperty
    public void setCacheSize(final Long cacheSize) {
        this.cacheSize = cacheSize;
    }

    /**
     * Get the cache expire time in seconds.
     * @return the number of seconds after which an element expires (default=600)
     */
    @JsonProperty
    public Long getCacheExpireSeconds() {
        return cacheExpireSeconds;
    }

    /**
     * Set the cache exprie time in seconds.
     * @param cacheExpireSeconds the number of seconds after which an element expires
     */
    @JsonProperty
    public void setCacheExpireSeconds(final Long cacheExpireSeconds) {
        this.cacheExpireSeconds = cacheExpireSeconds;
    }
}
