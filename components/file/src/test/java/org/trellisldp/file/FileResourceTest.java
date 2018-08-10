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

import static java.time.Instant.parse;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;

import java.io.File;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Test a file-based resource.
 */
public class FileResourceTest {

    private static final RDF rdf = new JenaRDF();

    @Test
    public void testResource() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File file = new File(getClass().getResource("/resource.nq").getFile());
        assertTrue(file.exists());
        final Resource res = new FileResource(identifier, file);

        assertEquals(identifier, res.getIdentifier());
        assertEquals(parse("2017-02-16T11:15:01Z"), res.getModified());
        assertEquals(LDP.BasicContainer, res.getInteractionModel());
        assertFalse(res.getMembershipResource().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.hasAcl());
        assertEquals(3L, res.stream(LDP.PreferContainment).count());
        assertEquals(3L, res.stream(Trellis.PreferUserManaged).count());
        assertEquals(2L, res.stream(Trellis.PreferServerManaged).count());
        assertEquals(8L, res.stream().count());
    }

    @Test
    public void testBinary() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "binary");
        final File file = new File(getClass().getResource("/binary.nq").getFile());
        assertTrue(file.exists());
        final Resource res = new FileResource(identifier, file);

        assertEquals(identifier, res.getIdentifier());
        assertEquals(parse("2017-02-16T11:17:00Z"), res.getModified());
        assertEquals(LDP.NonRDFSource, res.getInteractionModel());
        assertFalse(res.getMembershipResource().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertTrue(res.getBinary().isPresent());
        res.getBinary().ifPresent(binary -> {
            assertEquals(parse("2017-02-16T11:17:00Z"), binary.getModified());
            assertEquals(of(10L), binary.getSize());
            assertEquals(of("text/plain"), binary.getMimeType());
            assertEquals(rdf.createIRI("file:///path/to/binary"), binary.getIdentifier());
        });
        assertFalse(res.hasAcl());
        assertEquals(0L, res.stream(LDP.PreferContainment).count());
        assertEquals(2L, res.stream(Trellis.PreferUserManaged).count());
        assertEquals(6L, res.stream(Trellis.PreferServerManaged).count());
        assertEquals(8L, res.stream().count());
    }

    @Test
    public void testInvalidFile() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/resource.nq").getFile()).getParentFile();
        final File file = new File(dir, "nonexistent");
        assertFalse(file.exists());
        final Resource res = new FileResource(identifier, file);
        assertEquals(identifier, res.getIdentifier());
        assertNull(res.getInteractionModel());
        assertEquals(0L, res.stream().count());
    }

    @Test
    public void testIndirectContainer() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File file = new File(getClass().getResource("/ldpic.nq").getFile());
        assertTrue(file.exists());
        final Resource res = new FileResource(identifier, file);

        assertEquals(identifier, res.getIdentifier());
        assertEquals(parse("2017-02-16T11:15:01Z"), res.getModified());
        assertEquals(LDP.IndirectContainer, res.getInteractionModel());
        assertTrue(res.getMembershipResource().isPresent());
        res.getMembershipResource().ifPresent(rel -> assertEquals(rdf.createIRI(TRELLIS_DATA_PREFIX + "members"), rel));
        assertTrue(res.getMemberRelation().isPresent());
        res.getMemberRelation().ifPresent(rel -> assertEquals(DC.subject, rel));
        assertTrue(res.getInsertedContentRelation().isPresent());
        res.getInsertedContentRelation().ifPresent(rel -> assertEquals(DC.relation, rel));
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.hasAcl());
        assertEquals(3L, res.stream(LDP.PreferContainment).count());
        assertEquals(6L, res.stream(Trellis.PreferUserManaged).count());
        assertEquals(5L, res.stream(Trellis.PreferServerManaged).count());
        assertEquals(14L, res.stream().count());
    }

    @Test
    public void testDirectContainer() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File file = new File(getClass().getResource("/ldpdc.nq").getFile());
        assertTrue(file.exists());
        final Resource res = new FileResource(identifier, file);

        assertEquals(identifier, res.getIdentifier());
        assertEquals(parse("2017-02-16T11:15:01Z"), res.getModified());
        assertEquals(LDP.DirectContainer, res.getInteractionModel());
        assertTrue(res.getMembershipResource().isPresent());
        res.getMembershipResource().ifPresent(rel -> assertEquals(rdf.createIRI(TRELLIS_DATA_PREFIX + "members"), rel));
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertTrue(res.getMemberOfRelation().isPresent());
        res.getMemberOfRelation().ifPresent(rel -> assertEquals(DC.isPartOf, rel));
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.hasAcl());
        assertEquals(3L, res.stream(LDP.PreferContainment).count());
        assertEquals(5L, res.stream(Trellis.PreferUserManaged).count());
        assertEquals(4L, res.stream(Trellis.PreferServerManaged).count());
        assertEquals(12L, res.stream().count());
    }

    @Test
    public void testMementoResource() {
        final IRI memento = rdf.createIRI(TRELLIS_DATA_PREFIX + "memento");
        final File file = new File(getClass().getResource("/memento.nq").getFile());
        assertTrue(file.exists());
        final Resource res = new FileMementoResource(memento, file);

        assertEquals(memento, res.getIdentifier());
        assertEquals(parse("2017-01-14T11:00:01Z"), res.getModified());
        assertEquals(LDP.Container, res.getInteractionModel());
        assertFalse(res.hasAcl());
        assertFalse(res.getBinary().isPresent());
        assertFalse(res.getMemberRelation().isPresent());
        assertFalse(res.getMemberOfRelation().isPresent());
        assertFalse(res.getInsertedContentRelation().isPresent());
        assertFalse(res.getMembershipResource().isPresent());
        assertEquals(2L, res.stream(LDP.PreferContainment).count());
        assertEquals(3L, res.stream(Trellis.PreferUserManaged).count());
        assertEquals(2L, res.stream(Trellis.PreferServerManaged).count());
        assertEquals(7L, res.stream().count());
    }
}
