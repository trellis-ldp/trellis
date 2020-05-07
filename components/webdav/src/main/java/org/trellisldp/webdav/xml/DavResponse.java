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
package org.trellisldp.webdav.xml;

import static org.trellisldp.webdav.xml.DavUtils.DAV_NAMESPACE;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A PROPFIND response class.
 */
@XmlRootElement(name = "response", namespace = DAV_NAMESPACE)
public class DavResponse {
    private String href;
    private List<DavPropStat> propstats;
    private String description;

    /**
     * Get the href element.
     * @return the href value
     */
    @XmlElement(name = "href", namespace = DAV_NAMESPACE)
    public String getHref() {
        return href;
    }

    /**
     * Set the href element.
     * @param href the href value
     */
    public void setHref(final String href) {
        this.href = href;
    }

    /**
     * Get the propstat element.
     * @return the propstat element
     */
    @XmlElement(name = "propstat", namespace = DAV_NAMESPACE)
    public List<DavPropStat> getPropStats() {
        return propstats;
    }

    /**
     * Set the propstat element.
     * @param propstats the propstat element
     */
    public void setPropStats(final List<DavPropStat> propstats) {
        this.propstats = propstats;
    }

    /**
     * Get the description of the responses.
     * @return the response description
     */
    @XmlElement(name = "responsedescription", namespace = DAV_NAMESPACE)
    public String getDescription() {
        return description;
    }

    /**
     * Set the response description.
     * @param description the response description
     */
    public void setDescription(final String description) {
        this.description = description;
    }
}
