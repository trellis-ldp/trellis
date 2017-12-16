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
package org.trellisldp.audit;

import static java.time.Instant.now;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.vocabulary.XSD;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class DefaultAuditServiceTest {

    private static RDF rdf = new SimpleRDF();

    private final Instant created = now();

    private final IRI subject = rdf.createIRI("trellis:repository/resource");

    @Mock
    private Session mockSession;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousUser);
        when(mockSession.getCreated()).thenReturn(created);
        when(mockSession.getDelegatedBy()).thenReturn(of(Trellis.RepositoryAdministrator));
    }

    @Test
    public void testAuditCreation() {
        final Dataset dataset = rdf.createDataset();
        final AuditService svc = new DefaultAuditService();
        svc.creation(subject, mockSession).forEach(dataset::add);
        assertTrue(dataset.getGraph(Trellis.PreferAudit).filter(graph -> graph.size() == dataset.size()).isPresent());
        assertTrue(dataset.contains(null, null, type, PROV.Activity));
        assertTrue(dataset.contains(null, null, type, AS.Create));
        assertTrue(dataset.contains(null, subject, PROV.wasGeneratedBy, null));
        assertTrue(dataset.contains(null, null, PROV.wasAssociatedWith, Trellis.AnonymousUser));
        assertTrue(dataset.contains(null, null, PROV.actedOnBehalfOf, Trellis.RepositoryAdministrator));
        assertTrue(dataset.contains(null, null, PROV.startedAtTime,
                    rdf.createLiteral(created.toString(), XSD.dateTime)));
        assertEquals(6L, dataset.size());
    }

    @Test
    public void testAuditDeletion() {
        final Dataset dataset = rdf.createDataset();
        final AuditService svc = new DefaultAuditService();
        svc.deletion(subject, mockSession).forEach(dataset::add);
        assertTrue(dataset.getGraph(Trellis.PreferAudit).filter(graph -> graph.size() == dataset.size()).isPresent());
        assertTrue(dataset.contains(null, null, type, PROV.Activity));
        assertTrue(dataset.contains(null, null, type, AS.Delete));
        assertTrue(dataset.contains(null, subject, PROV.wasGeneratedBy, null));
        assertTrue(dataset.contains(null, null, PROV.wasAssociatedWith, Trellis.AnonymousUser));
        assertTrue(dataset.contains(null, null, PROV.actedOnBehalfOf, Trellis.RepositoryAdministrator));
        assertTrue(dataset.contains(null, null, PROV.startedAtTime,
                    rdf.createLiteral(created.toString(), XSD.dateTime)));
        assertEquals(6L, dataset.size());
    }

    @Test
    public void testAuditUpdate() {
        final Dataset dataset = rdf.createDataset();
        final AuditService svc = new DefaultAuditService();
        svc.update(subject, mockSession).forEach(dataset::add);
        assertTrue(dataset.getGraph(Trellis.PreferAudit).filter(graph -> graph.size() == dataset.size()).isPresent());
        assertTrue(dataset.contains(null, null, type, PROV.Activity));
        assertTrue(dataset.contains(null, null, type, AS.Update));
        assertTrue(dataset.contains(null, subject, PROV.wasGeneratedBy, null));
        assertTrue(dataset.contains(null, null, PROV.wasAssociatedWith, Trellis.AnonymousUser));
        assertTrue(dataset.contains(null, null, PROV.actedOnBehalfOf, Trellis.RepositoryAdministrator));
        assertTrue(dataset.contains(null, null, PROV.startedAtTime,
                    rdf.createLiteral(created.toString(), XSD.dateTime)));
        assertEquals(6L, dataset.size());
    }
}
