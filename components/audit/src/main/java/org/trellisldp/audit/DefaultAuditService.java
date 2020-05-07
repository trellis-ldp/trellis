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
package org.trellisldp.audit;

import static java.util.Arrays.asList;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.Trellis.PreferAudit;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.XSD;

/**
 * An {@link AuditService} that generates Audit-related {@link Quad}s for
 * various write operations.
 *
 * <p>This class makes use of the {@link PROV} vocabulary and {@link BlankNode}
 * objects in a {@code http://www.trellisldp.org/ns/trellis#PreferAudit} named
 * graph.
 * 
 * @author acoburn
 */
@ApplicationScoped
public class DefaultAuditService implements AuditService {

    private static final RDF rdf = RDFFactory.getInstance();

    @Override
    public List<Quad> creation(final IRI subject, final Session session) {
        return auditData(subject, session, asList(PROV.Activity, AS.Create));
    }

    @Override
    public List<Quad> deletion(final IRI subject, final Session session) {
        return auditData(subject, session, asList(PROV.Activity, AS.Delete));
    }

    @Override
    public List<Quad> update(final IRI subject, final Session session) {
        return auditData(subject, session, asList(PROV.Activity, AS.Update));
    }

    private List<Quad> auditData(final IRI subject, final Session session, final List<IRI> types) {
        final List<Quad> data = new ArrayList<>();
        final BlankNode bnode = rdf.createBlankNode();
        data.add(rdf.createQuad(PreferAudit, subject, PROV.wasGeneratedBy, bnode));
        types.forEach(t -> data.add(rdf.createQuad(PreferAudit, bnode, type, t)));
        data.add(rdf.createQuad(PreferAudit, bnode, PROV.wasAssociatedWith, session.getAgent()));
        data.add(rdf.createQuad(PreferAudit, bnode, PROV.atTime,
                    rdf.createLiteral(session.getCreated().toString(), XSD.dateTime)));
        session.getDelegatedBy().ifPresent(delegate ->
                data.add(rdf.createQuad(PreferAudit, bnode, PROV.actedOnBehalfOf, delegate)));
        return data;
    }
}
