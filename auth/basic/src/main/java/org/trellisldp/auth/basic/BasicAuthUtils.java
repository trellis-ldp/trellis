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
package org.trellisldp.auth.basic;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.slf4j.Logger;

final class BasicAuthUtils {

    private static final Logger LOGGER = getLogger(BasicAuthUtils.class);

    public static Stream<String> uncheckedLines(final Path path) {
        try {
            return Files.lines(path);
        } catch (final IOException ex) {
            LOGGER.error("Error processing credentials file", ex);
        }
        return Stream.empty();
    }

    private BasicAuthUtils() {
        // prevent instantiation
    }
}
