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
package org.trellisldp.event;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.vocabulary.AS.Create;
import static org.trellisldp.vocabulary.LDP.Container;
import static org.trellisldp.vocabulary.PROV.Activity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.trellisldp.vocabulary.AS;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class EventSerializerTest {

    private static final RDF rdf = new SimpleRDF();

    private final ActivityStreamService svc = new EventSerializer();

    private final Instant time = now();

    @Mock
    private Event mockEvent;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("info:event/12345"));
        when(mockEvent.getAgents()).thenReturn(singleton(rdf.createIRI("info:user/test")));
        when(mockEvent.getTarget()).thenReturn(of(rdf.createIRI("trellis:repository/resource")));
        when(mockEvent.getTypes()).thenReturn(singleton(Create));
        when(mockEvent.getTargetTypes()).thenReturn(singleton(Container));
        when(mockEvent.getInbox()).thenReturn(of(rdf.createIRI("info:ldn/inbox")));
        when(mockEvent.getCreated()).thenReturn(time);
    }

    @Test
    public void testSerialization() {
        final Optional<String> json = svc.serialize(mockEvent);
        assertTrue(json.isPresent());
        assertTrue(json.get().contains("\"inbox\":\"info:ldn/inbox\""));
    }

    @Test
    public void testSerializationStructure() throws Exception {
        when(mockEvent.getTypes()).thenReturn(asList(Create, Activity));

        final Optional<String> json = svc.serialize(mockEvent);
        assertTrue(json.isPresent());

        final ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = mapper.readValue(json.get(), Map.class);
        assertTrue(map.containsKey("@context"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("type"));
        assertTrue(map.containsKey("inbox"));
        assertTrue(map.containsKey("actor"));
        assertTrue(map.containsKey("object"));
        assertTrue(map.containsKey("published"));

        final List types = (List) map.get("type");
        assertTrue(types.contains("Create"));
        assertTrue(types.contains(Activity.getIRIString()));

        assertTrue(AS.URI.contains((String) map.get("@context")));

        final List actor = (List) map.get("actor");
        assertTrue(actor.contains("info:user/test"));

        assertTrue(map.get("id").equals("info:event/12345"));
        assertTrue(map.get("inbox").equals("info:ldn/inbox"));
        assertTrue(map.get("published").equals(time.toString()));
    }

    @Test
    public void testSerializationStructureNoEmptyElements() throws Exception {
        when(mockEvent.getInbox()).thenReturn(empty());
        when(mockEvent.getAgents()).thenReturn(emptyList());
        when(mockEvent.getTargetTypes()).thenReturn(emptyList());

        final Optional<String> json = svc.serialize(mockEvent);
        assertTrue(json.isPresent());

        final ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = mapper.readValue(json.get(), Map.class);
        assertTrue(map.containsKey("@context"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("type"));
        assertFalse(map.containsKey("inbox"));
        assertFalse(map.containsKey("actor"));
        assertTrue(map.containsKey("object"));
        assertTrue(map.containsKey("published"));

        final List types = (List) map.get("type");
        assertTrue(types.contains("Create"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> obj = (Map<String, Object>) map.get("object");
        assertTrue(obj.containsKey("id"));
        assertFalse(obj.containsKey("type"));

        assertTrue(AS.URI.contains((String) map.get("@context")));

        assertTrue(map.get("id").equals("info:event/12345"));
        assertTrue(map.get("published").equals(time.toString()));
    }
}
