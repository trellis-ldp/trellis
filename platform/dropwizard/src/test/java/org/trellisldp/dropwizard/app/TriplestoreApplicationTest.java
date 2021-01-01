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
package org.trellisldp.dropwizard.app;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import io.dropwizard.testing.DropwizardTestSupport;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

/**
 * Integration tests for Trellis.
 */
@TestInstance(PER_CLASS)
@DisabledIfEnvironmentVariable(named = "TRELLIS_TEST_TRIPLESTORE", matches = "false")
class TriplestoreApplicationTest extends AbstractApplicationTests {
    DropwizardTestSupport<AppConfiguration> getApp() {
        return new DropwizardTestSupport<>(TrellisApplication.class,
                resourceFilePath("trellis-config.yml"),
                config("notifications.type", "JMS"),
                config("notifications.connectionString", "vm://localhost"),
                config("auth.basic.usersFile", resourceFilePath("users.auth")),
                config("database.driverClass", "org.h2.Driver"),
                config("database.url", "jdbc:h2:" + resourceFilePath("app-data") + "/embeddeddb"),
                config("binaries", resourceFilePath("app-data") + "/binaries"),
                config("mementos", resourceFilePath("app-data") + "/mementos"),
                config("resources", resourceFilePath("app-data") + "/resources"),
                config("namespaces", resourceFilePath("app-data/namespaces.json")));
    }
}
