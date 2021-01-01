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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

class DBHealthCheckTest {

    @Test
    void testUnhealthyDefault() {
        final HealthCheck check = new DBHealthCheck();
        assertEquals(HealthCheckResponse.State.DOWN, check.call().getState(), "Database connection isn't healthy!");
    }

    @Test
    void testHealthy() throws SQLException {
        final DataSource mockDataSource = mock(DataSource.class);
        final Connection mockConnection = mock(Connection.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isClosed()).thenReturn(false);

        final HealthCheck check = new DBHealthCheck(mockDataSource);
        assertEquals(HealthCheckResponse.State.UP, check.call().getState(), "Database connection isn't healthy!");
    }

    @Test
    void testThrowsUnhealthy() throws SQLException {
        final DataSource mockDataSource = mock(DataSource.class);
        doThrow(SQLException.class).when(mockDataSource).getConnection();

        final HealthCheck check = new DBHealthCheck(mockDataSource);
        assertEquals(HealthCheckResponse.State.DOWN, check.call().getState(), "Database connection isn't healthy!");
    }

    @Test
    void testUnhealthy() throws SQLException {
        final DataSource mockDataSource = mock(DataSource.class);
        final Connection mockConnection = mock(Connection.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isClosed()).thenReturn(true);

        final HealthCheck check = new DBHealthCheck(mockDataSource);
        assertEquals(HealthCheckResponse.State.DOWN, check.call().getState(),
                "Database connection doesn't report as unhealthy!");
    }
}
