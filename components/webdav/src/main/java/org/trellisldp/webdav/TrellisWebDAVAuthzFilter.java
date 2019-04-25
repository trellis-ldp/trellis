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
package org.trellisldp.webdav;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getInstance;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.Session;
import org.trellisldp.http.WebAcFilter;

/**
 * A {@link ContainerRequestFilter} that implements WebAC-based authorization.
 *
 * @see <a href="https://github.com/solid/web-access-control-spec">SOLID WebACL Specification</a>
 *
 * @author acoburn
 */
@Provider
@Priority(AUTHORIZATION)
public class TrellisWebDAVAuthzFilter extends WebAcFilter {

    private static final RDF rdf = getInstance();
    private static final Set<String> readable = singleton("PROPFIND");
    private static final Set<String> writable = new HashSet<>(asList("PROPPATCH", "COPY", "MOVE"));
    private static final Set<String> appendable = singleton("MKCOL");

    /**
     * Create a new WebAc-based auth filter.
     *
     * @param accessService the access service
     */
    @Inject
    public TrellisWebDAVAuthzFilter(final AccessControlService accessService) {
        super(accessService);
    }

    /**
     * Create a WebAc-based auth filter.
     *
     * @param accessService the access service
     * @param challengeTypes the WWW-Authenticate challenge types
     * @param realm the authentication realm
     * @param baseUrl the baseURL
     */
    public TrellisWebDAVAuthzFilter(final AccessControlService accessService, final List<String> challengeTypes,
            final String realm, final String baseUrl) {
        super(accessService, challengeTypes, realm, baseUrl);
    }

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        final String path = ctx.getUriInfo().getPath();
        final Session s = getOrCreateSession(ctx);
        final String method = ctx.getMethod();

        final Set<IRI> modes = accessService.getAccessModes(rdf.createIRI(TRELLIS_DATA_PREFIX + path), s);
        if (readable.contains(method)) {
            verifyCanRead(modes, s, path);
        } else if (writable.contains(method)) {
            verifyCanWrite(modes, s, path);
        } else if (appendable.contains(method)) {
            verifyCanAppend(modes, s, path);
        }
    }
}
