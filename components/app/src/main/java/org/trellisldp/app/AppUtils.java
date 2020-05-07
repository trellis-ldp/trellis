/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
 *
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
package org.trellisldp.app;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;

public final class AppUtils {

    private static final Logger LOGGER = getLogger(AppUtils.class);

    /**
     * Print a banner to the logging system.
     *
     * @param name the application name
     * @param resource the classpath resource to use
     */
    public static void printBanner(final String name, final String resource) {
        final String banner = readResource(requireNonNull(resource, "resource cannot be null!"));
        LOGGER.info("Starting {}\n{}", name, banner);
    }

    private static String readResource(final String resource) {
        try (final InputStream resourceStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resource)) {
            if (resourceStream != null) {
                return readResource(resourceStream);
            }
            LOGGER.warn("Banner resource {} could not be read", resource);
        } catch (final IOException ex) {
            LOGGER.error("Error reading banner resource at {}: {}", resource, ex.getMessage());
        }
        return "";
    }

    private static String readResource(final InputStream resourceStream) throws IOException {
        try (final InputStreamReader inputStreamReader = new InputStreamReader(resourceStream, UTF_8);
            final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            return bufferedReader.lines().collect(joining(String.format("%n")));
        }
    }

    private AppUtils() {
        // prevent instatiation
    }
}
