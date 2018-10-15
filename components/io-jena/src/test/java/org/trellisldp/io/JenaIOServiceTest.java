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
package org.trellisldp.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Stream.of;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.NTRIPLES;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.RDFXML;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Syntax.LD_PATCH;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.vocabulary.JSONLD.compacted;
import static org.trellisldp.vocabulary.JSONLD.compacted_flattened;
import static org.trellisldp.vocabulary.JSONLD.expanded;
import static org.trellisldp.vocabulary.JSONLD.expanded_flattened;
import static org.trellisldp.vocabulary.JSONLD.flattened;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.trellisldp.api.CacheService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFaWriterService;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * @author acoburn
 */
public class JenaIOServiceTest {

    private static final JenaRDF rdf = new JenaRDF();
    private IOService service, service2, service3;

    @Mock
    private NamespaceService mockNamespaceService;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private OutputStream mockOutputStream;

    @Mock
    private RDFaWriterService mockHtmlSerializer;

    @Mock
    private CacheService<String, String> mockCache;

    @Mock
    private RDFSyntax mockSyntax;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        initMocks(this);
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("dcterms", DCTerms.NS);
        namespaces.put("rdf", RDF.uri);

        service = new JenaIOService(mockNamespaceService, null, mockCache,
                "http://www.w3.org/ns/anno.jsonld,,,", "http://www.trellisldp.org/ns/");

        service2 = new JenaIOService(mockNamespaceService, mockHtmlSerializer, mockCache, emptySet(),
                singleton("http://www.w3.org/ns/"));

