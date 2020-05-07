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
package org.trellisldp.http.impl;

import static java.util.Arrays.asList;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.trellisldp.http.core.HttpConstants.*;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_N_TRIPLES;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class OptionsHandlerTest extends BaseTestHandler {

    private static final String ERR_ACCEPT_PATCH = "Incorrect Accept-Patch header!";
    private static final String CHECK_ALLOW = "Check Allow headers";

    @Test
    void testOptionsLdpr() {
        when(mockIoService.supportedWriteSyntaxes()).thenReturn(asList(TURTLE, JSONLD, NTRIPLES));

        final OptionsHandler optionsHandler = new OptionsHandler(mockTrellisRequest, mockBundler, extensions);
        try (final Response res = optionsHandler.ldpOptions().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);

            final String acceptPost = res.getHeaderString(ACCEPT_POST);
            assertNotNull(acceptPost, "Missing Accept-Post header!");
            assertTrue(acceptPost.contains(APPLICATION_LD_JSON), "JSON-LD missing from acceptable POST formats!");
            assertTrue(acceptPost.contains(APPLICATION_N_TRIPLES), "N-Triples missing from acceptable POST formats!");
            assertTrue(acceptPost.contains(TEXT_TURTLE.split(";")[0]), "Turtle missing from acceptable POST formats!");
            assertAll(CHECK_ALLOW, checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH, POST)));
        }
    }

    @Test
    void testOptionsAcl() {
        when(mockTrellisRequest.getExt()).thenReturn("acl");

        final OptionsHandler optionsHandler = new OptionsHandler(mockTrellisRequest, mockBundler, extensions);
        try (final Response res = optionsHandler.ldpOptions().build()) {
            assertEquals(NO_CONTENT, res.getStatusInfo(), ERR_RESPONSE_CODE);
            assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), ERR_ACCEPT_PATCH);
            assertAll(CHECK_ALLOW, checkAllowHeader(res, asList(GET, HEAD, OPTIONS, POST, PUT, DELETE, PATCH)));
        }
    }
}
