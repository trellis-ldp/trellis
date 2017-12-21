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

import static org.trellisldp.api.RDFUtils.getInstance;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;

import org.trellisldp.api.AgentService;

/**
 * An {@link AgentService} implementation which applies a fixed prefix to each user.
 *
 * @author acoburn
 */
public class PrefixingAgent implements AgentService {

    private static final RDF rdf = getInstance();

    private final String prefix;

    /**
     * Create a prefixing agent service.
     *
     * @param prefix the prefix to apply to usernames
     */
    public PrefixingAgent(final String prefix) {
        this.prefix = prefix;
    }

    @Override
    public IRI asAgent(final String user) {
        return rdf.createIRI(prefix + user);
    }
}
