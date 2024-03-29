/*
 * Copyright (c) Aaron Coburn and individual contributors
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
package org.trellisldp.audit;

import static java.time.Instant.now;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.vocabulary.XSD;

/**
 * @author acoburn
 */
@ExtendWith(MockitoExtension.class)
class DefaultAuditServiceTest {

    private static final RDF rdf = new SimpleRDF();

    private final Instant created = now();
    private final IRI subject = rdf.createIRI("trellis:data/resource");

    @Mock
    private Session mockSession;

    @Test
    void testAuditCreation() {
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        when(mockSession.getCreated()).thenReturn(created);
        when(mockSession.getDelegatedBy()).thenReturn(of(Trellis.AdministratorAgent));

        final Dataset dataset = rdf.createDataset();
        final AuditService svc = new DefaultAuditService() {};
        svc.creation(subject, mockSession).forEach(dataset::add);
        assertTrue(dataset.getGraph(Trellis.PreferAudit).filter(graph -> graph.size() == dataset.size()).isPresent(),
                "Graph and dataset sizes don't match for creation event!");
        assertTrue(dataset.contains(null, null, type, AS.Create), "as:Create type not in create dataset!");
        assertAll("Event property check", checkEventProperties(dataset));
    }

    @Test
    void testAuditDeletion() {
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        when(mockSession.getCreated()).thenReturn(created);
        when(mockSession.getDelegatedBy()).thenReturn(of(Trellis.AdministratorAgent));

        final Dataset dataset = rdf.createDataset();
        final AuditService svc = new DefaultAuditService() {};
        svc.deletion(subject, mockSession).forEach(dataset::add);
        assertTrue(dataset.getGraph(Trellis.PreferAudit).filter(graph -> graph.size() == dataset.size()).isPresent(),
                "Graph and dataset sizes don't match for deletion event!");
        assertTrue(dataset.contains(null, null, type, AS.Delete), "as:Delete type not in delete dataset!");
        assertAll("Event property check", checkEventProperties(dataset));
    }

    @Test
    void testAuditUpdate() {
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        when(mockSession.getCreated()).thenReturn(created);
        when(mockSession.getDelegatedBy()).thenReturn(of(Trellis.AdministratorAgent));

        final Dataset dataset = rdf.createDataset();
        final AuditService svc = new DefaultAuditService() {};
        svc.update(subject, mockSession).forEach(dataset::add);
        assertTrue(dataset.getGraph(Trellis.PreferAudit).filter(graph -> graph.size() == dataset.size()).isPresent());
        assertTrue(dataset.contains(null, null, type, AS.Update));
        assertAll("Event property check", checkEventProperties(dataset));
    }

    private Stream<Executable> checkEventProperties(final Dataset dataset) {
        return Stream.of(
                () -> assertTrue(dataset.contains(null, null, type, PROV.Activity), "missing prov:Activity triple!"),
                () -> assertTrue(dataset.contains(null, subject, PROV.wasGeneratedBy, null),
                                 "missing prov:wasGeneratedBy triple!"),
                () -> assertTrue(dataset.contains(null, null, PROV.wasAssociatedWith, Trellis.AnonymousAgent),
                                 "missing prov:wasAssociatedWith triple!"),
                () -> assertTrue(dataset.contains(null, null, PROV.actedOnBehalfOf, Trellis.AdministratorAgent),
                                 "missing prov:actedOnBehalfOf triple!"),
                () -> assertTrue(dataset.contains(null, null, PROV.atTime,
                        rdf.createLiteral(created.toString(), XSD.dateTime)), "missing prov:atTime triple!"),
                () -> assertEquals(6L, dataset.size(), "Incorrect dataset size!"));
    }
}
