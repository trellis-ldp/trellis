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
package org.trellisldp.triplestore;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.builder;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.query.DatasetFactory.createTxnMem;
import static org.apache.jena.query.DatasetFactory.wrap;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.apache.jena.system.Txn.executeWrite;
import static org.apache.jena.tdb2.DatabaseMgr.connectDatasetGraph;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.normalizeIdentifier;
import static org.trellisldp.triplestore.TriplestoreUtils.OBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.PREDICATE;
import static org.trellisldp.triplestore.TriplestoreUtils.SUBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.asJenaDataset;
import static org.trellisldp.triplestore.TriplestoreUtils.getInstance;
import static org.trellisldp.triplestore.TriplestoreUtils.getObject;
import static org.trellisldp.vocabulary.Trellis.DeletedResource;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferAudit;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.trellisldp.api.DefaultIdentifierService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.XSD;

/**
 * A triplestore-based implementation of the Trellis ResourceService API.
 */
@ApplicationScoped
public class TriplestoreResourceService implements ResourceService {

    /** Copied from trellis-http in order to avoid an explicit dependency. */
    private static final String CONFIG_HTTP_EXTENSION_GRAPHS = "trellis.http.extension-graphs";
    /** The configuration key used to set where the RDF is stored. */
    public static final String CONFIG_TRIPLESTORE_RDF_LOCATION = "trellis.triplestore.rdf-location";
    /** The configuration key used to set whether the LDP type should be included in the body of the RDF. */
    public static final String CONFIG_TRIPLESTORE_LDP_TYPE = "trellis.triplestore.ldp-type";

    private static final String MODIFIED = "modified";

    private static final Logger LOGGER = getLogger(TriplestoreResourceService.class);
    private static final JenaRDF rdf = getInstance();
    private static final String ACL_EXT = "acl";

    private final Supplier<String> supplier;
    private final RDFConnection rdfConnection;
    private final boolean includeLdpType;
    private final Set<IRI> supportedIxnModels;
    private final Map<String, IRI> extensions;

    /**
     * Create a triplestore-backed resource service.
     */
    public TriplestoreResourceService() {
        this(buildRDFConnection(getConfig().getOptionalValue(CONFIG_TRIPLESTORE_RDF_LOCATION, String.class)
                    .orElse(null)));
    }

    /**
     * Create a triplestore-backed resource service.
     * @param rdfConnection the connection to an RDF datastore
     */
    public TriplestoreResourceService(final RDFConnection rdfConnection) {
        this(rdfConnection, new DefaultIdentifierService());
    }

    /**
     * Create a triplestore-backed resource service.
     * @param rdfConnection the connection to an RDF datastore
     * @param identifierService an ID supplier service
     */
    @Inject
    public TriplestoreResourceService(final RDFConnection rdfConnection, final IdentifierService identifierService) {
        this.includeLdpType = getConfig().getOptionalValue(CONFIG_TRIPLESTORE_LDP_TYPE, Boolean.class)
            .orElse(Boolean.TRUE);
        this.rdfConnection = requireNonNull(rdfConnection, "RDFConnection may not be null!");
        this.supplier = requireNonNull(identifierService, "IdentifierService may not be null!").getSupplier();
        this.supportedIxnModels = unmodifiableSet(new HashSet<>(asList(LDP.Resource, LDP.RDFSource, LDP.NonRDFSource,
                LDP.Container, LDP.BasicContainer, LDP.DirectContainer, LDP.IndirectContainer)));
        this.extensions = buildExtensionMap();
    }

    @Override
    public CompletionStage<Void> delete(final Metadata metadata) {
        LOGGER.debug("Deleting: {}", metadata.getIdentifier());
        return runAsync(() -> {
            try (final Dataset dataset = rdf.createDataset()) {
                final Instant eventTime = now();
                dataset.add(PreferServerManaged, metadata.getIdentifier(), DC.type, DeletedResource);
                dataset.add(PreferServerManaged, metadata.getIdentifier(), RDF.type, LDP.Resource);
                storeResource(metadata.getIdentifier(), dataset, eventTime, OperationType.DELETE);
            } catch (final Exception ex) {
                throw new RuntimeTrellisException("Error deleting resource: " + metadata.getIdentifier(), ex);
            }
        });
    }

    @Override
    public CompletionStage<Void> create(final Metadata metadata, final Dataset dataset) {
        LOGGER.debug("Creating: {}", metadata.getIdentifier());
        return runAsync(() -> createOrReplace(metadata, dataset, OperationType.CREATE));
    }

    @Override
    public CompletionStage<Void> replace(final Metadata metadata, final Dataset dataset) {
        LOGGER.debug("Persisting: {}", metadata.getIdentifier());
        return runAsync(() -> createOrReplace(metadata, dataset, OperationType.REPLACE));
    }

