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
package org.trellisldp.quarkus;

import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.slf4j.Logger;

/**
 * Web Application wrapper.
 */
@ApplicationPath("/")
@ApplicationScoped
public class TrellisApplication extends Application {

    private static final Logger LOGGER = getLogger(TrellisApplication.class);

    @PostConstruct
    void init() {
        final String name = "Trellis Database Application";
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
