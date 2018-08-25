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
import static java.util.stream.Stream.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;

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
    private static final Quad quad = rdf.createQuad(Trellis.PreferUserManaged,
            identifier, type, SKOS.Concept);

    @Mock
    private Resource mockResource;

    @Mock
    private MementoService mockMementoService;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.stream()).thenAnswer(inv -> of(quad));
        doCallRealMethod().when(mockMementoService).put(eq(mockResource));
    }

    @Test
    public void noAction() {
        testService.put(identifier, time, of(quad));

        assertEquals(MISSING_RESOURCE, testService.get(identifier, time).join(), "No-op service found a Memento!");
        assertTrue(testService.list(identifier).join().isEmpty(), "No-op service found a list of Mementos!");
        assertNull(testService.delete(identifier, time).join(), "No-op service responded incorrectly to a delete!");
    }

    @Test
    public void testPutResourceNoop() {
        testService.put(mockResource);
        verify(mockResource, never().description("getIdentifier was called in no-op service!")).getIdentifier();
        verify(mockResource, never().description("getModified was called in no-op service!")).getModified();
        verify(mockResource, never().description("stream was called in no-op service!")).stream();
    }

    @Test
    public void testPutResource() {
        mockMementoService.put(mockResource);
        verify(mockResource, description("getIdentifier was never called!")).getIdentifier();
        verify(mockResource, description("getModified was never called!")).getModified();
        verify(mockResource, description("stream was never called!")).stream();
    }
}