    private void createOrReplace(final Metadata metadata, final Dataset dataset, final OperationType type) {
        final Instant eventTime = now();

        // Set the LDP type
        dataset.add(PreferServerManaged, metadata.getIdentifier(), RDF.type, metadata.getInteractionModel());

        // Relocate some user-managed triples into the server-managed graph
        metadata.getMembershipResource().ifPresent(member -> {
            dataset.add(PreferServerManaged, metadata.getIdentifier(), LDP.member, normalizeIdentifier(member));
            dataset.add(PreferServerManaged, metadata.getIdentifier(), LDP.membershipResource, member);
        });

        metadata.getMemberRelation().ifPresent(relation ->
                dataset.add(PreferServerManaged, metadata.getIdentifier(), LDP.hasMemberRelation, relation));

        metadata.getMemberOfRelation().ifPresent(relation ->
                dataset.add(PreferServerManaged, metadata.getIdentifier(), LDP.isMemberOfRelation, relation));

        if (asList(LDP.IndirectContainer, LDP.DirectContainer).contains(metadata.getInteractionModel())) {
            dataset.add(PreferServerManaged, metadata.getIdentifier(), LDP.insertedContentRelation,
                    metadata.getInsertedContentRelation().orElse(LDP.MemberSubject));
        }

        // Set the parent relationship
        metadata.getContainer().ifPresent(parent ->
                dataset.add(PreferServerManaged, metadata.getIdentifier(), DC.isPartOf, parent));

        metadata.getBinary().ifPresent(binary -> {
            dataset.add(PreferServerManaged, metadata.getIdentifier(), DC.hasPart, binary.getIdentifier());
            binary.getMimeType().map(rdf::createLiteral).ifPresent(mimeType ->
                    dataset.add(PreferServerManaged, binary.getIdentifier(), DC.format, mimeType));
        });

        storeResource(metadata.getIdentifier(), dataset, eventTime, type);
    }

    private void storeResource(final IRI identifier, final Dataset dataset,
            final Instant eventTime, final OperationType type) {
        final Literal time = rdf.createLiteral(eventTime.toString(), XSD.dateTime);
        try {
            rdfConnection.update(buildUpdateRequest(identifier, time, dataset, type));
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Could not update data for " + identifier, ex);
        }
    }

    private Node getExtIRI(final IRI identifier, final String ext) {
        return createURI(identifier.getIRIString() + "?ext=" + ext);
    }

    private enum OperationType {
        DELETE, CREATE, REPLACE
    }

    /**
     * This is equivalent to the SPARQL below.
     *
     * <p><pre><code>
     * DELETE WHERE { GRAPH IDENTIFIER { ?s ?p ?o } };
     * DELETE WHERE { GRAPH IDENTIFIER?ext=acl { ?s ?p ?o } };
     * DELETE WHERE { GRAPH trellis:PreferServerManaged {
     *   IDENTIFIER a ldp:NonRDFSource .
     *   IDENTIFIER dc:hasPart ?s .
     *   ?s ?p ?o .
     * };
     * DELETE WHERE { GRAPH trellis:PreferServerManaged { IDENTIFIER ?p ?o } };
     * INSERT DATA {
     *   GRAPH IDENTIFIER { ... }
     *   GRAPH IDENTIFIER?ext=acl { ... }
     *   GRAPH trellis:PreferServerManaged { ... }
     *   GRAPH IDENTIFIER?ext=audit { ... }
     * }
     * </code></pre></p>
     */
    private UpdateRequest buildUpdateRequest(final IRI identifier, final Literal time, final Dataset dataset,
            final OperationType type) {

        // Set the time
        dataset.add(PreferServerManaged, identifier, DC.modified, time);

        final UpdateRequest req = new UpdateRequest();
        req.add(new UpdateDeleteWhere(new QuadAcc(singletonList(new Quad(rdf.asJenaNode(identifier),
                                SUBJECT, PREDICATE, OBJECT)))));
        extensions.forEach((ext, graph) ->
            req.add(new UpdateDeleteWhere(new QuadAcc(singletonList(new Quad(
                                getExtIRI(identifier, ext), SUBJECT, PREDICATE, OBJECT))))));

        req.add(new UpdateDeleteWhere(new QuadAcc(asList(
                            new Quad(rdf.asJenaNode(PreferServerManaged), rdf.asJenaNode(identifier),
                                rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.NonRDFSource)),
                            new Quad(rdf.asJenaNode(PreferServerManaged), rdf.asJenaNode(identifier),
                                rdf.asJenaNode(DC.hasPart), SUBJECT),
                            new Quad(rdf.asJenaNode(PreferServerManaged), SUBJECT, PREDICATE, OBJECT)))));
        req.add(new UpdateDeleteWhere(new QuadAcc(singletonList(new Quad(rdf.asJenaNode(PreferServerManaged),
                                rdf.asJenaNode(identifier), PREDICATE, OBJECT)))));

