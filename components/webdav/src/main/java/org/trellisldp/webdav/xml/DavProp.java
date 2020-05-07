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

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Element;

/**
 * A PROPFIND prop class.
 */
@XmlRootElement(name = "prop", namespace = DAV_NAMESPACE)
public class DavProp {
    private List<Element> nodes;

    /**
     * Get the the prop element from a propstat response.
     * @return the property element
     */
    @XmlAnyElement
    public List<Element> getNodes() {
        return nodes;
    }

    /**
     * Set the property elements.
     * @param nodes the child nodes
     */
    public void setNodes(final List<Element> nodes) {
        this.nodes = nodes;
    }
}
