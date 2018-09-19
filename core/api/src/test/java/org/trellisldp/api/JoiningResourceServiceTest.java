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

import static java.time.Instant.now;
import static java.util.Collections.emptySet;
import static java.util.Collections.synchronizedMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFTerm;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.JoiningResourceService.RetrievableResource;
import org.trellisldp.vocabulary.LDP;

public class JoiningResourceServiceTest {

    private static final IRI testResourceId1 = createIRI("http://example.com/1");
    private static final IRI testResourceId2 = createIRI("http://example.com/2");
    private static final IRI testResourceId3 = createIRI("http://example.com/3");

    private static IRI badId = createIRI("http://bad.com");

    private final ImmutableDataService<Resource> testImmutableService = new TestableImmutableService();

    private final MutableDataService<Resource> testMutableService = new TestableMutableDataService();

    private final ResourceService testable = new TestableJoiningResourceService(testImmutableService,
                    testMutableService);

    private static IRI createIRI(final String value) {
        return RDFUtils.getInstance().createIRI(value);
    }

    private static Quad createQuad(final BlankNodeOrIRI g, final BlankNodeOrIRI s, final IRI p, final RDFTerm o) {
        return RDFUtils.getInstance().createQuad(g, s, p, o);
    }

    private static class TestableRetrievalService implements RetrievalService<Resource> {

        protected final Map<IRI, Resource> resources = synchronizedMap(new HashMap<>());

        @Override
        public CompletableFuture<? extends Resource> get(final IRI identifier) {
            return completedFuture(resources.getOrDefault(identifier, MISSING_RESOURCE));
        }

        protected CompletableFuture<Void> isntBadId(final IRI identifier) {
            return runAsync(() -> {
                if (identifier.equals(badId)) {
                    throw new RuntimeTrellisException("Expected Exception");
                }
            });
        }

    }

    private static class TestableImmutableService extends TestableRetrievalService
                    implements ImmutableDataService<Resource> {

        @Override
        public CompletableFuture<Void> add(final IRI identifier, final Dataset dataset) {
            resources.compute(identifier, (id, old) -> {
                final TestResource newRes = new TestResource(id, dataset);
                return old == null ? newRes : new RetrievableResource(old, newRes);
            });
            return isntBadId(identifier);
        }
    }

    private static class TestableMutableDataService extends TestableRetrievalService
                    implements MutableDataService<Resource> {

        @Override
        public CompletableFuture<Void> create(final IRI id, final IRI ixnModel,
                final Dataset dataset, final IRI container, final Binary binary) {
            resources.put(id, new TestResource(id, dataset));
            return isntBadId(id);
        }

        @Override
        public CompletableFuture<Void> replace(final IRI id, final IRI ixnModel,
                final Dataset dataset, final IRI container, final Binary binary) {
            resources.replace(id, new TestResource(id, dataset));
            return isntBadId(id);
        }

        @Override
        public CompletableFuture<Void> delete(final IRI identifier, final IRI ixnModel,
                        final Dataset dataset) {
            resources.remove(identifier);
            return isntBadId(identifier);
        }
    };

    private static class TestableJoiningResourceService extends JoiningResourceService {

        public TestableJoiningResourceService(final ImmutableDataService<Resource> immutableData,
                        final MutableDataService<Resource> mutableData) {
            super(mutableData, immutableData);
        }

        @Override
        public String generateIdentifier() {
            return "new-identifier";
        }

        @Override
        public Set<IRI> supportedInteractionModels() {
            return emptySet();
        }
    }

    private static class TestResource implements Resource {

        private final Instant mod = now();
        private final Dataset dataset = RDFUtils.getInstance().createDataset();
        private final IRI id;

        public TestResource(final IRI id, final Quad... quads) {
            this.id = id;
            for (final Quad q : quads) {
                dataset.add(q);
            }
        }

        public TestResource(final IRI id, final Dataset quads) {
            this.id = id;
            quads.stream().forEach(dataset::add);
        }

        @Override
        public IRI getIdentifier() {
            return id;
        }

        @Override
        public IRI getInteractionModel() {
            return null;
        }

        @Override
        public Stream<? extends Quad> stream() {
            return dataset.stream();
        }

        @Override
        public Instant getModified() {
            return mod;
        }

        @Override
        public Boolean hasAcl() {
            return false;
        }

    }

    @Test
    public void testRoundtripping() {
        final Quad testQuad = createQuad(testResourceId1, testResourceId1, testResourceId1, badId);
        final Resource testResource = new TestResource(testResourceId1, testQuad);
        assertNull(testable.create(testResourceId1, testResource.getInteractionModel(),
                    testResource.dataset(), null, null).join(), "Couldn't create a resource!");
        Resource retrieved = testable.get(testResourceId1).join();
        assertEquals(testResource.getIdentifier(), retrieved.getIdentifier(), "Resource was retrieved with wrong ID!");
        assertEquals(testResource.stream().findFirst().get(), retrieved.stream().findFirst().get(),
                        "Resource was retrieved with wrong data!");

        final Quad testQuad2 = createQuad(testResourceId1, badId, testResourceId1, badId);
        final Resource testResource2 = new TestResource(testResourceId1, testQuad2);
        assertNull(testable.replace(testResourceId1, testResource2.getInteractionModel(),
                        testResource2.dataset(), null, null).join(), "Couldn't replace resource!");
        retrieved = testable.get(testResourceId1).join();
        assertEquals(testResource2.getIdentifier(), retrieved.getIdentifier(), "Resource was retrieved with wrong ID!");
        assertEquals(testResource2.stream().findFirst().get(), retrieved.stream().findFirst().get(),
                        "Resource was retrieved with wrong data!");

        assertNull(testable.delete(testResourceId1, testResource2.getInteractionModel(),
                        testResource2.dataset()).join(), "Couldn't delete resource!");
        assertEquals(MISSING_RESOURCE, testable.get(testResourceId1).join(), "Found resource after deleting it!");
    }

