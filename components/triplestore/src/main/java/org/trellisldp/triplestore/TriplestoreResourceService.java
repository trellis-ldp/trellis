/*
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
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.builder;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.system.Txn.executeWrite;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.triplestore.TriplestoreUtils.OBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.PREDICATE;
import static org.trellisldp.triplestore.TriplestoreUtils.SUBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.asJenaDataset;
import static org.trellisldp.triplestore.TriplestoreUtils.getBaseIRI;
import static org.trellisldp.triplestore.TriplestoreUtils.getInstance;
import static org.trellisldp.triplestore.TriplestoreUtils.getObject;
import static org.trellisldp.vocabulary.Trellis.DeletedResource;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferAudit;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
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
import org.apache.jena.sparql.syntax.ElementMinus;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.XSD;

/**
 * A triplestore-based implementation of the Trellis ResourceService API.
 */
public class TriplestoreResourceService extends DefaultAuditService implements ResourceService {

    private static final String PARENT = "parent";
    private static final String MODIFIED = "modified";
    private static final String MEMBER = "member";

    private static final Logger LOGGER = getLogger(TriplestoreResourceService.class);
    private static final JenaRDF rdf = getInstance();

    private final Supplier<String> supplier;
    private final RDFConnection rdfConnection;
    private final Set<IRI> supportedIxnModels;

    /**
     * Create a triplestore-backed resource service.
     * @param rdfConnection the connection to an RDF datastore
     * @param identifierService an ID supplier service
     */
    @Inject
    public TriplestoreResourceService(final RDFConnection rdfConnection, final IdentifierService identifierService) {
        super();
        requireNonNull(rdfConnection, "RDFConnection may not be null!");
        requireNonNull(identifierService, "IdentifierService may not be null!");
        this.rdfConnection = rdfConnection;
        this.supplier = identifierService.getSupplier();
        this.supportedIxnModels = unmodifiableSet(asList(LDP.Resource, LDP.RDFSource, LDP.NonRDFSource, LDP.Container,
                LDP.BasicContainer, LDP.DirectContainer, LDP.IndirectContainer).stream().collect(toSet()));
    }

    @Override
    public CompletableFuture<Void> create(final IRI id, final IRI ixnModel, final Dataset dataset, final IRI container,
            final Binary binary) {
        LOGGER.debug("Creating: {}", id);
        return runAsync(() ->
                createOrReplace(id, ixnModel, dataset, OperationType.CREATE, container, binary));
    }

    @Override
    public CompletableFuture<Void> delete(final IRI identifier, final IRI ixnModel, final Dataset dataset) {
        LOGGER.debug("Deleting: {}", identifier);
        return runAsync(() -> {
            final Instant eventTime = now();
            dataset.add(PreferServerManaged, identifier, DC.type, DeletedResource);
            dataset.add(PreferServerManaged, identifier, RDF.type, LDP.Resource);
            storeResource(identifier, dataset, eventTime, OperationType.DELETE);
        });
    }

    @Override
    public CompletableFuture<Void> replace(final IRI id, final IRI ixnModel, final Dataset dataset, final IRI container,
            final Binary binary) {
        LOGGER.debug("Updating: {}", id);
        return runAsync(() ->
                createOrReplace(id, ixnModel, dataset, OperationType.REPLACE, container, binary));
    }

