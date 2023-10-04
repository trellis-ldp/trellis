/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
package org.trellisldp.notification.jsonb;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.trellisldp.vocabulary.AS.Create;
import static org.trellisldp.vocabulary.LDP.Container;
import static org.trellisldp.vocabulary.PROV.Activity;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trellisldp.api.Notification;
import org.trellisldp.api.NotificationSerializationService;
import org.trellisldp.vocabulary.AS;

/**
 * @author acoburn
 */
@ExtendWith(MockitoExtension.class)
class DefaultNotificationSerializationServiceTest {

    private static final RDF rdf = new SimpleRDF();

    private final NotificationSerializationService svc = new DefaultNotificationSerializationService();

    private final Instant time = now();

    @Mock
    private Notification mockNotification;

    @Test
    void testSerialization() {
        when(mockNotification.getIdentifier()).thenReturn(rdf.createIRI("info:notification/12345"));
        when(mockNotification.getTypes()).thenReturn(List.of(Create));
        when(mockNotification.getAgents()).thenReturn(List.of(rdf.createIRI("info:user/test")));
        when(mockNotification.getObject()).thenReturn(Optional.of(rdf.createIRI("trellis:data/resource")));
        when(mockNotification.getObjectTypes()).thenReturn(List.of(Container));
        when(mockNotification.getObjectState()).thenReturn(Optional.of("etag:1234567"));
        when(mockNotification.getCreated()).thenReturn(time);

        final String json = svc.serialize(mockNotification);
        assertTrue(json.contains("\"state\":\"etag:1234567\""), "state not in serialization!");
    }

    @Test
    void testSerializationStructure() throws Exception {
        when(mockNotification.getIdentifier()).thenReturn(rdf.createIRI("info:notification/12345"));
        when(mockNotification.getAgents()).thenReturn(List.of(rdf.createIRI("info:user/test")));
        when(mockNotification.getObject()).thenReturn(Optional.of(rdf.createIRI("trellis:data/resource")));
        when(mockNotification.getObjectTypes()).thenReturn(List.of(Container));
        when(mockNotification.getCreated()).thenReturn(time);
        when(mockNotification.getTypes()).thenReturn(asList(Create, Activity));

        final String json = svc.serialize(mockNotification);

        final Jsonb jsonb = JsonbBuilder.create();
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = jsonb.fromJson(json, Map.class);
        assertTrue(map.containsKey("@context"), "@context property not in JSON structure!");
        assertTrue(map.containsKey("id"), "id property not in JSON structure!");
        assertTrue(map.containsKey("type"), "type property not in JSON structure!");
        assertTrue(map.containsKey("actor"), "actor property not in JSON structure!");
        assertTrue(map.containsKey("object"), "object property not in JSON structure!");
        assertTrue(map.containsKey("published"), "published property not in JSON structure!");

        final List<?> types = (List<?>) map.get("type");
        assertTrue(types.contains("Create"), "as:Create not in type list!");
        assertTrue(types.contains(Activity.getIRIString()), "prov:Activity not in type list!");

        final List<?> contexts = (List<?>) map.get("@context");
        assertTrue(contexts.contains(AS.getNamespace().replace("#", "")), "AS namespace not in @context!");

        final List<?> actor = (List<?>) map.get("actor");
        assertTrue(actor.contains("info:user/test"), "actor property has incorrect value!");

        assertEquals("info:notification/12345", map.get("id"), "id property has incorrect value!");
        assertEquals(time.toString(), map.get("published"), "published property has incorrect value!");
    }

    @Test
    void testSerializationStructureNoEmptyElements() throws Exception {
        when(mockNotification.getIdentifier()).thenReturn(rdf.createIRI("info:notification/12345"));
        when(mockNotification.getTypes()).thenReturn(List.of(Create));
        when(mockNotification.getObject()).thenReturn(Optional.of(rdf.createIRI("trellis:data/resource")));
        when(mockNotification.getCreated()).thenReturn(time);
        when(mockNotification.getAgents()).thenReturn(emptyList());
        when(mockNotification.getObjectTypes()).thenReturn(emptyList());

        final String json = svc.serialize(mockNotification);

        final Jsonb jsonb = JsonbBuilder.create();
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = jsonb.fromJson(json, Map.class);
        assertTrue(map.containsKey("@context"), "@context property not in JSON structure!");
        assertTrue(map.containsKey("id"), "id property not in JSON structure!");
        assertTrue(map.containsKey("type"), "type property not in JSON strucutre!");
        assertFalse(map.containsKey("actor"), "actor property unexpectedly in JSON structure!");
        assertTrue(map.containsKey("object"), "object property not in JSON structure!");
        assertTrue(map.containsKey("published"), "published property not in JSON structure!");

        final List<?> types = (List<?>) map.get("type");
        assertTrue(types.contains("Create"), "as:Create type not in type list!");

        @SuppressWarnings("unchecked")
        final Map<String, Object> obj = (Map<String, Object>) map.get("object");
        assertTrue(obj.containsKey("id"), "object id property not in JSON structure!");
        assertFalse(obj.containsKey("type"), "empty object type unexpectedly in JSON structure!");

        final List<?> contexts = (List<?>) map.get("@context");
        assertTrue(contexts.contains(AS.getNamespace().replace("#", "")), "AS namespace not in @context!");

        assertEquals("info:notification/12345", map.get("id"), "id property has incorrect value!");
        assertEquals(time.toString(), map.get("published"), "published property has incorrect value!");
    }
}
