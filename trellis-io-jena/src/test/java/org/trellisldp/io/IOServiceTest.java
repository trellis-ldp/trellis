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
import static java.util.Optional.empty;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.io.JenaIOService.IO_HTML_CSS;
import static org.trellisldp.io.JenaIOService.IO_HTML_ICON;
import static org.trellisldp.io.JenaIOService.IO_HTML_TEMPLATE;
import static org.trellisldp.io.JenaIOService.IO_JSONLD_DOMAINS;
import static org.trellisldp.io.JenaIOService.IO_JSONLD_PROFILES;
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
import org.mockito.Mock;
import org.trellisldp.api.CacheService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * @author acoburn
 */
public class IOServiceTest {

    private static final JenaRDF rdf = new JenaRDF();
    private IOService service, service2, service3;

    @Mock
    private NamespaceService mockNamespaceService;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private OutputStream mockOutputStream;

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

        final Map<String, String> properties = new HashMap<>();
        properties.put(IO_HTML_ICON, "//www.trellisldp.org/assets/img/trellis.png");
        properties.put(IO_HTML_CSS, "//www.trellisldp.org/assets/css/trellis.css");
        properties.put(IO_JSONLD_PROFILES, "http://www.w3.org/ns/anno.jsonld");
        properties.put(IO_JSONLD_DOMAINS, "http://www.trellisldp.org/ns/");

        service = new JenaIOService(mockNamespaceService, mockCache, properties);

        final Map<String, String> properties2 = new HashMap<>();
        properties.forEach(properties2::put);
        properties2.remove(IO_JSONLD_PROFILES);
        properties2.put(IO_JSONLD_DOMAINS, "http://www.w3.org/ns/");
        service2 = new JenaIOService(mockNamespaceService, mockCache, properties2);

