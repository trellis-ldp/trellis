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
package org.trellisldp.auth.basic;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.SecurityContext.BASIC_AUTH;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

/**
 * A basic authentication filter using an Authorization HTTP header.
 */
@Priority(AUTHENTICATION)
public class BasicAuthFilter implements ContainerRequestFilter {

    /** The configuration key controlling the location of the basic auth credentials file. **/
    public static final String CONFIG_AUTH_BASIC_CREDENTIALS = "trellis.auth.basic.credentials";

    /** The configuration key controlling the realm used in a WWW-Authenticate header, or 'trellis' by default. **/
    public static final String CONFIG_AUTH_REALM = "trellis.auth.realm";

    private final File file;
    private final String challenge;

    /**
     * Create a basic auth filter.
     */
    @Inject
    public BasicAuthFilter() {
        this(getConfig().getValue(CONFIG_AUTH_BASIC_CREDENTIALS, String.class));
    }

    /**
     * Create a basic auth filter.
     * @param credentialsFile a credentials file
     */
    public BasicAuthFilter(final String credentialsFile) {
        this(new File(credentialsFile));
    }

    /**
     * Create a basic auth filter.
     * @param file the credentials file
     */
    public BasicAuthFilter(final File file) {
        this(file, getConfig().getOptionalValue(CONFIG_AUTH_REALM, String.class).orElse("trellis"));
    }

    /**
     * Create a basic auth filter.
     * @param file the credentials file
     * @param realm the authentication realm
     */
    public BasicAuthFilter(final File file, final String realm) {
        this.file = file;
        this.challenge = "Basic realm=\"" + realm + "\"";
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {

        final boolean secure = ofNullable(requestContext.getSecurityContext()).filter(SecurityContext::isSecure)
            .isPresent();

        getCredentials(requestContext)
                        .map(credentials -> authenticate(credentials)
                                        .<RuntimeException>orElseThrow(() -> new NotAuthorizedException(challenge)))
                        .ifPresent(principal -> requestContext
                                        .setSecurityContext(new BasiAuthSecurityContext(principal, secure)));
    }

    private Optional<Principal> authenticate(final String credentials) {
        return ofNullable(Credentials.parse(credentials)).flatMap(creds ->
            of(file).filter(File::exists).map(File::toPath).flatMap(path -> {
                try (final Stream<String> lineStream = BasicAuthUtils.uncheckedLines(path)) {
                    return lineStream.map(String::trim).filter(line -> !line.startsWith("#"))
                        .map(line -> line.split(":", 3)).filter(x -> x.length == 3)
                        .filter(d -> d[0].trim().equals(creds.getUsername()) && d[1].trim().equals(creds.getPassword()))
                        .map(d -> d[2].trim()).findFirst().map(BasicPrincipal::new);
                }
            }));
    }

    private Optional<String> getCredentials(final ContainerRequestContext ctx) {
        return ofNullable(ctx.getHeaderString(AUTHORIZATION)).map(h -> h.split(" ", 2))
            .filter(pair -> pair[0].equalsIgnoreCase(BASIC_AUTH)).filter(pair -> pair.length == 2).map(pair -> pair[1]);
    }

    private static final class BasiAuthSecurityContext implements SecurityContext {
        private final Principal principal;
        private final boolean secure;

        private BasiAuthSecurityContext(final Principal principal, final boolean secure) {
            this.principal = principal;
            this.secure = secure;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isSecure() {
            return secure;
        }

        @Override
        public String getAuthenticationScheme() {
            return BASIC_AUTH;
        }

        @Override
        public boolean isUserInRole(final String role) {
            return true;
        }
    }
}
