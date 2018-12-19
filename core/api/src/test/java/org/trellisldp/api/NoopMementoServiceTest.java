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
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Stream.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;
import java.util.SortedSet;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Trellis;

public class NoopMementoServiceTest {

    private static final MementoService testService = new NoopMementoService();
    private static final RDF rdf = getInstance();
    private static final IRI identifier = rdf.createIRI("trellis:data/resource");
    private static final Instant time = now();
    private static final Quad quad = rdf.createQuad(Trellis.PreferUserManaged, identifier, type, SKOS.Concept);

    @Mock
    private Resource mockResource;

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private MementoService mockMementoService;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.stream()).thenAnswer(inv -> of(quad));
        doCallRealMethod().when(mockMementoService).put(any(ResourceService.class), any(IRI.class));
        when(mockResourceService.get(any(IRI.class))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockMementoService.put(any(Resource.class))).thenReturn(completedFuture(null));
    }

    @Test
    public void testPutDefaultMethod() {
        mockMementoService.put(mockResourceService, identifier).join();
        verify(mockResourceService).get(eq(identifier));
        verify(mockMementoService).put(eq(mockResource));
    }

    @Test
    public void testPutResourceServiceNoop() {
        testService.put(mockResourceService, identifier).join();
        verifyZeroInteractions(mockResourceService);
    }

    @Test
    public void testPutResourceNoop() {
        testService.put(mockResource).join();
        verifyZeroInteractions(mockResource);
    }

    @Test
    public void testGetResource() {
        assertEquals(MISSING_RESOURCE, testService.get(identifier, time).join());
    }

    @Test
    public void testMementos() {
        assertTrue(testService.mementos(identifier).thenApply(SortedSet::isEmpty).join());
    }
}