        final Map<String, String> properties3 = new HashMap<>();
        service3 = new JenaIOService(mockNamespaceService, mockCache, properties3);

        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);
        when(mockNamespaceService.getPrefix(eq("http://purl.org/dc/terms/"))).thenReturn(Optional.of("dc"));
        when(mockNamespaceService.getPrefix(eq("http://sws.geonames.org/4929022/"))).thenReturn(empty());
        when(mockNamespaceService.getPrefix(eq("http://www.w3.org/1999/02/22-rdf-syntax-ns#")))
            .thenReturn(Optional.of("rdf"));
        when(mockNamespaceService.getPrefix(eq("http://purl.org/dc/dcmitype/")))
            .thenReturn(Optional.of("dcmitype"));
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
        assertTrue(output.contains("\"http://purl.org/dc/terms/title\":[{\"@value\":\"A title\"}]"));
        assertFalse(output.contains("\"@context\":"));
        assertFalse(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        service3.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testJsonLdExpandedSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, expanded);
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"http://purl.org/dc/terms/title\":[{\"@value\":\"A title\"}]"));
        assertFalse(output.contains("\"@context\":"));
        assertFalse(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testJsonLdCustomSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.w3.org/ns/anno.jsonld"));
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"dcterms:title\":\"A title\""));
        assertTrue(output.contains("\"type\":\"Text\""));
        assertTrue(output.contains("\"@context\":"));
        assertTrue(output.contains("\"@context\":\"http://www.w3.org/ns/anno.jsonld\""));
        assertFalse(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testJsonLdCustomSerializer2() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service2.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.w3.org/ns/anno.jsonld"));
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"dcterms:title\":\"A title\""));
        assertTrue(output.contains("\"type\":\"Text\""));
        assertTrue(output.contains("\"@context\":"));
        assertTrue(output.contains("\"@context\":\"http://www.w3.org/ns/anno.jsonld\""));
        assertFalse(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        service2.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testJsonLdCustomUnrecognizedSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.example.org/context.jsonld"));
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"http://purl.org/dc/terms/title\":[{\"@value\":\"A title\"}]"));
        assertFalse(output.contains("\"@context\":"));
        assertFalse(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testJsonLdNullCache() throws UnsupportedEncodingException {
        final Map<String, String> properties = new HashMap<>();
        properties.put(IO_HTML_ICON, "//www.trellisldp.org/assets/img/trellis.png");
        properties.put(IO_HTML_CSS, "//www.trellisldp.org/assets/css/trellis.css");
        properties.put(IO_JSONLD_DOMAINS, "http://www.w3.org/ns/");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final IOService myservice = new JenaIOService(null, null, properties);
        myservice.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.w3.org/ns/anno.jsonld"));
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"http://purl.org/dc/terms/title\":[{\"@value\":\"A title\"}]"));
        assertFalse(output.contains("\"@context\":"));
        assertFalse(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        myservice.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testJsonLdCustomUnrecognizedSerializer2() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service2.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.example.org/context.jsonld"));
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"http://purl.org/dc/terms/title\":[{\"@value\":\"A title\"}]"));
        assertFalse(output.contains("\"@context\":"));
        assertFalse(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        service2.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testJsonLdCustomUnrecognizedSerializer3() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, rdf.createIRI("http://www.trellisldp.org/ns/nonexistent.jsonld"));
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"title\":\"A title\""));
        assertTrue(output.contains("\"@context\":"));
        assertFalse(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testJsonLdCompactedSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, compacted);
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"title\":\"A title\""));
        assertTrue(output.contains("\"@context\":"));
        assertFalse(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testJsonLdFlattenedSerializer() throws UnsupportedEncodingException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, JSONLD, flattened);
        final String output = out.toString("UTF-8");
        assertTrue(output.contains("\"title\":\"A title\""));
        assertTrue(output.contains("\"@context\":"));
        assertTrue(output.contains("\"@graph\":"));

        final Graph graph = rdf.createGraph();
        service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, JSONLD).forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testMalformedInput() {
        final ByteArrayInputStream in = new ByteArrayInputStream("<> <ex:test> a Literal\" . ".getBytes(UTF_8));
        assertThrows(RuntimeTrellisException.class, () -> service.read(in, null, TURTLE));
    }

    @Test
    public void testNTriplesSerializer() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service3.write(getTriples(), out, NTRIPLES);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.NTRIPLES);
        validateGraph(rdf.asGraph(graph));
    }

    @Test
    public void testBufferedSerializer() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, RDFXML);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.RDFXML);
        validateGraph(rdf.asGraph(graph));
    }

    @Test
    public void testTurtleSerializer() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, TURTLE);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final org.apache.jena.graph.Graph graph = createDefaultGraph();
        RDFDataMgr.read(graph, in, Lang.TURTLE);
        validateGraph(rdf.asGraph(graph));
    }

    @Test
    public void testTurtleReaderWithContext() {
        final Graph graph = rdf.createGraph();
        service.read(getClass().getResourceAsStream("/testRdf.ttl"), "trellis:repository/resource", TURTLE)
            .forEach(graph::add);
        validateGraph(graph);
    }

    @Test
    public void testHtmlSerializer() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getComplexTriples(), out, RDFA);
        final String html = new String(out.toByteArray(), UTF_8);
        assertTrue(html.contains("<title>A title</title>"));
        assertTrue(html.contains("_:B"));
        assertTrue(html.contains("<a href=\"http://sws.geonames.org/4929022/\">http://sws.geonames.org/4929022/</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/title\">dc:title</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/spatial\">dc:spatial</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/dcmitype/Text\">dcmitype:Text</a>"));
        assertTrue(html.contains("<h1>A title</h1>"));
    }

    @Test
    public void testHtmlSerializer2() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getComplexTriples(), out, RDFA, rdf.createIRI("http://example.org/"));
        final String html = new String(out.toByteArray(), UTF_8);
        assertTrue(html.contains("<title>A title</title>"));
        assertTrue(html.contains("_:B"));
        assertTrue(html.contains("<a href=\"http://sws.geonames.org/4929022/\">http://sws.geonames.org/4929022/</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/title\">dc:title</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/spatial\">dc:spatial</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/dcmitype/Text\">dcmitype:Text</a>"));
        assertTrue(html.contains("<h1>A title</h1>"));
    }

    @Test
    public void testHtmlSerializer3() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(IO_HTML_ICON, "//www.trellisldp.org/assets/img/trellis.png");
        properties.put(IO_HTML_CSS, "//www.trellisldp.org/assets/css/trellis.css");
        properties.put(IO_HTML_TEMPLATE, "/resource-test.mustache");

        final IOService service4 = new JenaIOService(mockNamespaceService, mockCache, properties);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getComplexTriples(), out, RDFA, rdf.createIRI("http://example.org/"));
        final String html = new String(out.toByteArray(), UTF_8);
        assertTrue(html.contains("<title>A title</title>"));
        assertTrue(html.contains("_:B"));
        assertTrue(html.contains("<a href=\"http://sws.geonames.org/4929022/\">http://sws.geonames.org/4929022/</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/title\">dc:title</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/spatial\">dc:spatial</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/dcmitype/Text\">dcmitype:Text</a>"));
        assertTrue(html.contains("<h1>A title</h1>"));
    }

    @Test
    public void testHtmlSerializer4() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(IO_HTML_ICON, "//www.trellisldp.org/assets/img/trellis.png");
        properties.put(IO_HTML_CSS, "//www.trellisldp.org/assets/css/trellis.css");
        properties.put(IO_HTML_TEMPLATE, getClass().getResource("/resource-test.mustache").toURI().getPath());

        final IOService service4 = new JenaIOService(mockNamespaceService, mockCache, properties);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getComplexTriples(), out, RDFA, rdf.createIRI("http://example.org/"));
        final String html = new String(out.toByteArray(), UTF_8);
        assertTrue(html.contains("<title>A title</title>"));
        assertTrue(html.contains("_:B"));
        assertTrue(html.contains("<a href=\"http://sws.geonames.org/4929022/\">http://sws.geonames.org/4929022/</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/title\">dc:title</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/spatial\">dc:spatial</a>"));
        assertTrue(html.contains("<a href=\"http://purl.org/dc/dcmitype/Text\">dcmitype:Text</a>"));
        assertTrue(html.contains("<h1>A title</h1>"));
    }

    @Test
    public void testUpdateError() {
        final Graph graph = rdf.createGraph();
        getTriples().forEach(graph::add);
        assertEquals(3L, graph.size());
        assertThrows(RuntimeTrellisException.class, () -> service.update(graph, "blah blah blah blah blah", null));
    }

    @Test
    public void testReadError() throws IOException {
        doThrow(new IOException()).when(mockInputStream).read(any(byte[].class), anyInt(), anyInt());
        assertThrows(RuntimeTrellisException.class, () -> service.read(mockInputStream, "context", TURTLE));
    }

    @Test
    public void testWriteError() throws IOException {
        doThrow(new IOException()).when(mockOutputStream).write(any(byte[].class), anyInt(), anyInt());
        assertThrows(RuntimeTrellisException.class, () -> service.write(getTriples(), mockOutputStream, TURTLE));
    }

    @Test
    public void testUpdate() {
        final Graph graph = rdf.createGraph();
        getTriples().forEach(graph::add);
        assertEquals(3L, graph.size());
        service.update(graph, "DELETE WHERE { ?s <http://purl.org/dc/terms/title> ?o }", "test:info");
        assertEquals(2L, graph.size());
        service.update(graph, "INSERT { " +
                "<> <http://purl.org/dc/terms/title> \"Other title\" } WHERE {}",
                "trellis:repository/resource");
        assertEquals(3L, graph.size());
        service.update(graph, "DELETE WHERE { ?s ?p ?o };" +
                "INSERT { <> <http://purl.org/dc/terms/title> \"Other title\" } WHERE {}",
                "trellis:repository");
        assertEquals(1L, graph.size());
        assertEquals("<trellis:repository>", graph.stream().findFirst().map(Triple::getSubject)
                .map(RDFTerm::ntriplesString).get());
    }

    @Test
    public void testWriteInvalidSyntax() {
        when(mockSyntax.mediaType()).thenReturn("fake/mediatype");

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(RuntimeTrellisException.class, () -> service.write(getTriples(), out, mockSyntax));
    }

    @Test
    public void testReadInvalidSyntax() {
        when(mockSyntax.mediaType()).thenReturn("fake/mediatype");
        final String output = "blah blah blah";

        assertThrows(RuntimeTrellisException.class, () ->
                service.read(new ByteArrayInputStream(output.getBytes(UTF_8)), null, mockSyntax));
    }

    private static Stream<Triple> getTriples() {
        final Node sub = createURI("trellis:repository/resource");
        return of(
                create(sub, title.asNode(), createLiteral("A title")),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(sub, type, Text.asNode()))
            .map(rdf::asTriple);
    }

    private static Stream<Triple> getComplexTriples() {
        final Node sub = createURI("trellis:repository/resource");
        final Node bn = createBlankNode();
        return of(
                create(sub, title.asNode(), createLiteral("A title")),
                create(sub, subject.asNode(), bn),
                create(bn, title.asNode(), createLiteral("Other title")),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(sub, type, Text.asNode()))
            .map(rdf::asTriple);

    }

    private static void validateGraph(final Graph graph) {
        getTriples().forEach(triple -> {
            assertTrue(graph.contains(triple));
        });
    }
}
