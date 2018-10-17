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

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.glassfish.jersey.test.TestProperties.CONTAINER_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class CrossOriginResourceSharingFilterAnyOriginTest extends BaseCrossOriginResourceSharingFilterTest {

    @Override
    public Application configure() {
        init();

        // Parallel tests require using random open ports
        forceSet(CONTAINER_PORT, getRandomPort().toString());

        final ResourceConfig config = new ResourceConfig();
        config.register(new TrellisHttpResource(mockBundler));
        config.register(new CrossOriginResourceSharingFilter(asList("*"),
                    asList("GET", "HEAD", "PATCH", "POST", "PUT"),
                    asList("Link", "Content-Type", "Accept", "Accept-Language", "Accept-Datetime"),
                    emptyList(), false, 0));
        return config;
    }

    @Test
    public void testGetCORS() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target(RESOURCE_PATH).request().header("Origin", origin)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(origin, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"), "Unexpected -Allow-Credentials header!");
        assertNull(res.getHeaderString("Access-Control-Max-Age"), "Unexpected -Max-Age header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"), "Unexpected -Allow-Headers header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"), "Unexpecgted -Allow-Methods header!");
    }

    @Test
    public void testGetCORSSimple() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target(RESOURCE_PATH).request().header("Origin", origin)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Accept").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(origin, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"), "Unexpected -Allow-Credentials header!");
        assertNull(res.getHeaderString("Access-Control-Max-Age"), "Unexpected -Max-Age header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"), "Unexpected -Allow-Methods header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"), "Unexpected -Allow-Headers header!");
    }

    @Test
    public void testOptionsPreflightSimple() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target(RESOURCE_PATH).request().header("Origin", origin)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Accept").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(origin, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertNull(res.getHeaderString("Access-Control-Max-Age"), "Unexpected -Max-Age header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"), "Unexpected -Allow-Credentials header!");
        assertTrue(res.getHeaderString("Access-Control-Allow-Headers").contains("accept"),
                "accept missing from -Allow-Headers!");
        assertFalse(res.getHeaderString("Access-Control-Allow-Methods").contains("POST"),
                "Unexpected POST in -Allow-Methods header!");
        assertTrue(res.getHeaderString("Access-Control-Allow-Methods").contains("PATCH"),
                "PATCH missing from -Allow-Methods header!");
    }


    @Test
    public void testCorsPreflight() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target(RESOURCE_PATH).request().header("Origin", origin)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Language, Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(origin, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"), "Unexpected -Allow-Credentials header!");
        assertNull(res.getHeaderString("Access-Control-Max-Age"), "Unexpected -Max-Age header!");

        final List<String> headers = stream(res.getHeaderString("Access-Control-Allow-Headers").split(","))
            .collect(toList());
        assertEquals(4L, headers.size(), "Incorrect -Allow-Headers count!");
        assertTrue(headers.contains("accept"), "Accept missing from -Allow-Headers!");
        assertTrue(headers.contains("link"), "Link missing from -Allow-Headers!");
        assertTrue(headers.contains("content-type"), "Content-Type missing from -Allow-Headers!");
        assertTrue(headers.contains("accept-datetime"), "Accept-Datetime missing from -Allow-Headers!");

        final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        assertEquals(4L, methods.size(), "Incorrect method count!");
        assertTrue(methods.contains("PUT"), "Missing PUT method in CORS header!");
        assertTrue(methods.contains("PATCH"), "Missing PATCH method in CORS header!");
        assertTrue(methods.contains("GET"), "Missing GET method in CORS header!");
        assertTrue(methods.contains("HEAD"), "Missing HEAD method in CORS header!");
    }

    @Test
    public void testCorsPreflightNoMatch() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target(RESOURCE_PATH).request().header("Origin", origin)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Language").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(origin, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"), "Unexpected -Allow-Credentials header!");
        assertNull(res.getHeaderString("Access-Control-Max-Age"), "Unexpected -Max-Age header!");

        assertNull(res.getHeaderString("Access-Control-Allow-Headers"), "Unexpected -Allow-Headers!");

        final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        assertEquals(4L, methods.size(), "Incorrect method count!");
        assertTrue(methods.contains("PUT"), "Missing PUT method in CORS header!");
        assertTrue(methods.contains("PATCH"), "Missing PATCH method in CORS header!");
        assertTrue(methods.contains("GET"), "Missing GET method in CORS header!");
        assertTrue(methods.contains("HEAD"), "Missing HEAD method in CORS header!");
    }
}
