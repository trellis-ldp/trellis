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

import static java.nio.file.Files.lines;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.SecurityContext.BASIC_AUTH;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
import static org.slf4j.LoggerFactory.getLogger;

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

import org.slf4j.Logger;

/**
 * A basic authentication filter using an Authorization HTTP header.
 */
@Priority(AUTHENTICATION)
public class BasicAuthFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = getLogger(BasicAuthFilter.class);

    public static final String CREDENTIALS_FILE = "trellis.auth.basic.credentialsfile";

    private final File file;

    /**
     * Create a basic auth filter.
     */
    @Inject
    public BasicAuthFilter() {
        this(getConfiguration().get(CREDENTIALS_FILE));
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
        this.file = file;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {

        final Boolean secure = ofNullable(requestContext.getSecurityContext()).filter(SecurityContext::isSecure)
            .isPresent();

        getCredentials(requestContext)
            .map(credentials -> authenticate(credentials).orElseThrow(() -> new NotAuthorizedException(BASIC_AUTH)))
            .ifPresent(principal -> requestContext.setSecurityContext(new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return principal;
                    }

                    @Override
                    public boolean isUserInRole(final String role) {
                        return true;
                    }

                    @Override
                    public boolean isSecure() {
                        return secure;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return BASIC_AUTH;
                    }
                }));
    }

    private Optional<Principal> authenticate(final String credentials) {
        return ofNullable(Credentials.parse(credentials)).flatMap(creds ->
            of(file).filter(File::exists).map(File::toPath).flatMap(path -> {
                try (final Stream<String> lineStream = lines(path)) {
                    return lineStream.map(String::trim).filter(line -> !line.startsWith("#"))
                        .map(line -> line.split(":", 3)).filter(x -> x.length == 3)
                        .filter(d -> d[0].trim().equals(creds.getUsername()) && d[1].trim().equals(creds.getPassword()))
                        .map(d -> d[2].trim()).findFirst().map(BasicPrincipal::new);
                } catch (final IOException ex) {
                    LOGGER.error("Error processing credentials file", ex);
                }
                return empty();
            }));
    }

    private Optional<String> getCredentials(final ContainerRequestContext ctx) {
        return ofNullable(ctx.getHeaderString(AUTHORIZATION)).map(h -> h.split(" ", 2))
            .filter(pair -> pair[0].equalsIgnoreCase(BASIC_AUTH)).filter(pair -> pair.length == 2).map(pair -> pair[1]);
    }
}
