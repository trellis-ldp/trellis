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
package org.trellisldp.http;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.http.core.HttpConstants.SESSION_PROPERTY;
import static org.trellisldp.vocabulary.Trellis.AdministratorAgent;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.slf4j.Logger;
import org.trellisldp.api.Session;
import org.trellisldp.http.core.HttpSession;

/**
 * A {@link ContainerRequestFilter} that converts a {@link java.security.Principal} into an
 * {@link org.apache.commons.rdf.api.IRI}-based WebID.
 *
 * <p>When no {@link java.security.Principal} is defined, an anonymous agent is used
 * ({@code http://www.trellisldp.org/ns/trellis#AnonymousAgent}).
 *
 * <p>When a {@link java.security.Principal} matches one of the elements defined in
 * {@code adminUsers}, then the WebID is set as an administrator agent
 * ({@code http://www.trellisldp.org/ns/trellis#AdministratorAgent}).
 *
 * @author acoburn
 */
@Provider
@Priority(AUTHORIZATION - 200)
public class AgentAuthorizationFilter implements ContainerRequestFilter {

    /** A configuration key controlling which agents should be considered administrators. **/
    public static final String CONFIG_HTTP_AGENT_ADMIN_USERS = "trellis.http.agent.adminusers";

    private static final RDF rdf = getInstance();
    private static final Logger LOGGER = getLogger(AgentAuthorizationFilter.class);

    private final Set<String> adminUsers;

    /**
     * Create an authorization filter.
     */
    @Inject
    public AgentAuthorizationFilter() {
        this(getConfiguredAdmins());
    }

    /**
     * Create an authorization filter.
     *
     * @param adminUsers the admin users
     */
    public AgentAuthorizationFilter(final Set<String> adminUsers) {
        this.adminUsers = adminUsers;
    }

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        ctx.setProperty(SESSION_PROPERTY, getSession(ctx.getSecurityContext().getUserPrincipal()));
    }

    private Session getSession(final Principal principal) {
        final String name = getPrincipalName(principal);
        if (name != null) {
            final IRI webid = rdf.createIRI(name);
            if (adminUsers.contains(name)) {
                LOGGER.info("{} acting as administrator user", name);
                return new HttpSession(AdministratorAgent, webid);
            // don't permit admin agent to be generated from the authn layer
            } else if (!AdministratorAgent.equals(webid)) {
                return new HttpSession(webid);
            }
        }
        // Default to anonymous session
        return new HttpSession();
    }

    private String getPrincipalName(final Principal principal) {
        if (principal != null) {
            final String name = principal.getName();
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
        }
        return null;
    }

    private static Set<String> getConfiguredAdmins() {
        final String admins = getConfig().getOptionalValue(CONFIG_HTTP_AGENT_ADMIN_USERS, String.class).orElse("");
        return stream(admins.split(",")).map(String::trim).collect(toSet());
    }
}
