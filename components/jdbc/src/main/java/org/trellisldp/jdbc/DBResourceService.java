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
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toMap;
import static org.apache.jena.commonsrdf.JenaCommonsRDF.toJena;
import static org.apache.jena.riot.Lang.NTRIPLES;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.jdbc.DBUtils.getObjectDatatype;
import static org.trellisldp.jdbc.DBUtils.getObjectLang;
import static org.trellisldp.jdbc.DBUtils.getObjectValue;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferAudit;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Update;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.StorageConflictException;
import org.trellisldp.api.TrellisRuntimeException;
import org.trellisldp.api.TrellisUtils;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;

/**
 * A Database-backed implementation of the Trellis ResourceService API.
 *
 * <p>Note: one can manipulate the size of a batched query by setting
 * a property for {@code trellis.db.batch-size}. By default, this
 * value is 1,000. One can also configure the persistence layer to add
 * the LDP type to the body of an RDF response by setting the environment
 * variable {@code trellis.db.ldp.type} to "true". By default, this value
 * is false.
 */
@ApplicationScoped
public class DBResourceService implements ResourceService {

    /** Copied from trellis-http in order to avoid an explicit dependency. */
    private static final String CONFIG_HTTP_EXTENSION_GRAPHS = "trellis.http.extension-graphs";

    /** Configuration key used to define a database connection url. */
    public static final String CONFIG_JDBC_URL = "trellis.jdbc.url";

    /** Configuration key used to define the size of database write batches. */
    public static final String CONFIG_JDBC_BATCH_SIZE = "trellis.jdbc.batch-size";

    /** The configuration key used to define whether to include the LDP type in an RDF body. */
    public static final String CONFIG_JDBC_LDP_TYPE = "trellis.jdbc.ldp-type";

    /** The configuration key used to define whether indirect containers are supported. */
    public static final String CONFIG_JDBC_DIRECT_CONTAINMENT = "trellis.jdbc.direct-containment";

    /** The configuration key used to define whether direct containers are supported. */
    public static final String CONFIG_JDBC_INDIRECT_CONTAINMENT = "trellis.jdbc.indirect-containment";

    private static final Logger LOGGER = getLogger(DBResourceService.class);
    private static final RDF rdf = RDFFactory.getInstance();
    private static final String ACL_EXT = "acl";

    private Supplier<String> supplier;
    private Jdbi jdbi;
    private Map<String, IRI> extensions;
    private Set<IRI> supportedIxnModels;

    @Inject
    @ConfigProperty(name = CONFIG_HTTP_EXTENSION_GRAPHS)
    Optional<String[]> extensionConfig;

    @Inject
    @ConfigProperty(name = CONFIG_JDBC_BATCH_SIZE,
                    defaultValue = "1000")
    int batchSize;

    @Inject
    @ConfigProperty(name = CONFIG_JDBC_LDP_TYPE,
                    defaultValue = "true")
    boolean includeLdpType;

    @Inject
    @ConfigProperty(name = CONFIG_JDBC_DIRECT_CONTAINMENT,
                    defaultValue = "true")
    boolean supportDirectContainment;

    @Inject
    @ConfigProperty(name = CONFIG_JDBC_INDIRECT_CONTAINMENT,
                    defaultValue = "true")
    boolean supportIndirectContainment;

    @Inject
    IdentifierService idService;

    @Inject
    DataSource ds;

    @PostConstruct
    void init() {
        jdbi = Jdbi.create(ds);
        supplier = idService.getSupplier();
        extensions = extensionConfig.map(DBResourceService::buildExtensionMap).orElseGet(() ->
                    singletonMap(ACL_EXT, PreferAccessControl));

        final Set<IRI> ixnModels = new HashSet<>(asList(LDP.Resource, LDP.RDFSource, LDP.NonRDFSource, LDP.Container,
                    LDP.BasicContainer));
        if (supportDirectContainment) {
            ixnModels.add(LDP.DirectContainer);
        }
        if (supportIndirectContainment) {
            ixnModels.add(LDP.IndirectContainer);
        }
        supportedIxnModels = unmodifiableSet(ixnModels);
        LOGGER.info("Using database persistence with TrellisLDP");
    }

    @Override
    public CompletionStage<Void> create(final Metadata metadata, final Dataset dataset) {
        LOGGER.debug("Creating: {}", metadata.getIdentifier());
        return runAsync(() -> storeResource(metadata, dataset, now(), OperationType.CREATE));
    }

    @Override
    public CompletionStage<Void> replace(final Metadata metadata, final Dataset dataset) {
        LOGGER.debug("Updating: {}", metadata.getIdentifier());
        return runAsync(() -> storeResource(metadata, dataset, now(), OperationType.REPLACE));
    }

    @Override
    public CompletionStage<Void> touch(final IRI id) {
        LOGGER.debug("Updating modification date for {}", id);
        final Instant time = now();
        return runAsync(() -> updateResourceModification(id, time));
    }

