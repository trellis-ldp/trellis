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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Base64;

import org.slf4j.Logger;

public class Credentials {
    private static final Logger LOGGER = getLogger(Credentials.class);

    private final String username;
    private final String password;

    /**
     * Create a credentials object with username and password.
     * @param username the username
     * @param password the password
     */
    public Credentials(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Get the username value.
     * @return the username value
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password value.
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Create a set of credentials.
     * @param encoded the encoded header
     * @return credentials or null on error
     */
    public static Credentials parse(final String encoded) {
        try {
            final String decoded = new String(Base64.getDecoder().decode(encoded), UTF_8);
            final String[] parts = decoded.split(":", 2);
            if (parts.length == 2) {
                return new Credentials(parts[0], parts[1]);
            }
        } catch (final IllegalArgumentException ex) {
            LOGGER.warn("Invalid credentials provided: {}", ex.getMessage());
        }
        return null;
    }


    @Override
    public String toString() {
        return "Credentials[username=" + username + "]";
    }
}
