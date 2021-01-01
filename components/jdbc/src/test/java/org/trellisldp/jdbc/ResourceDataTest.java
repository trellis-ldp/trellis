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
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * ResourceService tests.
 */
@DisabledOnOs(WINDOWS)
class ResourceDataTest {

    private static final RDF rdf = RDFFactory.getInstance();
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final EmbeddedPostgres pg = DBTestUtils.setupDatabase("build");

    private final Map<String, IRI> extensions = singletonMap("acl", Trellis.PreferAccessControl);

    @Test
    void testTimestampOnRootIsRecent() {
        final Instant time = now().minusSeconds(1L);
        final Resource res = DBResource.findResource(pg.getPostgresDatabase(), root, extensions, false)
            .toCompletableFuture().join();
        assertEquals(root, res.getIdentifier());
        assertTrue(res.getModified().isAfter(time));
    }

    @Test
    void testNoIxnModel() throws SQLException {
        final ResultSet mockResultSet = mock(ResultSet.class);
        final ResourceData rd = new ResourceData(mockResultSet);
        assertEquals(LDP.Resource, rd.getInteractionModel());

        when(mockResultSet.getString(eq("interaction_model"))).thenReturn("http://www.w3.org/ns/ldp#RDFSource");

        final ResourceData rd2 = new ResourceData(mockResultSet);
        assertEquals(LDP.RDFSource, rd2.getInteractionModel());
    }
}
