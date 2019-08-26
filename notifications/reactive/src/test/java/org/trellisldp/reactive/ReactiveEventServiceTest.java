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
package org.trellisldp.reactive;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import io.smallrye.reactive.messaging.MediatorFactory;
import io.smallrye.reactive.messaging.extension.MediatorManager;
import io.smallrye.reactive.messaging.extension.ReactiveMessagingExtension;
import io.smallrye.reactive.messaging.impl.ConfiguredChannelFactory;
import io.smallrye.reactive.messaging.impl.InternalChannelRegistry;

import java.time.Instant;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.*;
import org.trellisldp.event.DefaultActivityStreamService;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

public class ReactiveEventServiceTest {

    private static final RDF rdf = new SimpleRDF();
    private static WeldContainer container;

    private final Instant time = now();

    @Mock
    private Event mockEvent;

    @BeforeAll
    public static void initialize() {
        // Setup Weld
        final Weld weld = new Weld();
        weld.addBeanClass(MediatorFactory.class);
        weld.addBeanClass(MediatorManager.class);
        weld.addBeanClass(InternalChannelRegistry.class);
        weld.addBeanClass(ConfiguredChannelFactory.class);

        weld.addExtension(new ReactiveMessagingExtension());

        weld.addBeanClass(TestCollector.class);
        weld.addBeanClass(ReactiveEventService.class);
        weld.addBeanClass(DefaultActivityStreamService.class);
        weld.addBeanClass(ReactiveEventService.class);
        weld.disableDiscovery();

        container = weld.initialize();
    }

    @AfterAll
    public static void tearDown() {
        if (container != null) {
            container.shutdown();
        }
    }

    @BeforeEach
    public void setUp() {
        initMocks(this);

        when(mockEvent.getTarget()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")));
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:test"));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getTargetTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
    }

    @Test
    public void testReactiveStream() {
        final TestCollector collector = getInstance(TestCollector.class);
        final ReactiveEventService service = getInstance(ReactiveEventService.class);

        service.emit(mockEvent);
        await().atMost(FIVE_SECONDS).until(() -> collector.getResults().size() == 1);
        service.emit(mockEvent);
        await().atMost(FIVE_SECONDS).until(() -> collector.getResults().size() == 2);
        service.emit(mockEvent);
        service.emit(mockEvent);
        await().atMost(FIVE_SECONDS).until(() -> collector.getResults().size() == 4);
    }

    private <T> T getInstance(final Class<T> type) {
        return container.getBeanManager().createInstance().select(type).get();
    }
}
