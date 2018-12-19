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
package org.trellisldp.http;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;

/**
 * @author acoburn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseCrossOriginResourceSharingFilterTest extends BaseTrellisHttpResourceTest {

    protected static final String ORIGIN = "http://example.com";

    protected void init() {
        initMocks(this);
    }

    @Override
    protected Client getClient() {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        return ClientBuilder.newClient(clientConfig);
    }

    protected Stream<Executable> checkAllowMethods(final Response res, final List<String> expected) {
        final List<String> actual = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        return Stream.concat(
                expected.stream().map(method -> () -> assertTrue(actual.contains(method),
                        "Method " + method + " not in actual -Allow-Methods header!")),
                actual.stream().map(method -> () -> assertTrue(expected.contains(method),
                        "Method " + method + " not in expected -Allow-Methods header!")));
    }

    protected Stream<Executable> checkNoCORSHeaders(final Response res) {
        return Stream.of(
                () -> assertNull(res.getHeaderString("Access-Control-Allow-Origin"), "Unexpected -Allow-Origin!"),
                () -> assertNull(res.getHeaderString("Access-Control-Allow-Credentials"),
                                 "Unexpected -Allow-Credentials!"),
                () -> assertNull(res.getHeaderString("Access-Control-Max-Age"), "Unexpected -Max-Age header!"),
                () -> assertNull(res.getHeaderString("Access-Control-Expose-Headers"), "Unexpected -Expose-Headers!"),
                () -> assertNull(res.getHeaderString("Access-Control-Allow-Headers"), "Unexpected -Allow-Headers!"),
                () -> assertNull(res.getHeaderString("Access-Control-Allow-Methods"), "Unexpected -Allow-Methods!"));
    }
}
