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

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * Configuration for the JWT service.
 */
public class JwtAuthConfiguration {

    private boolean enabled = true;

    private String key;

    private String keyStore;

    @NotNull
    private String keyStorePassword = "";

    private String jwks;

    @NotNull
    private List<String> keyIds = emptyList();

    private WebIdOIDCConfiguration webIdOIDC = new WebIdOIDCConfiguration();

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
     * Get the base64-encoded JWT key.
     * @return the key
     */
    @JsonProperty
    public String getKey() {
        return key;
    }

    /**
     * Set the base64-encoded JWT key.
     * @param key the key
     */
    @JsonProperty
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * Set the keystore location.
     * @param keyStore the keystore location
     */
    @JsonProperty
    public void setKeyStore(final String keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * Get the keystore location.
     * @return the keystore location
     */
    @JsonProperty
    public String getKeyStore() {
        return keyStore;
    }

    /**
     * Set the keystore password.
     * @param keyStorePassword the password
     */
    @JsonProperty
    public void setKeyStorePassword(final String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    /**
     * Get the keystore password.
     * @return the keystore password
     */
    @JsonProperty
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * Set the key ids.
     * @param ids the key ids
     */
    @JsonProperty
    public void setKeyIds(final List<String> ids) {
        this.keyIds = ids;
    }

    /**
     * Get the key ids.
     * @return the key ids
     */
    @JsonProperty
    public List<String> getKeyIds() {
        return keyIds;
    }

    /**
     * Get the JWKS location.
     * @return the location of a JWKS document
     */
    @JsonProperty
    public String getJwks() {
        return jwks;
    }

    /**
     * Set the jwks location.
     * @param jwks the location of a JWKS document
     */
    @JsonProperty
    public void setJwks(final String jwks) {
        this.jwks = jwks;
    }

    /**
     * Get the WebId OIDC configuration.
     * @return the configuration object
     */
    @JsonProperty
    public WebIdOIDCConfiguration getWebIdOIDC() {
        return webIdOIDC;
    }

    /**
     * Set the WebId OIDC configuration.
     * @param webIdOIDC the configuration object
     */
    @JsonProperty
    public void setWebIdOIDC(final WebIdOIDCConfiguration webIdOIDC) {
        this.webIdOIDC = webIdOIDC;
    }
}
