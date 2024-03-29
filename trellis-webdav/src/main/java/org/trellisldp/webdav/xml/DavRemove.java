/*
 * Copyright (c) Aaron Coburn and individual contributors
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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * A PROPPATCH update class.
 */
@XmlRootElement(name = "remove", namespace = DAV_NAMESPACE)
public class DavRemove {

    private DavProp prop;

    /**
     * Get the prop element.
     * @return the prop element
     */
    @XmlElement(name = "prop", namespace = DAV_NAMESPACE)
    public DavProp getProp() {
        return prop;
    }

    /**
     * Set the prop element.
     * @param prop the set element
     */
    public void setProp(final DavProp prop) {
        this.prop = prop;
    }
}
