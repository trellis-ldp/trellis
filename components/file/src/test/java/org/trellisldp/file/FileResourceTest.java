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
package org.trellisldp.file;

import static java.time.Instant.parse;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

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
class FileResourceTest {

    private static final RDF rdf = new JenaRDF();

    @Test
    void testResource() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File file = new File(getClass().getResource("/resource.nq").getFile());
        assertTrue(file.exists(), "Resource file doesn't exist!");
        final Resource res = new FileResource(identifier, file);

        assertEquals(identifier, res.getIdentifier(), "Incorrect identifier!");
        assertEquals(parse("2017-02-16T11:15:01Z"), res.getModified(), "Incorrect modification date!");
        assertEquals(LDP.BasicContainer, res.getInteractionModel(), "Incorrect interaction model!");
        assertFalse(res.getMembershipResource().isPresent(), "Unexpected ldp:membershipResource value!");
        assertFalse(res.getMemberRelation().isPresent(), "Unexpected ldp:memberRelation value!");
        assertFalse(res.getMemberOfRelation().isPresent(), "Unexpected ldp:isMemberOfRelation value!");
        assertFalse(res.getInsertedContentRelation().isPresent(), "Unexpected ldp:insertedContentRelation value!");
        assertFalse(res.getBinaryMetadata().isPresent(), "Unexpected binary present!");
        assertFalse(res.hasMetadata(Trellis.PreferAccessControl), "Unexpected ACL present!");
        assertFalse(res.getContainer().isPresent(), "Unexpected parent resource!");
        assertEquals(3L, res.stream(LDP.PreferContainment).count(), "Incorrect containment count!");
        assertEquals(3L, res.stream(Trellis.PreferUserManaged).count(), "Incorrect user triple count!");
        assertEquals(1L, res.stream(Trellis.PreferServerManaged).count(), "Incorrect server managed count!");
        assertEquals(7L, res.stream().count(), "Incorrect total triple count!");
    }

    @Test
    void testBinary() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "binary");
        final File file = new File(getClass().getResource("/binary.nq").getFile());
        assertTrue(file.exists(), "Resource file doesn't exist!");
        final Resource res = new FileResource(identifier, file);

        assertEquals(identifier, res.getIdentifier(), "Incorrect identifier!");
        assertEquals(parse("2017-02-16T11:17:00Z"), res.getModified(), "Incorrect modification date!");
        assertEquals(LDP.NonRDFSource, res.getInteractionModel(), "Incorrect interaction model!");
        assertFalse(res.getMembershipResource().isPresent(), "Unexpected ldp:membershipResource value!");
        assertFalse(res.getMemberRelation().isPresent(), "Unexpected ldp:memberRelation value!");
        assertFalse(res.getMemberOfRelation().isPresent(), "Unexpected ldp:isMemberOfRelation value!");
        assertFalse(res.getInsertedContentRelation().isPresent(), "Unexpected ldp:insertedContentRelation value!");
        assertTrue(res.getBinaryMetadata().isPresent(), "Missing binary metadata!");
        res.getBinaryMetadata().ifPresent(binary -> {
            assertEquals(of("text/plain"), binary.getMimeType(), "Incorrect binary mime type!");
            assertEquals(rdf.createIRI("file:///path/to/binary"), binary.getIdentifier(), "Incorrect binary id!");
        });
        assertFalse(res.getContainer().isPresent(), "Unexpected parent resource!");
        assertFalse(res.hasMetadata(Trellis.PreferAccessControl), "Unexpected ACL present!");
        assertEquals(0L, res.stream(LDP.PreferContainment).count(), "Incorrect containment triple count!");
        assertEquals(2L, res.stream(Trellis.PreferUserManaged).count(), "Incorrect user triple count!");
        assertEquals(1L, res.stream(Trellis.PreferServerManaged).count(), "Incorrect server managed count!");
        assertEquals(3L, res.stream().count(), "Incorrect total triple count!");
    }

    @Test
    void testInvalidFile() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File dir = new File(getClass().getResource("/resource.nq").getFile()).getParentFile();
        final File file = new File(dir, "nonexistent");
        assertFalse(file.exists(), "Non-existent file shouldn't exist!");
        final Resource res = new FileResource(identifier, file);
        assertEquals(identifier, res.getIdentifier(), "Incorrect identifier!");
        assertNull(res.getInteractionModel(), "Unexpected interaction model!");
        assertEquals(0L, res.stream().count(), "Incorrect total triple count!");
    }

    @Test
    void testIndirectContainer() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File file = new File(getClass().getResource("/ldpic.nq").getFile());
        assertTrue(file.exists(), "Resource file doesn't exist!");
        final Resource res = new FileResource(identifier, file);

        assertEquals(identifier, res.getIdentifier(), "Incorrect identifier!");
        assertEquals(parse("2017-02-16T11:15:01Z"), res.getModified(), "Incorrect modification date!");
        assertEquals(LDP.IndirectContainer, res.getInteractionModel(), "Incorrect interaction model!");
        assertTrue(res.getMembershipResource().isPresent(), "Missing ldp:membershipResource!");
        res.getMembershipResource().ifPresent(rel ->
                assertEquals(rdf.createIRI(TRELLIS_DATA_PREFIX + "members"), rel, "Incorrect ldp:membershipResource!"));
        assertTrue(res.getMemberRelation().isPresent(), "Missing ldp:memberRelation!");
        res.getMemberRelation().ifPresent(rel -> assertEquals(DC.subject, rel, "Incorrect ldp:memberRelation!"));
        assertTrue(res.getInsertedContentRelation().isPresent(), "Missing ldp:insertedContentRelation!");
        res.getInsertedContentRelation().ifPresent(rel ->
                assertEquals(DC.relation, rel, "Incorrect ldp:insertedContentRelation!"));
        assertFalse(res.getMemberOfRelation().isPresent(), "Unexpected ldp:isMemberOfRelation!");
        assertFalse(res.getBinaryMetadata().isPresent(), "Unexpected binary metadata!");
        assertFalse(res.hasMetadata(Trellis.PreferAccessControl), "Unexpected ACL!");
        assertEquals(3L, res.stream(LDP.PreferContainment).count(), "Incorrect containment triple count!");
        assertEquals(6L, res.stream(Trellis.PreferUserManaged).count(), "Incorrect user triple count!");
        assertEquals(1L, res.stream(Trellis.PreferServerManaged).count(), "Incorrect server triple count!");
        assertEquals(10L, res.stream().count(), "Incorrect total triple count!");
    }

    @Test
    void testDirectContainer() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final File file = new File(getClass().getResource("/ldpdc.nq").getFile());
        assertTrue(file.exists(), "Resource file doesn't exist!");
        final Resource res = new FileResource(identifier, file);

        assertEquals(identifier, res.getIdentifier(), "Incorrect identifier!");
        assertEquals(parse("2017-02-16T11:15:01Z"), res.getModified(), "Incorrect modification date!");
        assertEquals(LDP.DirectContainer, res.getInteractionModel(), "Incorrect interaction model!");
        assertTrue(res.getMembershipResource().isPresent(), "Missing ldp:membershipResource!");
        res.getMembershipResource().ifPresent(rel ->
                assertEquals(rdf.createIRI(TRELLIS_DATA_PREFIX + "members"), rel, "Incorrect ldp:membershipResource!"));
        assertFalse(res.getMemberRelation().isPresent(), "Unexpected ldp:memberRelation!");
        assertFalse(res.getInsertedContentRelation().isPresent(), "Unexpected ldp:insertedContentRelation!");
        assertTrue(res.getMemberOfRelation().isPresent(), "Missing ldp:isMemberOfRelation!");
        res.getMemberOfRelation().ifPresent(rel -> assertEquals(DC.isPartOf, rel, "Incorrect ldp:isMemberOfRelation!"));
        assertFalse(res.getBinaryMetadata().isPresent(), "Unexpected binary metadata!");
        assertFalse(res.hasMetadata(Trellis.PreferAccessControl), "Unexpected ACL!");
        assertEquals(3L, res.stream(LDP.PreferContainment).count(), "Incorrect containment triple count!");
        assertEquals(5L, res.stream(Trellis.PreferUserManaged).count(), "Incorrect user triple count!");
        assertEquals(1L, res.stream(Trellis.PreferServerManaged).count(), "Incorrect server triple count!");
        assertEquals(9L, res.stream().count(), "Incorrect total triple count!");
    }
}
