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

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.RDFUtils.getInstance;

import java.io.IOException;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
public class TrellisDatasetTest {

    private static final RDF rdf = getInstance();

    @Mock
    private Dataset mockDataset;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
        doThrow(new IOException()).when(mockDataset).close();
    }

    @Test
    public void testCloseDatasetError() {
        assertThrows(RuntimeTrellisException.class, () -> {
            try (final TrellisDataset dataset = new TrellisDataset(mockDataset)) {
                // nothing here
            }
        });
    }

    @Test
    public void testToString() {
        final TrellisDataset dataset = TrellisDataset.createDataset();
        final Literal title = rdf.createLiteral("The title");
        final Literal label = rdf.createLiteral("A preferred label", "eng");
        final Literal subject = rdf.createLiteral("http://example.com/subject");
        final IRI identifier = rdf.createIRI("http://example.com/resource");

        dataset.add(rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, title));
        dataset.add(rdf.createQuad(Trellis.PreferUserManaged, identifier, SKOS.prefLabel, label));
        dataset.add(rdf.createQuad(null, identifier, DC.subject, subject));

        final String asString = dataset.toString();
        assertTrue(asString.contains(format("%1$s %2$s %3$s %4$s .", identifier, DC.title, title,
                        Trellis.PreferUserManaged)));
        assertTrue(asString.contains(format("%1$s %2$s %3$s %4$s .", identifier, SKOS.prefLabel, label,
                        Trellis.PreferUserManaged)));
        assertTrue(asString.contains(format("%1$s %2$s %3$s  .", identifier, DC.subject, subject)));
    }
}