        final QuadDataAcc sink = new QuadDataAcc(synchronizedList(new ArrayList<>()));
        if (type == OperationType.DELETE) {
            dataset.stream().filter(q -> q.getGraphName().filter(PreferServerManaged::equals).isPresent())
                    .map(rdf::asJenaQuad).forEach(sink::addQuad);
        } else {
            dataset.stream().filter(q -> q.getGraphName().filter(PreferServerManaged::equals).isPresent())
                    .map(rdf::asJenaQuad).forEach(sink::addQuad);
            dataset.getGraph(PreferUserManaged).ifPresent(g -> g.stream()
                    .map(t -> new Quad(rdf.asJenaNode(identifier), rdf.asJenaTriple(t))).forEach(sink::addQuad));
            dataset.getGraph(PreferAudit).ifPresent(g -> g.stream()
                    .map(t -> new Quad(getExtIRI(identifier, "audit"), rdf.asJenaTriple(t))).forEach(sink::addQuad));
            extensions.forEach((ext, graph) ->
                    dataset.getGraph(graph).ifPresent(g -> g.stream()
                        .map(t -> new Quad(getExtIRI(identifier, ext), rdf.asJenaTriple(t))).forEach(sink::addQuad)));
        }
        req.add(new UpdateDataInsert(sink));

