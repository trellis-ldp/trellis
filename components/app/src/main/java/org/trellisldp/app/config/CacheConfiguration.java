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
 * Cache-related configuration.
 */
public class CacheConfiguration {

    private Integer maxAge = 86400;

    private Boolean mustRevalidate = true;

    private Boolean noCache = false;

    /**
     * Set the cache max-age value.
     * @param maxAge the cache max age header value
     */
    @JsonProperty
    public void setMaxAge(final Integer maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Get the value of the cache max age.
     * @return the cache max age header value
     */
    @JsonProperty
    public Integer getMaxAge() {
        return maxAge;
    }

    /**
     * Set the cache must-revalidate value.
     * @param mustRevalidate the cache must-revalidate header value
     */
    @JsonProperty
    public void setMustRevalidate(final Boolean mustRevalidate) {
        this.mustRevalidate = mustRevalidate;
    }

    /**
     * Get the value of the must-revalidate value.
     * @return the must-revalidate value
     */
    @JsonProperty
    public Boolean getMustRevalidate() {
        return mustRevalidate;
    }

    /**
     * Set the no-cache value.
     * @param noCache the no-cache header value
     */
    @JsonProperty
    public void setNoCache(final Boolean noCache) {
        this.noCache = noCache;
    }

    /**
     * Get the value of the no-cache value.
     * @return the no-cache value
     */
    @JsonProperty
    public Boolean getNoCache() {
        return noCache;
    }
}
