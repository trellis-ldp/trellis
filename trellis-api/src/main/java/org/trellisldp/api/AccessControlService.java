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
package org.trellisldp.api;

import java.util.Set;

import org.apache.commons.rdf.api.IRI;

/**
 * The AccessControlService provides methods for checking user access to given resources
 * as well as services for finding the applicable access control resource.
 *
 * @see <a href="https://www.w3.org/wiki/WebAccessControl">W3C WebAccessControl</a>
 * and <a href="https://github.com/solid/web-access-control-spec">Solid WebAC specification</a>
 *
 * @author acoburn
 */
public interface AccessControlService {

    /**
     * Get the allowable access modes for the given session
     * to the specified resource.
     * @param identifier the resource identifier
     * @param session the agent's session
     * @return a set of allowable access modes
     */
    Set<IRI> getAccessModes(IRI identifier, Session session);
}
