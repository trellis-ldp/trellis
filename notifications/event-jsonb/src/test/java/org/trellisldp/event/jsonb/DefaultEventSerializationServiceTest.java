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
package org.trellisldp.event.jsonb;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.vocabulary.AS.Create;
import static org.trellisldp.vocabulary.LDP.Container;
import static org.trellisldp.vocabulary.PROV.Activity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventSerializationService;
import org.trellisldp.vocabulary.AS;

/**
 * @author acoburn
 */
class DefaultEventSerializationServiceTest {

    private static final RDF rdf = new SimpleRDF();

    private final EventSerializationService svc = new DefaultEventSerializationService();

    private final Instant time = now();

    @Mock
    private Event mockEvent;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("info:event/12345"));
        when(mockEvent.getTypes()).thenReturn(singleton(Create));
        when(mockEvent.getAgents()).thenReturn(singleton(rdf.createIRI("info:user/test")));
        when(mockEvent.getObject()).thenReturn(of(rdf.createIRI("trellis:data/resource")));
        when(mockEvent.getObjectTypes()).thenReturn(singleton(Container));
        when(mockEvent.getInbox()).thenReturn(of(rdf.createIRI("info:ldn/inbox")));
        when(mockEvent.getCreated()).thenReturn(time);
    }

    @Test
    void testSerialization() {
        final String json = svc.serialize(mockEvent);
        assertTrue(json.contains("\"inbox\":\"info:ldn/inbox\""), "ldp:inbox not in serialization!");
    }

    @Test
    void testSerializationStructure() throws Exception {
        when(mockEvent.getTypes()).thenReturn(asList(Create, Activity));

        final String json = svc.serialize(mockEvent);

        final Jsonb jsonb = JsonbBuilder.create();
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = jsonb.fromJson(json, Map.class);
        assertTrue(map.containsKey("@context"), "@context property not in JSON structure!");
        assertTrue(map.containsKey("id"), "id property not in JSON structure!");
        assertTrue(map.containsKey("type"), "type property not in JSON structure!");
        assertTrue(map.containsKey("inbox"), "inbox property not in JSON structure!");
        assertTrue(map.containsKey("actor"), "actor property not in JSON structure!");
        assertTrue(map.containsKey("object"), "object property not in JSON structure!");
        assertTrue(map.containsKey("published"), "published property not in JSON structure!");

        final List<?> types = (List<?>) map.get("type");
        assertTrue(types.contains("Create"), "as:Create not in type list!");
        assertTrue(types.contains(Activity.getIRIString()), "prov:Activity not in type list!");

        assertTrue(AS.getNamespace().contains((String) map.get("@context")), "AS namespace not in @context!");

        final List<?> actor = (List<?>) map.get("actor");
        assertTrue(actor.contains("info:user/test"), "actor property has incorrect value!");

        assertEquals("info:event/12345", map.get("id"), "id property has incorrect value!");
        assertEquals("info:ldn/inbox", map.get("inbox"), "inbox property has incorrect value!");
        assertEquals(time.toString(), map.get("published"), "published property has incorrect value!");
    }

    @Test
    void testSerializationStructureNoEmptyElements() throws Exception {
        when(mockEvent.getInbox()).thenReturn(empty());
        when(mockEvent.getAgents()).thenReturn(emptyList());
        when(mockEvent.getObjectTypes()).thenReturn(emptyList());

        final String json = svc.serialize(mockEvent);

        final Jsonb jsonb = JsonbBuilder.create();
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = jsonb.fromJson(json, Map.class);
        assertTrue(map.containsKey("@context"), "@context property not in JSON structure!");
        assertTrue(map.containsKey("id"), "id property not in JSON structure!");
        assertTrue(map.containsKey("type"), "type property not in JSON strucutre!");
        assertFalse(map.containsKey("inbox"), "inbox property unexpectedly in JSON structure!");
        assertFalse(map.containsKey("actor"), "actor property unexpectedly in JSON structure!");
        assertTrue(map.containsKey("object"), "object property not in JSON structure!");
        assertTrue(map.containsKey("published"), "published property not in JSON structure!");

        final List<?> types = (List<?>) map.get("type");
        assertTrue(types.contains("Create"), "as:Create type not in type list!");

        @SuppressWarnings("unchecked")
        final Map<String, Object> obj = (Map<String, Object>) map.get("object");
        assertTrue(obj.containsKey("id"), "object id property not in JSON structure!");
        assertFalse(obj.containsKey("type"), "empty object type unexpectedly in JSON structure!");

        assertTrue(AS.getNamespace().contains((String) map.get("@context")), "AS namespace not in @context!");

        assertEquals("info:event/12345", map.get("id"), "id property has incorrect value!");
        assertEquals(time.toString(), map.get("published"), "published property has incorrect value!");
    }
}
