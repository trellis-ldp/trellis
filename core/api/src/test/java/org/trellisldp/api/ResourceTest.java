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

import static java.util.Collections.singleton;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
class ResourceTest {

    private static final RDF rdf = new SimpleRDF();

    private final IRI prefer = rdf.createIRI("http://example.org/prefer/Custom");

    @Mock
    private Resource mockResource;

    @BeforeEach
    void setUp() {
        initMocks(this);
        doCallRealMethod().when(mockResource).getMembershipResource();
        doCallRealMethod().when(mockResource).getMemberRelation();
        doCallRealMethod().when(mockResource).getMemberOfRelation();
        doCallRealMethod().when(mockResource).getInsertedContentRelation();
        doCallRealMethod().when(mockResource).getRevision();
        doCallRealMethod().when(mockResource).stream(any(IRI.class));
        doCallRealMethod().when(mockResource).stream(anyCollection());
        doCallRealMethod().when(mockResource).getBinaryMetadata();
        doCallRealMethod().when(mockResource).hasMetadata(any(IRI.class));
        doCallRealMethod().when(mockResource).getMetadataGraphNames();
        doCallRealMethod().when(mockResource).getExtraLinkRelations();
        doCallRealMethod().when(mockResource).dataset();

        when(mockResource.stream()).thenAnswer((x) -> empty());
    }

    @Test
    void testResource() {
        assertEquals(0L, mockResource.stream(prefer).count(), "Resource stream has extra triples!");
        assertEquals(0L, mockResource.stream(singleton(prefer)).count(), "Resource stream has extra triples!");
        assertNotNull(mockResource.getRevision(), "Resource revision should not be null!");
        assertFalse(mockResource.getMembershipResource().isPresent(), "Membership resource unexpectedly present!");
        assertFalse(mockResource.getMemberRelation().isPresent(), "Member relation unexpectedly present!");
        assertFalse(mockResource.getMemberOfRelation().isPresent(), "Member of relation unexpectedly present!");
        assertFalse(mockResource.getInsertedContentRelation().isPresent(), "Inserted content relation is present!");
        assertFalse(mockResource.getBinaryMetadata().isPresent(), "Binary is unexpectedly present!");
        assertFalse(mockResource.getExtraLinkRelations().findFirst().isPresent(), "Extra links unexpectedly present!");
        assertFalse(mockResource.hasMetadata(Trellis.PreferAccessControl), "ACL unexpectedly present!");
    }

    @Test
    void testResourceWithQuads() {
        final IRI subject = rdf.createIRI("ex:subject");
        when(mockResource.stream()).thenAnswer((x) -> of(
                    rdf.createQuad(prefer, subject, DC.title, rdf.createLiteral("A title")),
                    rdf.createQuad(PreferUserManaged, subject, DC.title, rdf.createLiteral("Other title"))));

        assertEquals(1L, mockResource.stream(prefer).count(), "Resource stream has wrong number of triples!");
        assertEquals(1L, mockResource.stream(singleton(prefer)).count(), "Resource has wrong number of triples!");
        assertEquals(2L, mockResource.dataset().size());
    }

    @Test
    void testSingletons() {
        assertEquals(MISSING_RESOURCE, MISSING_RESOURCE, "Missing resource singleton doesn't act like a singleton!");
        assertEquals(DELETED_RESOURCE, DELETED_RESOURCE, "Deleted resource singleton doesn't act like a singleton!");
        assertNotEquals(MISSING_RESOURCE, DELETED_RESOURCE, "Deleted and missing resources match each other!");
    }

    @Test
    void testMissingResource() {
        assertNull(MISSING_RESOURCE.getIdentifier(), "Missing resource has an identifier!");
        assertNull(MISSING_RESOURCE.getInteractionModel(), "Missing resource has an interaction model!");
        assertNull(MISSING_RESOURCE.getModified(), "Missing resource has a last modified date!");
        assertNull(MISSING_RESOURCE.getRevision(), "Missing resource has a non-null revision!");
        assertFalse(MISSING_RESOURCE.getContainer().isPresent(), "Missing resource has a parent resource!");
        assertEquals(0L, MISSING_RESOURCE.stream().count(), "Missing resource contains triples!");
        assertEquals("A non-existent resource", MISSING_RESOURCE.toString(), "Missing resource has wrong string repr.");
    }

    @Test
    void testDeletedResource() {
        assertNull(DELETED_RESOURCE.getIdentifier(), "Deleted resource has an identifier!");
        assertNull(DELETED_RESOURCE.getInteractionModel(), "Deleted resource has an interaction model!");
        assertNull(DELETED_RESOURCE.getModified(), "Deleted resource has a modification date!");
        assertNull(DELETED_RESOURCE.getRevision(), "Deleted resource has a non-null revision!");
        assertFalse(DELETED_RESOURCE.getContainer().isPresent(), "Deleted resource has a parent resource!");
        assertEquals(0L, DELETED_RESOURCE.stream().count(), "Deleted resource contains triples!");
        assertEquals("A deleted resource", DELETED_RESOURCE.toString(), "Deleted resource has wrong string repr.");
    }
}
