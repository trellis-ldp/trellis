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

import java.util.List;

/**
 * Configuration for the JWT service.
 */
public class JwtAuthConfiguration {

    private Boolean enabled = true;

    private Boolean isEncoded = false;

    private String key;

    private String keyStore;

    private String keyStorePassword;

    private List<String> keyIds;

    /**
     * Get whether basic authentication has been enabled.
     * @return true if basic auth is enabled; false otherwise
     */
    @JsonProperty
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Enable or disable basic authentication.
     * @param enabled true if basic auth is enabled; false otherwise
     */
    @JsonProperty
    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the JWT key.
     * @return the key
     */
    @JsonProperty
    public String getKey() {
        return key;
    }

    /**
     * Set the JWT key.
     * @param key the key
     */
    @JsonProperty
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * Get whether the key is base64 encoded.
     * @return true if the key is base64 encoded; false otherwise
     */
    @JsonProperty
    public Boolean getBase64Encoded() {
        return isEncoded;
    }

    /**
     * Set whether the key is base64 encoded.
     * @param isEncoded true if the key is base64 encoded; false otherwise
     */
    @JsonProperty
    public void setBase64Encoded(final Boolean isEncoded) {
        this.isEncoded = isEncoded;
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
}
