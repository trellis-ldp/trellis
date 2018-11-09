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
package org.trellisldp.http.impl;

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getInstance;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.ResourceTemplate;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;
import org.trellisldp.vocabulary.Trellis;

public class TrellisResourceTemplateTest {

    private static final RDF rdf = getInstance();

    @Test
    public void testResourceTemplate() {
        final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
        final IRI id = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final IRI member = rdf.createIRI(TRELLIS_DATA_PREFIX + "member");
        final IRI inbox = rdf.createIRI("http://www.example.com/inbox");
        final IRI annotations = rdf.createIRI("http://www.example.com/annotations");
        final IRI annotations2 = rdf.createIRI("http://www.example.com/annotations2");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, id, LDP.membershipResource, member);
        dataset.add(Trellis.PreferUserManaged, id, LDP.insertedContentRelation, rdf.createBlankNode());
        dataset.add(Trellis.PreferUserManaged, id, LDP.inbox, inbox);
        dataset.add(Trellis.PreferUserManaged, id, OA.annotationService, annotations);
        dataset.add(Trellis.PreferUserManaged, id, OA.annotationService, annotations2);

        final ResourceTemplate tpl = new TrellisResourceTemplate(id, LDP.RDFSource, dataset, root, null);

        assertEquals(id, tpl.getIdentifier());
        assertEquals(LDP.RDFSource, tpl.getInteractionModel());
        assertEquals(dataset, tpl.dataset());
        assertEquals(dataset.size(), tpl.stream().count());
        assertEquals(of(root), tpl.getContainer());
        assertFalse(tpl.getBinaryTemplate().isPresent());
        assertEquals(of(member), tpl.getMembershipResource());
        assertFalse(tpl.getMemberRelation().isPresent());
        assertFalse(tpl.getMemberOfRelation().isPresent());
        assertFalse(tpl.getInsertedContentRelation().isPresent());
        assertTrue(tpl.getExtraLinkRelations().anyMatch(pair -> inbox.getIRIString().equals(pair.getKey())
                    && LDP.inbox.getIRIString().equals(pair.getValue())));
        assertEquals(2L, tpl.getExtraLinkRelations()
                .filter(pair -> OA.annotationService.getIRIString().equals(pair.getValue())).count());
    }

}
