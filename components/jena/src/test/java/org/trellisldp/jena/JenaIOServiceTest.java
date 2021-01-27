/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
package org.trellisldp.jena;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Stream.of;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.RDFXML;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.apache.jena.commonsrdf.JenaCommonsRDF.fromJena;
import static org.apache.jena.graph.Factory.createDefaultGraph;
import static org.apache.jena.graph.NodeFactory.createBlankNode;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.vocabulary.DCTerms.spatial;
import static org.apache.jena.vocabulary.DCTerms.subject;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.DCTypes.Text;
import static org.apache.jena.vocabulary.RDF.Nodes.type;
import static org.apache.jena.vocabulary.RDF.uri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.Syntax.LD_PATCH;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.vocabulary.JSONLD.compacted;
import static org.trellisldp.vocabulary.JSONLD.expanded;
import static org.trellisldp.vocabulary.JSONLD.flattened;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.apache.jena.commonsrdf.JenaCommonsRDF;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trellisldp.api.CacheService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.RDFaWriterService;
import org.trellisldp.api.TrellisRuntimeException;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
@ExtendWith(MockitoExtension.class)
class JenaIOServiceTest {

    private static final RDF rdf = RDFFactory.getInstance();
    private static final String identifier = "http://example.com/resource";

    private final Map<String, String> namespaces = new HashMap<>();

    private JenaIOService service, service2, service3;

    @Mock
    private NamespaceService mockNamespaceService;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private OutputStream mockOutputStream;

    @Mock
    private RDFaWriterService mockRdfaWriterService;

    @Mock
    private CacheService<String, String> mockCache;

    @Mock
    private RDFSyntax mockSyntax;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        namespaces.put("dcterms", DCTerms.NS);
        namespaces.put("rdf", uri);

        service = new JenaIOService();
        service.namespaceService = mockNamespaceService;
        service.htmlSerializer = mockRdfaWriterService;
        service.cache = mockCache;
        service.relativeIriConfig = Optional.empty();
        service.allowedContextsConfig = Optional.of(new String[]{"http://www.w3.org/ns/anno.jsonld"});
        service.allowedDomainsConfig = Optional.of(new String[]{"http://www.trellisldp.org/ns/"});
        service.init();

        service2 = new JenaIOService();
        service2.namespaceService = mockNamespaceService;
        service2.htmlSerializer = mockRdfaWriterService;
        service2.cache = mockCache;
        service2.relativeIriConfig = Optional.empty();
        service2.allowedContextsConfig = Optional.empty();
        service2.allowedDomainsConfig = Optional.of(new String[]{"http://www.w3.org/ns/"});
        service2.init();

