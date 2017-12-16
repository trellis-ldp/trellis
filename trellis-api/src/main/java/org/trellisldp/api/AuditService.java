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

import java.util.List;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;

/**
 * @author acoburn
 */
public interface AuditService {

    /**
     * Generate the audit quads for a Create event
     * @param identifier the resource identifier
     * @param session the session data
     * @return the list of quads
     */
    List<Quad> creation(IRI identifier, Session session);

    /**
     * Generate the audit quads for a Delete event
     * @param identifier the resource identifier
     * @param session the session data
     * @return the list of quads
     */
    List<Quad> deletion(IRI identifier, Session session);

    /**
     * Generate the audit quads for an Update event
     * @param identifier the resource identifier
     * @param session the session data
     * @return the list of quads
     */
    List<Quad> update(IRI identifier, Session session);
}
