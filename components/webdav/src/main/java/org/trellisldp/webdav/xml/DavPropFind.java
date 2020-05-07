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
 * A PROPFIND query class.
 */
@XmlRootElement(name = "propfind", namespace = DAV_NAMESPACE)
public class DavPropFind {

    private DavPropName propname;
    private DavAllProp allprop;
    private DavProp prop;

    /**
     * Get the allprop element.
     * @return the allprop element
     */
    @XmlElement(name = "allprop", namespace = DAV_NAMESPACE)
    public DavAllProp getAllProp() {
        return allprop;
    }

    /**
     * Set the allprop element.
     * @param allprop the allprop element
     */
    public void setAllProp(final DavAllProp allprop) {
        this.allprop = allprop;
    }

    /**
     * Get the propname element.
     * @return the propname element
     */
    @XmlElement(name = "propname", namespace = DAV_NAMESPACE)
    public DavPropName getPropName() {
        return propname;
    }

    /**
     * Set the propname element.
     * @param propname the propname
     */
    public void setPropName(final DavPropName propname) {
        this.propname = propname;
    }

    /**
     * Get the propname element.
     * @return the propname element
     */
    @XmlElement(name = "prop", namespace = DAV_NAMESPACE)
    public DavProp getProp() {
        return prop;
    }

    /**
     * Set the propelement.
     * @param prop the prop
     */
    public void setProp(final DavProp prop) {
        this.prop = prop;
    }
}
