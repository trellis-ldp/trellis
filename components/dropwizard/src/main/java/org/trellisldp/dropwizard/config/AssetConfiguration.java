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
import static java.util.Collections.singletonList;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Configuration for asset locations.
 */
public class AssetConfiguration {

    private String icon = "//www.trellisldp.org/assets/img/trellis.png";

    private String template;

    private List<String> css = singletonList("//www.trellisldp.org/assets/css/trellis.css");

    private List<String> js = emptyList();

    /**
     * Set an icon for the HTML view.
     * @param icon a URL for a site icon
     */
    @JsonProperty
    public void setIcon(final String icon) {
        this.icon = icon;
    }

    /**
     * Get an icon for the HTML view.
     * @return a URL for a site icon
     */
    @JsonProperty
    public String getIcon() {
        return icon;
    }

    /**
     * Set any stylesheets for use with the HTML view.
     * @param css the CSS URLs
     */
    @JsonProperty
    public void setCss(final List<String> css) {
        this.css = css;
    }

    /**
     * Get any stylesheets for use with the HTML view.
     * @return the CSS URLs
     */
    @JsonProperty
    public List<String> getCss() {
        return css;
    }

    /**
     * Set any javascript resources for use with the HTML view.
     * @param js the javascript URLs
     */
    @JsonProperty
    public void setJs(final List<String> js) {
        this.js = js;
    }

    /**
     * Get any javascript resources for use with the HTML view.
     * @return the javascript URLs
     */
    @JsonProperty
    public List<String> getJs() {
        return js;
    }

    /**
     * Set the template filename.
     * @param template the template filename
     */
    @JsonProperty
    public void setTemplate(final String template) {
        this.template = template;
    }

    /**
     * Get the template filename.
     * @return the template filename
     */
    @JsonProperty
    public String getTemplate() {
        return template;
    }
}
