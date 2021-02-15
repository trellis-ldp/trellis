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

import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.testcontainers.containers.PostgreSQLContainer;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * DB Test Utilities.
 */
final class DBTestUtils {

    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:" + getConfig()
            .getValue("trellis.jdbc.test.postgres-container-version", String.class));
    private static final Logger LOGGER = getLogger(DBTestUtils.class);

    private static DataSource datasource;

    static void initialize() {
        postgres.start();
        final PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(postgres.getJdbcUrl());
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        try {
            // Set up database migrations
            try (final Connection c = ds.getConnection()) {
                final Liquibase liquibase = new Liquibase("org/trellisldp/jdbc/migrations.yml",
                        new ClassLoaderResourceAccessor(),
                        new JdbcConnection(c));
                final Contexts ctx = null;
                liquibase.update(ctx);
            }
        } catch (final SQLException | LiquibaseException ex) {
            LOGGER.error("Error setting up tests", ex);
        }
        datasource = ds;
    }

    static synchronized DataSource setupDatabase() {
        if (datasource == null) {
            initialize();
        }
        return datasource;
    }

    private DBTestUtils() {
        // prevent instantiation
    }
}
