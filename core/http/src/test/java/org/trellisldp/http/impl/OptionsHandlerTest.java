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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.domain.HttpConstants.PATCH;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_LD_JSON;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_N_TRIPLES;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;

import java.util.List;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class OptionsHandlerTest {

    private static final String baseUrl = "http://localhost:8080/repo";

    private final MementoService mementoService = new NoopMementoService();

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private Resource mockResource;

    @Mock
    private LdpRequest mockRequest;

    @Mock
    private IOService mockIoService;

    @Mock
    private ServiceBundler mockBundler;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockBundler.getIOService()).thenReturn(mockIoService);
        when(mockBundler.getMementoService()).thenReturn(mementoService);
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> empty());
        when(mockRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockRequest.getPath()).thenReturn("/");
        when(mockIoService.supportedReadSyntaxes()).thenReturn(of(TURTLE, JSONLD).collect(toList()));
        when(mockIoService.supportedWriteSyntaxes()).thenReturn(of(TURTLE, JSONLD, NTRIPLES).collect(toList()));
    }

    @Test
    public void testOptionsLdprs() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);

        final OptionsHandler optionsHandler = new OptionsHandler(mockRequest, mockBundler, false, null);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));
    }

    @Test
    public void testOptionsLdpc() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);

        final OptionsHandler optionsHandler = new OptionsHandler(mockRequest, mockBundler, false, baseUrl);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));

        final String acceptPost = res.getHeaderString(ACCEPT_POST);
        assertNotNull(acceptPost);
        assertTrue(acceptPost.contains(APPLICATION_LD_JSON));
        assertTrue(acceptPost.contains(APPLICATION_N_TRIPLES));
        assertTrue(acceptPost.contains(TEXT_TURTLE.split(";")[0]));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH, POST)));
    }

    @Test
    public void testOptionsLdpnr() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);

        final OptionsHandler optionsHandler = new OptionsHandler(mockRequest, mockBundler, false, null);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));
    }

    @Test
    public void testOptionsAcl() {
        when(mockRequest.getExt()).thenReturn("acl");

        final OptionsHandler optionsHandler = new OptionsHandler(mockRequest, mockBundler, false, baseUrl);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertEquals(APPLICATION_SPARQL_UPDATE, res.getHeaderString(ACCEPT_PATCH));
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS, PUT, DELETE, PATCH)));
    }

    @Test
    public void testOptionsMemento() {
        final OptionsHandler optionsHandler = new OptionsHandler(mockRequest, mockBundler, true, null);
        final Response res = optionsHandler.ldpOptions(optionsHandler.initialize(mockResource)).build();

        assertEquals(NO_CONTENT, res.getStatusInfo());
        assertNull(res.getHeaderString(ACCEPT_POST));
        assertNull(res.getHeaderString(ACCEPT_PATCH));
        assertAll(checkAllowHeader(res, asList(GET, HEAD, OPTIONS)));
    }

    private Stream<Executable> checkAllowHeader(final Response res, final List<String> methods) {
        final String allow = res.getHeaderString(ALLOW);
        return of(
                () -> assertNotNull(allow),
                () -> assertTrue(allow.contains(GET) || !methods.contains(GET)),
                () -> assertTrue(allow.contains(HEAD) || !methods.contains(HEAD)),
                () -> assertTrue(allow.contains(OPTIONS) || !methods.contains(OPTIONS)),
                () -> assertTrue(allow.contains(PUT) || !methods.contains(PUT)),
                () -> assertTrue(allow.contains(DELETE) || !methods.contains(DELETE)),
                () -> assertTrue(allow.contains(POST) || !methods.contains(POST)),
                () -> assertTrue(allow.contains(PATCH) || !methods.contains(PATCH)),

                () -> assertFalse(methods.contains(GET) && !allow.contains(GET)),
                () -> assertFalse(methods.contains(HEAD) && !allow.contains(HEAD)),
                () -> assertFalse(methods.contains(OPTIONS) && !allow.contains(OPTIONS)),
                () -> assertFalse(methods.contains(PUT) && !allow.contains(PUT)),
                () -> assertFalse(methods.contains(DELETE) && !allow.contains(DELETE)),
                () -> assertFalse(methods.contains(POST) && !allow.contains(POST)),
                () -> assertFalse(methods.contains(PATCH) && !allow.contains(PATCH)));
    }
}