    private void createOrReplace(final IRI identifier, final IRI ixnModel,
                    final Dataset dataset, final OperationType type, final IRI container, final Binary binary) {
        final Instant eventTime = now();

        // Set the LDP type
        dataset.add(PreferServerManaged, identifier, RDF.type, ixnModel);

        // Relocate some user-managed triples into the server-managed graph
        if (LDP.DirectContainer.equals(ixnModel) || LDP.IndirectContainer.equals(ixnModel)) {
            dataset.getGraph(PreferUserManaged).ifPresent(g -> {
                g.stream(identifier, LDP.membershipResource, null).findFirst().ifPresent(t -> {
                    // This allows for HTTP resource URL-based queries
                    dataset.add(PreferServerManaged, identifier, LDP.member, getBaseIRI(t.getObject()));
                    dataset.add(PreferServerManaged, identifier, LDP.membershipResource, t.getObject());
                });
                g.stream(identifier, LDP.hasMemberRelation, null).findFirst().ifPresent(t -> dataset
                                .add(PreferServerManaged, identifier, LDP.hasMemberRelation, t.getObject()));
                g.stream(identifier, LDP.isMemberOfRelation, null).findFirst().ifPresent(t -> dataset
                                .add(PreferServerManaged, identifier, LDP.isMemberOfRelation, t.getObject()));
                dataset.add(PreferServerManaged, identifier, LDP.insertedContentRelation,
                                g.stream(identifier, LDP.insertedContentRelation, null).map(Triple::getObject)
                                                .findFirst().orElse(LDP.MemberSubject));
            });
        }

        // Set the parent relationship
        if (nonNull(container)) {
            dataset.add(PreferServerManaged, identifier, DC.isPartOf, container);
        }

        if (nonNull(binary)) {
            dataset.add(PreferServerManaged, identifier, DC.hasPart, binary.getIdentifier());
            dataset.add(PreferServerManaged, binary.getIdentifier(), DC.modified,
                    rdf.createLiteral(binary.getModified().toString(), XSD.dateTime));
            binary.getMimeType().map(rdf::createLiteral).ifPresent(mimeType ->
                    dataset.add(PreferServerManaged, binary.getIdentifier(), DC.format, mimeType));
            binary.getSize().map(size -> rdf.createLiteral(size.toString(), XSD.long_)).ifPresent(size ->
                    dataset.add(PreferServerManaged, binary.getIdentifier(), DC.extent, size));
        }

        storeResource(identifier, dataset, eventTime, type);
    }

    private void storeResource(final IRI identifier, final Dataset dataset,
            final Instant eventTime, final OperationType type) {
        final Literal time = rdf.createLiteral(eventTime.toString(), XSD.dateTime);
        try {
            rdfConnection.update(buildUpdateRequest(identifier, time, dataset, type));
        } catch (final Exception ex) {
            LOGGER.error("Could not update data: {}", ex.getMessage());
            throw new RuntimeTrellisException(ex);
        }
    }

