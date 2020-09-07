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
package org.trellisldp.jdbc;

import static java.io.File.separator;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.SortedSet;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.slf4j.Logger;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.file.FileMementoService;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

@DisabledOnOs(WINDOWS)
class DBWrappedMementoServiceTest {
    private static final Logger LOGGER = getLogger(DBWrappedMementoService.class);
    private static final RDF rdf = RDFFactory.getInstance();
    private static final IRI root = rdf.createIRI("trellis:data/");
    private static EmbeddedPostgres pg = null;

    static {
        try {
            pg = EmbeddedPostgres.builder()
                .setDataDirectory("build" + separator + "pgdata-" + new RandomStringGenerator
                            .Builder().withinRange('a', 'z').build().generate(10)).start();

            // Set up database migrations
            try (final Connection c = pg.getPostgresDatabase().getConnection()) {
                final Liquibase liquibase = new Liquibase("org/trellisldp/jdbc/migrations.yml",
                        new ClassLoaderResourceAccessor(),
                        new JdbcConnection(c));
                final Contexts ctx = null;
                liquibase.update(ctx);
            }

        } catch (final IOException | SQLException | LiquibaseException ex) {
            LOGGER.error("Error setting up tests", ex);
        }
    }

    @Test
    void testMementoService() {
        final MementoService svc = new DBWrappedMementoService(pg.getPostgresDatabase());

        final Instant time = now();
        final IRI identifier = rdf.createIRI("trellis:data/resource");

        final Resource mockResource = mock(Resource.class);

        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                    rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("Title"))));
        when(mockResource.getBinaryMetadata()).thenReturn(empty());
        when(mockResource.getMemberOfRelation()).thenReturn(empty());
        when(mockResource.getMemberRelation()).thenReturn(empty());
        when(mockResource.getMembershipResource()).thenReturn(empty());
        when(mockResource.getInsertedContentRelation()).thenReturn(empty());

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

        final Resource res = svc.get(identifier, time).toCompletableFuture().join();
        assertEquals(time, res.getModified());
    }

    @Test
    void testNoArgCtor() {
        assertDoesNotThrow(() -> new DBWrappedMementoService());
    }

    @Test
    void testNoOpMementoService() {
        final MementoService svc = new DBWrappedMementoService(pg.getPostgresDatabase(),
                new NoopMementoService());

        final Instant time = now();
        final IRI identifier = rdf.createIRI("trellis:data/resource");

        final Resource mockResource = mock(Resource.class);

        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                    rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("Title"))));
        when(mockResource.getBinaryMetadata()).thenReturn(empty());
        when(mockResource.getMemberOfRelation()).thenReturn(empty());
        when(mockResource.getMemberRelation()).thenReturn(empty());
        when(mockResource.getMembershipResource()).thenReturn(empty());
        when(mockResource.getInsertedContentRelation()).thenReturn(empty());

        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        when(mockResource.getModified()).thenReturn(time.plusSeconds(2L));
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        when(mockResource.getModified()).thenReturn(time.plusSeconds(4L));
        assertDoesNotThrow(svc.put(mockResource).toCompletableFuture()::join);

        assertTrue(svc.mementos(identifier).toCompletableFuture().join().isEmpty());

        assertEquals(Resource.SpecialResources.MISSING_RESOURCE,
                svc.get(identifier, time).toCompletableFuture().join());
    }


    @Test
    void testMementoUtils() {
        final String dir = DBWrappedMementoService.class.getResource("/mementos").getFile();
        final MementoService svc = new DBWrappedMementoService(pg.getPostgresDatabase(),
                new FileMementoService(dir, true));

        final Instant time = now();
        final IRI identifier = rdf.createIRI("trellis:data/resource");

        final Resource mockResource = mock(Resource.class);

        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getContainer()).thenReturn(of(root));
        when(mockResource.stream()).thenAnswer(inv -> Stream.of(
                    rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("Title"))));
        when(mockResource.getBinaryMetadata()).thenReturn(empty());
        when(mockResource.getMemberOfRelation()).thenReturn(empty());
        when(mockResource.getMemberRelation()).thenReturn(empty());
        when(mockResource.getMembershipResource()).thenReturn(empty());
        when(mockResource.getInsertedContentRelation()).thenReturn(empty());

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

        final Resource res = svc.get(identifier, time).toCompletableFuture().join();
        assertEquals(time, res.getModified());
    }
}

