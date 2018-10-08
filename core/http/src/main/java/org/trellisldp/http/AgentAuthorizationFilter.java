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
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.http.domain.HttpConstants.SESSION_PROPERTY;
import static org.trellisldp.vocabulary.Trellis.AdministratorAgent;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.trellisldp.api.AgentService;
import org.trellisldp.http.impl.HttpSession;

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
@Priority(AUTHORIZATION - 200)
public class AgentAuthorizationFilter implements ContainerRequestFilter {

    /** A configuration key controlling which agents should be considered administrators. **/
    public static final String CONFIG_HTTP_AGENT_ADMIN_USERS = "trellis.http.agent.adminusers";

    private static final Logger LOGGER = getLogger(AgentAuthorizationFilter.class);

    private final AgentService agentService;
    private final Set<String> adminUsers;

    /**
     * Create an authorization filter.
     *
     * @param agentService the agent service
     */
    @Inject
    public AgentAuthorizationFilter(final AgentService agentService) {
        this(agentService, getConfiguredAdmins());
    }

    /**
     * Create an authorization filter.
     *
     * @param agentService the agent service
     * @param adminUsers the admin users
     */
    public AgentAuthorizationFilter(final AgentService agentService, final Set<String> adminUsers) {
        this.agentService = agentService;
        this.adminUsers = adminUsers;
    }

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        final SecurityContext sec = ctx.getSecurityContext();
        final String name = getPrincipalName(sec.getUserPrincipal());
        LOGGER.debug("Checking security context: {}", name);
        if (adminUsers.contains(name)) {
            ctx.setProperty(SESSION_PROPERTY, new HttpSession(AdministratorAgent));
        } else {
            ctx.setProperty(SESSION_PROPERTY, new HttpSession(agentService.asAgent(name)));
        }
    }

    private static String getPrincipalName(final Principal principal) {
        if (nonNull(principal) && !principal.getName().isEmpty()) {
            return principal.getName();
        }
        return null;
    }

    private static Set<String> getConfiguredAdmins() {
        final String admins = getConfiguration().getOrDefault(CONFIG_HTTP_AGENT_ADMIN_USERS, "");
        return stream(admins.split(",")).map(String::trim).collect(toSet());
    }
}
