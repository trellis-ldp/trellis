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

import static org.junit.jupiter.api.Assertions.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class SmokeIT {

    private static final Logger LOGGER = getLogger(SmokeIT.class);

    private static final String testUri = "http://localhost:" + Integer.getInteger("testServerHttpPort", 9080) + "/";

    private static final HttpClient testClient = HttpClients.createDefault();

    @Test
    private void findTrellis() throws IOException {
        final HttpResponse response = testClient.execute(new HttpGet(testUri));
        final int statusCode = response.getStatusLine().getStatusCode();
        final String statusPhrase = response.getStatusLine().getReasonPhrase();
        LOGGER.info("Got response {} for {}", statusCode, statusPhrase);
        for (final Header h : response.getAllHeaders()) {
            LOGGER.info("Header: {} {}", h.getName(), h.getValue());
        }
        assertEquals(200, statusCode);
    }

}
