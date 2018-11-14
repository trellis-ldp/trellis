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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.vocabulary.LDP;

/**
 * @author acoburn
 */
public class ResourceServiceTest {

    private static final RDF rdf = new JenaRDF();
    private static final IRI existing = rdf.createIRI("trellis:data/existing");

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private static Resource mockResource;

    @Mock
    private RetrievalService<Resource> mockRetrievalService;

    private static class MyRetrievalService implements RetrievalService<Resource> {
        @Override
        public CompletableFuture<Resource> get(final IRI id) {
            return completedFuture(mockResource);
        }
    }

    @BeforeEach
    public void setUp() {
        initMocks(this);
        doCallRealMethod().when(mockResourceService).skolemize(any());
        doCallRealMethod().when(mockResourceService).unskolemize(any());
        doCallRealMethod().when(mockResourceService).toInternal(any(), any());
        doCallRealMethod().when(mockResourceService).toExternal(any(), any());
        doCallRealMethod().when(mockResourceService).create(any(), any());

        when(mockRetrievalService.get(eq(existing))).thenAnswer(inv -> completedFuture(mockResource));
    }

    @Test
    public void testRetrievalService2() {
        final RetrievalService<Resource> svc = new MyRetrievalService();
        assertEquals(mockResource, svc.get(existing).join(), "Incorrect resource returned by retrieval service!");
    }

    @Test
    public void testRetrievalService() {
        assertEquals(mockResource, mockRetrievalService.get(existing).join(),
                "Incorrect resource found by retrieval service!");
    }

    @Test
    public void testDefaultCreate() {
        final IRI root = rdf.createIRI("trellis:data/");
        final Dataset dataset = rdf.createDataset();
        final Metadata metadata = Metadata.builder(existing).container(root).interactionModel(LDP.RDFSource).build();

        when(mockResourceService.replace(eq(metadata), eq(dataset))).thenReturn(completedFuture(null));

        assertDoesNotThrow(() -> mockResourceService.create(metadata, dataset).join());
        verify(mockResourceService).replace(eq(metadata), eq(dataset));
    }

    @Test
    public void testSkolemization() {
        final BlankNode bnode = rdf.createBlankNode("testing");
        final IRI iri = rdf.createIRI("trellis:bnode/testing");
        final IRI resource = rdf.createIRI("trellis:data/resource");

        assertTrue(mockResourceService.skolemize(bnode) instanceof IRI, "Blank node not skolemized into IRI!");
        assertTrue(((IRI) mockResourceService.skolemize(bnode)).getIRIString().startsWith("trellis:bnode/"),
                "Skolem node has wrong prefix!");
        assertTrue(mockResourceService.unskolemize(iri) instanceof BlankNode, "Skolem IRI not unskolemized to bnode!");
        assertEquals(mockResourceService.unskolemize(iri), mockResourceService.unskolemize(iri),
                "Unskolemized bnodes (from the same skolem node) don't match!");
        assertFalse(mockResourceService.unskolemize(rdf.createLiteral("Test")) instanceof BlankNode,
                "Unskolemized literal transformed into blank node!");
        assertFalse(mockResourceService.unskolemize(resource) instanceof BlankNode,
                "Unskolemized resource IRI transformed into blank node!");
        assertFalse(mockResourceService.skolemize(rdf.createLiteral("Test2")) instanceof IRI,
                "Unskolemized literal transformed into IRI!");
    }

    @Test
    public void testInternalExternal() {
        final String baseUrl = "http://example.com/";
        final IRI external = rdf.createIRI(baseUrl + "resource");
        final IRI internal = rdf.createIRI("trellis:data/resource");
        final IRI other = rdf.createIRI("http://example.org/resource");
        assertEquals(internal, mockResourceService.toInternal(external, baseUrl), "Bad external->internal conversion!");
        assertEquals(external, mockResourceService.toExternal(internal, baseUrl), "Bad internal->external conversion!");
        assertEquals(other, mockResourceService.toInternal(other, baseUrl), "Bad conversion of out-of-domain resource");
        assertEquals(other, mockResourceService.toExternal(other, baseUrl), "Bad conversion of out-of-domain resource");

        final BlankNode bnode = rdf.createBlankNode();
        assertEquals(bnode, mockResourceService.toInternal(bnode, baseUrl), "Bad conversion of blank node!");
        assertEquals(bnode, mockResourceService.toExternal(bnode, baseUrl), "Bad conversion of blank node!");

        final Literal literal = rdf.createLiteral("A literal");
        assertEquals(literal, mockResourceService.toInternal(literal, baseUrl), "Bad conversion of literal!");
        assertEquals(literal, mockResourceService.toExternal(literal, baseUrl), "Bad conversion of literal!");
    }
}
