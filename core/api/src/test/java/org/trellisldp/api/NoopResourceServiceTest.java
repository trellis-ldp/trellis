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

import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getInstance;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.LDP;

public class NoopResourceServiceTest {

    private static final ResourceService testService = new NoopResourceService();
    private static final RDF rdf = getInstance();
    private static final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");

    @Test
    public void testGetResource() {
        assertEquals(MISSING_RESOURCE, testService.get(identifier).toCompletableFuture().join());
    }

    @Test
    public void testReplaceResource() {
        final Metadata metadata = Metadata.builder(identifier).interactionModel(LDP.RDFSource).build();
        final Dataset dataset = rdf.createDataset();
        assertDoesNotThrow(() -> testService.replace(metadata, dataset).toCompletableFuture().join());
    }

    @Test
    public void testDeleteResource() {
        final Metadata metadata = Metadata.builder(identifier).interactionModel(LDP.RDFSource).build();
        assertDoesNotThrow(() -> testService.delete(metadata).toCompletableFuture().join());
    }

    @Test
    public void testAddResource() {
        final Dataset dataset = rdf.createDataset();
        assertDoesNotThrow(() -> testService.add(identifier, dataset).toCompletableFuture().join());
    }

    @Test
    public void testTouchResource() {
        assertDoesNotThrow(() -> testService.touch(identifier).toCompletableFuture().join());
    }

    @Test
    public void testSupportedModels() {
        assertTrue(testService.supportedInteractionModels().isEmpty());
    }

    @Test
    public void testGetIdentifier() {
        assertEquals(TRELLIS_DATA_PREFIX, testService.generateIdentifier());
    }
}

