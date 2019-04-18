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
package org.trellisldp.agent;

import static org.trellisldp.api.TrellisUtils.getInstance;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.AgentService;
import org.trellisldp.vocabulary.Trellis;

/**
 * An {@link org.trellisldp.api.AgentService} implementation that converts user strings
 * directly to {@link org.apache.commons.rdf.api.IRI}s.
 *
 * <p>A value of {@code null} passed to {@link SimpleAgentService#asAgent} will result
 * in a {@code trellis:AnonymousAgent} value.
 *
 * @author acoburn
 */
public class SimpleAgentService implements AgentService {

    private static final RDF rdf = getInstance();

    @Override
    public IRI asAgent(final String user) {
        if (user != null && !user.isEmpty()) {
            return rdf.createIRI(user);
        }
        return Trellis.AnonymousAgent;
    }
}
