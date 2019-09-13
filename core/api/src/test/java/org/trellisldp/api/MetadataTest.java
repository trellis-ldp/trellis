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
package org.trellisldp.api;

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;

public class MetadataTest {

    private static final RDF rdf = TrellisUtils.getInstance();
    private static final IRI identifier = rdf.createIRI("trellis:data/resource");
    private static final IRI member = rdf.createIRI("trellis:data/member");
    private static final IRI root = rdf.createIRI("trellis:data/");

    @Test
    public void testMetadataIndirectContainer() {
        final Metadata metadata = Metadata.builder(identifier)
                .interactionModel(LDP.IndirectContainer)
                .container(root).memberRelation(LDP.member)
                .membershipResource(member)
                .insertedContentRelation(FOAF.primaryTopic)
                .revision("blahblahblah").build();
        assertEquals(identifier, metadata.getIdentifier());
        assertEquals(LDP.IndirectContainer, metadata.getInteractionModel());
        assertEquals(of(root), metadata.getContainer());
        assertEquals(of(member), metadata.getMembershipResource());
        assertEquals(of(LDP.member), metadata.getMemberRelation());
        assertEquals(of(FOAF.primaryTopic), metadata.getInsertedContentRelation());
        assertFalse(metadata.getMemberOfRelation().isPresent());
        assertFalse(metadata.getHasAcl());
        assertFalse(metadata.getBinary().isPresent());
        assertEquals(of("blahblahblah"), metadata.getRevision());
    }

    @Test
    public void testMetadataDirectContainer() {
        final Metadata metadata = Metadata.builder(identifier)
                .interactionModel(LDP.DirectContainer)
                .container(root).memberOfRelation(DC.isPartOf)
                .membershipResource(member).hasAcl(true).build();
        assertEquals(identifier, metadata.getIdentifier());
        assertEquals(LDP.DirectContainer, metadata.getInteractionModel());
        assertEquals(of(root), metadata.getContainer());
        assertEquals(of(member), metadata.getMembershipResource());
        assertEquals(of(DC.isPartOf), metadata.getMemberOfRelation());
        assertFalse(metadata.getInsertedContentRelation().isPresent());
        assertFalse(metadata.getMemberRelation().isPresent());
        assertFalse(metadata.getBinary().isPresent());
        assertTrue(metadata.getHasAcl());
    }

    @Test
    public void testMetadataBinary() {
        final BinaryMetadata binary = BinaryMetadata.builder(rdf.createIRI("http://example.com/binary")).build();
        final Metadata metadata = Metadata.builder(identifier)
                .interactionModel(LDP.NonRDFSource)
                .container(root).binary(binary).hasAcl(false).build();
        assertEquals(identifier, metadata.getIdentifier());
        assertEquals(LDP.NonRDFSource, metadata.getInteractionModel());
        assertEquals(of(root), metadata.getContainer());
        assertEquals(of(binary), metadata.getBinary());
        assertFalse(metadata.getMembershipResource().isPresent());
        assertFalse(metadata.getMemberOfRelation().isPresent());
        assertFalse(metadata.getInsertedContentRelation().isPresent());
        assertFalse(metadata.getMemberRelation().isPresent());
        assertFalse(metadata.getHasAcl());
    }

    @Test
    public void testMetadataResource() {
        final Resource mockResource = mock(Resource.class);
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getIdentifier()).thenReturn(identifier);

        final Metadata metadata = Metadata.builder(mockResource).build();
        assertEquals(identifier, metadata.getIdentifier());
        assertEquals(LDP.RDFSource, metadata.getInteractionModel());
        assertEquals(of(root), metadata.getContainer());
        assertFalse(metadata.getBinary().isPresent());
        assertFalse(metadata.getMembershipResource().isPresent());
        assertFalse(metadata.getMemberOfRelation().isPresent());
        assertFalse(metadata.getInsertedContentRelation().isPresent());
        assertFalse(metadata.getMemberRelation().isPresent());
        assertFalse(metadata.getRevision().isPresent());
        assertFalse(metadata.getHasAcl());
    }
}
