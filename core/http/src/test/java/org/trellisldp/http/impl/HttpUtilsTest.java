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

import static java.time.Instant.ofEpochSecond;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_XML_TYPE;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.TrellisUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.http.core.HttpConstants.PRECONDITION_REQUIRED;
import static org.trellisldp.vocabulary.JSONLD.compacted;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.IOService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
class HttpUtilsTest {

    private static final RDF rdf = RDFFactory.getInstance();
    private static final IOService ioService = new JenaIOService();
    private static final IRI identifier = rdf.createIRI("trellis:data/resource");

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private Dataset mockDataset;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void testGetSyntax() {
        final List<MediaType> types = asList(APPLICATION_JSON_TYPE, TEXT_XML_TYPE,
                new MediaType("text", "turtle"));

        assertEquals(TURTLE, HttpUtils.getSyntax(ioService, types, null), "Cannot determine Turtle syntax!");
    }

    @Test
    void testGetSyntaxEmpty() {
        assertNull(HttpUtils.getSyntax(ioService, emptyList(), "some/type"), "Syntax not rejected!");
        assertEquals(TURTLE, HttpUtils.getSyntax(ioService, emptyList(), null), "Turtle not default syntax!");
    }

    @Test
    void testGetSyntaxFallback() {
        final List<MediaType> types = asList(APPLICATION_JSON_TYPE, TEXT_XML_TYPE,
                new MediaType("text", "turtle"));

        assertNull(HttpUtils.getSyntax(ioService, types, "application/json"),
                "Non-RDF syntax is incorrectly handled!");
    }

    @Test
    void testCheckIfModifiedSince() {
        final String time = "Wed, 21 Oct 2015 07:28:00 GMT";
        assertThrows(RedirectionException.class, () ->
                HttpUtils.checkIfModifiedSince("GET", time, ofEpochSecond(1445412479)));
        assertDoesNotThrow(() -> HttpUtils.checkIfModifiedSince("GET", time, ofEpochSecond(1445412480)));
    }

    @Test
    void testDefaultProfile() {
        final String defaultProfile = "http://example.com/profile";
        final IRI profile = HttpUtils.getDefaultProfile(JSONLD, identifier, defaultProfile);
        assertEquals(rdf.createIRI(defaultProfile), profile);
    }

    @Test
    void testGetSyntaxError() {
        final List<MediaType> types = asList(APPLICATION_JSON_TYPE, TEXT_XML_TYPE);

        assertThrows(NotAcceptableException.class, () -> HttpUtils.getSyntax(ioService, types, null),
                "Not-Acceptable Exception should be thrown when nothing matches");
    }

    @Test
    void testFilterPrefer1() {
        final Set<IRI> filtered = HttpUtils.triplePreferences(
                    Prefer.valueOf("return=representation; include=\"" +
                        Trellis.PreferServerManaged.getIRIString() + "\""));

        assertTrue(filtered.contains(Trellis.PreferServerManaged), "Prefer filter misses server managed quad!");
        assertTrue(filtered.contains(Trellis.PreferUserManaged), "Prefer filter misses user managed quad!");
        assertEquals(4, filtered.size(), "Incorrect size of filtered quad list");
    }

    @Test
    void testFilterPrefer2() {
        final Set<IRI> filtered2 = HttpUtils.triplePreferences(Prefer.valueOf("return=representation"));

        assertTrue(filtered2.contains(Trellis.PreferUserManaged), "Prefer filter omits user managed quad!");
        assertEquals(4, filtered2.size(), "Incorrect size of filtered quad list!");
    }

    @Test
    void testFilterPrefer3() {
        final Set<IRI> filtered3 = HttpUtils.triplePreferences(Prefer.valueOf("return=representation; include=\"" +
                        Trellis.PreferAudit.getIRIString() + "\""));

        assertTrue(filtered3.contains(Trellis.PreferAudit), "Prefer filter omits audit quad!");
        assertTrue(filtered3.contains(Trellis.PreferServerManaged),
                "Prefe filter omits server managed quad!");
        assertTrue(filtered3.contains(Trellis.PreferUserManaged), "Prefer filter omits user managed quad!");
        assertEquals(5, filtered3.size(), "Incorrect size of filtered quad list!");
    }

    @Test
    void testFilterPrefer4() {
        final Set<IRI> filtered4 = HttpUtils.triplePreferences(Prefer.valueOf("return=representation; include=\"" +
                        Trellis.PreferUserManaged.getIRIString() + "\""));

        assertFalse(filtered4.contains(Trellis.PreferAudit), "Prefer filter doesn't omit audit quad!");
        assertTrue(filtered4.contains(Trellis.PreferServerManaged), "Prefer filter omits server managed quad!");
        assertTrue(filtered4.contains(Trellis.PreferUserManaged), "Prefer filter omits user managed quad!");
        assertEquals(4, filtered4.size(), "Incorrect size of filtered quad list!");
    }

