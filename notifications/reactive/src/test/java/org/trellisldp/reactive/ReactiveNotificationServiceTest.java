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
package org.trellisldp.reactive;

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.trellisldp.api.*;
import org.trellisldp.notification.jackson.DefaultNotificationSerializationService;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

@ExtendWith(WeldJunit5Extension.class)
@ExtendWith(MockitoExtension.class)
class ReactiveNotificationServiceTest {

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
                                           NotificationCollector.class,
                                           ReactiveNotificationService.class,
                                           DefaultNotificationSerializationService.class,
                                           ConfigProducer.class)
                                       .extensions(new ReactiveMessagingExtension()));

    @Inject
    private TestCollector collector;

    @Inject
    private NotificationCollector notifications;

    @Inject
    private ReactiveNotificationService service;

    @Mock
    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        notifications.clear();
        collector.clear();
    }

    @Test
    void testReactiveStream() {
        when(mockNotification.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")));
        when(mockNotification.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockNotification.getIdentifier()).thenReturn(rdf.createIRI("urn:test"));
        when(mockNotification.getCreated()).thenReturn(time);
        when(mockNotification.getTypes()).thenReturn(singleton(AS.Update));
        when(mockNotification.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));

        service.emit(mockNotification);
        await().atMost(5, SECONDS).until(() -> collector.getResults().size() == 1);
        assertEquals(1, collector.getResults().size(), "Incorrect number of messages!");
        service.emit(mockNotification);
        await().atMost(5, SECONDS).until(() -> collector.getResults().size() == 2);
        assertEquals(2, collector.getResults().size(), "Incorrect number of messages!");
        service.emit(mockNotification);
        service.emit(mockNotification);
        await().atMost(5, SECONDS).until(() -> collector.getResults().size() == 4);
        assertEquals(4, collector.getResults().size(), "Incorrect number of messages!");
    }

    @Test
    void testCdiNotification() {
        when(mockNotification.getObject()).thenReturn(of(rdf.createIRI(TRELLIS_DATA_PREFIX + "resource")));
        when(mockNotification.getAgents()).thenReturn(singleton(Trellis.AdministratorAgent));
        when(mockNotification.getIdentifier()).thenReturn(rdf.createIRI("urn:test"));
        when(mockNotification.getCreated()).thenReturn(time);
        when(mockNotification.getTypes()).thenReturn(singleton(AS.Update));
        when(mockNotification.getObjectTypes()).thenReturn(singleton(LDP.RDFSource));

        service.emit(mockNotification);
        await().atMost(5, SECONDS).until(() -> notifications.getResults().size() > 0);
    }
}
