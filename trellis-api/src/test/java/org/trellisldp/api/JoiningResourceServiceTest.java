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
import static java.util.Collections.synchronizedMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.api.JoiningResourceService.RetrievableResource;
import org.trellisldp.vocabulary.LDP;

@RunWith(JUnitPlatform.class)
public class JoiningResourceServiceTest {

    private static IRI createIRI(final String value) {
        return RDFUtils.getInstance().createIRI(value);
    }

    private static Quad createQuad(final BlankNodeOrIRI g, final BlankNodeOrIRI s, final IRI p, final RDFTerm o) {
        return RDFUtils.getInstance().createQuad(g, s, p, o);
    }

    private static final IRI testResourceId1 = createIRI("http://example.com/1");
    private static final IRI testResourceId2 = createIRI("http://example.com/2");
    private static final IRI testResourceId3 = createIRI("http://example.com/3");
    private static final Session mockSession = mock(Session.class);

    private static IRI badId = createIRI("http://bad.com");

    private static class TestableRetrievalService implements RetrievalService<IRI, Resource> {

        protected final Map<IRI, Resource> resources = synchronizedMap(new HashMap<>());

        @Override
        public Optional<? extends Resource> get(final IRI identifier) {
            return Optional.ofNullable(resources.get(identifier));
        }

        protected CompletableFuture<Boolean> isntBadId(final IRI identifier) {
            return completedFuture(!identifier.equals(badId));
        }

    }

    private static class TestableImmutableService extends TestableRetrievalService
                    implements ImmutableDataService<IRI, Resource> {

        @Override
        public Future<Boolean> add(final IRI identifier, final Session session, final Resource newRes) {
            resources.compute(identifier, (id, old) -> old == null ? newRes : new RetrievableResource(old, newRes));
            return isntBadId(identifier);
        }
    }

    private final ImmutableDataService<IRI, Resource> testImmutableService = new TestableImmutableService();

    private static class TestableMutableDataService extends TestableRetrievalService
                    implements MutableDataService<IRI, Resource> {

        @Override
        public Future<Boolean> create(final IRI identifier, final Session session, final Resource resource) {
            resources.put(identifier, resource);
            return isntBadId(identifier);
        }

        @Override
        public Future<Boolean> replace(final IRI identifier, final Session session, final Resource resource) {
            resources.replace(identifier, resource);
            return isntBadId(identifier);
        }

        @Override
        public Future<Boolean> delete(final IRI identifier, final Session session, final Resource resource) {
            resources.remove(identifier);
            return isntBadId(identifier);
        }
    };

    private final MutableDataService<IRI, Resource> testMutableService = new TestableMutableDataService();

    private static class TestableJoiningResourceService extends JoiningResourceService {

        public TestableJoiningResourceService(final ImmutableDataService<IRI, Resource> immutableData,
                        final MutableDataService<IRI, Resource> mutableData) {
            super(mutableData, immutableData);
        }

        @Override
        public List<Range<Instant>> getMementos(final IRI identifier) {
            return Collections.emptyList();
        }

        @Override
        public Stream<IRI> compact(final IRI identifier, final Instant from, final Instant until) {
            return Stream.empty();
        }

        @Override
        public Stream<IRI> purge(final IRI identifier) {
            return Stream.empty();
        }

        @Override
        public Stream<? extends Triple> scan() {
            return Stream.empty();
        }

        @Override
        public String generateIdentifier() {
            return "new-identifier";
        }

    }

    private final ResourceService testable = new TestableJoiningResourceService(testImmutableService,
                    testMutableService);

    private static class TestResource implements Resource {

        private final Instant mod = Instant.now();
        private final Dataset dataset = RDFUtils.getInstance().createDataset();
        private final IRI id;

