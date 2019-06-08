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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static org.trellisldp.api.TrellisUtils.getInstance;

import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.interceptor.Interceptor;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;

/**
 * A no-op AccessControlService implementation.
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class NoopAccessControlService implements AccessControlService {

    private static final RDF rdf = getInstance();
    private static final String URI = "http://www.w3.org/ns/auth/acl#";
    private static final Set<IRI> modes = unmodifiableSet(asList("Control", "Read", "Write", "Append")
            .stream().map(URI::concat).map(rdf::createIRI).collect(toSet()));

    @Override
    public Set<IRI> getAccessModes(final IRI identifier, final Session session) {
        return modes;
    }
}
