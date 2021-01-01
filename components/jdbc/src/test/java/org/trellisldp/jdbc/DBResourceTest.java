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

import static java.io.File.separator;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Predicate.isEqual;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Metadata.builder;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.vocabulary.RDF.type;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.text.RandomStringGenerator;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.DefaultIdentifierService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.OA;
import org.trellisldp.vocabulary.RDFS;
import org.trellisldp.vocabulary.Trellis;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * ResourceService tests.
 */
@DisabledOnOs(WINDOWS)
class DBResourceTest {

    private static final Logger LOGGER = getLogger(DBResourceTest.class);
    private static final RDF rdf = RDFFactory.getInstance();

    private static final IdentifierService idService = new DefaultIdentifierService();

    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final Map<String, IRI> extensions = singletonMap("acl", Trellis.PreferAccessControl);

    private static EmbeddedPostgres pg = null;

    private static ResourceService svc = null;

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

            svc = new DBResourceService(pg.getPostgresDatabase());

        } catch (final IOException | SQLException | LiquibaseException ex) {
            LOGGER.error("Error setting up tests", ex);
        }
    }

    @Test
    void testNoargResourceService() {
        try {
            System.setProperty(DBResourceService.CONFIG_JDBC_URL, pg.getJdbcUrl("postgres", "postgres"));
            final ResourceService svc2 = new DBResourceService();
            final Resource res = svc2.get(root).toCompletableFuture().join();
            assertEquals(LDP.BasicContainer, res.getInteractionModel());
            assertFalse(res.getContainer().isPresent());
        } finally {
            System.clearProperty(DBResourceService.CONFIG_JDBC_URL);
        }
    }

    @Test
    void testDisableExtendedContainers() {
        try {
            System.setProperty(DBResourceService.CONFIG_JDBC_DIRECT_CONTAINMENT, "false");
            System.setProperty(DBResourceService.CONFIG_JDBC_INDIRECT_CONTAINMENT, "false");
            assertTrue(svc.supportedInteractionModels().contains(LDP.IndirectContainer));
            assertTrue(svc.supportedInteractionModels().contains(LDP.DirectContainer));
            final ResourceService svc2 = new DBResourceService(pg.getPostgresDatabase());
            assertFalse(svc2.supportedInteractionModels().contains(LDP.IndirectContainer));
            assertFalse(svc2.supportedInteractionModels().contains(LDP.DirectContainer));

        } finally {
            System.clearProperty(DBResourceService.CONFIG_JDBC_DIRECT_CONTAINMENT);
            System.clearProperty(DBResourceService.CONFIG_JDBC_INDIRECT_CONTAINMENT);
        }
    }

    @Test
    void testNoargResourceServiceNoConfig() {
        assertDoesNotThrow(() -> new DBResourceService());
    }

    @Test
    void getRoot() {
        assertEquals(root, DBResource.findResource(pg.getPostgresDatabase(), root, extensions, false)
                .toCompletableFuture().join().getIdentifier(), "Check the root resource");
    }

    @Test
    void testReinit() {
        final ResourceService svc2 = new DBResourceService(pg.getPostgresDatabase());
        assertNotNull(svc2);
    }

    @Test
    void getNonExistent() {
        assertEquals(MISSING_RESOURCE, DBResource.findResource(pg.getPostgresDatabase(),
                    rdf.createIRI(TRELLIS_DATA_PREFIX + "other"), extensions, false).toCompletableFuture().join(),
                "Check for non-existent resource");
    }

    @Test
    void testMetadata() {
        final Resource res = DBResource.findResource(pg.getPostgresDatabase(), root, extensions, false)
            .toCompletableFuture().join();
        assertFalse(res.hasMetadata(Trellis.PreferAccessControl));
        assertFalse(res.hasMetadata(rdf.createIRI("http://example.com/Extension")));
    }

    @Test
    void getMembershipQuads() {
        assertAll(
            () -> DBResource.findResource(Jdbi.create(pg.getPostgresDatabase()), root, extensions, false, true, true)
                .thenAccept(res ->
                    assertEquals(0L, res.stream(LDP.PreferMembership).count())).toCompletableFuture().join(),
            () -> DBResource.findResource(Jdbi.create(pg.getPostgresDatabase()), root, extensions, false, false, false)
                .thenAccept(res ->
                    assertEquals(0L, res.stream(LDP.PreferMembership).count())).toCompletableFuture().join());
    }

    @Test
    void getBinary() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "binary");
        final IRI binaryIri = rdf.createIRI("http://example.com/resource");
        final Dataset dataset = rdf.createDataset();
        final BinaryMetadata binary = BinaryMetadata.builder(binaryIri).mimeType("text/plain").build();
        assertNull(svc.create(builder(identifier).interactionModel(LDP.NonRDFSource).container(root).binary(binary)
                    .build(), dataset).toCompletableFuture().join());
        svc.get(identifier).thenAccept(res -> {
            assertTrue(res.getBinaryMetadata().isPresent());
            assertEquals(of(root), res.getContainer());
            assertFalse(res.stream(LDP.PreferContainment).anyMatch(triple ->
                    triple.getSubject().equals(identifier) && triple.getPredicate().equals(DC.hasPart) &&
                    triple.getObject().equals(binaryIri)));
        }).toCompletableFuture().join();
    }

    @Test
    void getRootContent() {
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, root, DC.title, rdf.createLiteral("A title", "eng"));
        assertNull(svc.replace(builder(root).interactionModel(LDP.BasicContainer).build(), dataset)
                .toCompletableFuture().join());
        svc.get(root).thenAccept(res -> {
            assertEquals(LDP.BasicContainer, res.getInteractionModel());
            assertFalse(res.getContainer().isPresent());
            assertTrue(res.stream().anyMatch(quad ->
                        quad.getGraphName().filter(isEqual(Trellis.PreferUserManaged)).isPresent() &&
                        quad.getSubject().equals(root) && quad.getPredicate().equals(DC.title) &&
                        quad.getObject().equals(rdf.createLiteral("A title", "eng"))));
            assertTrue(res.stream(Trellis.PreferUserManaged).anyMatch(triple ->
                    triple.getSubject().equals(root) && triple.getPredicate().equals(DC.title)
                    && triple.getObject().equals(rdf.createLiteral("A title", "eng"))));
        }).toCompletableFuture().join();
    }

    @Test
    void testTouchMethod() {
        final Instant time = svc.get(root).thenApply(Resource::getModified).toCompletableFuture().join();
        svc.touch(root).toCompletableFuture().join();
        assertNotEquals(time, svc.get(root).thenApply(Resource::getModified).toCompletableFuture().join());
    }

    @Test
    void getAclQuads() {
        assertAll(() ->
            DBResource.findResource(pg.getPostgresDatabase(), root, extensions, true).thenAccept(res -> {
                assertEquals(0L, res.stream(Trellis.PreferAccessControl).count());
                assertEquals(0L, res.stream(Trellis.PreferUserManaged).count());
                assertEquals(1L, res.stream(Trellis.PreferServerManaged).count());
            }).toCompletableFuture().join());
    }

    @Test
    void getFilteredServeManagedQuads() {
        assertAll(() ->
            DBResource.findResource(pg.getPostgresDatabase(), root, extensions, false).thenAccept(res -> {
                assertEquals(0L, res.stream(Trellis.PreferUserManaged).count());
                assertEquals(0L, res.stream(Trellis.PreferServerManaged).count());
            }).toCompletableFuture().join());
    }

    @Test
    void testNonEmptyContainer() {
        final IRI iri = rdf.createIRI("http://example.com/#foo");
        final IRI container = rdf.createIRI(TRELLIS_DATA_PREFIX + idService.getSupplier().get());
        final IRI child = rdf.createIRI(container.getIRIString() + "/" + idService.getSupplier().get());
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, container, RDFS.label, rdf.createLiteral("A container"));
        final Dataset childDataset = rdf.createDataset();
        childDataset.add(Trellis.PreferUserManaged, child, FOAF.primaryTopic, iri);
        assertDoesNotThrow(() -> allOf(
                    svc.create(builder(container).interactionModel(LDP.BasicContainer).container(root).build(), dataset)
                        .toCompletableFuture(),
                    svc.create(builder(child).interactionModel(LDP.RDFSource).container(container).build(),
                        childDataset).toCompletableFuture()).join());

        svc.get(container).thenAccept(res -> {
            assertEquals(1L, res.stream(LDP.PreferContainment).count());
            assertEquals(of(rdf.createQuad(LDP.PreferContainment, rdf.createIRI(container.getIRIString() + "/"),
                            LDP.contains, child)), res.stream(LDP.PreferContainment).findFirst());
        }).toCompletableFuture().join();
        assertThrows(CompletionException.class, svc.delete(builder(container).interactionModel(LDP.Resource)
                .build()).toCompletableFuture()::join, "No exception with a non-empty container!");
        assertDoesNotThrow(svc.delete(builder(child).interactionModel(LDP.Resource)
                .build()).toCompletableFuture()::join, "Error deleting child resource");
        assertDoesNotThrow(svc.delete(builder(container).interactionModel(LDP.Resource)
                .build()).toCompletableFuture()::join, "Error deleting child resource");
    }

    @Test
    void testExtensionQuads() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "auth#acl");
        final Dataset dataset = rdf.createDataset();
        final IRI extGraph = rdf.createIRI("http://example.com/TestGraph");
        final IRI relation = rdf.createIRI("http://example.com/Resource");
        dataset.add(extGraph, identifier, DC.relation, relation);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Read);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Write);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Control);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Append);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.agentClass, FOAF.Agent);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.accessTo,
                rdf.createIRI(TRELLIS_DATA_PREFIX + "auth"));

        assertDoesNotThrow(() -> svc.create(builder(identifier).interactionModel(LDP.RDFSource).container(root).build(),
                    dataset).toCompletableFuture().join());
        svc.get(identifier).thenAccept(res -> assertAll("Check response triples",
                    () -> assertEquals(6L, res.stream(Trellis.PreferAccessControl).count()),
                    () -> assertEquals(1L, res.stream(extGraph).count())))
            .toCompletableFuture().join();
    }

    @Test
    void testAuthQuads() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "auth#acl");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Read);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Write);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Control);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Append);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.agentClass, FOAF.Agent);
        dataset.add(Trellis.PreferAccessControl, identifier, ACL.accessTo,
                rdf.createIRI(TRELLIS_DATA_PREFIX + "auth"));

        assertDoesNotThrow(() -> svc.create(builder(identifier).interactionModel(LDP.RDFSource).container(root)
                    .metadataGraphNames(singleton(Trellis.PreferAccessControl)).build(), dataset)
                .toCompletableFuture().join());
        svc.get(identifier).thenAccept(res -> {
            assertTrue(res.hasMetadata(Trellis.PreferAccessControl));
            assertTrue(res.getMetadataGraphNames().contains(Trellis.PreferAccessControl));
            assertEquals(6L, res.stream(Trellis.PreferAccessControl).count());
        }).toCompletableFuture().join();
    }

    @Test
    void testDirectContainer() {
        final IRI member = rdf.createIRI(TRELLIS_DATA_PREFIX + idService.getSupplier().get());
        final IRI dc = rdf.createIRI(TRELLIS_DATA_PREFIX + idService.getSupplier().get());
        final IRI child = rdf.createIRI(dc.getIRIString() + "/" + idService.getSupplier().get());
        final Dataset dcDataset = rdf.createDataset();
        dcDataset.add(Trellis.PreferUserManaged, dc, LDP.hasMemberRelation, LDP.member);
        dcDataset.add(Trellis.PreferUserManaged, dc, LDP.membershipResource, member);
        assertDoesNotThrow(() -> allOf(
                    svc.create(builder(dc).interactionModel(LDP.DirectContainer).container(root)
                        .memberRelation(LDP.member).membershipResource(member).build(), dcDataset)
                        .toCompletableFuture(),
                    svc.create(builder(member).interactionModel(LDP.RDFSource).container(root).build(),
                        rdf.createDataset()).toCompletableFuture(),
                    svc.create(builder(child).interactionModel(LDP.RDFSource).container(dc).build(),
                        rdf.createDataset()).toCompletableFuture()).join());

        svc.get(member).thenAccept(res -> {
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertEquals(of(rdf.createQuad(LDP.PreferMembership, member, LDP.member, child)),
                    res.stream(LDP.PreferMembership).findFirst());
        }).toCompletableFuture().join();
    }

    @Test
    void testDirectContainerInverse() {
        final IRI dc = rdf.createIRI(TRELLIS_DATA_PREFIX + idService.getSupplier().get());
        final IRI child = rdf.createIRI(dc.getIRIString() + "/" + idService.getSupplier().get());
        final IRI member = rdf.createIRI(TRELLIS_DATA_PREFIX + idService.getSupplier().get());
        final Dataset dcDataset = rdf.createDataset();
        dcDataset.add(Trellis.PreferUserManaged, dc, LDP.isMemberOfRelation, DC.isPartOf);
        dcDataset.add(Trellis.PreferUserManaged, dc, LDP.membershipResource, member);
        assertDoesNotThrow(() -> allOf(
                    svc.create(builder(dc).interactionModel(LDP.DirectContainer).container(root)
                        .memberOfRelation(DC.isPartOf).membershipResource(member).build(), dcDataset)
                        .toCompletableFuture(),
                    svc.create(builder(member).interactionModel(LDP.Container).container(root).build(),
                        rdf.createDataset()).toCompletableFuture(),
                    svc.create(builder(child).interactionModel(LDP.RDFSource).container(dc).build(),
                        rdf.createDataset()).toCompletableFuture()).join());

        svc.get(child).thenAccept(res -> {
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertEquals(of(rdf.createQuad(LDP.PreferMembership, child, DC.isPartOf, member)),
                    res.stream(LDP.PreferMembership).findFirst());
        }).toCompletableFuture().join();
    }

    @Test
    void testIndirectContainer() {
        final IRI iri = rdf.createIRI("http://example.com/#foo");
        final IRI member = rdf.createIRI(TRELLIS_DATA_PREFIX + idService.getSupplier().get());
        final IRI ic = rdf.createIRI(TRELLIS_DATA_PREFIX + idService.getSupplier().get());
        final IRI child = rdf.createIRI(ic.getIRIString() + "/" + idService.getSupplier().get());
        final Dataset icDataset = rdf.createDataset();
        icDataset.add(Trellis.PreferUserManaged, ic, LDP.membershipResource, member);
        icDataset.add(Trellis.PreferUserManaged, ic, LDP.hasMemberRelation, LDP.member);
        icDataset.add(Trellis.PreferUserManaged, ic, LDP.insertedContentRelation, FOAF.primaryTopic);
        final Dataset childDataset = rdf.createDataset();
        childDataset.add(Trellis.PreferUserManaged, child, FOAF.primaryTopic, iri);
        assertDoesNotThrow(() -> allOf(
                    svc.create(builder(ic).interactionModel(LDP.IndirectContainer).container(root)
                        .membershipResource(member).memberRelation(LDP.member)
                        .insertedContentRelation(FOAF.primaryTopic).build(), icDataset)
                        .toCompletableFuture(),
                    svc.create(builder(member).interactionModel(LDP.RDFSource).container(root).build(),
                        rdf.createDataset()).toCompletableFuture(),
                    svc.create(builder(child).interactionModel(LDP.RDFSource).container(ic).build(),
                        childDataset).toCompletableFuture()).join());

        svc.get(member).thenAccept(res -> {
            assertEquals(1L, res.stream(LDP.PreferMembership).count());
            assertEquals(of(rdf.createQuad(LDP.PreferMembership, member, LDP.member, iri)),
                    res.stream(LDP.PreferMembership).findFirst());
        }).toCompletableFuture().join();
    }

    @Test
    void testEmptyAudit() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + idService.getSupplier().get());

        assertNull(svc.create(builder(identifier).interactionModel(LDP.RDFSource).container(root).build(),
                    rdf.createDataset()).toCompletableFuture().join());
        assertNull(svc.add(identifier, rdf.createDataset()).toCompletableFuture().join());
        svc.get(identifier).thenAccept(res -> assertEquals(0L, res.stream(Trellis.PreferAudit).count()))
            .toCompletableFuture().join();
    }

    @Test
    void getExtraLinkRelations() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "extras");
        final String inbox = "http://example.com/inbox";
        final String annotations = "http://example.com/annotations";
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferUserManaged, identifier, LDP.inbox, rdf.createIRI(inbox));
        dataset.add(Trellis.PreferUserManaged, identifier, OA.annotationService, rdf.createIRI(annotations));
        assertNull(svc.create(builder(identifier).interactionModel(LDP.RDFSource).container(root).build(), dataset)
                .toCompletableFuture().join());
        svc.get(identifier).thenAccept(res ->
            assertTrue(res.stream(Trellis.PreferUserManaged).anyMatch(triple ->
                    triple.getSubject().equals(identifier) && triple.getPredicate().equals(LDP.inbox) &&
                    triple.getObject().equals(rdf.createIRI(inbox))))).toCompletableFuture().join();
        DBResource.findResource(pg.getPostgresDatabase(), identifier, extensions, true).thenAccept(res -> {
            assertEquals(2L, res.stream(Trellis.PreferUserManaged).count());
            assertEquals(1L, res.stream(Trellis.PreferServerManaged).count());
            assertEquals(2L, res.getExtraLinkRelations().count());
            assertTrue(res.getExtraLinkRelations().anyMatch(rel -> rel.getKey().equals(annotations)
                        && rel.getValue().equals(OA.annotationService.getIRIString())));
            assertTrue(res.getExtraLinkRelations().anyMatch(rel -> rel.getKey().equals(inbox)
                        && rel.getValue().equals(LDP.inbox.getIRIString())));
        }).toCompletableFuture().join();
    }

    @Test
    void testAddErrorCondition() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Dataset dataset = rdf.createDataset();
        dataset.add(Trellis.PreferAudit, rdf.createBlankNode(), type, rdf.createLiteral("Invalid quad"));
        final CompletableFuture<Void> future = svc.add(identifier, dataset).toCompletableFuture();
        assertThrows(CompletionException.class, future::join);
    }

    @Test
    void testCreateErrorCondition() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final CompletableFuture<Void> future = svc.create(builder(identifier)
                .interactionModel(LDP.RDFSource).build(), null).toCompletableFuture();
        assertThrows(CompletionException.class, future::join);
    }

    @Test
    void testTouchErrorCondition() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Jdbi mockJdbi = mock(Jdbi.class);
        doThrow(RuntimeException.class).when(mockJdbi).useHandle(any());

        final ResourceService svc2 = new DBResourceService(mockJdbi, 100, false, idService);
        final CompletableFuture<Void> future = svc2.touch(identifier).toCompletableFuture();
        assertThrows(CompletionException.class, future::join);
    }

    @Test
    void testCreateErrorCondition2() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        final Dataset dataset = rdf.createDataset();
        final Jdbi mockJdbi = mock(Jdbi.class);
        doThrow(RuntimeException.class).when(mockJdbi).useTransaction(any());

        final ResourceService svc2 = new DBResourceService(mockJdbi, 100, false, idService);
        final CompletableFuture<Void> future = svc2.create(builder(identifier).interactionModel(LDP.Container).build(),
                dataset).toCompletableFuture();

        assertThrows(CompletionException.class, future::join);
    }

    @Test
    void testAdjustIdentifier() {
        final String identifier = TRELLIS_DATA_PREFIX + "resource";
        assertEquals(identifier + "/", DBResource.adjustIdentifier(identifier, LDP.Container.getIRIString()));
        assertEquals(identifier + "/", DBResource.adjustIdentifier(identifier + "/", LDP.Container.getIRIString()));
        assertEquals(identifier, DBResource.adjustIdentifier(identifier, LDP.RDFSource.getIRIString()));
    }

    @Test
    void testBuildExtensionMap() {
        final Map<String, IRI> extensions = DBResourceService.buildExtensionMap(
                "foo=http://example.com/Foo,bar=http://example.com/Bar");
        assertEquals(2, extensions.size());
        assertEquals(rdf.createIRI("http://example.com/Foo"), extensions.get("foo"));
        assertEquals(rdf.createIRI("http://example.com/Bar"), extensions.get("bar"));
    }

    @Test
    void testBuildExtensionMapOddities() {
        final Map<String, IRI> extensions = DBResourceService.buildExtensionMap(
                "foo, ,bar=http://example.com/Bar, baz = , = baz ");
        assertEquals(1, extensions.size());
        assertEquals(rdf.createIRI("http://example.com/Bar"), extensions.get("bar"));
    }

    @Test
    void testBuildDefaultExtensionMap() {
        final String prop = "trellis.http.extension-graphs";
        final String original = getConfig().getValue(prop, String.class);
        try {
            System.clearProperty(prop);
            final Map<String, IRI> graphs = DBResourceService.buildExtensionMap();
            assertEquals(1, graphs.size());
            assertEquals(Trellis.PreferAccessControl, graphs.get("acl"));
        } finally {
            System.setProperty(prop, original);
        }
    }

    @Test
    void testSerializationError() {
        final Graph mockGraph = mock(Graph.class, inv -> {
            throw new IOException("Expected");
        });
        assertThrows(UncheckedIOException.class, () -> DBResourceService.serializeGraph(mockGraph));
    }
}
