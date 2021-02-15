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
package org.trellisldp.jdbc;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.SortedSet;

import javax.sql.DataSource;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;

@DisabledOnOs(WINDOWS)
@ExtendWith(MockitoExtension.class)
class DBWrappedMementoServiceTest {
    private static final Logger LOGGER = getLogger(DBWrappedMementoService.class);
    private static final RDF rdf = RDFFactory.getInstance();
    private static final IRI root = rdf.createIRI("trellis:data/");
    private static final DataSource ds = DBTestUtils.setupDatabase();

    @Mock
    MementoService mockMementoService;

    @Test
    void testMementoService() {
        final Resource mockResource = mock(Resource.class);

        when(mockMementoService.put(any(Resource.class))).thenAnswer(inv -> completedFuture(null));
        when(mockMementoService.get(any(IRI.class), any(Instant.class))).thenAnswer(inv ->
                completedFuture(mockResource));
        final MementoService svc = new DBWrappedMementoService(ds, mockMementoService);

        final Instant time = now();
        final IRI identifier = rdf.createIRI("trellis:data/resource");

        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getModified()).thenReturn(time);

        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        when(mockResource.getModified()).thenReturn(time.plusSeconds(2L));
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        when(mockResource.getModified()).thenReturn(time.plusSeconds(4L));
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        final SortedSet<Instant> mementos = svc.mementos(identifier).toCompletableFuture().join();
        assertTrue(mementos.contains(time.truncatedTo(SECONDS)));
        assertTrue(mementos.contains(time.plusSeconds(2L).truncatedTo(SECONDS)));
        assertTrue(mementos.contains(time.plusSeconds(4L).truncatedTo(SECONDS)));

        when(mockResource.getModified()).thenReturn(time);
        final Resource res = svc.get(identifier, time).toCompletableFuture().join();
        assertEquals(time, res.getModified());
    }

    @Test
    void testNoArgCtor() {
        assertDoesNotThrow(() -> new DBWrappedMementoService());
    }

    @Test
    void testNoOpMementoService() {
        final MementoService svc = new DBWrappedMementoService(ds, new NoopMementoService());

        final Instant time = now();
        final IRI identifier = rdf.createIRI("trellis:data/resource");

        final Resource mockResource = mock(Resource.class);

        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        assertTrue(svc.mementos(identifier).toCompletableFuture().join().isEmpty());

        assertEquals(Resource.SpecialResources.MISSING_RESOURCE,
                svc.get(identifier, time).toCompletableFuture().join());
    }


    @Test
    void testMementoUtils() {
        final Resource mockResource = mock(Resource.class);

        when(mockMementoService.put(any(Resource.class))).thenAnswer(inv -> completedFuture(null));
        when(mockMementoService.get(any(IRI.class), any(Instant.class))).thenAnswer(inv ->
                completedFuture(mockResource));
        final MementoService svc = new DBWrappedMementoService(ds, mockMementoService);

        final Instant time = now();
        final IRI identifier = rdf.createIRI("trellis:data/resource");

        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getModified()).thenReturn(time);

        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        when(mockResource.getModified()).thenReturn(time.plusSeconds(2L));
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        when(mockResource.getModified()).thenReturn(time.plusSeconds(4L));
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        final SortedSet<Instant> mementos = svc.mementos(identifier).toCompletableFuture().join();
        assertTrue(mementos.contains(time.truncatedTo(SECONDS)));
        assertTrue(mementos.contains(time.plusSeconds(2L).truncatedTo(SECONDS)));
        assertTrue(mementos.contains(time.plusSeconds(4L).truncatedTo(SECONDS)));

        when(mockResource.getModified()).thenReturn(time);
        final Resource res = svc.get(identifier, time).toCompletableFuture().join();
        assertEquals(time, res.getModified());
    }
}