    @Override
    public CompletionStage<Resource> get(final IRI identifier) {
        return DBResource.findResource(jdbi, identifier, extensions, includeLdpType,
                supportDirectContainment, supportIndirectContainment);
    }

    @Override
    public String generateIdentifier() {
        return supplier.get();
    }

    @Override
    public CompletionStage<Void> add(final IRI id, final Dataset dataset) {
        final String query
            = "INSERT INTO log (id, subject, predicate, object, lang, datatype) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
        return runAsync(() -> {
            try {
                jdbi.useHandle(handle -> dataset.getGraph(PreferAudit).ifPresent(graph -> {
                        try (final PreparedBatch batch = handle.prepareBatch(query)) {
                            graph.stream().forEach(triple -> batch
                                    .bind(0, id.getIRIString())
                                    .bind(1, ((IRI) triple.getSubject()).getIRIString())
                                    .bind(2, triple.getPredicate().getIRIString())
                                    .bind(3, getObjectValue(triple.getObject()))
                                    .bind(4, getObjectLang(triple.getObject()))
                                    .bind(5, getObjectDatatype(triple.getObject())).add());
                            if (batch.size() > 0) {
                                batch.execute();
                            }
                        }
                    }));
            } catch (final Exception ex) {
                throw new TrellisRuntimeException("Error storing audit dataset for " + id, ex);
            }
        });
    }

    @Override
    public Set<IRI> supportedInteractionModels() {
        return supportedIxnModels;
    }

    @Override
    public CompletionStage<Void> delete(final Metadata metadata) {
        LOGGER.debug("Deleting: {}", metadata.getIdentifier());
        final Metadata md = Metadata.builder(metadata.getIdentifier()).interactionModel(LDP.Resource).build();
        return delete(md, rdf.createDataset());
    }

    private CompletionStage<Void> delete(final Metadata metadata, final Dataset dataset) {
        return runAsync(() -> storeResource(metadata, dataset, now(), OperationType.DELETE))
            .whenComplete((a, b) -> DBUtils.closeDataset(dataset));
    }

    private void updateResourceModification(final IRI identifier, final Instant time) {
        final String query = "UPDATE resource SET modified=? WHERE subject=?";
        try {
            jdbi.useHandle(handle -> {
                try (final Update update = handle.createUpdate(query)
                        .bind(0, time.toEpochMilli())
                        .bind(1, identifier.getIRIString())) {
                    update.execute();
                }
            });
        } catch (final Exception ex) {
            throw new TrellisRuntimeException("Error updating modification date for " + identifier, ex);
        }
    }

    private static int updateResource(final Handle handle, final Metadata metadata, final Dataset dataset,
            final Instant time, final boolean isDelete) {

        handle.execute("DELETE FROM resource WHERE subject = ?", metadata.getIdentifier().getIRIString());
        final String query
            = "INSERT INTO resource (subject, interaction_model, modified, deleted, is_part_of, acl, "
            + "ldp_member, ldp_membership_resource, ldp_has_member_relation, ldp_is_member_of_relation, "
            + "ldp_inserted_content_relation, binary_location, binary_format) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // Set ldp:insertedContentRelation only for LDP-IC and LDP-DC resources
        final String icr = asList(LDP.DirectContainer, LDP.IndirectContainer).contains(metadata.getInteractionModel())
            ? metadata.getInsertedContentRelation().orElse(LDP.MemberSubject).getIRIString() : null;

        try (final Update update = handle.createUpdate(query)
                .bind(0, metadata.getIdentifier().getIRIString())
                .bind(1, metadata.getInteractionModel().getIRIString())
                .bind(2, time.toEpochMilli())
                .bind(3, isDelete)
                .bind(4, metadata.getContainer().map(IRI::getIRIString).orElse(null))
                .bind(5, dataset.contains(of(PreferAccessControl), null, null, null))
                .bind(6, metadata.getMembershipResource().map(TrellisUtils::normalizeIdentifier)
                    .map(IRI::getIRIString).orElse(null))
                .bind(7, metadata.getMembershipResource().map(IRI::getIRIString).orElse(null))
                .bind(8, metadata.getMemberRelation().map(IRI::getIRIString).orElse(null))
                .bind(9, metadata.getMemberOfRelation().map(IRI::getIRIString).orElse(null))
                .bind(10, icr)
                .bind(11, metadata.getBinary().map(BinaryMetadata::getIdentifier).map(IRI::getIRIString).orElse(null))
                .bind(12, metadata.getBinary().flatMap(BinaryMetadata::getMimeType).orElse(null))) {
            return update.executeAndReturnGeneratedKeys("id").mapTo(Integer.class).one();
        }
    }

    private static void updateDescription(final Handle handle, final int resourceId, final Dataset dataset,
            final int batchSize) {
        dataset.getGraph(PreferUserManaged).ifPresent(graph ->
                batchUpdateTriples(handle, resourceId, "description", graph, batchSize));
    }

