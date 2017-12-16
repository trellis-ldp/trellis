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

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.vocabulary.RDF.type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class ResourceServiceTest {

    private static final RDF rdf = new SimpleRDF();
    private static final IRI existing = rdf.createIRI("trellis:repository/existing");

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private static Resource mockResource;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        doCallRealMethod().when(mockResourceService).skolemize(any());
        doCallRealMethod().when(mockResourceService).unskolemize(any());
        doCallRealMethod().when(mockResourceService).getContainer(any());
        doCallRealMethod().when(mockResourceService).export(any());
        doCallRealMethod().when(mockResourceService).toInternal(any(), any());
        doCallRealMethod().when(mockResourceService).toExternal(any(), any());

        when(mockResourceService.scan()).thenAnswer(inv ->
            asList(rdf.createTriple(existing, type, LDP.Container)).stream());
    }

    @Test
    public void testSkolemization() {
        final BlankNode bnode = rdf.createBlankNode("testing");
        final IRI iri = rdf.createIRI("trellis:bnode/testing");
        final IRI resource = rdf.createIRI("trellis:repository/resource");

        assertTrue(mockResourceService.skolemize(bnode) instanceof IRI);
        assertTrue(((IRI) mockResourceService.skolemize(bnode)).getIRIString().startsWith("trellis:bnode/"));
        assertTrue(mockResourceService.unskolemize(iri) instanceof BlankNode);
        assertEquals(mockResourceService.unskolemize(iri), mockResourceService.unskolemize(iri));

        assertFalse(mockResourceService.unskolemize(rdf.createLiteral("Test")) instanceof BlankNode);
        assertFalse(mockResourceService.unskolemize(resource) instanceof BlankNode);
        assertFalse(mockResourceService.skolemize(rdf.createLiteral("Test2")) instanceof IRI);
    }

    @Test
    public void testExport() {
        final Set<IRI> graphs = new HashSet<>();
        graphs.add(Trellis.PreferAccessControl);
        graphs.add(Trellis.PreferAudit);
        graphs.add(Trellis.PreferServerManaged);
        graphs.add(Trellis.PreferUserManaged);
        when(mockResource.getIdentifier()).thenReturn(existing);
        when(mockResource.stream(eq(graphs))).thenAnswer(inv ->
                Stream.of(rdf.createTriple(existing, DC.title, rdf.createLiteral("A title"))));
        when(mockResourceService.get(eq(existing))).thenReturn(of(mockResource));

        final List<Quad> export = mockResourceService.export(graphs).collect(toList());
        assertEquals(1L, export.size());
        assertEquals(of(existing), export.get(0).getGraphName());
        assertEquals(existing, export.get(0).getSubject());
        assertEquals(DC.title, export.get(0).getPredicate());
        assertEquals(rdf.createLiteral("A title"), export.get(0).getObject());
    }

    @Test
    public void testGetContainer() {
        final IRI root = rdf.createIRI("trellis:repository");
        assertEquals(of(root), mockResourceService.getContainer(existing));
        assertFalse(mockResourceService.getContainer(root).isPresent());
    }

    @Test
    public void testInternalExternal() {
        final String baseUrl = "http://example.com/";
        final IRI external = rdf.createIRI(baseUrl + "repository/resource");
        final IRI internal = rdf.createIRI("trellis:repository/resource");
        final IRI other = rdf.createIRI("http://example.org/repository/resource");
        assertEquals(internal, mockResourceService.toInternal(external, baseUrl));
        assertEquals(external, mockResourceService.toExternal(internal, baseUrl));
        assertEquals(other, mockResourceService.toInternal(other, baseUrl));
        assertEquals(other, mockResourceService.toExternal(other, baseUrl));

        final BlankNode bnode = rdf.createBlankNode();
        assertEquals(bnode, mockResourceService.toInternal(bnode, baseUrl));
        assertEquals(bnode, mockResourceService.toExternal(bnode, baseUrl));

        final Literal literal = rdf.createLiteral("A literal");
        assertEquals(literal, mockResourceService.toInternal(literal, baseUrl));
        assertEquals(literal, mockResourceService.toExternal(literal, baseUrl));
    }
}
