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
package org.trellisldp.microprofile;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.trellisldp.auth.oauth.OAuthFilter;
import org.trellisldp.http.*;

/**
 * Web Application wrapper.
 */
@ApplicationPath("/")
@ApplicationScoped
public class WebApplication extends Application {

    private static final Logger LOGGER = getLogger(WebApplication.class);

    @Inject
    private TrellisHttpResource httpResource;

    @Inject
    private AgentAuthorizationFilter agentFilter;

    @Inject
    private TrellisHttpFilter httpFilter;

    @Inject
    private CrossOriginResourceSharingFilter corsFilter;

    @Inject
    private CacheControlFilter cacheFilter;

    @Inject
    private OAuthFilter oauthFilter;

    @PostConstruct
    private void init() {
        printBanner("Trellis Microprofile");
    }

    @Override
    public Set<Object> getSingletons() {
        return asList(httpResource, httpFilter, corsFilter, cacheFilter, agentFilter, oauthFilter).stream()
            .collect(toSet());
    }

    private void printBanner(final String name) {
        try (final InputStream resourceStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("banner.txt");
             final InputStreamReader inputStreamReader = new InputStreamReader(resourceStream);
             final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            final String banner = bufferedReader.lines().collect(joining(String.format("%n")));
            LOGGER.info("Starting {}\n{}", name, banner);
        } catch (final IllegalArgumentException | IOException ignored) {
            LOGGER.info("Starting {}", name);
        }
    }
}
