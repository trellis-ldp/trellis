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
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.slf4j.LoggerFactory.getLogger;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.slf4j.Logger;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.vocabulary.LDP;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

@DisabledOnOs(WINDOWS)
class DBNamespaceServiceTest {
    private static final Logger LOGGER = getLogger(DBNamespaceService.class);

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
    void testNoargNamespaceService() {
        try {
            System.setProperty(DBResourceService.CONFIG_JDBC_URL, pg.getJdbcUrl("postgres", "postgres"));
            final NamespaceService svc = new DBNamespaceService();

            assertTrue(svc.getNamespaces().containsKey("ldp"));
            assertEquals(LDP.getNamespace(), svc.getNamespaces().get("ldp"));
        } finally {
            System.clearProperty(DBResourceService.CONFIG_JDBC_URL);
        }
    }

    @Test
    void testNoargNamespaceServiceNoConfig() {
        assertDoesNotThrow(() -> new DBNamespaceService());
    }

    @Test
    void testNamespaceService() {
        final NamespaceService svc = new DBNamespaceService(pg.getPostgresDatabase());

        assertTrue(svc.getNamespaces().containsKey("ldp"));
        assertEquals(LDP.getNamespace(), svc.getNamespaces().get("ldp"));
        final int size = svc.getNamespaces().size();

        assertTrue(svc.setPrefix("ex", "http://example.com/"));
        assertEquals(size + 1, svc.getNamespaces().size());
        assertEquals("http://example.com/", svc.getNamespaces().get("ex"));
        assertFalse(svc.setPrefix("ex", "http://example.com/Other/"));
        assertFalse(svc.setPrefix("", "http://example.com/Resource/"));
    }
}