    private static void updateAcl(final Handle handle, final int resourceId, final Dataset dataset,
            final int batchSize) {
        dataset.getGraph(PreferAccessControl).ifPresent(graph ->
                batchUpdateTriples(handle, resourceId, "acl", graph, batchSize));
    }

    private static void batchUpdateTriples(final Handle handle, final int resourceId, final String table,
            final Graph graph, final int batchSize) {
        final String query
            = "INSERT INTO " + table + " (resource_id, subject, predicate, object, lang, datatype) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
        try (final PreparedBatch batch = handle.prepareBatch(query)) {
            graph.stream().sequential().forEach(triple -> {
                batch.bind(0, resourceId)
                     .bind(1, ((IRI) triple.getSubject()).getIRIString())
                     .bind(2, triple.getPredicate().getIRIString())
                     .bind(3, getObjectValue(triple.getObject()))
                     .bind(4, getObjectLang(triple.getObject()))
                     .bind(5, getObjectDatatype(triple.getObject())).add();
                if (batch.size() >= batchSize) {
                    batch.execute();
                }
            });
            if (batch.size() > 0) {
                batch.execute();
            }
        }
    }

    private static void updateExtension(final Handle handle, final int resourceId, final String ext,
            final Graph graph) {
        final String query = "INSERT INTO extension (resource_id, ext, data) VALUES (?, ?, ?)";
        final String serialized = serializeGraph(graph);
        try (final Update update = handle.createUpdate(query).bind(0, resourceId).bind(1, ext).bind(2, serialized)) {
            update.execute();
        }
    }

    private static void updateExtra(final Handle handle, final int resourceId, final IRI identifier,
            final Dataset dataset) {
        dataset.getGraph(PreferUserManaged).ifPresent(graph -> {
            final String query = "INSERT INTO extra (resource_id, predicate, object) VALUES (?, ?, ?)";
            try (final PreparedBatch batch = handle.prepareBatch(query)) {
                graph.stream(identifier, LDP.inbox, null).map(Triple::getObject).filter(IRI.class::isInstance)
                    .map(IRI.class::cast).map(IRI::getIRIString).findFirst().ifPresent(iri ->
                            batch.bind(0, resourceId)
                                 .bind(1, LDP.inbox.getIRIString())
                                 .bind(2, iri)
                                 .add());

                graph.stream(identifier, OA.annotationService, null).map(Triple::getObject)
                    .filter(IRI.class::isInstance).map(IRI.class::cast).map(IRI::getIRIString).findFirst()
                    .ifPresent(iri ->
                            batch.bind(0, resourceId)
                                 .bind(1, OA.annotationService.getIRIString())
                                 .bind(2, iri).add());

                if (batch.size() > 0) {
                    batch.execute();
                }
            }
        });
    }

    private void storeResource(final Metadata metadata, final Dataset dataset, final Instant time,
            final OperationType opType) {
        try {
            jdbi.useTransaction(handle -> {
                final int resourceId = updateResource(handle, metadata, dataset, time,
                        opType == OperationType.DELETE);
                updateDescription(handle, resourceId, dataset, batchSize);
                updateAcl(handle, resourceId, dataset, batchSize);
                updateExtra(handle, resourceId, metadata.getIdentifier(), dataset);
                extensions.forEach((ext, graph) ->
                        dataset.getGraph(graph).filter(g -> !"acl".equals(ext)).ifPresent(g ->
                            updateExtension(handle, resourceId, ext, g)));
                if (opType == OperationType.DELETE) {
                    // Verify that the container really is empty
                    final String query = "SELECT EXISTS(SELECT 1 FROM resource WHERE is_part_of = ?)";
                    if (Boolean.TRUE.equals(handle.select(query, metadata.getIdentifier().getIRIString())
                            .map((rs, ctx) -> rs.getBoolean(1)).one())) {
                        throw new StorageConflictException("Cannot delete non-empty containers");
                    }
                }
            });
        } catch (final TrellisRuntimeException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new TrellisRuntimeException("Could not update data for " + metadata.getIdentifier(), ex);
        }
    }

    private enum OperationType {
        DELETE, CREATE, REPLACE
    }

    /*
     * Build a map suitable for extension graphs from a config string.
     * @param extensions the config values
     * @return the formatted map
     */
    static Map<String, IRI> buildExtensionMap(final String[] extensions) {
        return stream(extensions).map(item -> item.split("=")).filter(kv -> kv.length == 2)
            .filter(kv -> !kv[0].trim().isEmpty() && !kv[1].trim().isEmpty())
            .collect(toMap(kv -> kv[0].trim(), kv -> rdf.createIRI(kv[1].trim())));
    }

    /**
     * Serialize a graph into a string of N-Triples.
     * @param graph the RDF graph
     * @return the serialized form
     */
    static String serializeGraph(final Graph graph) {
        try (final StringWriter writer = new StringWriter()) {
            RDFDataMgr.write(writer, toJena(graph), NTRIPLES);
            return writer.toString();
        } catch (final IOException ex) {
            throw new UncheckedIOException("Error writing extension data", ex);
        }
    }
}
