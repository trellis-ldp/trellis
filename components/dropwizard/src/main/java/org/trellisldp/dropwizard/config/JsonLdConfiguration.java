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

import static java.util.Collections.emptySet;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

import javax.validation.constraints.NotNull;

/**
 * A configuration to control how custom JSON-LD profiles are handled.
 */
public class JsonLdConfiguration {

    @NotNull
    private Set<String> whitelist = emptySet();

    @NotNull
    private Set<String> whitelistDomains = emptySet();

    private long profileCacheSize = 100L;

    private long profileCacheExpireHours = 24L;

    /**
     * Get the whitelist of custom JSON-LD profiles.
     * @return the json-ld profile whitelist
     */
    @JsonProperty
    public Set<String> getContextWhitelist() {
        return whitelist;
    }

    /**
     * Set the whitelist of custom JSON-LD profiles.
     * @param whitelist the json-ld profile witelist
     */
    @JsonProperty
    public void setContextWhitelist(final Set<String> whitelist) {
        this.whitelist = whitelist;
    }

    /**
     * Get the domain whitelist of custom JSON-LD profiles.
     * @return the json-ld profile domain whitelist
     */
    @JsonProperty
    public Set<String> getContextDomainWhitelist() {
        return whitelistDomains;
    }

    /**
     * Set the domain whitelist of custom JSON-LD profiles.
     * @param whitelistDomains the json-ld domain profile witelist
     */
    @JsonProperty
    public void setContextDomainWhitelist(final Set<String> whitelistDomains) {
        this.whitelistDomains = whitelistDomains;
    }

    /**
     * Get the JSON-LD profile cache expire time in hours (default=24).
     * @return the json-ld profile cache expire time in hours
     */
    @JsonProperty
    public long getCacheExpireHours() {
        return profileCacheExpireHours;
    }

    /**
     * Set the JSON-LD profile cache exire time in hours.
     * @param profileCacheExpireHours the json-ld profile cache expire time in hours.
     */
    @JsonProperty
    public void setCacheExpireHours(final long profileCacheExpireHours) {
        this.profileCacheExpireHours = profileCacheExpireHours;
    }

    /**
     * Get the JSON-LD profile cache size (default=100).
     * @return the json-ld profile cache size
     */
    @JsonProperty
    public long getCacheSize() {
        return profileCacheSize;
    }

    /**
     * Set the JSON-LD profile cache size.
     * @param profileCacheSize the size of the json-ld profile cache
     */
    @JsonProperty
    public void setCacheSize(final long profileCacheSize) {
        this.profileCacheSize = profileCacheSize;
    }
}
