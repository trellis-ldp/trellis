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

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDFS;

/**
 * @author acoburn
 */
public class ConstraintServiceTest {

    private static final RDF rdf = RDFUtils.getInstance();

    /**
     * A mapping of LDP types to their supertype
     */
    private static final Map<IRI, IRI> superClassOf;

    static {
        final Map<IRI, IRI> data = new HashMap<>();
        data.put(LDP.NonRDFSource, LDP.Resource);
        data.put(LDP.RDFSource, LDP.Resource);
        data.put(LDP.Container, LDP.RDFSource);
        data.put(LDP.BasicContainer, LDP.Container);
        data.put(LDP.DirectContainer, LDP.Container);
        data.put(LDP.IndirectContainer, LDP.Container);
        superClassOf = unmodifiableMap(data);
    }

    private static Stream<IRI> ldpResourceTypes(final IRI interactionModel) {
        return of(interactionModel).filter(type -> superClassOf.containsKey(type) || LDP.Resource.equals(type))
            .flatMap(type -> concat(ldpResourceTypes(superClassOf.get(type)), of(type)));
    }

    @Test
    public void testResource() {
        assertEquals(1, ldpResourceTypes(LDP.Resource).count());
        final Set<IRI> types = ldpResourceTypes(LDP.Resource).collect(toSet());
        assertEquals(1, types.size());
        assertTrue(types.contains(LDP.Resource));
    }

    @Test
    public void testNonLDP() {
        assertEquals(0, ldpResourceTypes(RDFS.label).count());
    }

    @Test
    public void testRDFSource() {
        assertEquals(2, ldpResourceTypes(LDP.RDFSource).count());
        final Set<IRI> types = ldpResourceTypes(LDP.RDFSource).collect(toSet());
        assertEquals(2, types.size());
        assertTrue(types.contains(LDP.Resource));
        assertTrue(types.contains(LDP.RDFSource));
    }

    @Test
    public void testNonRDFSource() {
        assertEquals(2, ldpResourceTypes(LDP.NonRDFSource).count());
        final Set<IRI> types = ldpResourceTypes(LDP.NonRDFSource).collect(toSet());
        assertEquals(2, types.size());
        assertTrue(types.contains(LDP.Resource));
        assertTrue(types.contains(LDP.NonRDFSource));
    }

    @Test
    public void testContainer() {
        assertEquals(3, ldpResourceTypes(LDP.Container).count());
        final Set<IRI> types = ldpResourceTypes(LDP.Container).collect(toSet());
        assertEquals(3, types.size());
        assertTrue(types.contains(LDP.Resource));
        assertTrue(types.contains(LDP.RDFSource));
        assertTrue(types.contains(LDP.Container));
    }

    @Test
    public void testBasicContainer() {
        assertEquals(4, ldpResourceTypes(LDP.BasicContainer).count());
        final Set<IRI> types = ldpResourceTypes(LDP.BasicContainer).collect(toSet());
        assertEquals(4, types.size());
        assertTrue(types.contains(LDP.Resource));
        assertTrue(types.contains(LDP.RDFSource));
        assertTrue(types.contains(LDP.Container));
        assertTrue(types.contains(LDP.BasicContainer));
    }

    @Test
    public void testDirectContainer() {
        assertEquals(4, ldpResourceTypes(LDP.DirectContainer).count());
        final Set<IRI> types = ldpResourceTypes(LDP.DirectContainer).collect(toSet());
        assertEquals(4, types.size());
        assertTrue(types.contains(LDP.Resource));
        assertTrue(types.contains(LDP.RDFSource));
        assertTrue(types.contains(LDP.Container));
        assertTrue(types.contains(LDP.DirectContainer));
    }

    @Test
    public void testIndirectContainer() {
        assertEquals(4, ldpResourceTypes(LDP.IndirectContainer).count());
        final Set<IRI> types = ldpResourceTypes(LDP.IndirectContainer).collect(toSet());
        assertEquals(4, types.size());
        assertTrue(types.contains(LDP.Resource));
        assertTrue(types.contains(LDP.RDFSource));
        assertTrue(types.contains(LDP.Container));
        assertTrue(types.contains(LDP.IndirectContainer));
    }

    @Test
    public void testConstrainedBy() {
        final ConstraintService svc = mock(ConstraintService.class);

        doCallRealMethod().when(svc).constrainedBy(eq(LDP.RDFSource), any(Graph.class));
        when(svc.constrainedBy(any(IRI.class), any(Graph.class), eq(RDFUtils.TRELLIS_DATA_PREFIX)))
            .thenAnswer(inv -> Stream.empty());

        assertEquals(0L, svc.constrainedBy(LDP.RDFSource, rdf.createGraph()).count());
    }
}
