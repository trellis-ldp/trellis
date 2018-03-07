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

import static java.util.Objects.isNull;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.http.domain.HttpConstants.SESSION_PROPERTY;
import static org.trellisldp.vocabulary.Trellis.AdministratorAgent;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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

    private static final Logger LOGGER = getLogger(AgentAuthorizationFilter.class);

    private final AgentService agentService;
    private final Set<String> adminUsers = new HashSet<>();

    /**
     * Create an authorization filter.
     *
     * @param agentService the agent service
     */
    @Inject
    public AgentAuthorizationFilter(final AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Set any admin users for the auth filter.
     *
     * @param adminUsers users that should be treated as server administrators
     */
    public void setAdminUsers(final List<String> adminUsers) {
        adminUsers.forEach(this.adminUsers::add);
    }

    @Override
    public void filter(final ContainerRequestContext ctx) throws IOException {
        final SecurityContext sec = ctx.getSecurityContext();
        LOGGER.debug("Checking security context: {}", sec.getUserPrincipal());
        if (isNull(sec.getUserPrincipal())) {
            ctx.setProperty(SESSION_PROPERTY, new HttpSession());
        } else if (adminUsers.contains(sec.getUserPrincipal().getName())) {
            ctx.setProperty(SESSION_PROPERTY, new HttpSession(AdministratorAgent));
        } else if (sec.getUserPrincipal().getName().isEmpty()) {
            ctx.setProperty(SESSION_PROPERTY, new HttpSession());
        } else {
            ctx.setProperty(SESSION_PROPERTY, new HttpSession(agentService.asAgent(sec.getUserPrincipal().getName())));
        }
    }
}
