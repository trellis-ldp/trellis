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

import static org.slf4j.LoggerFactory.getLogger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementException;
import org.slf4j.Logger;
import org.trellisldp.api.NamespaceService;

/**
 * A namespace service that stores data in a database.
 */
@Alternative
public class DBNamespaceService implements NamespaceService {

    private static final Logger LOGGER = getLogger(DBNamespaceService.class);

    private Jdbi jdbi;

    @Inject
    DataSource ds;

    @PostConstruct
    void init() {
        jdbi = Jdbi.create(ds);
        LOGGER.info("Initialized DB Namespace Service");
    }

    @Override
    public Map<String, String> getNamespaces() {
        final Map<String, String> namespaces = new HashMap<>();
        jdbi.useHandle(handle ->
                handle.select("SELECT prefix, namespace FROM namespaces")
                    .map((rs, ctx) -> Map.entry(rs.getString("prefix"), rs.getString("namespace")))
                    .forEach(pair -> namespaces.put(pair.getKey(), pair.getValue())));
        return namespaces;
    }

    @Override
    public boolean setPrefix(final String prefix, final String namespace) {
        if (!prefix.isEmpty()) {
            try {
                jdbi.useHandle(handle ->
                    handle.execute("INSERT INTO namespaces (prefix, namespace) VALUES (?, ?)", prefix, namespace));
                return true;
            } catch (final StatementException ex) {
                LOGGER.debug("Could not save prefix {} with namespace {}: {}", prefix, namespace, ex.getMessage());
            }
        }
        return false;
    }
}
