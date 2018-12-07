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
import static org.junit.jupiter.api.Assertions.assertAll;
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
public class CrossOriginResourceSharingFilterSpecificOriginTest extends BaseCrossOriginResourceSharingFilterTest {

    private static final String MAX_AGE = "180";
    private static final String TRUE = "true";

    @Override
    public Application configure() {
        init();

        final ResourceConfig config = new ResourceConfig();
        config.register(new TrellisHttpResource(mockBundler));
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

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertAll("Check that there are no CORS headers", checkNoCORSHeaders(res));
    }

    @Test
    public void testGetCORS() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin!");
        assertEquals(TRUE, res.getHeaderString("Access-Control-Allow-Credentials"), "Incorrect -Allow-Credentials!");
        assertTrue(stream(res.getHeaderString("Access-Control-Expose-Headers").split(","))
                .anyMatch("Accept-Patch"::equalsIgnoreCase), "Missing accept-patch in -Expose-Headers!");
        assertNull(res.getHeaderString("Access-Control-Max-Age"), "Unexpected -Max-Age header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"), "Unexpected -Allow-Headers!");
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"), "Unexpected -Allow-Methods!");
    }

    @Test
    public void testGetCORSSimple() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Accept").get();

        assertEquals(SC_OK, res.getStatus(), "Unexpected response code!");
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertEquals(TRUE, res.getHeaderString("Access-Control-Allow-Credentials"), "Incorrect -Allow-Credentials!");
        assertTrue(stream(res.getHeaderString("Access-Control-Expose-Headers").split(","))
                .anyMatch("Accept-Patch"::equalsIgnoreCase), "Missing Accept-Patch from -Expose-Headers!");
        assertNull(res.getHeaderString("Access-Control-Max-Age"), "Unexpected -Max-Age header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Methods"), "Unexpected -Allow-Methods header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"), "Unexpected -Allow-Headers header!");
    }

    @Test
    public void testOptionsPreflightSimple() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Accept").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(MAX_AGE, res.getHeaderString("Access-Control-Max-Age"), "Incorreect -Max-Age header!");
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertEquals(TRUE, res.getHeaderString("Access-Control-Allow-Credentials"), "Incorrect -Allow-Credentials!");
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"), "Unexpected -Expose-Headers header!");
        assertTrue(res.getHeaderString("Access-Control-Allow-Headers").contains("accept"),
                "accept missing from Allow-Headers!");
        assertFalse(res.getHeaderString("Access-Control-Allow-Methods").contains("POST"),
                "Unexpected POST in -Allow-Methods!");
        assertTrue(res.getHeaderString("Access-Control-Allow-Methods").contains("PATCH"),
                "Missing PATCH in -Allow-Methods!");
    }

    @Test
    public void testCorsPreflight() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Language, Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertEquals(TRUE, res.getHeaderString("Access-Control-Allow-Credentials"), "Incorrect -Allow-Credentials!");
        assertEquals(MAX_AGE, res.getHeaderString("Access-Control-Max-Age"), "Incorrect -Max-Age header!");
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"), "Unexpected -Expose-Headers header!");

        final List<String> headers = stream(res.getHeaderString("Access-Control-Allow-Headers").split(","))
            .collect(toList());
        assertEquals(4L, headers.size(), "Incorrect count of -Allow-Headers values!");
        assertTrue(headers.contains("accept"), "accept missing from -Allow-Headers!");
        assertTrue(headers.contains("link"), "link missing from -Allow-Headers!");
        assertTrue(headers.contains("content-type"), "content-type missing from -Allow-Headers!");
        assertTrue(headers.contains("accept-patch"), "accept-patch missing from -Allow-Headers!");
        assertAll("Check the Allow-Methods values", checkAllowMethods(res, asList("HEAD", "GET", "PATCH", "PUT")));
   }

    @Test
    public void testCorsPreflightNoRequestHeaders() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertEquals(TRUE, res.getHeaderString("Access-Control-Allow-Credentials"), "Incorrect -Allow-Credentials!");
        assertEquals(MAX_AGE, res.getHeaderString("Access-Control-Max-Age"), "Incorrect -Max-Age header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"), "Unexpected -Allow-Headers header!");
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"), "Unexpected -Expose-Headers header!");
        assertAll("Check the Allow-Methods values", checkAllowMethods(res, asList("HEAD", "GET", "PATCH", "PUT")));
    }

    @Test
    public void testCorsPreflightNoMatch() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Language").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertEquals(ORIGIN, res.getHeaderString("Access-Control-Allow-Origin"), "Incorrect -Allow-Origin header!");
        assertEquals(MAX_AGE, res.getHeaderString("Access-Control-Max-Age"), "Incorrect -Max-Age header!");
        assertEquals(TRUE, res.getHeaderString("Access-Control-Allow-Credentials"), "Incorrect -Allow-Credentials!");
        assertNull(res.getHeaderString("Access-Control-Expose-Headers"), "Unexpected -Expose-Headers header!");
        assertNull(res.getHeaderString("Access-Control-Allow-Headers"), "Unexpected -Allow-Headers header!");
        assertAll("Check the Allow-Methods values", checkAllowMethods(res, asList("HEAD", "GET", "PATCH", "PUT")));
    }

    @Test
    public void testOptionsPreflightInvalid() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", "http://foo.com")
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check that there are no CORS headers", checkNoCORSHeaders(res));
    }

    @Test
    public void testOptionsPreflightInvalid2() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "PUT")
            .header("Access-Control-Request-Headers", "Content-Type, Link, Bar").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check that there are no CORS headers", checkNoCORSHeaders(res));
    }

    @Test
    public void testOptionsPreflightInvalid3() {
        final Response res = target(RESOURCE_PATH).request().header("Origin", ORIGIN)
            .header("Access-Control-Request-Method", "FOO")
            .header("Access-Control-Request-Headers", "Content-Type, Link").options();

        assertEquals(SC_NO_CONTENT, res.getStatus(), "Unexpected response code!");
        assertAll("Check that there are no CORS headers", checkNoCORSHeaders(res));
    }
}