    @Test
    public void testMergingBehavior() {
        final Quad testMutableQuad = createQuad(testResourceId2, testResourceId2, testResourceId1, badId);
        final Quad testImmutableQuad = createQuad(testResourceId2, testResourceId2, testResourceId1, badId);

        // store some data in mutable and immutable sides under the same resource ID
        final Resource testMutableResource = new TestResource(testResourceId2, testMutableQuad);
        assertNull(testable.create(testResourceId2, testMutableResource.getInteractionModel(),
                        testMutableResource.dataset(), null, null).join(), "Couldn't create a mutable resource!");
        final Resource testImmutableResource = new TestResource(testResourceId2, testImmutableQuad);
        assertNull(testable.add(testResourceId2, testImmutableResource.dataset()).join(),
                        "Couldn't create an immutable resource!");

        final Resource retrieved = testable.get(testResourceId2).join();
        assertEquals(testMutableResource.getIdentifier(), retrieved.getIdentifier(),
                        "Resource was retrieved with wrong ID!");
        final Dataset quads = retrieved.dataset();
        assertTrue(quads.contains(testImmutableQuad), "Resource was retrieved without its immutable data!");
        assertTrue(quads.contains(testMutableQuad), "Resource was retrieved without its mutable data!");
        quads.remove(testImmutableQuad);
        quads.remove(testMutableQuad);
        assertEquals(0, quads.size(), "Resource was retrieved with too much data!");
    }

    @Test
    public void testBadPersist() {
        final Quad testQuad = createQuad(badId, testResourceId1, testResourceId1, badId);
        final Resource testResource = new TestResource(badId, testQuad);
        assertThrows(CompletionException.class, () -> testable.create(badId,
                    testResource.getInteractionModel(), testResource.dataset(),
                    null, null).join(), "Could create a resource when underlying services should reject it!");
    }

    @Test
    public void testAppendSemantics() {
        final Quad testFirstQuad = createQuad(testResourceId3, testResourceId2, testResourceId1, badId);
        final Quad testSecondQuad = createQuad(testResourceId3, testResourceId2, testResourceId1, badId);

        // store some data in mutable and immutable sides under the same resource ID
        final Resource testFirstResource = new TestResource(testResourceId3, testFirstQuad);
        assertNull(testable.add(testResourceId3, testFirstResource.dataset()).join(),
                        "Couldn't create an immutable resource!");
        final Resource testSecondResource = new TestResource(testResourceId3, testSecondQuad);
        assertNull(testable.add(testResourceId3, testSecondResource.dataset()).join(),
                        "Couldn't add to an immutable resource!");

        final Resource retrieved = testable.get(testResourceId3).join();
        assertEquals(testResourceId3, retrieved.getIdentifier(), "Resource was retrieved with wrong ID!");
        final Dataset quads = retrieved.dataset();
        assertTrue(quads.contains(testFirstQuad), "Resource was retrieved without its immutable data!");
        assertTrue(quads.contains(testSecondQuad), "Resource was retrieved without its mutable data!");
        quads.remove(testFirstQuad);
        quads.remove(testSecondQuad);
        assertEquals(0, quads.size(), "Resource was retrieved with too much data!");
    }

    @Test
    public void testRetrievableResource() {
        final Instant time = now();
        final Quad quad = createQuad(testResourceId2, testResourceId2, testResourceId1, badId);
        final Resource mockMutable = mock(Resource.class);

        when(mockMutable.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockMutable.getModified()).thenReturn(time);
        when(mockMutable.hasAcl()).thenReturn(true);
        when(mockMutable.stream()).thenAnswer(inv -> Stream.of(quad));

        final Resource res = new RetrievableResource(mockMutable, null);
        assertEquals(LDP.RDFSource, res.getInteractionModel(), "Resource retrieved with wrong interaction model!");
        assertEquals(time, res.getModified(), "Resource has wrong modified date!");
        assertTrue(res.hasAcl(), "Resource is missing ACL!");
        assertTrue(res.stream().anyMatch(quad::equals), "Expected quad not present in resource stream!");
    }

    @Test
    public void testPersistableResource() {
        final Instant time = now();
        final IRI identifier = createIRI("trellis:identifier");
        final Quad quad = createQuad(testResourceId2, testResourceId2, testResourceId1, badId);
        final Dataset dataset = RDFUtils.getInstance().createDataset();
        dataset.add(quad);

        final Resource res = new JoiningResourceService.PersistableResource(identifier, LDP.Container, dataset);
        assertEquals(identifier, res.getIdentifier(), "Resource has wrong ID!");
        assertEquals(LDP.Container, res.getInteractionModel(), "Resource has wrong LDP type!");
        assertFalse(res.getModified().isBefore(time), "Resource modification date predates its creation!");
        assertFalse(res.getModified().isAfter(now()), "Resource modification date is too late!");
        assertTrue(res.stream().anyMatch(quad::equals), "Expected quad not present in resource stream");
        assertThrows(UnsupportedOperationException.class, res::hasAcl, "ACL retrieval should throw an exception!");
    }
}
