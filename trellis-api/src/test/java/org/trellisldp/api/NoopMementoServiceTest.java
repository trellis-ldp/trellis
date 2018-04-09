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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.SKOS;
import org.trellisldp.vocabulary.Trellis;

public class NoopMementoServiceTest {

    @Test
    public void noAction() {
        final MementoService testService = new NoopMementoService();
        final RDF rdf = getInstance();
        final IRI identifier = rdf.createIRI("trellis:data/resource");
        final Instant time = now();
        testService.put(identifier, time, of(rdf.createQuad(
                        Trellis.PreferUserManaged, identifier, type, SKOS.Concept)));

        assertFalse(testService.get(identifier, time).isPresent());
        assertTrue(testService.list(identifier).isEmpty());
        assertTrue(testService.delete(identifier, time));
    }
}