        public TestResource(final IRI id, final Quad... quads) {
            this.id = id;
            for (final Quad q : quads) {
                dataset.add(q);
            }
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
    public void testRoundtripping() throws InterruptedException, ExecutionException {
        final Quad testQuad = createQuad(testResourceId1, testResourceId1, testResourceId1, badId);
        final Resource testResource = new TestResource(testResourceId1, testQuad);
        assertTrue(testable.create(testResourceId1, mockSession, testResource).get(), "Couldn't create a resource!");
        Resource retrieved = testable.get(testResourceId1).orElseThrow(AssertionError::new);
        assertEquals(testResource.getIdentifier(), retrieved.getIdentifier(), "Resource was retrieved with wrong ID!");
        assertEquals(testResource.stream().findFirst().get(), retrieved.stream().findFirst().get(),
                        "Resource was retrieved with wrong data!");

        final Quad testQuad2 = createQuad(testResourceId1, badId, testResourceId1, badId);
        final Resource testResource2 = new TestResource(testResourceId1, testQuad2);
        assertTrue(testable.replace(testResourceId1, mockSession, testResource2).get(), "Couldn't replace resource!");
        retrieved = testable.get(testResourceId1).orElseThrow(AssertionError::new);
        assertEquals(testResource2.getIdentifier(), retrieved.getIdentifier(), "Resource was retrieved with wrong ID!");
        assertEquals(testResource2.stream().findFirst().get(), retrieved.stream().findFirst().get(),
                        "Resource was retrieved with wrong data!");

        assertTrue(testable.delete(testResourceId1, mockSession, testResource2).get(), "Couldn't delete resource!");
        assertFalse(testable.get(testResourceId1).isPresent(), "Found resource after deleting it!");
    }

    @Test
    public void testMergingBehavior() throws InterruptedException, ExecutionException {
        final Quad testMutableQuad = createQuad(testResourceId2, testResourceId2, testResourceId1, badId);
        final Quad testImmutableQuad = createQuad(testResourceId2, testResourceId2, testResourceId1, badId);

        // store some data in mutable and immutable sides under the same resource ID
        final Resource testMutableResource = new TestResource(testResourceId2, testMutableQuad);
        assertTrue(testable.create(testResourceId2, mockSession, testMutableResource).get(),
                "Couldn't create a mutable resource!");
        final Resource testImmutableResource = new TestResource(testResourceId2, testImmutableQuad);
        assertTrue(testable.add(testResourceId2, mockSession, testImmutableResource).get(),
                        "Couldn't create an immutable resource!");

        final Resource retrieved = testable.get(testResourceId2).orElseThrow(AssertionError::new);
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
    public void testBadPersist() throws InterruptedException, ExecutionException {
        final Quad testQuad = createQuad(badId, testResourceId1, testResourceId1, badId);
        final Resource testResource = new TestResource(badId, testQuad);
        assertFalse(testable.create(badId, mockSession, testResource).get(),
                        "Could create a resource when underlying services should reject it!");
    }

    @Test
    public void testAppendSemantics() throws InterruptedException, ExecutionException {
        final Quad testFirstQuad = createQuad(testResourceId3, testResourceId2, testResourceId1, badId);
        final Quad testSecondQuad = createQuad(testResourceId3, testResourceId2, testResourceId1, badId);

        // store some data in mutable and immutable sides under the same resource ID
        final Resource testFirstResource = new TestResource(testResourceId3, testFirstQuad);
        assertTrue(testable.add(testResourceId3, mockSession, testFirstResource).get(),
                "Couldn't create an immutable resource!");
        final Resource testSecondResource = new TestResource(testResourceId3, testSecondQuad);
        assertTrue(testable.add(testResourceId3, mockSession, testSecondResource).get(),
                "Couldn't add to an immutable resource!");

        final Resource retrieved = testable.get(testResourceId3).orElseThrow(AssertionError::new);
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
        assertEquals(LDP.RDFSource, res.getInteractionModel());
        assertEquals(time, res.getModified());
        assertTrue(res.hasAcl());
        assertTrue(res.stream().filter(quad::equals).findFirst().isPresent());
    }

    @Test
    public void testPersistableResource() {
        final Instant time = now();
        final IRI identifier = createIRI("trellis:identifier");
        final Quad quad = createQuad(testResourceId2, testResourceId2, testResourceId1, badId);
        final Dataset dataset = RDFUtils.getInstance().createDataset();
        dataset.add(quad);

        final Resource res = new JoiningResourceService.PersistableResource(identifier, LDP.Container, dataset);
        assertEquals(LDP.Container, res.getInteractionModel());
        assertFalse(res.getModified().isBefore(time));
        assertFalse(res.getModified().isAfter(now()));
        assertTrue(res.stream().filter(quad::equals).findFirst().isPresent());
        assertThrows(UnsupportedOperationException.class, () -> res.hasAcl());
    }
}
