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
package org.trellisldp.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Optional;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.Trellis;

/**
 * Test the file utilities.
 */
@RunWith(JUnitPlatform.class)
public class FileUtilsTest {

    private static final RDF rdf = new JenaRDF();

    @Test
    public void testParseQuad() {
        final Optional<Quad> quad = FileUtils.parseQuad(
                "<trellis:data/resource> <http://purl.org/dc/terms/title> "
                + "\"Some title\" <http://www.trellisldp.org/ns/trellis#PreferUserManaged> .");
        assertTrue(quad.isPresent());
        quad.ifPresent(q -> {
            assertEquals("trellis:data/resource", ((IRI) q.getSubject()).getIRIString());
            assertEquals(DC.title, q.getPredicate());
            assertEquals("Some title", ((Literal) q.getObject()).getLexicalForm());
            assertTrue(q.getGraphName().isPresent());
            q.getGraphName().ifPresent(g -> assertEquals(Trellis.PreferUserManaged, g));
        });
    }

    @Test
    public void testParseQuadWithComment() {
        final Optional<Quad> quad = FileUtils.parseQuad(
                "<trellis:data/resource> <http://purl.org/dc/terms/title> "
                + "\"Some title\" <http://www.trellisldp.org/ns/trellis#PreferUserManaged> . # some comment");
        assertTrue(quad.isPresent());
        quad.ifPresent(q -> {
            assertEquals("trellis:data/resource", ((IRI) q.getSubject()).getIRIString());
            assertEquals(DC.title, q.getPredicate());
            assertEquals("Some title", ((Literal) q.getObject()).getLexicalForm());
            assertTrue(q.getGraphName().isPresent());
            q.getGraphName().ifPresent(g -> assertEquals(Trellis.PreferUserManaged, g));
        });
    }

    @Test
    public void testParseQuadNoGraph() {
        final Optional<Quad> quad = FileUtils.parseQuad(
                "<trellis:data/resource> <http://purl.org/dc/terms/title> "
                + "\"Some title\" .");
        assertTrue(quad.isPresent());
        quad.ifPresent(q -> {
            assertEquals("trellis:data/resource", ((IRI) q.getSubject()).getIRIString());
            assertEquals(DC.title, q.getPredicate());
            assertEquals("Some title", ((Literal) q.getObject()).getLexicalForm());
            assertFalse(q.getGraphName().isPresent());
        });
    }

    @Test
    public void testParseBadQuad() {
        assertFalse(FileUtils.parseQuad("blah blah blah").isPresent());
    }

    @Test
    public void testSerializeQuad() {
        final Quad quad = rdf.createQuad(Trellis.PreferServerManaged, rdf.createIRI("trellis:data/resource"),
                DC.subject, rdf.createIRI("http://example.org"));
        assertEquals("<trellis:data/resource> <http://purl.org/dc/terms/subject> <http://example.org> "
                + "<http://www.trellisldp.org/ns/trellis#PreferServerManaged> .", FileUtils.serializeQuad(quad));
    }

    @Test
    public void testSerializeQuadDefaultGraph() {
        final Quad quad = rdf.createQuad(null, rdf.createIRI("trellis:data/resource"),
                DC.subject, rdf.createIRI("http://example.org"));
        assertEquals("<trellis:data/resource> <http://purl.org/dc/terms/subject> <http://example.org> .",
                FileUtils.serializeQuad(quad));
    }

    @Test
    public void testDirectory() {
        final File dir = new File("/var/lib/trellis");
        final File resDir1 = FileUtils.getResourceDirectory(dir, rdf.createIRI("trellis:data/resource"));
        assertEquals("/var/lib/trellis/35/97/1a/f68d4d5afced3770fc13fb8e560dc253", resDir1.getAbsolutePath());
        final File resDir2 = FileUtils.getResourceDirectory(dir, rdf.createIRI("trellis:data/resource/2"));
        assertEquals("/var/lib/trellis/de/79/19/b43b345b666ac3dab33585181e216228", resDir2.getAbsolutePath());
    }

}
