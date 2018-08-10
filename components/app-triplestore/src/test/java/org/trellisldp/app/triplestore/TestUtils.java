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
package org.trellisldp.app.triplestore;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

import io.dropwizard.testing.DropwizardTestSupport;

final class TestUtils {

    public static DropwizardTestSupport<AppConfiguration> buildApplication() {
        return new DropwizardTestSupport<AppConfiguration>(TrellisApplication.class,
                resourceFilePath("trellis-config.yml"),
                config("binaries", resourceFilePath("data") + "/binaries"),
                config("mementos", resourceFilePath("data") + "/mementos"),
                config("namespaces", resourceFilePath("data/namespaces.json")));
    }

    private TestUtils() {
        // prevent instantiation
    }
}
