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

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
class ConstraintServiceTest {

    private static final RDF rdf = RDFFactory.getInstance();
    private static final String INCORRECT_LDP_TYPES = "Incorrect number of LDP types found";
    private static final String INCORRECT_UNIQUE_LDP_TYPES = "Incorrect number of unique LDP types found";
    private static final String NO_LDP_RESOURCE = "ldp:Resource not among LDP types";
    private static final String NO_LDP_RDFSOURCE = "ldp:RDFSource not among LDP types";
    private static final String NO_LDP_CONTAINER = "ldp:Container not among LDP types";

    private static Stream<IRI> ldpResourceTypes(final IRI interactionModel) {
        return of(interactionModel).filter(type -> LDP.getSuperclassOf(type) != null || LDP.Resource.equals(type))
            .flatMap(type -> concat(ldpResourceTypes(LDP.getSuperclassOf(type)), of(type)));
    }

    @Test
    void testResource() {
        assertEquals(1, ldpResourceTypes(LDP.Resource).count(), INCORRECT_LDP_TYPES);
        final Set<IRI> types = ldpResourceTypes(LDP.Resource).collect(toSet());
        assertEquals(1, types.size(), INCORRECT_UNIQUE_LDP_TYPES);
        assertTrue(types.contains(LDP.Resource), NO_LDP_RESOURCE);
    }

    @Test
    void testNonLDP() {
        assertEquals(0, ldpResourceTypes(RDFS.label).count(), "ldp types found when they shouldn't be");
    }

    @Test
    void testRDFSource() {
        assertEquals(2, ldpResourceTypes(LDP.RDFSource).count(), INCORRECT_LDP_TYPES);
        final Set<IRI> types = ldpResourceTypes(LDP.RDFSource).collect(toSet());
        assertEquals(2, types.size(), INCORRECT_UNIQUE_LDP_TYPES);
        assertTrue(types.contains(LDP.Resource), NO_LDP_RESOURCE);
        assertTrue(types.contains(LDP.RDFSource), NO_LDP_RDFSOURCE);
    }

    @Test
    void testNonRDFSource() {
        assertEquals(2, ldpResourceTypes(LDP.NonRDFSource).count(), INCORRECT_LDP_TYPES);
        final Set<IRI> types = ldpResourceTypes(LDP.NonRDFSource).collect(toSet());
        assertEquals(2, types.size(), INCORRECT_UNIQUE_LDP_TYPES);
        assertTrue(types.contains(LDP.Resource), NO_LDP_RESOURCE);
        assertTrue(types.contains(LDP.NonRDFSource), "ldp:NonRDFSource not among LDP types");
    }

    @Test
    void testContainer() {
        assertEquals(3, ldpResourceTypes(LDP.Container).count(), INCORRECT_LDP_TYPES);
        final Set<IRI> types = ldpResourceTypes(LDP.Container).collect(toSet());
        assertEquals(3, types.size(), INCORRECT_UNIQUE_LDP_TYPES);
        assertTrue(types.contains(LDP.Resource), NO_LDP_RESOURCE);
        assertTrue(types.contains(LDP.RDFSource), NO_LDP_RDFSOURCE);
        assertTrue(types.contains(LDP.Container), NO_LDP_CONTAINER);
    }

    @Test
    void testBasicContainer() {
        assertEquals(4, ldpResourceTypes(LDP.BasicContainer).count(), INCORRECT_LDP_TYPES);
        final Set<IRI> types = ldpResourceTypes(LDP.BasicContainer).collect(toSet());
        assertEquals(4, types.size(), INCORRECT_UNIQUE_LDP_TYPES);
        assertTrue(types.contains(LDP.Resource), NO_LDP_RESOURCE);
        assertTrue(types.contains(LDP.RDFSource), NO_LDP_RDFSOURCE);
        assertTrue(types.contains(LDP.Container), NO_LDP_CONTAINER);
        assertTrue(types.contains(LDP.BasicContainer), "ldp:BasicContainer not among LDP types");
    }

    @Test
    void testDirectContainer() {
        assertEquals(4, ldpResourceTypes(LDP.DirectContainer).count(), INCORRECT_LDP_TYPES);
        final Set<IRI> types = ldpResourceTypes(LDP.DirectContainer).collect(toSet());
        assertEquals(4, types.size(), INCORRECT_UNIQUE_LDP_TYPES);
        assertTrue(types.contains(LDP.Resource), NO_LDP_RESOURCE);
        assertTrue(types.contains(LDP.RDFSource), NO_LDP_RDFSOURCE);
        assertTrue(types.contains(LDP.Container), NO_LDP_CONTAINER);
        assertTrue(types.contains(LDP.DirectContainer), "ldp:DirectContainer not among LDP types");
    }

    @Test
    void testIndirectContainer() {
        assertEquals(4, ldpResourceTypes(LDP.IndirectContainer).count(), INCORRECT_LDP_TYPES);
        final Set<IRI> types = ldpResourceTypes(LDP.IndirectContainer).collect(toSet());
        assertEquals(4, types.size(), INCORRECT_UNIQUE_LDP_TYPES);
        assertTrue(types.contains(LDP.Resource), NO_LDP_RESOURCE);
        assertTrue(types.contains(LDP.RDFSource), NO_LDP_RDFSOURCE);
        assertTrue(types.contains(LDP.Container), NO_LDP_CONTAINER);
        assertTrue(types.contains(LDP.IndirectContainer), "ldp:IndirectContainer not among LDP types");
    }

    @Test
    void testConstrainedBy() {
        final ConstraintService svc = mock(ConstraintService.class);
        final IRI identifier = rdf.createIRI(TrellisUtils.TRELLIS_DATA_PREFIX + "resource");

        doCallRealMethod().when(svc).constrainedBy(any(IRI.class), eq(LDP.RDFSource), any(Graph.class));
        when(svc.constrainedBy(any(IRI.class), any(IRI.class), any(Graph.class), eq(TrellisUtils.TRELLIS_DATA_PREFIX)))
            .thenAnswer(inv -> Stream.empty());

        assertEquals(0L, svc.constrainedBy(identifier, LDP.RDFSource, rdf.createGraph()).count(),
                "Unexpected constraint found");
    }
}