    /**
     * This is equivalent to the SPARQL query below.
     *
     * <p><pre><code>
     * WITH trellis:PreferServerManaged
     *   DELETE { ?parent dc:modified ?modified }
     *   INSERT { ?parent dc:modified TIME }
     *   WHERE {
     *     IDENTIFIER dc:isPartOf ?parent .
     *     ?parent dc:modified ?modified .
     *     MINUS { ?parent a ldp:RDFSource }
     *     MINUS { ?parent a ldp:NonRDFSource }
     * }
     * </code></pre></p>
     */
    private Update getParentUpdateModificationRequest(final IRI identifier, final Literal time) {
        final Var parent = Var.alloc(PARENT);
        final Var modified = Var.alloc(MODIFIED);
        final UpdateDeleteInsert modify = new UpdateDeleteInsert();
        modify.setWithIRI(rdf.asJenaNode(PreferServerManaged));
        modify.getDeleteAcc().addTriple(triple(parent, rdf.asJenaNode(DC.modified), modified));
        modify.getInsertAcc().addTriple(triple(parent, rdf.asJenaNode(DC.modified), rdf.asJenaNode(time)));
        final ElementGroup eg = new ElementGroup();
        final ElementPathBlock epb1 = new ElementPathBlock();
        epb1.addTriple(triple(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.isPartOf), parent));
        epb1.addTriple(triple(parent, rdf.asJenaNode(DC.modified), modified));
        eg.addElement(epb1);
        final ElementPathBlock epb2 = new ElementPathBlock();
        epb2.addTriple(triple(parent, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.RDFSource)));
        eg.addElement(new ElementMinus(epb2));
        final ElementPathBlock epb3 = new ElementPathBlock();
        epb3.addTriple(triple(parent, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.NonRDFSource)));
        eg.addElement(new ElementMinus(epb3));
        modify.setElement(eg);
        return modify;
    }

    /**
     * This is equivalent to the SPARQL below.
     *
     * <p><pre><code>
     * WITH trellis:PreferServerManaged
     *   DELETE { ?member dc:modified ?modified }
     *   INSERT { ?member dc:modified TIME }
     *   WHERE {
     *     IDENTIFIER dc:isPartOf ?parent .
     *     ?parent ldp:membershipResource ?member .
     *     ?parent ldp:hasMemberRelation ?any .
     *     ?member dc:modified ?modified
     * }
     * </code></pre></p>
     */
    private Update getMemberUpdateModificationRequest(final IRI identifier, final Literal time) {
        final Var parent = Var.alloc(PARENT);
        final Var modified = Var.alloc(MODIFIED);
        final Var member = Var.alloc(MEMBER);
        final Var any = Var.alloc("any");
        final UpdateDeleteInsert modification = new UpdateDeleteInsert();
        modification.setWithIRI(rdf.asJenaNode(PreferServerManaged));
        modification.getDeleteAcc().addTriple(triple(member, rdf.asJenaNode(DC.modified), modified));
        modification.getInsertAcc().addTriple(triple(member, rdf.asJenaNode(DC.modified), rdf.asJenaNode(time)));
        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(triple(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.isPartOf), parent));
        epb.addTriple(triple(parent, rdf.asJenaNode(LDP.membershipResource), member));
        epb.addTriple(triple(parent, rdf.asJenaNode(LDP.hasMemberRelation), any));
        epb.addTriple(triple(member, rdf.asJenaNode(DC.modified), modified));
        modification.setElement(epb);
        return modification;
    }

    private Node getAclIRI(final IRI identifier) {
        return createURI(identifier.getIRIString() + "?ext=acl");
    }

    private Node getAuditIRI(final IRI identifier) {
        return createURI(identifier.getIRIString() + "?ext=audit");
    }

    private enum OperationType {
        DELETE, CREATE, REPLACE;
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
     * }
     * DELETE WHERE { GRAPH trellis:PreferServerManaged { IDENTIFIER ?p ?o } };
     * INSERT DATA {
     *   GRAPH IDENTIFIER { ... }
     *   GRAPH IDENTIFIER?ext=acl { ... }
     *   GRAPH trellis:PreferServerManaged { ... }
     *   GRAPH IDENTIFIER?ext=audit { ... }
     * };
     * // this next clause happens first in an HTTP delete operation
     * WITH trellis:PreferServerManaged
     *   DELETE { ?parent dc:modified ?x }
     *   INSERT { ?parent dc:modified TIME }
     *   WHERE {
     *     IDENTIFIER dc:isPartOf ?parent .
     *     ?parent rdf:type ldp:Container
     * }
     * </code></pre></p>
     */
    private UpdateRequest buildUpdateRequest(final IRI identifier, final Literal time, final Dataset dataset,
            final OperationType type) {

        // Set the time
        dataset.add(PreferServerManaged, identifier, DC.modified, time);

        final UpdateRequest req = new UpdateRequest();
        if (type == OperationType.DELETE) {
            // Update the parent container's modified date
            req.add(getParentUpdateModificationRequest(identifier, time));

            // Likewise the member resource.
            req.add(getMemberUpdateModificationRequest(identifier, time));
        }

        req.add(new UpdateDeleteWhere(new QuadAcc(singletonList(new Quad(rdf.asJenaNode(identifier),
                                SUBJECT, PREDICATE, OBJECT)))));
        req.add(new UpdateDeleteWhere(new QuadAcc(singletonList(new Quad(
                                getAclIRI(identifier), SUBJECT, PREDICATE, OBJECT)))));
        req.add(new UpdateDeleteWhere(new QuadAcc(asList(
                            new Quad(rdf.asJenaNode(PreferServerManaged), rdf.asJenaNode(identifier),
                                rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.NonRDFSource)),
                            new Quad(rdf.asJenaNode(PreferServerManaged), rdf.asJenaNode(identifier),
                                rdf.asJenaNode(DC.hasPart), SUBJECT),
                            new Quad(rdf.asJenaNode(PreferServerManaged), SUBJECT, PREDICATE, OBJECT)))));
        req.add(new UpdateDeleteWhere(new QuadAcc(singletonList(new Quad(rdf.asJenaNode(PreferServerManaged),
                                rdf.asJenaNode(identifier), PREDICATE, OBJECT)))));

        if (type == OperationType.DELETE) {
            final QuadDataAcc sink = new QuadDataAcc(synchronizedList(new ArrayList<>()));
            dataset.stream().filter(q -> q.getGraphName().filter(PreferServerManaged::equals).isPresent())
                    .map(rdf::asJenaQuad).forEach(sink::addQuad);
            dataset.getGraph(PreferAudit).ifPresent(g -> g.stream()
                    .map(t -> new Quad(getAuditIRI(identifier), rdf.asJenaTriple(t))).forEach(sink::addQuad));
            req.add(new UpdateDataInsert(sink));
        } else {
            final QuadDataAcc sink = new QuadDataAcc(synchronizedList(new ArrayList<>()));
            dataset.stream().filter(q -> q.getGraphName().filter(PreferServerManaged::equals).isPresent())
                    .map(rdf::asJenaQuad).forEach(sink::addQuad);
            dataset.getGraph(PreferUserManaged).ifPresent(g -> g.stream()
                    .map(t -> new Quad(rdf.asJenaNode(identifier), rdf.asJenaTriple(t))).forEach(sink::addQuad));
            dataset.getGraph(PreferAccessControl).ifPresent(g -> g.stream()
                    .map(t -> new Quad(getAclIRI(identifier), rdf.asJenaTriple(t))).forEach(sink::addQuad));
            dataset.getGraph(PreferAudit).ifPresent(g -> g.stream()
                    .map(t -> new Quad(getAuditIRI(identifier), rdf.asJenaTriple(t))).forEach(sink::addQuad));
            req.add(new UpdateDataInsert(sink));
        }

        if (type == OperationType.CREATE) {
            // Update the parent's modification date
            req.add(getParentUpdateModificationRequest(identifier, time));

            // Likewise update the member resource.
            req.add(getMemberUpdateModificationRequest(identifier, time));

        } else if (type != OperationType.DELETE) {
            // Indirect containers member resources are _always_ updated.
            final Var parent = Var.alloc(PARENT);
            final Var modified = Var.alloc(MODIFIED);
            final Var member = Var.alloc(MEMBER);
            final UpdateDeleteInsert modify = new UpdateDeleteInsert();
            modify.setWithIRI(rdf.asJenaNode(PreferServerManaged));
            modify.getDeleteAcc().addTriple(triple(member, rdf.asJenaNode(DC.modified), modified));
            modify.getInsertAcc().addTriple(triple(member, rdf.asJenaNode(DC.modified), rdf.asJenaNode(time)));
            final ElementPathBlock epb = new ElementPathBlock();
            epb.addTriple(triple(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.modified), modified));
            epb.addTriple(triple(parent, rdf.asJenaNode(LDP.membershipResource), member));
            epb.addTriple(triple(parent, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.IndirectContainer)));
            epb.addTriple(triple(member, rdf.asJenaNode(DC.modified), modified));
            modify.setElement(epb);
            req.add(modify);
        }
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

            sink.addQuad(new Quad(getAclIRI(root), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.mode),
                            rdf.asJenaNode(ACL.Read))));
            sink.addQuad(new Quad(getAclIRI(root), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.mode),
                            rdf.asJenaNode(ACL.Write))));
            sink.addQuad(new Quad(getAclIRI(root), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.mode),
                            rdf.asJenaNode(ACL.Control))));
            sink.addQuad(new Quad(getAclIRI(root), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.agentClass),
                            rdf.asJenaNode(FOAF.Agent))));
            sink.addQuad(new Quad(getAclIRI(root), triple(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.accessTo),
                            rdf.asJenaNode(root))));

            update.add(new UpdateDataInsert(sink));
            rdfConnection.update(update);
        }
    }

    @Override
    public CompletableFuture<Resource> get(final IRI identifier) {
        return TriplestoreResource.findResource(rdfConnection, identifier);
    }

    @Override
    public String generateIdentifier() {
        return supplier.get();
    }

    @Override
    public CompletableFuture<Void> add(final IRI id, final Dataset dataset) {
        return runAsync(() -> {
            final IRI graphName = rdf.createIRI(id.getIRIString() + "?ext=audit");
            try (final Dataset data = rdf.createDataset()) {
                dataset.getGraph(PreferAudit).ifPresent(g ->
                        g.stream().forEach(t -> data.add(graphName, t.getSubject(), t.getPredicate(), t.getObject())));
                executeWrite(rdfConnection, () -> rdfConnection.loadDataset(asJenaDataset(data)));
            } catch (final Exception ex) {
                LOGGER.error("Error storing audit dataset: {}", ex.getMessage());
                throw new RuntimeTrellisException(ex);
            }
        });
    }

    @Override
    public Set<IRI> supportedInteractionModels() {
        return supportedIxnModels;
    }

    /**
     * Alias{@link org.apache.jena.graph.Triple#create(Node, Node, Node)} to
     * avoid collision with {@link ResourceService#create(IRI, IRI, Dataset)}.
     *
     * @param subj the subject
     * @param pred the predicate
     * @param obj the object
     * @return a {@link org.apache.jena.graph.Triple}
     */
    private static org.apache.jena.graph.Triple triple(final Node subj, final Node pred, final Node obj) {
        return org.apache.jena.graph.Triple.create(subj, pred, obj);
    }
}
