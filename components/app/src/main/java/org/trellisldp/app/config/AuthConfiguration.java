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

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * Configuration for authN/authZ.
 */
public class AuthConfiguration {
    private JwtAuthConfiguration jwt = new JwtAuthConfiguration();
    private BasicAuthConfiguration basic = new BasicAuthConfiguration();
    private WebacConfiguration webac = new WebacConfiguration();
    private boolean hideForbiddenResources = false;

    @NotNull
    private String realm = "trellis";

    @NotNull
    private List<String> adminUsers = new ArrayList<>();

    /**
     * Set the admin users.
     * @param adminUsers the admin users
     */
    @JsonProperty
    public void setAdminUsers(final List<String> adminUsers) {
        this.adminUsers = adminUsers;
    }

    /**
     * Get the admin users.
     * @return the admin users
     */
    @JsonProperty
    public List<String> getAdminUsers() {
        return adminUsers;
    }

    /**
     * Set the basic auth configuration.
     * @param basic the basic auth config
     */
    @JsonProperty
    public void setBasic(final BasicAuthConfiguration basic) {
        this.basic = basic;
    }

    /**
     * Get the basic auth configuration.
     * @return the basic auth config
     */
    @JsonProperty
    public BasicAuthConfiguration getBasic() {
        return basic;
    }

    /**
     * Set the jwt auth configuration.
     * @param jwt the jwt auth config
     */
    @JsonProperty
    public void setJwt(final JwtAuthConfiguration jwt) {
        this.jwt = jwt;
    }

    /**
     * Get the jwt auth configuration.
     * @return the jwt auth config
     */
    @JsonProperty
    public JwtAuthConfiguration getJwt() {
        return jwt;
    }

    /**
     * Set the webac auth configuration.
     * @param webac the webac auth config
     */
    @JsonProperty
    public void setWebac(final WebacConfiguration webac) {
        this.webac = webac;
    }

    /**
     * Get the webac auth configuration.
     * @return the webac auth config
     */
    @JsonProperty
    public WebacConfiguration getWebac() {
        return webac;
    }

    /**
     * Get the security realm.
     * @return the realm; by default, this is 'trellis'
     */
    @JsonProperty
    public String getRealm() {
        return realm;
    }

    /**
     * Set the security realm.
     * @param realm the security realm
     */
    @JsonProperty
    public void setRealm(final String realm) {
        this.realm = realm;
    }

    /**
     * Get whether to hide forbidden resources.
     * @return whether to hide forbidden resources; by default, this is false
     */
    @JsonProperty
    public boolean getHideForbiddenResources() {
        return hideForbiddenResources;
    }

    /**
     * Set whether to hide forbidden resources.
     * @param hideForbiddenResources true to hide forbidden resources; false otherwise
     */
    @JsonProperty
    public void setHideForbiddenResources(final boolean hideForbiddenResources) {
        this.hideForbiddenResources = hideForbiddenResources;
    }
}