        return req;
    }

    /**
     * This code is equivalent to the SPARQL query below.
     *
     * <p><pre><code>
     * WITH trellis:PreferServerManaged
     *   DELETE { IDENTIFIER dc:modified ?time }
     *   INSERT { IDENTIFIER dc:modified TIME }
     *   WHERE { IDENTIFIER dc:modified ?time } .
     * </code></pre></p>
     */
    private UpdateRequest buildUpdateModificationRequest(final IRI identifier, final Literal time) {
        final UpdateRequest req = new UpdateRequest();
        final Var modified = Var.alloc(MODIFIED);
        final UpdateDeleteInsert modify = new UpdateDeleteInsert();
        modify.setWithIRI(rdf.asJenaNode(PreferServerManaged));
        modify.getDeleteAcc().addTriple(triple(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.modified), modified));
        modify.getInsertAcc().addTriple(triple(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.modified),
                    rdf.asJenaNode(time)));
        final ElementGroup eg = new ElementGroup();
        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(triple(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.modified), modified));
        eg.addElement(epb);
        modify.setElement(eg);
        req.add(modify);
        return req;
    }

    /**
     * This code is equivalent to the SPARQL queries below.
     *
     * <pre><code>
     * SELECT ?object WHERE {
     *   GRAPH trellis:PreferServerManaged { IDENTIFIER rdf:type ?object }
     * }
     * </code></pre>
     *
     * <pre><code>
     * INSERT DATA {
     *   GRAPH trellis:PreferServerManaged {
     *     IDENTIFIER rdf:type ldp:Container ;
     *                dc:modified "NOW"^^xsd:dateTime }
     *   GRAPH IDENTIFIER?ext=audit {
     *     IDENTIFIER prov:wasGeneratedBy [
     *       rdf:type prov:Activity , as:Create ;
     *       prov:wasAssociatedWith trellis:AdministorAgent ;
     *       prov:atTime "TIME"^^xsd:dateTime ] }
     *   GRAPH IDENTIFIER?ext=acl {
     *     IDENTIFIER acl:mode acl.Read , acl:Write , acl:Control ;
     *       acl:agentClass foaf:Agent ;
     *       acl:accessTo IDENTIFIER }
     * }
     *
     * </code></pre>
     */
    @PostConstruct
    public void initialize() {
        final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(OBJECT);

        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(triple(rdf.asJenaNode(root), rdf.asJenaNode(RDF.type), OBJECT));

        final ElementNamedGraph ng = new ElementNamedGraph(rdf.asJenaNode(PreferServerManaged), epb);

        final ElementGroup elg = new ElementGroup();
        elg.addElement(ng);

        q.setQueryPattern(elg);

        final Stream.Builder<RDFTerm> builder = builder();
        rdfConnection.querySelect(q, qs -> builder.accept(getObject(qs)));
        if (!builder.build().findFirst().isPresent()) {
            final Literal time = rdf.createLiteral(now().toString(), XSD.dateTime);
            final IRI auth = rdf.createIRI(TRELLIS_DATA_PREFIX + "#auth");
            final UpdateRequest update = new UpdateRequest();

            final QuadDataAcc sink = new QuadDataAcc();
            sink.addQuad(new Quad(rdf.asJenaNode(PreferServerManaged), triple(rdf.asJenaNode(root),
                            rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.BasicContainer))));
            sink.addQuad(new Quad(rdf.asJenaNode(PreferServerManaged), triple(rdf.asJenaNode(root),
                            rdf.asJenaNode(DC.modified), rdf.asJenaNode(time))));

            sink.addQuad(new Quad(getExtIRI(root, ACL_EXT), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.mode),
                            rdf.asJenaNode(ACL.Read))));
            sink.addQuad(new Quad(getExtIRI(root, ACL_EXT), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.mode),
                            rdf.asJenaNode(ACL.Write))));
            sink.addQuad(new Quad(getExtIRI(root, ACL_EXT), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.mode),
                            rdf.asJenaNode(ACL.Control))));
            sink.addQuad(new Quad(getExtIRI(root, ACL_EXT), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.agentClass),
                            rdf.asJenaNode(FOAF.Agent))));
            sink.addQuad(new Quad(getExtIRI(root, ACL_EXT), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.accessTo),
                            rdf.asJenaNode(root))));

            update.add(new UpdateDataInsert(sink));
            rdfConnection.update(update);
        }
        LOGGER.info("Initialized Trellis Triplestore Resource Service");
    }

    @Override
    public CompletionStage<Resource> get(final IRI identifier) {
        return TriplestoreResource.findResource(rdfConnection, identifier, extensions, includeLdpType);
    }

    @Override
    public String generateIdentifier() {
        return supplier.get();
    }

    @Override
    public CompletionStage<Void> add(final IRI id, final Dataset dataset) {
        return runAsync(() -> {
            final IRI graphName = rdf.createIRI(id.getIRIString() + "?ext=audit");
            try (final Dataset data = rdf.createDataset()) {
                dataset.getGraph(PreferAudit).ifPresent(g ->
                        g.stream().forEach(t -> data.add(graphName, t.getSubject(), t.getPredicate(), t.getObject())));
                executeWrite(rdfConnection, () -> rdfConnection.loadDataset(asJenaDataset(data)));
            } catch (final Exception ex) {
                throw new RuntimeTrellisException("Error storing audit dataset for " + id, ex);
            }
        });
    }

    @Override
    public CompletionStage<Void> touch(final IRI identifier) {
        final Literal time = rdf.createLiteral(now().toString(), XSD.dateTime);
        return runAsync(() -> {
            try {
                rdfConnection.update(buildUpdateModificationRequest(identifier, time));
            } catch (final Exception ex) {
                throw new RuntimeTrellisException("Could not update data for " + identifier, ex);
            }
        });
    }

    @Override
    public Set<IRI> supportedInteractionModels() {
        return supportedIxnModels;
    }

    /**
     * Build an RDF connection from a location value.
     *
     * @implNote A null value will create an in-memory RDF store, a file path will create
     *           a TDB2 RDF store, and a URL will use a remote triplestore.
     * @param location the location of the RDF
     * @return a connection to the RDF store
     */
    public static RDFConnection buildRDFConnection(final String location) {
        if (location != null) {
            if (location.startsWith("http://") || location.startsWith("https://")) {
                // Remote
                LOGGER.info("Using remote Triplestore for persistence at {}", location);
                return connect(location);
            }
            // TDB2
            LOGGER.info("Using local TDB2 database at {}", location);
            return connect(wrap(connectDatasetGraph(location)));
        }
        // in-memory
        LOGGER.info("Using an in-memory dataset for resources");
        return connect(createTxnMem());
    }

    /**
     * Alias{@link org.apache.jena.graph.Triple#create(Node, Node, Node)} to
     * avoid collision with {@link ResourceService#create(Metadata, Dataset)}.
     *
     * @param subj the subject
     * @param pred the predicate
     * @param obj the object
     * @return a {@link org.apache.jena.graph.Triple}
     */
    static org.apache.jena.graph.Triple triple(final Node subj, final Node pred, final Node obj) {
        return org.apache.jena.graph.Triple.create(subj, pred, obj);
    }

    /*
     * Build a map suitable for extension graphs from a config string.
     * @param extensions the config value
     * @return the formatted map
     */
    static Map<String, IRI> buildExtensionMap(final String extensions) {
        return stream(extensions.split(",")).map(item -> item.split("=")).filter(kv -> kv.length == 2)
            .filter(kv -> !kv[0].trim().isEmpty() && !kv[1].trim().isEmpty())
            .collect(toMap(kv -> kv[0].trim(), kv -> rdf.createIRI(kv[1].trim())));
    }

    /**
     * Build an extension map from configuration.
     * @return the formatted map
     */
    static Map<String, IRI> buildExtensionMap() {
        return getConfig().getOptionalValue(CONFIG_HTTP_EXTENSION_GRAPHS, String.class)
            .map(TriplestoreResourceService::buildExtensionMap).orElseGet(() ->
                    singletonMap(ACL_EXT, PreferAccessControl));
    }
}
