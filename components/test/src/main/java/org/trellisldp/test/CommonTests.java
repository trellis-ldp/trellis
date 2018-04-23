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
package org.trellisldp.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

/**
 * Common test interface.
 */
public interface CommonTests {

    /**
     * Get the HTTP client.
     * @return the client
     */
    Client getClient();

    /**
     * Get the base URL.
     * @return the base URL.
     */
    String getBaseURL();

    /**
     * Get a web target pointing to the base URL.
     * @return the web target
     */
    default WebTarget target() {
        return target(getBaseURL());
    }

    /**
     * Get a web target pointing to the provided URL.
     * @param url the URL
     * @return the web target
     */
    default WebTarget target(final String url) {
        return getClient().target(url);
    }
}
