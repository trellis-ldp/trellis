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

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.TrellisUtils.getInstance;

import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;

class NoopEventSerializationServiceTest {

    private static final RDF rdf = getInstance();
    private static final String identifier = "urn:uuid:e5297053-52fd-4bed-a840-7ef2ba94f7de";

    @Test
    void testNoopEventSerializationService() {
        final Event event = mock(Event.class);
        when(event.getIdentifier()).thenReturn(rdf.createIRI(identifier));
        final EventSerializationService svc = new NoopEventSerializationService();
        assertEquals("{\n  \"@context\": \"https://www.w3.org/ns/activitystreams\",\n" +
                "  \"id\": \"" + identifier + "\"\n}", svc.serialize(event));
    }

    @Test
    void testNoopEventSerializationWithEventIRI() {
        final String target = "http://example.com/resource";
        final Event event = mock(Event.class);
        when(event.getTarget()).thenReturn(of(rdf.createIRI(target)));
        when(event.getIdentifier()).thenReturn(rdf.createIRI(identifier));
        final EventSerializationService svc = new NoopEventSerializationService();
        assertEquals("{\n  \"@context\": \"https://www.w3.org/ns/activitystreams\",\n" +
                "  \"object\": \"" + target + "\",\n" +
                "  \"id\": \"" + identifier + "\"\n}", svc.serialize(event));
    }
}

