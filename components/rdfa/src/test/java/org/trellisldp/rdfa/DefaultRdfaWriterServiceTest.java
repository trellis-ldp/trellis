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
package org.trellisldp.rdfa;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createBlankNode;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.vocabulary.DCTerms.spatial;
import static org.apache.jena.vocabulary.DCTerms.subject;
import static org.apache.jena.vocabulary.DCTerms.title;
import static org.apache.jena.vocabulary.DCTypes.Text;
import static org.apache.jena.vocabulary.RDF.Nodes.type;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.NoopNamespaceService;
import org.trellisldp.api.RDFaWriterService;

/**
 * @author acoburn
 */
class DefaultRdfaWriterServiceTest {

    private static final JenaRDF rdf = new JenaRDF();

    @Mock
    private NamespaceService mockNamespaceService;

    @Mock
    private OutputStream mockOutputStream;

    private RDFaWriterService service;

    @BeforeEach
    void setUp() {
        initMocks(this);
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("dc", DCTerms.NS);
        namespaces.put("rdf", RDF.uri);
        namespaces.put("dcmitype", "http://purl.org/dc/dcmitype/");
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        service = new DefaultRdfaWriterService(mockNamespaceService);

    }

    @Test
    void testWriteError() throws IOException {
        doThrow(new IOException()).when(mockOutputStream).write(any(byte[].class), anyInt(), anyInt());
        assertThrows(UncheckedIOException.class, () -> service.write(getTriples(), mockOutputStream, ""),
                "IOException in write operation doesn't cause failure!");
    }

    @Test
    void testDefaultSerializer() {
        final RDFaWriterService svc = new DefaultRdfaWriterService();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        svc.write(getTriples2(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlWithoutNamespaces(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testDefaultRdfaWriterService() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, null);
        assertAll("HTML check", checkHtmlFromTriples(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testDefaultRdfaWriterService2() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlFromTriples(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testDefaultRdfaWriterService3() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final RDFaWriterService service4 = new DefaultRdfaWriterService(mockNamespaceService, "/resource-test.mustache",
                "//www.trellisldp.org/assets/css/trellis.css", "", "//www.trellisldp.org/assets/img/trellis.png");

        service4.write(getTriples(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlFromTriples(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testDefaultRdfaWriterService4() throws Exception {
        final String path = getClass().getResource("/resource-test.mustache").toURI().getPath();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final RDFaWriterService service4 = new DefaultRdfaWriterService(mockNamespaceService, path,
                "//www.trellisldp.org/assets/css/trellis.css", "", null);
        service4.write(getTriples(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlFromTriples(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testDefaultRdfaWriterService5() throws Exception {
        final String path = getClass().getResource("/resource-test.mustache").toURI().getPath();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final RDFaWriterService service4 = new DefaultRdfaWriterService(new NoopNamespaceService(), path,
                null, (String) null, null);

        service4.write(getTriples2(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlWithoutNamespaces(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testHtmlNullSerializer() throws Exception {
        final String path = getClass().getResource("/resource-test.mustache").toURI().getPath();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final RDFaWriterService service6 = new DefaultRdfaWriterService(new NoopNamespaceService(), path,
                (List<String>) null, null, null);

        service6.write(getTriples2(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlWithoutNamespaces(new String(out.toByteArray(), UTF_8)));
    }

    private static Stream<Executable> checkHtmlWithoutNamespaces(final String html) {
        return of(
            () -> assertTrue(html.contains("<title>http://example.com/</title>"), "Title not in HTML!"),
            () -> assertTrue(html.contains("_:B"), "bnode value not in HTML!"),
            () -> assertTrue(
                    html.contains("<a href=\"http://sws.geonames.org/4929022/\">http://sws.geonames.org/4929022/</a>"),
                    "Geonames object IRI not in HTML!"),
            () -> assertTrue(
                    html.contains("<a href=\"http://purl.org/dc/terms/spatial\">http://purl.org/dc/terms/spatial</a>"),
                    "dc:spatial predicate not in HTML!"),
            () -> assertTrue(
                    html.contains("<a href=\"http://purl.org/dc/dcmitype/Text\">http://purl.org/dc/dcmitype/Text</a>"),
                    "dcmi type not in HTML output!"),
            () -> assertTrue(html.contains("<a href=\"mailto:user@example.com\">mailto:user@example.com</a>"),
                    "email IRI not in output!"),
            () -> assertTrue(html.contains("<h1>http://example.com/</h1>"), "Default title not in output!"));
    }

    private static Stream<Executable> checkHtmlFromTriples(final String html) {
        return of(
                () -> assertTrue(html.contains("<title>A title</title>"), "Title not in HTML!"),
                () -> assertTrue(html.contains("_:B"), "BNode not in HTML output!"),
                () -> assertTrue(
                    html.contains("<a href=\"http://sws.geonames.org/4929022/\">http://sws.geonames.org/4929022/</a>"),
                    "Geonames object IRI not in HTML!"),
                () -> assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/title\">dc:title</a>"),
                                 "dc:title predicate not in HTML!"),
                () -> assertTrue(html.contains("<a href=\"http://purl.org/dc/terms/spatial\">dc:spatial</a>"),
                                 "dc:spatial predicate not in HTML!"),
                () -> assertTrue(html.contains("<a href=\"http://purl.org/dc/dcmitype/Text\">dcmitype:Text</a>"),
                                 "dcmitype:Text object not in HTML!"),
                () -> assertTrue(html.contains("<h1>A title</h1>"), "Title value not in HTML!"));
    }

    private static Stream<Triple> getTriples() {
        final Node sub = createURI("trellis:data/resource");
        final Node bn = createBlankNode();
        return of(
                create(sub, title.asNode(), createLiteral("A title")),
                create(sub, subject.asNode(), bn),
                create(bn, title.asNode(), createLiteral("Other title")),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929023/")),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929024/")),
                create(sub, type, Text.asNode()))
            .map(rdf::asTriple);
    }

    private static Stream<Triple> getTriples2() {
        final Node sub = createURI("trellis:data/resource");
        final Node other = createURI("mailto:user@example.com");
        final Node bn = createBlankNode();
        return of(
                create(sub, subject.asNode(), bn),
                create(sub, spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(bn, type, Text.asNode()),
                create(bn, subject.asNode(), other),
                create(sub, type, Text.asNode()))
            .map(rdf::asTriple);
    }
}
