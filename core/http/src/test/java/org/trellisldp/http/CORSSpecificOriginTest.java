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
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
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
public class CORSSpecificOriginTest extends BaseCORSTest {

    @Override
    public Application configure() {
        init();

        final ResourceConfig config = new ResourceConfig();
        config.register(new LdpResource(mockBundler));
        config.register(new CrossOriginResourceSharingFilter(asList(ORIGIN),
                    asList("GET", "HEAD", "PATCH", "POST", "PUT"),
                    asList("Link", "Content-Type", "Accept", "Accept-Language", "Accept-Patch"),
                    asList("Accept-Patch"), true, 180));
        return config;
    }

    @Test
    public void testGetCORSInvalid() {
        final String baseUri = getBaseUri().toString();
        final String origin = baseUri.substring(0, baseUri.length() - 1);
        final Response res = target(RESOURCE_PATH).request().header("Origin", origin)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").get();

        assertEquals(SC_OK, res.getStatus());
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
    }

    @Test
    public void testGetCORS() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").get();

        assertEquals(SC_OK, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertTrue(stream(res.getHeaderString("Access-Control-Expose-Headers").split(","))
                .anyMatch("Accept-Patch"::equalsIgnoreCase));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

    @Test
    public void testGetCORSSimple() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Accept").get();

        assertEquals(SC_OK, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertTrue(stream(res.getHeaderString("Access-Control-Expose-Headers").split(","))
                .anyMatch("Accept-Patch"::equalsIgnoreCase));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
    }

    @Test
    public void testOptionsPreflightSimple() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Accept").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("180", res.getHeaderString("Access-Control-Max-Age"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
        assertTrue(res.getHeaderString("Access-Control-Allow-Headers").contains("accept"));
        assertFalse(res.getHeaderString("Access-Control-Allow-Methods").contains("POST"));
        assertTrue(res.getHeaderString("Access-Control-Allow-Methods").contains("PATCH"));
    }

    @Test
    public void testCorsPreflight() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Language, Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertEquals("180", res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));

        final List<String> headers = stream(res.getHeaderString("Access-Control-Allow-Headers").split(","))
            .collect(toList());
        assertEquals(4L, headers.size());
        assertTrue(headers.contains("accept"));
        assertTrue(headers.contains("link"));
        assertTrue(headers.contains("content-type"));
        assertTrue(headers.contains("accept-patch"));

        final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        assertEquals(4L, methods.size());
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("HEAD"));
        assertTrue(methods.contains("PUT"));
        assertTrue(methods.contains("PATCH"));
    }

    @Test
    public void testCorsPreflightNoRequestHeaders() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertEquals("180", res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));

        final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        assertEquals(4L, methods.size());
        assertTrue(methods.contains("PUT"));
        assertTrue(methods.contains("PATCH"));
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("HEAD"));
    }

    @Test
    public void testCorsPreflightNoMatch() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Language").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("180", res.getHeaderString("Access-Control-Max-Age"));
        assertEquals("true", res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));

        final List<String> methods = stream(res.getHeaderString("Access-Control-Allow-Methods").split(","))
            .collect(toList());
        assertEquals(4L, methods.size());
        assertTrue(methods.contains("HEAD"));
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("PATCH"));
        assertTrue(methods.contains("PUT"));
    }

    @Test
    public void testOptionsPreflightInvalid() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", "http://foo.com")
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

    @Test
    public void testOptionsPreflightInvalid2() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link, Bar").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

    @Test
    public void testOptionsPreflightInvalid3() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "FOO")
            .header("Access-Control-Request-Headers", "Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus());
        assertNull(res.getHeaderString("Access-Control-Allow-Origin"));
        assertNull(res.getHeaderString("Access-Control-Allow-Credentials"));
        assertNull(res.getHeaderString("Access-Control-Max-Age"));
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"));
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"));
    }

}
