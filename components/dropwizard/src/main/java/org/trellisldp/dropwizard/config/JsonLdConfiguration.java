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
    private Set<String> allowed = emptySet();

    @NotNull
    private Set<String> allowedDomains = emptySet();

    private long profileCacheSize = 100L;

    private long profileCacheExpireHours = 24L;

    /**
     * Get the list of allowed custom JSON-LD profiles.
     * @return the json-ld profile allow list
     * @deprecated Please use {@link #getAllowedContexts}
     */
    @JsonProperty
    @Deprecated
    public Set<String> getContextWhitelist() {
        return getAllowedContexts();
    }

    /**
     * Set the list of allowed custom JSON-LD profiles.
     * @param allowed the allowed json-ld profiles
     * @deprecated Please use {@link #setAllowedContexts}
     */
    @JsonProperty
    @Deprecated
    public void setContextWhitelist(final Set<String> allowed) {
        setAllowedContexts(allowed);
    }

    /**
     * Get the allowed domains for custom JSON-LD profiles.
     * @return the json-ld profile domain list
     * @deprecated Please use {@link #getAllowedContextDomains}
     */
    @JsonProperty
    @Deprecated
    public Set<String> getContextDomainWhitelist() {
        return getAllowedContextDomains();
    }

    /**
     * Set the allowed domains for custom JSON-LD profiles.
     * @param allowedDomains the json-ld domain profile witelist
     * @deprecated Please use {@link #setAllowedContextDomains}
     */
    @JsonProperty
    @Deprecated
    public void setContextDomainWhitelist(final Set<String> allowedDomains) {
        setAllowedContextDomains(allowedDomains);
    }

    /**
     * Get the list of allowed custom JSON-LD profiles.
     * @return the json-ld profile allow list
     */
    @JsonProperty
    public Set<String> getAllowedContexts() {
        return allowed;
    }

    /**
     * Set the list of allowed custom JSON-LD profiles.
     * @param allowed the allowed json-ld profiles
     */
    @JsonProperty
    public void setAllowedContexts(final Set<String> allowed) {
        this.allowed = allowed;
    }

    /**
     * Get the allowed domains for custom JSON-LD profiles.
     * @return the json-ld profile domain list
     */
    @JsonProperty
    public Set<String> getAllowedContextDomains() {
        return allowedDomains;
    }

    /**
     * Set the allowed domains for custom JSON-LD profiles.
     * @param allowedDomains the json-ld domain profile witelist
     */
    @JsonProperty
    public void setAllowedContextDomains(final Set<String> allowedDomains) {
        this.allowedDomains = allowedDomains;
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
