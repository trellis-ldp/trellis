/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
import java.io.UncheckedIOException;

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
        final String banner = readResource(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(requireNonNull(resource, "resource cannot be null!")));
        LOGGER.info("Starting {}\n{}", name, banner);
    }

    static String readResource(final InputStream resourceStream) {
        if (resourceStream != null) {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(resourceStream, UTF_8);
                final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                return bufferedReader.lines().collect(joining(String.format("%n")));
            } catch (final IOException | UncheckedIOException ex) {
                LOGGER.error("Error reading banner resource: {}", ex.getMessage());
            }
        }
        return "";
    }

    private AppUtils() {
        // prevent instatiation
    }
}
