/*
 * Copyright (c) Aaron Coburn and individual contributors
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.slf4j.Logger;
import org.trellisldp.vocabulary.LDP;

@DisabledOnOs(WINDOWS)
class DBNamespaceServiceTest {
    private static final Logger LOGGER = getLogger(DBNamespaceService.class);

    private static DataSource ds = DBTestUtils.setupDatabase();

    @Test
    void testNamespaceService() {
        final DBNamespaceService svc = new DBNamespaceService();
        svc.ds = ds;
        assertDoesNotThrow(svc::init);

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