    @Test
    void testSkolemize() throws Exception {
        final String baseUrl = "http://example.org/";
        final Literal literal = rdf.createLiteral("A title");
        final BlankNode bnode = rdf.createBlankNode("foo");

        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockResourceService.unskolemize(any(IRI.class)))
            .thenAnswer(inv -> {
                final String uri = ((IRI) inv.getArgument(0)).getIRIString();
                if (uri.startsWith(TRELLIS_BNODE_PREFIX)) {
                    return bnode;
                }
                return inv.getArgument(0);
            });
        when(mockResourceService.toExternal(any(RDFTerm.class), eq(baseUrl))).thenAnswer(inv -> {
            final RDFTerm term = inv.getArgument(0);
            if (term instanceof IRI) {
                final String identifierString = ((IRI) term).getIRIString();
                if (identifierString.startsWith(TRELLIS_DATA_PREFIX)) {
                    return rdf.createIRI(baseUrl + identifierString.substring(TRELLIS_DATA_PREFIX.length()));
                }
            }
            return term;
        });
        when(mockResourceService.toInternal(any(RDFTerm.class), eq(baseUrl))).thenAnswer(inv -> {
            final RDFTerm term = inv.getArgument(0);
            if (term instanceof IRI) {
                final String identifierString = ((IRI) term).getIRIString();
                if (identifierString.startsWith(baseUrl)) {
                    return rdf.createIRI(TRELLIS_DATA_PREFIX + identifierString.substring(baseUrl.length()));
                }
            }
            return term;
        });

        when(mockResourceService.unskolemize(any(Literal.class))).then(returnsFirstArg());

        final IRI subject = rdf.createIRI("http://example.org/resource");
        try (final Graph graph = rdf.createGraph()) {
            graph.add(rdf.createTriple(subject, DC.title, literal));
            graph.add(rdf.createTriple(subject, DC.subject, bnode));
            graph.add(rdf.createTriple(bnode, DC.title, literal));

            final List<Triple> triples = graph.stream()
                .map(HttpUtils.skolemizeTriples(mockResourceService, "http://example.org/"))
                .collect(toList());

            assertTrue(triples.stream().anyMatch(t -> t.getSubject().equals(identifier)),
                    "subject not in triple stream!");
            assertTrue(triples.stream().anyMatch(t -> t.getObject().equals(literal)), "Literal not in triple stream!");
            assertTrue(triples.stream().anyMatch(t -> t.getSubject().ntriplesString()
                        .startsWith("<" + TRELLIS_BNODE_PREFIX)), "Skolemized bnode not in triple stream!");

            assertAll(triples.stream().map(HttpUtils.unskolemizeTriples(mockResourceService, "http://example.org/"))
                .map(t -> () -> assertTrue(graph.contains(t), "Graph doesn't include triple: " + t)));
        }
    }

    @Test
    void testMatchIdentifier() {
        final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
        final IRI resource = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI resourceSlash = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource/");
        assertTrue(HttpUtils.matchIdentifier(root, root));
        assertTrue(HttpUtils.matchIdentifier(resourceSlash, resource));
        assertFalse(HttpUtils.matchIdentifier(resource, resourceSlash));
        assertFalse(HttpUtils.matchIdentifier(root, resource));
        assertFalse(HttpUtils.matchIdentifier(rdf.createBlankNode(), root));
    }

    @Test
    void testProfile() {
        final List<MediaType> types = asList(APPLICATION_JSON_TYPE, TEXT_XML_TYPE,
                new MediaType("application", "ld+json", singletonMap("profile", compacted.getIRIString())));
        assertEquals(compacted, HttpUtils.getProfile(types, JSONLD), "Incorrect json-ld profile!");
    }

    @Test
    void testMultipleProfiles() {
        final List<MediaType> types = asList(APPLICATION_JSON_TYPE, TEXT_XML_TYPE,
                new MediaType("application", "ld+json", singletonMap("profile", "first second")));
        assertEquals(rdf.createIRI("first"), HttpUtils.getProfile(types, JSONLD), "Incorrect json-ld profile!");
    }

    @Test
    void testNoProfile() {
        final List<MediaType> types = asList(APPLICATION_JSON_TYPE, TEXT_XML_TYPE,
                new MediaType("application", "ld+json"));
        assertNull(HttpUtils.getProfile(types, JSONLD), "Unexpected json-ld profile!");
    }

    @Test
    void testRequirePreconditions() {
        assertDoesNotThrow(() -> HttpUtils.checkRequiredPreconditions(true, "*", null));
        assertDoesNotThrow(() -> HttpUtils.checkRequiredPreconditions(true, null, "Mon, 17 Dec 2018 07:28:00 GMT"));
        assertDoesNotThrow(() -> HttpUtils.checkRequiredPreconditions(false, null, null));
        try (final Response res = assertThrows(ClientErrorException.class, () ->
                HttpUtils.checkRequiredPreconditions(true, null, null)).getResponse()) {
            assertEquals(PRECONDITION_REQUIRED, res.getStatus());
        }
    }

    @Test
    void testCloseDataset() throws Exception {
        doThrow(new IOException()).when(mockDataset).close();
        assertThrows(RuntimeTrellisException.class, () -> HttpUtils.closeDataset(mockDataset));
    }

}