        service3 = new JenaIOService(mockNamespaceService, null, mockCache, emptySet(), emptySet());

        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);
        when(mockCache.get(anyString(), any(Function.class))).thenAnswer(inv -> {
            final String key = inv.getArgument(0);
            final Function<String, String> mapper = inv.getArgument(1);
            return mapper.apply(key);
        });
    }

    @Test
    public void testJsonLdDefaultSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service3.write(getTriples(), out, JSONLD);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service3.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check expanded serialization", checkExpandedSerialization(output, graph));
    }

    @Test
    public void testJsonLdExpandedSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, expanded);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check expanded serialization", checkExpandedSerialization(output, graph));
    }

    @Test
    public void testJsonLdCustomSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.w3.org/ns/anno.jsonld"));
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"dcterms:title\":\"A title\""), "missing dcterms:title!");
        assertTrue(output.contains("\"type\":\"Text\""), "missing rdf:type Text!");
        assertTrue(output.contains("\"@context\":"), "missing @context!");
        assertTrue(output.contains("\"@context\":\"http://www.w3.org/ns/anno.jsonld\""), "Incorrect @context value!");
        assertFalse(output.contains("\"@graph\":"), "unexpected @graph!");

        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertTrue(validateGraph(graph), "Not all triples present in output graph!");
    }

    @Test
    public void testJsonLdCustomSerializerNoCache() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final IOService svc = new JenaIOService(mockNamespaceService, null, null,
                "http://www.w3.org/ns/anno.jsonld,,,", "http://www.trellisldp.org/ns/");

        svc.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.w3.org/ns/anno.jsonld"));
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check expanded serialization", checkExpandedSerialization(output, graph));
    }


    @Test
    public void testJsonLdCustomSerializer2() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service2.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.w3.org/ns/anno.jsonld"));
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
    public void testJsonLdCustomUnrecognizedSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.example.org/context.jsonld"));
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check expanded serialization", checkExpandedSerialization(output, graph));
    }

    @Test
    public void testJsonLdNullCache() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final IOService myservice = new JenaIOService();
        myservice.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.w3.org/ns/anno.jsonld"));
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        myservice.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check expanded serialization", checkExpandedSerialization(output, graph));
    }

    @Test
    public void testJsonLdCustomUnrecognizedSerializer2() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service2.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.example.org/context.jsonld"));
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service2.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check expanded serialization", checkExpandedSerialization(output, graph));
    }

    @Test
    public void testJsonLdCustomUnrecognizedSerializer3() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.trellisldp.org/ns/nonexistent.jsonld"));
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check compact serialization", checkCompactSerialization(output, graph));
    }

    @Test
    public void testJsonLdCompactedSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, compacted);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check compact serialization", checkCompactSerialization(output, graph));
    }

    @Test
    public void testJsonLdFlattenedSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, flattened);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check flattened serialization", checkFlattenedSerialization(output, graph));
    }

    @Test
    public void testJsonLdFlattenedSerializer2() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, compacted_flattened);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check flattened serialization", checkFlattenedSerialization(output, graph));
    }

    @Test
    public void testJsonLdFlattenedSerializer3() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, expanded_flattened);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check flattened serialization", checkFlattenedSerialization(output, graph));
    }

    @Test
    public void testJsonLdFlattenedSerializer4() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, compacted, flattened);
        final String output = out.toString("UTF-8");
        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), JSONLD, null).forEach(graph::add);
        assertAll("Check flattened serialization", checkFlattenedSerialization(output, graph));
    }

    @Test
    public void testMalformedInput() {
        final ByteArrayInputStream in = new ByteArrayInputStream("<> <ex:test> a Literal\" . ".getBytes(UTF_8));
        assertThrows(RuntimeTrellisException.class, () ->
                service.read(in, TURTLE, null), "No exception on malformed input!");
    }

    @Test
    public void testNTriplesSerializer() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service3.write(getTriples(), out, NTRIPLES);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.NTRIPLES);
        assertTrue(validateGraph(rdf.asGraph(graph)), "Failed round-trip for N-Triples!");
    }

    @Test
    public void testBufferedSerializer() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, RDFXML);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.RDFXML);
        assertTrue(validateGraph(rdf.asGraph(graph)), "Failed round-trip for RDFXML!");
    }

    @Test
    public void testTurtleSerializer() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, TURTLE);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.TURTLE);
        assertTrue(validateGraph(rdf.asGraph(graph)), "Failed round-trip for Turtle!");
    }

    @Test
    public void testTurtleReaderWithContext() {
        final Graph graph = rdf.createGraph();
        service.read(getClass().getResourceAsStream("/testRdf.ttl"), TURTLE, "trellis:data/resource")
            .forEach(graph::add);
        assertTrue(validateGraph(graph), "Failed round-trip for Turtle using a context value!");
    }

    @Test
    public void testHtmlSerializer() throws Exception {
        final IOService service4 = new JenaIOService(mockNamespaceService, mockHtmlSerializer);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service4.write(getComplexTriples(), out, RDFA, rdf.createIRI("http://example.org/"));
        verify(mockHtmlSerializer).write(any(), eq(out), eq("http://example.org/"));
    }

    @Test
    public void testHtmlSerializer2() throws Exception {
        final IOService service4 = new JenaIOService(mockNamespaceService, mockHtmlSerializer);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service4.write(getComplexTriples(), out, RDFA);
        verify(mockHtmlSerializer).write(any(), eq(out), eq(null));
    }

    @Test
    public void testNullHtmlSerializer() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, RDFA);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.TURTLE);
        assertTrue(validateGraph(rdf.asGraph(graph)), "null HTML serialization didn't default to Turtle!");
    }

    @Test
    public void testUpdateError() {
        final Graph graph = rdf.createGraph();
        getTriples().forEach(graph::add);
        assertEquals(3L, graph.size(), "Incorrect graph size!");
        assertThrows(RuntimeTrellisException.class, () ->
                service.update(graph, "blah blah blah blah blah", SPARQL_UPDATE, null), "no exception on bad update!");
    }

    @Test
    public void testReadError() throws IOException {
        doThrow(new IOException()).when(mockInputStream).read(any(byte[].class), anyInt(), anyInt());
        assertThrows(RuntimeTrellisException.class, () -> service.read(mockInputStream, TURTLE, "context"),
                "No read exception on bad input stream!");
    }

    @Test
    public void testWriteError() throws IOException {
        doThrow(new IOException()).when(mockOutputStream).write(any(byte[].class), anyInt(), anyInt());
        assertThrows(RuntimeTrellisException.class, () -> service.write(getTriples(), mockOutputStream, TURTLE),
                "No write exception on bad input stream!");
    }

    @Test
    public void testUpdate() {
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
    public void testUpdateInvalidSyntax() {
        final Graph graph = rdf.createGraph();
        getTriples().forEach(graph::add);
        final String patch = "UpdateList <#> <http://example.org/vocab#preferredLanguages> 1..2 ( \"fr\" ) .";
        assertThrows(RuntimeTrellisException.class, () -> service.update(graph, patch, LD_PATCH, null),
                "No exception thrown with invalid update syntax!");
    }

    @Test
    public void testWriteInvalidSyntax() {
        when(mockSyntax.mediaType()).thenReturn("fake/mediatype");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(RuntimeTrellisException.class, () -> service.write(getTriples(), out, mockSyntax),
                "No exception thrown with invalid write syntax!");
    }

    @Test
    public void testReadInvalidSyntax() {
        when(mockSyntax.mediaType()).thenReturn("fake/mediatype");
        final String output = "blah blah blah";

        assertThrows(RuntimeTrellisException.class, () ->
                service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), mockSyntax, null),
                "No exception thrown with invalid read syntax!");
    }

    @Test
    public void testReadSyntaxes() {
        assertTrue(service.supportedReadSyntaxes().contains(TURTLE), "Turtle not supported for reading!");
        assertTrue(service.supportedReadSyntaxes().contains(JSONLD), "JSON-LD not supported for reading!");
        assertTrue(service.supportedReadSyntaxes().contains(NTRIPLES), "N-Triples not supported for reading!");
        assertFalse(service.supportedReadSyntaxes().contains(RDFXML), "RDF/XML unexpectedly supported for reading!");
        assertFalse(service.supportedReadSyntaxes().contains(RDFA), "RDFa unexpectedly supported for reading!");
        assertTrue(service2.supportedReadSyntaxes().contains(RDFA), "RDFa not supported for reading!");
    }

    @Test
    public void testWriteSyntaxes() {
        assertTrue(service.supportedWriteSyntaxes().contains(TURTLE), "Turtle not supported for writing!");
        assertTrue(service.supportedWriteSyntaxes().contains(JSONLD), "JSON-LD not supported for writing!");
        assertTrue(service.supportedWriteSyntaxes().contains(NTRIPLES), "N-Triples not supported for writing!");
        assertFalse(service.supportedWriteSyntaxes().contains(RDFXML), "RDF/XML unexpectedly supported for writing!");
        assertFalse(service.supportedWriteSyntaxes().contains(RDFA), "RDFa unexpectedly supported for writing!");
        assertFalse(service2.supportedWriteSyntaxes().contains(RDFA), "RDFa not supported for writing!");
    }

    @Test
    public void testUpdateSyntaxes() {
        assertTrue(service.supportedUpdateSyntaxes().contains(SPARQL_UPDATE), "SPARQL-Update not supported!");
        assertFalse(service.supportedUpdateSyntaxes().contains(LD_PATCH), "LD-PATCH unexpectedly supported!");
    }

    private static Stream<Triple> getTriples() {
        final Node sub = createURI("trellis:data/resource");
        return of(
                create(sub, title.asNode(), createLiteral("A title")),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(sub, type, Text.asNode()))
            .map(rdf::asTriple);
    }

    private static Stream<Triple> getComplexTriples() {
        final Node sub = createURI("trellis:data/resource");
        final Node bn = createBlankNode();
        return of(
                create(sub, title.asNode(), createLiteral("A title")),
                create(sub, subject.asNode(), bn),
                create(bn, title.asNode(), createLiteral("Other title")),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(sub, type, Text.asNode()))
            .map(rdf::asTriple);

    }

    private static Boolean validateGraph(final Graph graph) {
        return getTriples().map(graph::contains).reduce(true, (acc, x) -> acc && x);
    }

    private static Stream<Executable> checkExpandedSerialization(final String serialized, final Graph graph) {
        return of(
                () -> assertTrue(serialized.contains("\"http://purl.org/dc/terms/title\":[{\"@value\":\"A title\"}]"),
                    "no expanded dc:title property!"),
                () -> assertFalse(serialized.contains("\"@context\":"), "unexpected @context!"),
                () -> assertFalse(serialized.contains("\"@graph\":"), "unexpected @graph!"),
                () -> assertTrue(validateGraph(graph), "Not all triples present in output graph!"));
    }

    private static Stream<Executable> checkCompactSerialization(final String serialized, final Graph graph) {
        return of(
                () -> assertTrue(serialized.contains("\"title\":\"A title\""), "missing/invalid dc:title value!"),
                () -> assertTrue(serialized.contains("\"@context\":"), "missing @context!"),
                () -> assertFalse(serialized.contains("\"@graph\":"), "unexpected @graph!"),
                () -> assertTrue(validateGraph(graph), "Not all triples present in output graph!"));
    }

    private static Stream<Executable> checkFlattenedSerialization(final String serialized, final Graph graph) {
        return of(
                () -> assertTrue(serialized.contains("\"title\":\"A title\""), "missing/invalid dc:title value!"),
                () -> assertTrue(serialized.contains("\"@context\":"), "missing @context!"),
                () -> assertTrue(serialized.contains("\"@graph\":"), "unexpected @graph!"),
                () -> assertTrue(validateGraph(graph), "Not all triples present in output graph!"));
    }
}
