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
package org.trellisldp.rdfa;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createBlankNode;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Triple;
import org.apache.jena.commonsrdf.JenaCommonsRDF;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DCTypes;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.NoopNamespaceService;

/**
 * @author acoburn
 */
@ExtendWith(MockitoExtension.class)
class DefaultRdfaWriterServiceTest {

    private final Map<String, String> namespaces = new HashMap<>();

    @Mock
    private NamespaceService mockNamespaceService;

    @Mock
    private OutputStream mockOutputStream;

    private DefaultRdfaWriterService service;

    @BeforeEach
    void setUp() {
        namespaces.put("dc", DCTerms.NS);
        namespaces.put("rdf", RDF.uri);
        namespaces.put("dcmitype", "http://purl.org/dc/dcmitype/");

        service = new DefaultRdfaWriterService();
        service.namespaceService = mockNamespaceService;
        service.icon = Optional.of("//www.trellisldp.org/assets/img/trellis.png");
        service.css = Optional.of(new String[]{"//www.trellisldp.org/assets/css/trellis.css"});
        service.js = Optional.empty();
        service.templateLocation = "/org/trellisldp/rdf/resource.mustache";
        service.init();
    }

    @Test
    void testWriteError() throws IOException {
        doThrow(new IOException()).when(mockOutputStream).write(any(byte[].class), anyInt(), anyInt());
        final Stream<Triple> triples = getTriples();
        assertThrows(UncheckedIOException.class, () -> service.write(triples, mockOutputStream, ""),
                "IOException in write operation doesn't cause failure!");
    }

    @Test
    void testDefaultSerializer() {
        final DefaultRdfaWriterService svc = new DefaultRdfaWriterService();
        svc.namespaceService = new NoopNamespaceService();
        svc.icon = Optional.of("//www.trellisldp.org/assets/img/trellis.png");
        svc.css = Optional.empty();
        svc.js = Optional.empty();
        svc.templateLocation = "/org/trellisldp/rdf/resource.mustache";
        svc.init();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        svc.write(getTriples2(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlWithoutNamespaces(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testDefaultRdfaWriterService() {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, null);
        assertAll("HTML check", checkHtmlFromTriples(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testDefaultRdfaWriterService2() {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(getTriples(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlFromTriples(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testDefaultRdfaWriterService4() throws Exception {
        when(mockNamespaceService.getNamespaces()).thenReturn(namespaces);

        final String path = getClass().getResource("/resource-test.mustache").toURI().getPath();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DefaultRdfaWriterService svc4 = new DefaultRdfaWriterService();
        svc4.namespaceService = mockNamespaceService;
        svc4.icon = Optional.empty();
        svc4.css = Optional.empty();
        svc4.js = Optional.empty();
        svc4.templateLocation = "/org/trellisldp/rdf/resource.mustache";
        svc4.init();

        svc4.write(getTriples(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlFromTriples(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testDefaultRdfaWriterService5() throws Exception {
        final String path = getClass().getResource("/resource-test.mustache").toURI().getPath();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DefaultRdfaWriterService svc5 = new DefaultRdfaWriterService();
        svc5.namespaceService = new NoopNamespaceService();
        svc5.icon = Optional.empty();
        svc5.css = Optional.empty();
        svc5.js = Optional.empty();
        svc5.templateLocation = path;
        svc5.init();

        svc5.write(getTriples2(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlWithoutNamespaces(new String(out.toByteArray(), UTF_8)));
    }

    @Test
    void testHtmlNullSerializer() throws Exception {
        final String path = getClass().getResource("/resource-test.mustache").toURI().getPath();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DefaultRdfaWriterService svc6 = new DefaultRdfaWriterService();
        svc6.namespaceService = new NoopNamespaceService();
        svc6.icon = Optional.empty();
        svc6.css = Optional.empty();
        svc6.js = Optional.empty();
        svc6.templateLocation = path;
        svc6.init();

        svc6.write(getTriples2(), out, "http://example.com/");
        assertAll("HTML check", checkHtmlWithoutNamespaces(new String(out.toByteArray(), UTF_8)));
    }

    static Stream<Executable> checkHtmlWithoutNamespaces(final String html) {
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

    static Stream<Executable> checkHtmlFromTriples(final String html) {
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

    static Stream<Triple> getTriples() {
        final Node sub = createURI("trellis:data/resource");
        final Node bn = createBlankNode();
        return of(
                create(sub, DCTerms.title.asNode(), createLiteral("A title")),
                create(sub, DCTerms.subject.asNode(), bn),
                create(bn, DCTerms.title.asNode(), createLiteral("Other title")),
                create(sub, DCTerms.spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(sub, DCTerms.spatial.asNode(), createURI("http://sws.geonames.org/4929023/")),
                create(sub, DCTerms.spatial.asNode(), createURI("http://sws.geonames.org/4929024/")),
                create(sub, RDF.type.asNode(), DCTypes.Text.asNode()))
            .map(JenaCommonsRDF::fromJena);
    }

    static Stream<Triple> getTriples2() {
        final Node sub = createURI("trellis:data/resource");
        final Node other = createURI("mailto:user@example.com");
        final Node bn = createBlankNode();
        return of(
                create(sub, DCTerms.subject.asNode(), bn),
                create(sub, DCTerms.spatial.asNode(), createURI("http://sws.geonames.org/4929022/")),
                create(bn, RDF.type.asNode(), DCTypes.Text.asNode()),
                create(bn, DCTerms.subject.asNode(), other),
                create(sub, RDF.type.asNode(), DCTypes.Text.asNode()))
            .map(JenaCommonsRDF::fromJena);
    }
}
