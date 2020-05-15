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

import java.sql.Connection;
import java.sql.SQLException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

/**
 * Check the health of the database connection.
 */
@Liveness
@Readiness
@ApplicationScoped
public class DBHealthCheck implements HealthCheck {

    private final DataSource dataSource;

    /**
     * For use with RESTeasy and CDI proxies.
     *
     * @apiNote This construtor is used by CDI runtimes that require a public, no-argument constructor.
     *          It should not be invoked directly in user code.
     */
    public DBHealthCheck() {
        this(null);
    }

    /**
     * Create a db connection health checker.
     * @param dataSource the datasource
     */
    @Inject
    public DBHealthCheck(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public HealthCheckResponse call() {
        if (dataSource != null) {
            try (final Connection conn = dataSource.getConnection()) {
                return HealthCheckResponse.named(DBHealthCheck.class.getSimpleName())
                    .state(!conn.isClosed()).build();
            } catch (final SQLException ex) {
                return HealthCheckResponse.named(DBHealthCheck.class.getSimpleName())
                    .withData("exception", ex.getMessage()).down().build();
            }
        }
        return HealthCheckResponse.named(DBHealthCheck.class.getSimpleName()).down().build();
    }
}