        service3 = new JenaIOService();
        service3.namespaceService = mockNamespaceService;
        service3.htmlSerializer = mockRdfaWriterService;
        service3.cache = mockCache;
        service3.relativeIriConfig = Optional.of(Boolean.TRUE);
        service3.allowedContextsConfig = Optional.empty();
        service3.allowedDomainsConfig = Optional.empty();
        service3.init();
    }

    @Test
    void testJsonLdDefaultSerializer() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service3.write(getTriples(), out, JSONLD, identifier);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service3.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check compact serialization", checkCompactSerialization(output, graph));
    }

    @Test
    void testJsonLdExpandedSerializer() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, expanded);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check expanded serialization", checkExpandedSerialization(output, graph));
    }

    @Test
    void testJsonLdExpandedFlatSerializer() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, expanded, flattened);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check expanded serialization", checkExpandedSerialization(output, graph));
    }

    @Test
    void testJsonLdFlatExpandedSerializer() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, flattened, expanded);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check expanded serialization", checkFlattenedSerialization(output, graph));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testJsonLdCustomSerializer() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);
        when(mockCache.get(anyString(), any(Function.class))).thenAnswer(inv -> {
            final String key = inv.getArgument(0);
            final Function<String, String> mapper = inv.getArgument(1);
            return mapper.apply(key);
        });

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, rdf.createIRI("http://www.w3.org/ns/anno.jsonld"));
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"dcterms:title\":\"A title\""), "missing dcterms:title!");
        assertTrue(output.contains("\"type\":\"Text\""), "missing rdf:type Text!");
        assertTrue(output.contains("\"@context\":"), "missing @context!");
        assertTrue(output.contains("\"@context\":\"http://www.w3.org/ns/anno.jsonld\""), "Incorrect @context value!");
        assertFalse(output.contains("\"@graph\":"), "unexpected @graph!");

        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, identifier).forEach(graph::add);
        assertTrue(validateGraph(graph), "Not all triples present in output graph!");
    }

    @Test
    void testJsonLdCustomSerializerNoopCache() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final IOService svc = JenaIOService.newJenaIOService();

        svc.write(getTriples(), out, JSONLD, identifier);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, identifier).forEach(graph::add);
        assertTrue(validateGraph(graph), "Not all triples present in output graph!");
    }


    @Test
    @SuppressWarnings("unchecked")
    void testJsonLdCustomSerializer2() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);
        when(mockCache.get(anyString(), any(Function.class))).thenAnswer(inv -> {
            final String key = inv.getArgument(0);
            final Function<String, String> mapper = inv.getArgument(1);
            return mapper.apply(key);
        });

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service2.write(getTriples(), out, JSONLD, identifier, rdf.createIRI("http://www.w3.org/ns/anno.jsonld"));
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"dcterms:title\":\"A title\""), "missing/incorrect dcterms:title!");
        assertTrue(output.contains("\"type\":\"Text\""), "missing/incorrect rdf:type!");
        assertTrue(output.contains("\"@context\":"), "missing @context!");
        assertTrue(output.contains("\"@context\":\"http://www.w3.org/ns/anno.jsonld\""), "Incorrect @context value!");
        assertFalse(output.contains("\"@graph\":"), "unexpected @graph!");

        final Graph graph = rdf.createGraph();
        service2.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertTrue(validateGraph(graph), "Not all triples present in output graph!");
    }

    @Test
    void testJsonLdCustomUnrecognizedSerializer() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, rdf.createIRI("http://www.example.org/context.jsonld"));
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check compact serialization", checkCompactSerialization(output, graph));
    }

    @Test
    void testJsonLdCustomUnrecognizedSerializer2() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service2.write(getTriples(), out, JSONLD, identifier, rdf.createIRI("http://www.example.org/context.jsonld"));
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service2.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check compact serialization", checkCompactSerialization(output, graph));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testJsonLdCustomUnrecognizedSerializer3() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);
        when(mockCache.get(anyString(), any(Function.class))).thenAnswer(inv -> {
            final String key = inv.getArgument(0);
            final Function<String, String> mapper = inv.getArgument(1);
            return mapper.apply(key);
        });

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier,
                rdf.createIRI("http://www.trellisldp.org/ns/nonexistent.jsonld"));
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check compact serialization", checkCompactSerialization(output, graph));
    }

    @Test
    void testJsonLdCompactedSerializer() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, compacted);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check compact serialization", checkCompactSerialization(output, graph));
    }

    @Test
    void testJsonLdFlattenedSerializer() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, flattened);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check flattened serialization", checkFlattenedSerialization(output, graph));
    }

    @Test
    void testJsonLdFlattenedSerializer2() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, flattened, compacted);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check flattened serialization", checkFlattenedSerialization(output, graph));
    }

    @Test
    void testJsonLdFlattenedSerializer3() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, flattened, expanded);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check flattened serialization", checkFlattenedSerialization(output, graph));
    }

    @Test
    void testJsonLdFlattenedSerializer4() throws UnsupportedEncodingException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, identifier, compacted, flattened);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check flattened serialization", checkCompactSerialization(output, graph));
    }

    @Test
    void testMalformedInput() {
        final ByteArrayInputStream in = new ByteArrayInputStream("<> <ex:test> a Literal\" . ".getBytes(UTF_8));
        assertThrows(TrellisRuntimeException.class, () ->
                service.read(in, TURTLE, null), "No exception on malformed input!");
    }

    @Test
    void testNTriplesSerializer() {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service3.write(getTriples(), out, NTRIPLES, identifier);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.NTRIPLES);
        assertTrue(validateGraph(fromJena(graph)), "Failed round-trip for N-Triples!");
    }

    @Test
    void testBufferedSerializer() {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, RDFXML, identifier);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.RDFXML);
        assertTrue(validateGraph(fromJena(graph)), "Failed round-trip for RDFXML!");
    }

    @Test
    void testTurtleSerializer() {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, TURTLE, identifier);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.TURTLE);
        assertTrue(validateGraph(fromJena(graph)), "Failed round-trip for Turtle!");
    }

    @Test
    void testTurtleReaderWithContext() {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final Graph graph = rdf.createGraph();
        service.read(getClass().getResourceAsStream("/testRdf.ttl"), TURTLE, identifier)
            .forEach(graph::add);
        assertTrue(validateGraph(graph), "Failed round-trip for Turtle using a context value!");
    }

    @Test
    void testHtmlSerializer() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getComplexTriples(), out, RDFA, "http://example.org/");
        verify(mockRdfaWriterService).write(any(), eq(out), eq("http://example.org/"));
    }

    @Test
    void testHtmlSerializer2() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getComplexTriples(), out, RDFA, null);
        verify(mockRdfaWriterService).write(any(), eq(out), eq(null));
    }

    @Test
    void testUpdateError() {
        final Graph graph = rdf.createGraph();
        getTriples().forEach(graph::add);
        assertEquals(3L, graph.size(), "Incorrect graph size!");
        assertThrows(TrellisRuntimeException.class, () ->
                service.update(graph, "blah blah blah blah blah", SPARQL_UPDATE, null), "no exception on bad update!");
    }

    @Test
    void testReadError() throws IOException {
        doThrow(new IOException()).when(mockInputStream).read(any(byte[].class), anyInt(), anyInt());
        assertThrows(TrellisRuntimeException.class, () -> service.read(mockInputStream, TURTLE, "context"),
                "No read exception on bad input stream!");
    }

    @Test
    void testWriteError() throws IOException {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);
        doThrow(new IOException()).when(mockOutputStream).write(any(byte[].class), anyInt(), anyInt());
        final Stream<Triple> triples = getTriples();
        assertThrows(TrellisRuntimeException.class, () -> service.write(triples, mockOutputStream, TURTLE,
                    identifier), "No write exception on bad input stream!");
    }

    @Test
    void testUpdate() {
        final Graph graph = rdf.createGraph();
        getTriples().forEach(graph::add);
        assertEquals(3L, graph.size(), "Incorrect graph size!");
        service.update(graph, "DELETE WHERE { ?s <http://purl.org/dc/terms/title> ?o }", SPARQL_UPDATE, "test:info");
        assertEquals(2L, graph.size(), "Incorrect graph size, post update!");
        service.update(graph, "INSERT { " +
                "<> <http://purl.org/dc/terms/title> \"Other title\" } WHERE {}", SPARQL_UPDATE,
                "trellis:data/resource");
        assertEquals(3L, graph.size(), "Incorrect graph size, after adding triple!");
        service.update(graph, "DELETE WHERE { ?s ?p ?o };" +
                "INSERT { <> <http://purl.org/dc/terms/title> \"Other title\" } WHERE {}", SPARQL_UPDATE,
                "trellis:data/");
        assertEquals(1L, graph.size(), "Incorrect graph size after removing triples!");
        assertEquals("<trellis:data/>", graph.stream().findFirst().map(Triple::getSubject)
                .map(RDFTerm::ntriplesString).get(), "Incorrect graph subject from updates!");
    }

    @Test
    void testUpdateInvalidSyntax() {
        final Graph graph = rdf.createGraph();
        getTriples().forEach(graph::add);
        final String patch = "UpdateList <#> <http://example.org/vocab#preferredLanguages> 1..2 ( \"fr\" ) .";
        assertThrows(TrellisRuntimeException.class, () -> service.update(graph, patch, LD_PATCH, null),
                "No exception thrown with invalid update syntax!");
    }

    @Test
    void testWriteInvalidSyntax() {
        when(mockSyntax.mediaType()).thenReturn("fake/mediatype");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Stream<Triple> triples = getTriples();
        assertThrows(TrellisRuntimeException.class, () -> service.write(triples, out, mockSyntax,
                    identifier), "No exception thrown with invalid write syntax!");
    }

    @Test
    void testReadInvalidSyntax() {
        when(mockSyntax.mediaType()).thenReturn("fake/mediatype");
        final String output = "blah blah blah";

        final InputStream input = new ByteArrayInputStream(output.getBytes(UTF_8));
        assertThrows(TrellisRuntimeException.class, () -> service.read(input, mockSyntax, null),
                "No exception thrown with invalid read syntax!");
    }

    @Test
    void testReadSyntaxes() {
        assertTrue(service.supportedReadSyntaxes().contains(TURTLE), "Turtle not supported for reading!");
        assertTrue(service.supportedReadSyntaxes().contains(JSONLD), "JSON-LD not supported for reading!");
        assertTrue(service.supportedReadSyntaxes().contains(NTRIPLES), "N-Triples not supported for reading!");
        assertTrue(service.supportedReadSyntaxes().contains(RDFA), "RDFa not supported for reading!");
        assertFalse(service.supportedReadSyntaxes().contains(RDFXML), "RDF/XML unexpectedly supported for reading!");
        assertFalse(JenaIOService.newJenaIOService().supportedReadSyntaxes().contains(RDFA),
                "RDFa unexpectedly supported!");
    }

    @Test
    void testWriteSyntaxes() {
        assertTrue(service.supportedWriteSyntaxes().contains(TURTLE), "Turtle not supported for writing!");
        assertTrue(service.supportedWriteSyntaxes().contains(JSONLD), "JSON-LD not supported for writing!");
        assertTrue(service.supportedWriteSyntaxes().contains(NTRIPLES), "N-Triples not supported for writing!");
        assertFalse(service.supportedWriteSyntaxes().contains(RDFXML), "RDF/XML unexpectedly supported for writing!");
        assertFalse(service.supportedWriteSyntaxes().contains(RDFA), "RDFa unexpectedly supported for writing!");
    }

    @Test
    void testUpdateSyntaxes() {
        assertTrue(service.supportedUpdateSyntaxes().contains(SPARQL_UPDATE), "SPARQL-Update not supported!");
        assertFalse(service.supportedUpdateSyntaxes().contains(LD_PATCH), "LD-PATCH unexpectedly supported!");
    }

    @Test
    void testShouldUseRelativeIRIs() {
        assertTrue(JenaIOService.shouldUseRelativeIRIs(true));
        assertTrue(JenaIOService.shouldUseRelativeIRIs(false, Trellis.SerializationRelative));
        assertTrue(JenaIOService.shouldUseRelativeIRIs(false, Trellis.SerializationRelative,
                    Trellis.SerializationAbsolute));
        assertTrue(JenaIOService.shouldUseRelativeIRIs(true, Trellis.PreferUserManaged));

        assertFalse(JenaIOService.shouldUseRelativeIRIs(false));
        assertFalse(JenaIOService.shouldUseRelativeIRIs(true, Trellis.SerializationAbsolute));
        assertFalse(JenaIOService.shouldUseRelativeIRIs(true, Trellis.SerializationAbsolute,
                    Trellis.SerializationRelative));
        assertFalse(JenaIOService.shouldUseRelativeIRIs(false, Trellis.PreferServerManaged));
    }

    @Test
    void testShouldSkipNamespace() {
        final String foo = "http://example.com/foo";
        final String ns = "http://example.com/namespace#";
        final Set<String> namespaces = Set.of(ns);
        assertFalse(JenaIOService.shouldAddNamespace(namespaces, ns, null),
                "Added namespace that already exists");
        assertFalse(JenaIOService.shouldAddNamespace(namespaces, foo, null),
                "Added namespace with null base definition");
        assertFalse(JenaIOService.shouldAddNamespace(namespaces, foo, "http://example.com/bar"),
                "Added namespace with matching scheme/host/port");
        assertFalse(JenaIOService.shouldAddNamespace(namespaces, "not a url", "also not a url"),
                "Added invalid namespace");
        assertTrue(JenaIOService.shouldAddNamespace(namespaces, foo, "http://example.com:80/bar"),
                "Missed namespace with different port definition!");
        assertTrue(JenaIOService.shouldAddNamespace(namespaces, foo, "https://example.com/bar"),
                "Missed namespace with different scheme!");
        assertTrue(JenaIOService.shouldAddNamespace(namespaces, foo, "http://example.com:8080/bar"),
                "Missed namespace with different port definition!");
        assertTrue(JenaIOService.shouldAddNamespace(namespaces, "http://test.example/foo", "http://example.com/bar"),
                "Missed namespace with different host!");
    }

    static Stream<Triple> getTriples() {
        final Node sub = createURI(identifier);
        return of(
                create(sub, title.asNode(), createLiteral("A title")),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(sub, type, Text.asNode()))
            .map(JenaCommonsRDF::fromJena);
    }

    static Stream<Triple> getComplexTriples() {
        final Node sub = createURI(identifier);
        final Node bn = createBlankNode();
        return of(
                create(sub, title.asNode(), createLiteral("A title")),
                create(sub, subject.asNode(), bn),
                create(bn, title.asNode(), createLiteral("Other title")),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(sub, type, Text.asNode()))
            .map(JenaCommonsRDF::fromJena);

    }

    static boolean validateGraph(final Graph graph) {
        return getTriples().map(graph::contains).reduce(true, (acc, x) -> acc && x);
    }

    static Stream<Executable> checkExpandedSerialization(final String serialized, final Graph graph) {
        return of(
                () -> assertTrue(serialized.contains("\"http://purl.org/dc/terms/title\":[{\"@value\":\"A title\"}]"),
                    "no expanded dc:title property!"),
                () -> assertFalse(serialized.contains("\"@context\":"), "unexpected @context!"),
                () -> assertFalse(serialized.contains("\"@graph\":"), "unexpected @graph!"),
                () -> assertTrue(validateGraph(graph), "Not all triples present in output graph!"));
    }

    static Stream<Executable> checkCompactSerialization(final String serialized, final Graph graph) {
        return of(
                () -> assertTrue(serialized.contains("\"title\":\"A title\""), "missing/invalid dc:title value!"),
                () -> assertTrue(serialized.contains("\"@context\":"), "missing @context!"),
                () -> assertFalse(serialized.contains("\"@graph\":"), "unexpected @graph!"),
                () -> assertTrue(validateGraph(graph), "Not all triples present in output graph!"));
    }

    static Stream<Executable> checkFlattenedSerialization(final String serialized, final Graph graph) {
        return of(
                () -> assertTrue(serialized.contains("\"title\":\"A title\""), "missing/invalid dc:title value!"),
                () -> assertTrue(serialized.contains("\"@context\":"), "missing @context!"),
                () -> assertTrue(serialized.contains("\"@graph\":"), "unexpected @graph!"),
                () -> assertTrue(validateGraph(graph), "Not all triples present in output graph!"));
    }
}
