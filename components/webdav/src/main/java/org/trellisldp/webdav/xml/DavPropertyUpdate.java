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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A PROPPATCH update class.
 */
@XmlRootElement(name = "propertyupdate", namespace = DAV_NAMESPACE)
public class DavPropertyUpdate {

    private DavSet set;
    private DavRemove remove;

    /**
     * Get the set element.
     * @return the set element
     */
    @XmlElement(name = "set", namespace = DAV_NAMESPACE)
    public DavSet getSet() {
        return set;
    }

    /**
     * Set the set element.
     * @param set the set element
     */
    public void setSet(final DavSet set) {
        this.set = set;
    }

    /**
     * Get the remove element.
     * @return the remove element
     */
    @XmlElement(name = "remove", namespace = DAV_NAMESPACE)
    public DavRemove getRemove() {
        return remove;
    }

    /**
     * Set the remove element.
     * @param remove the remove element
     */
    public void setRemove(final DavRemove remove) {
        this.remove = remove;
    }
}
