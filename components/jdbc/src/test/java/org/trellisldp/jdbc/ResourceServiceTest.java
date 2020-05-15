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

import static org.junit.jupiter.api.condition.OS.WINDOWS;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.trellisldp.api.ResourceService;
import org.trellisldp.test.AbstractResourceServiceTests;

/**
 * ResourceService tests.
 */
@DisabledOnOs(WINDOWS)
class ResourceServiceTest extends AbstractResourceServiceTests {

    private static final EmbeddedPostgres pg = DBTestUtils.setupDatabase("build");

    private final ResourceService svc = new DBResourceService(pg.getPostgresDatabase());

    @Override
    public ResourceService getResourceService() {
        return svc;
    }
}
