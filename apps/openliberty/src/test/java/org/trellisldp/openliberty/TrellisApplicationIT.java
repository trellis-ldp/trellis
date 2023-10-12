/*
 * Copyright (c) Aaron Coburn and individual contributors
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
package org.trellisldp.openliberty;

import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static java.lang.Integer.getInteger;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.TestInstance;
import org.trellisldp.test.AbstractApplicationLdpTests;

@TestInstance(PER_CLASS)
class TrellisApplicationIT extends AbstractApplicationLdpTests {

    private final Client CLIENT = newBuilder().build();
    private final String TRELLIS_URL = "http://localhost:" + getInteger("trellis.port", 9080) + "/trellis/";

    @Override
    public Client getClient() {
        return this.CLIENT;
    }

    @Override
    public String getBaseURL() {
        return this.TRELLIS_URL;
    }
}
