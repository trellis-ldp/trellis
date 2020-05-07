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
package org.trellisldp.api;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
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
import org.trellisldp.vocabulary.Trellis;

class MetadataTest {

    private static final RDF rdf = RDFFactory.getInstance();
    private static final IRI identifier = rdf.createIRI("trellis:data/resource");
    private static final IRI member = rdf.createIRI("trellis:data/member");
    private static final IRI root = rdf.createIRI("trellis:data/");

    @Test
    void testMetadataIndirectContainer() {
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
        assertTrue(metadata.getMetadataGraphNames().isEmpty());
        assertFalse(metadata.getBinary().isPresent());
        assertEquals(of("blahblahblah"), metadata.getRevision());
    }

    @Test
    void testMetadataDirectContainer() {
        final Metadata metadata = Metadata.builder(identifier)
                .interactionModel(LDP.DirectContainer)
                .container(root).memberOfRelation(DC.isPartOf)
                .membershipResource(member).metadataGraphNames(singleton(Trellis.PreferAccessControl)).build();
        assertEquals(identifier, metadata.getIdentifier());
        assertEquals(LDP.DirectContainer, metadata.getInteractionModel());
        assertEquals(of(root), metadata.getContainer());
        assertEquals(of(member), metadata.getMembershipResource());
        assertEquals(of(DC.isPartOf), metadata.getMemberOfRelation());
        assertFalse(metadata.getInsertedContentRelation().isPresent());
        assertFalse(metadata.getMemberRelation().isPresent());
        assertFalse(metadata.getBinary().isPresent());
        assertTrue(metadata.getMetadataGraphNames().contains(Trellis.PreferAccessControl));
    }

    @Test
    void testMetadataBinary() {
        final BinaryMetadata binary = BinaryMetadata.builder(rdf.createIRI("http://example.com/binary")).build();
        final Metadata metadata = Metadata.builder(identifier)
                .interactionModel(LDP.NonRDFSource)
                .container(root).binary(binary).metadataGraphNames(emptySet()).build();
        assertEquals(identifier, metadata.getIdentifier());
        assertEquals(LDP.NonRDFSource, metadata.getInteractionModel());
        assertEquals(of(root), metadata.getContainer());
        assertEquals(of(binary), metadata.getBinary());
        assertFalse(metadata.getMembershipResource().isPresent());
        assertFalse(metadata.getMemberOfRelation().isPresent());
        assertFalse(metadata.getInsertedContentRelation().isPresent());
        assertFalse(metadata.getMemberRelation().isPresent());
        assertTrue(metadata.getMetadataGraphNames().isEmpty());
    }

    @Test
    void testMetadataResource() {
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
        assertTrue(metadata.getMetadataGraphNames().isEmpty());
    }

    @Test
    void testMetadataLdpNrResource() {
        final IRI binaryId = rdf.createIRI("http://example.com/binary");
        final String mimeType = "text/plain";
        final String revision = "lh2RdovVvkK3xY7mBpxnMwk7bas";
        final BinaryMetadata binary = BinaryMetadata.builder(binaryId).mimeType(mimeType).build();

        final Resource mockResource = mock(Resource.class);
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getBinaryMetadata()).thenReturn(of(binary));
        when(mockResource.getRevision()).thenReturn(revision);
        when(mockResource.getMetadataGraphNames()).thenReturn(singleton(Trellis.PreferAccessControl));

        final Metadata metadata = Metadata.builder(mockResource).build();
        assertEquals(identifier, metadata.getIdentifier());
        assertEquals(LDP.NonRDFSource, metadata.getInteractionModel());
        assertEquals(of(root), metadata.getContainer());
        assertEquals(of(binaryId), metadata.getBinary().map(BinaryMetadata::getIdentifier));
        assertEquals(of(mimeType), metadata.getBinary().flatMap(BinaryMetadata::getMimeType));
        assertFalse(metadata.getMembershipResource().isPresent());
        assertFalse(metadata.getMemberOfRelation().isPresent());
        assertFalse(metadata.getInsertedContentRelation().isPresent());
        assertFalse(metadata.getMemberRelation().isPresent());
        assertEquals(of(revision), metadata.getRevision());
        assertTrue(metadata.getMetadataGraphNames().contains(Trellis.PreferAccessControl));
    }
}
