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

import static java.util.Collections.synchronizedMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.api.JoiningResourceService.RetrievableResource;

@RunWith(JUnitPlatform.class)
public class JoiningResourceServiceTest {

    private static IRI createIRI(String value) {
        return RDFUtils.getInstance().createIRI(value);
    }

    private static Quad createQuad(BlankNodeOrIRI g, BlankNodeOrIRI s, IRI p, RDFTerm o) {
        return RDFUtils.getInstance().createQuad(g, s, p, o);
    }

    private static final IRI testResourceId = createIRI("http://example.com");

    private static IRI badId = createIRI("http://bad.com");

    private static class TestableRetrievalService implements RetrievalService<IRI, Resource> {

        protected final Map<IRI, Resource> resources = synchronizedMap(new HashMap<>());

        @Override
        public Optional<? extends Resource> get(IRI identifier) {
            return Optional.ofNullable(resources.get(identifier));
        }

        protected CompletableFuture<Boolean> isntBadId(IRI identifier) {
            return completedFuture(!identifier.equals(badId));
        }

    }

    private static class TestableImmutableService extends TestableRetrievalService
                    implements ImmutableDataService<IRI, Resource> {

        @Override
        public Future<Boolean> add(IRI identifier, Resource newRes) {
            resources.compute(identifier, (id, old) -> old == null ? newRes : new RetrievableResource(old, newRes));
            return isntBadId(identifier);
        }
    }

    private ImmutableDataService<IRI, Resource> testImmutableService = new TestableImmutableService();

    private static class TestableMutableDataService extends TestableRetrievalService
                    implements MutableDataService<IRI, Resource> {

        @Override
        public Future<Boolean> create(IRI identifier, Resource resource) {
            resources.put(identifier, resource);
            return isntBadId(identifier);
        }

        @Override
        public Future<Boolean> replace(IRI identifier, Resource resource) {
            resources.replace(identifier, resource);
            return isntBadId(identifier);
        }

        @Override
        public Future<Boolean> delete(IRI identifier, Resource resource) {
            resources.remove(identifier);
            return isntBadId(identifier);
        }
    };

    private MutableDataService<IRI, Resource> testMutableService = new TestableMutableDataService();

    private static class TestableJoiningResourceService extends JoiningResourceService {

        public TestableJoiningResourceService(ImmutableDataService<IRI, Resource> immutableData,
                        MutableDataService<IRI, Resource> mutableData) {
            super(mutableData, immutableData);
        }

        @Override
        public List<Range<Instant>> getMementos(IRI identifier) {
            return Collections.emptyList();
        }

        @Override
        public Stream<IRI> compact(IRI identifier, Instant from, Instant until) {
            return Stream.empty();
        }

        @Override
        public Stream<IRI> purge(IRI identifier) {
            return Stream.empty();
        }

        @Override
        public Stream<? extends Triple> scan() {
            return Stream.empty();
        }

        @Override
        public Supplier<String> getIdentifierSupplier() {
            return String::new;
        }

    }

    private ResourceService testable = new TestableJoiningResourceService(testImmutableService, testMutableService);

    private static class TestResource implements Resource {

        private Instant mod = Instant.now();
        private Quad quad;
        private IRI id;

        public TestResource(IRI id, Quad quad) {
            this.id = id;
            this.quad = quad;
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
            return Stream.of(quad);
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
        Quad testQuad = createQuad(testResourceId, testResourceId, testResourceId, badId);
        Resource testResource = new TestResource(testResourceId, testQuad);
        assertTrue(testable.create(testResourceId, testResource).get(), "Couldn't create a resource!");
        Resource retrieved = testable.get(testResourceId).orElseThrow(AssertionError::new);
        assertEquals(testResource.getIdentifier(), retrieved.getIdentifier(), "Resource was retrieved with wrong ID!");
        assertEquals(testResource.stream().findFirst().get(), retrieved.stream().findFirst().get(),
                        "Resource was retrieved with wrong data!");

        Quad testQuad2 = createQuad(testResourceId, badId, testResourceId, badId);
        Resource testResource2 = new TestResource(testResourceId, testQuad2);
        assertTrue(testable.replace(testResourceId, testResource2).get(), "Couldn't replace resource!");
        retrieved = testable.get(testResourceId).orElseThrow(AssertionError::new);
        assertEquals(testResource2.getIdentifier(), retrieved.getIdentifier(), "Resource was retrieved with wrong ID!");
        assertEquals(testResource2.stream().findFirst().get(), retrieved.stream().findFirst().get(),
                        "Resource was retrieved with wrong data!");

        assertTrue(testable.delete(testResourceId, testResource2).get(), "Couldn't delete resource!");
        assertFalse(testable.get(testResourceId).isPresent(), "Found resource after deleting it!");

    }
}
