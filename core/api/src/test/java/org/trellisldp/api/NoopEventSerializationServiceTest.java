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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.PROV;

class NoopEventSerializationServiceTest {

    private static final RDF rdf = RDFFactory.getInstance();
    private static final String identifier = "urn:uuid:e5297053-52fd-4bed-a840-7ef2ba94f7de";

    @Test
    void testNoopEventSerializationService() {
        final Event event = mock(Event.class);
        when(event.getTypes()).thenReturn(emptyList());
        when(event.getIdentifier()).thenReturn(rdf.createIRI(identifier));
        final EventSerializationService svc = new NoopEventSerializationService();
        assertEquals("{\n  \"@context\": \"https://www.w3.org/ns/activitystreams\",\n" +
                "  \"id\": \"" + identifier + "\"\n}", svc.serialize(event));
    }

    @Test
    void testNoopEventSerializationWithEventIRIAndType() {
        final String object = "http://example.com/resource";
        final Event event = mock(Event.class);
        when(event.getObject()).thenReturn(of(rdf.createIRI(object)));
        when(event.getTypes()).thenReturn(singletonList(AS.Update));
        when(event.getIdentifier()).thenReturn(rdf.createIRI(identifier));
        final EventSerializationService svc = new NoopEventSerializationService();
        assertEquals("{\n  \"@context\": \"https://www.w3.org/ns/activitystreams\",\n" +
                "  \"id\": \"" + identifier + "\",\n" +
                "  \"type\": \"Update\",\n" +
                "  \"object\": \"" + object + "\"\n}",
                svc.serialize(event));
    }

    @Test
    void testNoopEventSerializationWithEventIRIAndMultipleTypes() {
        final String object = "http://example.com/resource";
        final Event event = mock(Event.class);
        when(event.getObject()).thenReturn(of(rdf.createIRI(object)));
        when(event.getTypes()).thenReturn(asList(AS.Update, PROV.Activity));
        when(event.getIdentifier()).thenReturn(rdf.createIRI(identifier));
        final EventSerializationService svc = new NoopEventSerializationService();
        assertEquals("{\n  \"@context\": \"https://www.w3.org/ns/activitystreams\",\n" +
                "  \"id\": \"" + identifier + "\",\n" +
                "  \"type\": [\"Update\",\"http://www.w3.org/ns/prov#Activity\"],\n" +
                "  \"object\": \"" + object + "\"\n}",
                svc.serialize(event));
    }
}

