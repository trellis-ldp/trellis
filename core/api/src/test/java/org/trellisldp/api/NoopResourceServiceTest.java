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

import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.LDP;

class NoopResourceServiceTest {

    private static final ResourceService testService = new NoopResourceService();
    private static final RDF rdf = RDFFactory.getInstance();
    private static final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");

    @Test
    void testGetResource() {
        assertEquals(MISSING_RESOURCE, testService.get(identifier).toCompletableFuture().join());
    }

    @Test
    void testReplaceResource() throws Exception {
        final Metadata metadata = Metadata.builder(identifier).interactionModel(LDP.RDFSource).build();
        try (final Dataset dataset = rdf.createDataset()) {
            assertDoesNotThrow(() -> testService.replace(metadata, dataset).toCompletableFuture().join());
        }
    }

    @Test
    void testDeleteResource() {
        final Metadata metadata = Metadata.builder(identifier).interactionModel(LDP.RDFSource).build();
        assertDoesNotThrow(() -> testService.delete(metadata).toCompletableFuture().join());
    }

    @Test
    void testAddResource() throws Exception {
        try (final Dataset dataset = rdf.createDataset()) {
            assertDoesNotThrow(() -> testService.add(identifier, dataset).toCompletableFuture().join());
        }
    }

    @Test
    void testTouchResource() {
        assertDoesNotThrow(() -> testService.touch(identifier).toCompletableFuture().join());
    }

    @Test
    void testSupportedModels() {
        assertTrue(testService.supportedInteractionModels().isEmpty());
    }

    @Test
    void testGetIdentifier() {
        assertEquals(TRELLIS_DATA_PREFIX, testService.generateIdentifier());
    }
}

