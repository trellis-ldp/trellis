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
package org.trellisldp.http.impl;

public class GetConfiguration {

    private final boolean memento;
    private final boolean weakEtags;
    private final boolean mementoDates;
    private final String jsonLdProfile;
    private final String baseUrl;

    /**
     * Create a configuration object for the GetHandler.
     * @param memento whether this resource is a memento
     * @param weakEtags whether to use weak ETags
     * @param mementoDates whether to use memento date parameters
     * @param jsonLdProfile the default JSON-LD profile
     * @param baseUrl the configured baseURL
     */
    public GetConfiguration(final boolean memento, final boolean weakEtags, final boolean mementoDates,
            final String jsonLdProfile, final String baseUrl) {
        this.memento = memento;
        this.weakEtags = weakEtags;
        this.mementoDates = mementoDates;
        this.jsonLdProfile = jsonLdProfile;
        this.baseUrl = baseUrl;
    }

    /**
     * Get whether the resource is a Memento.
     * @return true if the resource is a Memento; false otherwise
     */
    public boolean isMemento() {
        return memento;
    }

    /**
     * Get whether to use weak ETags.
     * @return true to use weak ETags; false otherwise
     */
    public boolean useWeakEtags() {
        return weakEtags;
    }

    /**
     * Get whether to include Memento dates in header responses.
     * @return true to include Memento date parameters
     */
    public boolean includeMementoDates() {
        return mementoDates;
    }

    /**
     * Get the default JSON-LD profile.
     * @return the default JSON-LD profile, may be {@code null}
     */
    public String defaultJsonLdProfile() {
        return jsonLdProfile;
    }

    /**
     * Get the base url.
     * @return the configured base url, may be {@code null}
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}
