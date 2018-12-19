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
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.TrellisUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.http.core.HttpConstants.PRECONDITION_REQUIRED;
import static org.trellisldp.vocabulary.JSONLD.compacted;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.IOService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
public class HttpUtilsTest {

    private static final RDF rdf = getInstance();
    private static final IOService ioService = new JenaIOService();
    private static final IRI identifier = rdf.createIRI("trellis:data/resource");
    private static final Quad QUAD1 = rdf.createQuad(Trellis.PreferAudit, identifier, DC.creator,
            rdf.createLiteral("me"));
    private static final Quad QUAD2 = rdf.createQuad(Trellis.PreferServerManaged, identifier, DC.modified,
            rdf.createLiteral("now"));
    private static final Quad QUAD3 = rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.subject,
            rdf.createLiteral("subj"));
    private static final List<Quad> QUADS = asList(QUAD1, QUAD2, QUAD3);

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private InputStream mockInputStream;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testGetSyntax() {
        final List<MediaType> types = asList(
                new MediaType("application", "json"),
                new MediaType("text", "xml"),
                new MediaType("text", "turtle"));

        assertEquals(of(TURTLE), HttpUtils.getSyntax(ioService, types, empty()), "Cannot determine Turtle syntax!");
    }

    @Test
    public void testGetSyntaxEmpty() {
        assertFalse(HttpUtils.getSyntax(ioService, emptyList(), of("some/type")).isPresent(), "Syntax not rejected!");
        assertEquals(of(TURTLE), HttpUtils.getSyntax(ioService, emptyList(), empty()), "Turtle not default syntax!");
    }

    @Test
    public void testGetSyntaxFallback() {
        final List<MediaType> types = asList(
                new MediaType("application", "json"),
                new MediaType("text", "xml"),
                new MediaType("text", "turtle"));

        assertFalse(HttpUtils.getSyntax(ioService, types, of("application/json")).isPresent(),
                "Non-RDF syntax is incorrectly handled!");
    }

    @Test
    public void testGetSyntaxError() {
        final List<MediaType> types = asList(
                new MediaType("application", "json"),
                new MediaType("text", "xml"));

        assertThrows(NotAcceptableException.class, () -> HttpUtils.getSyntax(ioService, types, empty()),
                "Not-Acceptable Exception should be thrown when nothing matches");
    }

    @Test
    public void testFilterPrefer1() {
        final List<Quad> filtered = QUADS.stream().filter(HttpUtils.filterWithPrefer(
                    Prefer.valueOf("return=representation; include=\"" +
                        Trellis.PreferServerManaged.getIRIString() + "\""))).collect(toList());

        assertFalse(filtered.contains(QUAD2), "Prefer filter doesn't catch quad!");
        assertTrue(filtered.contains(QUAD3), "Prefer filter misses quad!");
        assertEquals(1, filtered.size(), "Incorrect size of filtered quad list");
    }

    @Test
    public void testFilterPrefer2() {
        final List<Quad> filtered2 = QUADS.stream().filter(HttpUtils.filterWithPrefer(
                    Prefer.valueOf("return=representation"))).collect(toList());

        assertTrue(filtered2.contains(QUAD3), "Prefer filter omits quad!");
        assertEquals(1, filtered2.size(), "Incorrect size of filtered quad list!");
    }

    @Test
    public void testFilterPrefer3() {
        final List<Quad> filtered3 = QUADS.stream().filter(HttpUtils.filterWithPrefer(
                    Prefer.valueOf("return=representation; include=\"" +
                        Trellis.PreferAudit.getIRIString() + "\""))).collect(toList());

        assertTrue(filtered3.contains(QUAD1), "Prefer filter omits quad!");
        assertFalse(filtered3.contains(QUAD2), "Prefer filter doesn't catch quad!");
        assertTrue(filtered3.contains(QUAD3), "Prefer filter omits quad!");
        assertEquals(2, filtered3.size(), "Incorrect size of filtered quad list!");
    }

    @Test
    public void testFilterPrefer4() {
        final List<Quad> filtered4 = QUADS.stream().filter(HttpUtils.filterWithPrefer(
                    Prefer.valueOf("return=representation; include=\"" +
                        Trellis.PreferUserManaged.getIRIString() + "\""))).collect(toList());

        assertFalse(filtered4.contains(QUAD1), "Prefer filter doesn't omit quad!");
        assertFalse(filtered4.contains(QUAD2), "Prefer filter doesn't omit quad!");
        assertTrue(filtered4.contains(QUAD3), "Prefer filter omits quad!");
        assertEquals(1, filtered4.size(), "Incorrect size of filtered quad list!");
    }

    @Test
    public void testSkolemize() {
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
                return (IRI) inv.getArgument(0);
            });
        when(mockResourceService.toExternal(any(RDFTerm.class), eq(baseUrl))).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            if (term instanceof IRI) {
                final String identifierString = ((IRI) term).getIRIString();
                if (identifierString.startsWith(TRELLIS_DATA_PREFIX)) {
                    return rdf.createIRI(baseUrl + identifierString.substring(TRELLIS_DATA_PREFIX.length()));
                }
            }
            return term;
        });
        when(mockResourceService.toInternal(any(RDFTerm.class), eq(baseUrl))).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
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
        final Graph graph = rdf.createGraph();
        graph.add(rdf.createTriple(subject, DC.title, literal));
        graph.add(rdf.createTriple(subject, DC.subject, bnode));
        graph.add(rdf.createTriple(bnode, DC.title, literal));

        final List<Triple> triples = graph.stream()
            .map(HttpUtils.skolemizeTriples(mockResourceService, "http://example.org/"))
            .collect(toList());

        assertTrue(triples.stream().anyMatch(t -> t.getSubject().equals(identifier)), "subject not in triple stream!");
        assertTrue(triples.stream().anyMatch(t -> t.getObject().equals(literal)), "Literal not in triple stream!");
        assertTrue(triples.stream().anyMatch(t -> t.getSubject().ntriplesString()
                    .startsWith("<" + TRELLIS_BNODE_PREFIX)), "Skolemized bnode not in triple stream!");

        assertAll(triples.stream().map(HttpUtils.unskolemizeTriples(mockResourceService, "http://example.org/"))
            .map(t -> () -> assertTrue(graph.contains(t), "Graph doesn't include triple: " + t)));
    }

    @Test
    public void testProfile() {
        final List<MediaType> types = asList(
                new MediaType("application", "json"),
                new MediaType("text", "xml"),
                new MediaType("application", "ld+json", singletonMap("profile", compacted.getIRIString())));
        assertEquals(compacted, HttpUtils.getProfile(types, JSONLD), "Incorrect json-ld profile!");
    }

    @Test
    public void testMultipleProfiles() {
        final List<MediaType> types = asList(
                new MediaType("application", "json"),
                new MediaType("text", "xml"),
                new MediaType("application", "ld+json", singletonMap("profile", "first second")));
        assertEquals(rdf.createIRI("first"), HttpUtils.getProfile(types, JSONLD), "Incorrect json-ld profile!");
    }

    @Test
    public void testNoProfile() {
        final List<MediaType> types = asList(
                new MediaType("application", "json"),
                new MediaType("text", "xml"),
                new MediaType("application", "ld+json"));
        assertNull(HttpUtils.getProfile(types, JSONLD), "Unexpected json-ld profile!");
    }

    @Test
    public void testCloseInputStreamWithError() throws IOException {
        doThrow(new IOException()).when(mockInputStream).close();
        assertThrows(UncheckedIOException.class, () ->
                HttpUtils.closeInputStreamAsync(mockInputStream).accept(null, null),
                "No exception on bad InputStream!");
    }

    @Test
    public void testRequirePreconditions() {
        assertDoesNotThrow(() -> HttpUtils.checkRequiredPreconditions(true, "*", null));
        assertDoesNotThrow(() -> HttpUtils.checkRequiredPreconditions(true, null, "Mon, 17 Dec 2018 07:28:00 GMT"));
        assertDoesNotThrow(() -> HttpUtils.checkRequiredPreconditions(false, null, null));
        final Response res = assertThrows(ClientErrorException.class, () ->
                HttpUtils.checkRequiredPreconditions(true, null, null)).getResponse();
        assertEquals(PRECONDITION_REQUIRED, res.getStatus());
    }
}
