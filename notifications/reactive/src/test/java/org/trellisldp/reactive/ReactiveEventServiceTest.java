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
package org.trellisldp.reactive;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import io.smallrye.config.inject.ConfigProducer;
import io.smallrye.reactive.messaging.MediatorFactory;
import io.smallrye.reactive.messaging.extension.MediatorManager;
import io.smallrye.reactive.messaging.extension.ReactiveMessagingExtension;
import io.smallrye.reactive.messaging.impl.ConfiguredChannelFactory;
import io.smallrye.reactive.messaging.impl.InternalChannelRegistry;

import java.time.Instant;

import javax.inject.Inject;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.trellisldp.api.*;
import org.trellisldp.event.jackson.DefaultEventSerializationService;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

@ExtendWith(WeldJunit5Extension.class)
class ReactiveEventServiceTest {

    private static final RDF rdf = new SimpleRDF();
    private final Instant time = now();

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                       .beanClasses(
                                           MediatorFactory.class,
                                           MediatorManager.class,
                                           InternalChannelRegistry.class,
                                           ConfiguredChannelFactory.class,
                                           TestCollector.class,
                                           ReactiveEventService.class,
                                           DefaultEventSerializationService.class,
                                           ConfigProducer.class)
                                       .extensions(new ReactiveMessagingExtension()));

    @Inject
    private TestCollector collector;

    @Inject
    private ReactiveEventService service;

    @Mock
    private Event mockEvent;

    @BeforeEach
    void setUp() {
        initMocks(this);

        when(mockEvent.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")));
        when(mockEvent.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockEvent.getIdentifier()).thenReturn(rdf.createIRI("urn:test"));
        when(mockEvent.getCreated()).thenReturn(time);
        when(mockEvent.getTypes()).thenReturn(singleton(AS.Update));
        when(mockEvent.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));
        when(mockEvent.getInbox()).thenReturn(empty());
    }

    @Test
    void testNoargCtor() {
        final ReactiveEventService svc = new ReactiveEventService();
        assertDoesNotThrow(() -> svc.emit(mockEvent));
    }

    @Test
    void testReactiveStream() {
        service.emit(mockEvent);
        await().atMost(5, SECONDS).until(() -> collector.getResults().size() == 1);
        assertEquals(1, collector.getResults().size(), "Incorrect number of messages!");
        service.emit(mockEvent);
        await().atMost(5, SECONDS).until(() -> collector.getResults().size() == 2);
        assertEquals(2, collector.getResults().size(), "Incorrect number of messages!");
        service.emit(mockEvent);
        service.emit(mockEvent);
        await().atMost(5, SECONDS).until(() -> collector.getResults().size() == 4);
        assertEquals(4, collector.getResults().size(), "Incorrect number of messages!");
    }
}
