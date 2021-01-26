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

import static java.time.Instant.ofEpochSecond;
import static java.util.Objects.requireNonNull;
import static java.util.ServiceLoader.load;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.commons.rdf.api.IRI;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.trellisldp.api.*;

@Alternative
@Priority(10)
public class DBWrappedMementoService implements MementoService {

    private static final Logger LOGGER = getLogger(DBWrappedMementoService.class);
    private final Jdbi jdbi;
    private final MementoService svc;

    /**
     * Create a new DB enhanced MementoService object.
     *
     * <p>This constructor is generally used by CDI proxies and should
     * not be invoked directly.
     */
    public DBWrappedMementoService() {
        jdbi = null;
        svc = null;
    }

    /**
     * Create a new DB enhanced MementoService object.
     * @param ds the DataSource object
     */
    @Inject
    public DBWrappedMementoService(final DataSource ds) {
        this(ds, load(MementoService.class).findFirst().orElseGet(NoopMementoService::new));
    }

    /**
     * Create a new DB enhanced MementoService object.
     * @param ds the DataSource object
     * @param service the memento service implementation
     */
    public DBWrappedMementoService(final DataSource ds, final MementoService service) {
        this(Jdbi.create(ds), service);
    }

    /**
     * Create a new DB enhanced MementoService object.
     * @param jdbi the JDBI object
     * @param service the memento service implementation
     */
    public DBWrappedMementoService(final Jdbi jdbi, final MementoService service) {
        this.jdbi = requireNonNull(jdbi, "DB connection may not be null!");
        this.svc = requireNonNull(service, "Memento service may not be null!");
    }

    @Override
    public CompletionStage<Resource> get(final IRI identifier, final Instant time) {
        return svc.get(identifier, getTime(identifier, time));
    }

    @Override
    public CompletionStage<Void> put(final Resource resource) {
        if (svc instanceof NoopMementoService) {
            return svc.put(resource);
        }
        return svc.put(resource).thenRun(() -> putTime(resource.getIdentifier(), resource.getModified()));
    }

    @Override
    public CompletionStage<SortedSet<Instant>> mementos(final IRI identifier) {
        if (svc instanceof NoopMementoService) {
            return svc.mementos(identifier);
        }
        return supplyAsync(() -> {
            final SortedSet<Instant> instants = new TreeSet<>();
            jdbi.useHandle(handle -> handle
                    .select("SELECT moment FROM memento WHERE subject = ?")
                    .bind(0, identifier.getIRIString())
                    .mapTo(Long.class)
                    .forEach(moment -> instants.add(ofEpochSecond(moment))));
            return instants;
        });
    }

    private Instant getTime(final IRI identifier, final Instant instant) {
        return jdbi.withHandle(handle -> handle
                .select("SELECT moment FROM memento WHERE subject = ? AND moment <= ? ORDER BY moment DESC")
                .bind(0, identifier.getIRIString())
                .bind(1, instant.getEpochSecond())
                .mapTo(Long.class)
                .findFirst()).map(Instant::ofEpochSecond).orElse(instant);
    }

    private void putTime(final IRI identifier, final Instant instant) {
        try {
            jdbi.useHandle(handle ->
                    handle.execute("INSERT INTO memento (subject, moment) VALUES (?, ?)",
                        identifier.getIRIString(), instant.getEpochSecond()));
        } catch (final UnableToExecuteStatementException ex) {
            LOGGER.debug("Unable to insert memento value: {}", ex.getMessage());
        }
    }
}
