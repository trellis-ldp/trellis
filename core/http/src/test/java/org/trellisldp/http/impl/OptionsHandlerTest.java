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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.core.HttpConstants.PATCH;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_LD_JSON;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_N_TRIPLES;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class OptionsHandlerTest extends BaseTestHandler {

    @Test
    public void testOptionsLdprs() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);

        final OptionsHandler optionsHandler = new OptionsHandler(mockLdpRequest, mockBundler, false, null);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));
    }

    @Test
    public void testOptionsLdpc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockIoService.supportedWriteSyntaxes()).thenReturn(asList(TURTLE, JSONLD, NTRIPLES));

        final OptionsHandler optionsHandler = new OptionsHandler(mockLdpRequest, mockBundler, false, baseUrl);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");

        final String acceptPost = res.getHeaderString(ACCEPT_POST);
        assertNotNull(acceptPost, "Missing Accept-Post header!");
        assertTrue(acceptPost.contains(APPLICATION_LD_JSON), "JSON-LD missing from acceptable POST formats!");
        assertTrue(acceptPost.contains(APPLICATION_N_TRIPLES), "N-Triples missing from acceptable POST formats!");
        assertTrue(acceptPost.contains(TEXT_TURTLE.split(";")[0]), "Turtle missing from acceptable POST formats!");
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH, POST)));
    }

    @Test
    public void testOptionsLdpnr() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);

        final OptionsHandler optionsHandler = new OptionsHandler(mockLdpRequest, mockBundler, false, null);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));
    }

    @Test
    public void testOptionsAcl() {
        when(mockLdpRequest.getExt()).thenReturn("acl");

        final OptionsHandler optionsHandler = new OptionsHandler(mockLdpRequest, mockBundler, false, baseUrl);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH), "Incorrect Accept-Patch header!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header!");
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));
    }

    @Test
    public void testOptionsMemento() {
        final OptionsHandler optionsHandler = new OptionsHandler(mockLdpRequest, mockBundler, true, null);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo(), "Incorrect response code!");
        assertNull(res.getHeaderString(ACCEPT_POST), "Unexpected Accept-Post header on Memento!");
        assertNull(res.getHeaderString(ACCEPT_PATCH), "Unexpected Accept-Patch header on Memento!");
        assertAll("Check Allow headers", checkAllowHeader(res, asList(GET, HEAD, OPTIONS)));
    }
}
