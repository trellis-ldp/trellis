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
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.empty;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.system.Txn.executeWrite;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_SESSION_BASE_URL;
import static org.trellisldp.triplestore.TriplestoreUtils.OBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.PREDICATE;
import static org.trellisldp.triplestore.TriplestoreUtils.SUBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.asJenaDataset;
import static org.trellisldp.triplestore.TriplestoreUtils.getBaseIRI;
import static org.trellisldp.triplestore.TriplestoreUtils.getInstance;
import static org.trellisldp.triplestore.TriplestoreUtils.getObject;
import static org.trellisldp.triplestore.TriplestoreUtils.getPredicate;
import static org.trellisldp.triplestore.TriplestoreUtils.getSubject;
import static org.trellisldp.vocabulary.Trellis.DeletedResource;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferAudit;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.RDFNode;
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
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.Session;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
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

    private static final Predicate<BlankNodeOrIRI> isUserGraph = PreferUserManaged::equals;
    private static final Predicate<BlankNodeOrIRI> isServerGraph = PreferServerManaged::equals;

    private final Supplier<String> supplier;
    private final RDFConnection rdfConnection;
    private final Optional<EventService> eventService;
    private final Optional<MementoService> mementoService;
    private final Set<IRI> supportedIxnModels;

    /**
     * Create a triplestore-backed resource service.
     * @param rdfConnection the connection to an RDF datastore
     * @param identifierService an ID supplier service
     * @param mementoService a service for memento resources
     * @param eventService an event service
     */
    @Inject
    public TriplestoreResourceService(final RDFConnection rdfConnection, final IdentifierService identifierService,
            final MementoService mementoService, final EventService eventService) {
        requireNonNull(rdfConnection, "RDFConnection may not be null!");
        requireNonNull(identifierService, "IdentifierService may not be null!");
        this.rdfConnection = rdfConnection;
        this.supplier = identifierService.getSupplier();
        this.eventService = ofNullable(eventService);
        this.mementoService = ofNullable(mementoService);
        this.supportedIxnModels = unmodifiableSet(asList(LDP.Resource, LDP.RDFSource, LDP.NonRDFSource, LDP.Container,
                LDP.BasicContainer, LDP.DirectContainer, LDP.IndirectContainer).stream().collect(toSet()));
        init();
    }

    @Override
    public Future<Boolean> create(final IRI id, final Session session, final IRI ixnModel, final Dataset dataset,
                    final IRI container, final Binary binary) {
        LOGGER.debug("Creating: {}", id);
        return supplyAsync(() ->
                createOrReplace(id, session, ixnModel, dataset, OperationType.CREATE, container, binary));
    }

    @Override
    public Future<Boolean> delete(final IRI identifier, final Session session, final IRI ixnModel,
            final Dataset dataset) {
        LOGGER.debug("Deleting: {}", identifier);
        return supplyAsync(() -> {
            final Instant eventTime = now();
            dataset.add(PreferServerManaged, identifier, DC.type, DeletedResource);
            dataset.add(PreferServerManaged, identifier, RDF.type, LDP.Resource);
            return storeAndNotify(identifier, session, dataset, eventTime, OperationType.DELETE);
        });
    }

    @Override
    public Future<Boolean> replace(final IRI id, final Session session, final IRI ixnModel, final Dataset dataset,
                    final IRI container, final Binary binary) {
        LOGGER.debug("Updating: {}", id);
        return supplyAsync(() ->
                createOrReplace(id, session, ixnModel, dataset, OperationType.REPLACE, container, binary));
    }

    private Boolean createOrReplace(final IRI identifier, final Session session, final IRI ixnModel,
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

        return storeAndNotify(identifier, session, dataset, eventTime, type);
    }

    private Boolean storeAndNotify(final IRI identifier, final Session session, final Dataset dataset,
            final Instant eventTime, final OperationType type) {
        final Literal time = rdf.createLiteral(eventTime.toString(), XSD.dateTime);
        try {
            rdfConnection.update(buildUpdateRequest(identifier, time, dataset, type));
            if (type != OperationType.DELETE) {
                mementoService.ifPresent(svc -> get(identifier).ifPresent(res ->
                            svc.put(identifier, eventTime, res.stream())));
            }
            emitEvents(identifier, session, type, time, dataset);
        } catch (final Exception ex) {
            LOGGER.error("Could not update data: {}", ex.getMessage());
            throw new RuntimeTrellisException(ex);
        }
        return true;
    }

    @Override
    public List<Range<Instant>> getMementos(final IRI identifier) {
        return mementoService.map(svc -> svc.list(identifier)).orElse(emptyList());
    }

    private void emitEvents(final IRI identifier, final Session session, final OperationType opType,
            final Literal time, final Dataset dataset) {

        // Get the base URL
        final Optional<String> baseUrl = session.getProperty(TRELLIS_SESSION_BASE_URL);
        final IRI inbox = dataset.getGraph(PreferUserManaged)
            .flatMap(graph -> graph.stream(null, LDP.inbox, null).map(Triple::getObject)
                    .filter(term -> term instanceof IRI).map(term -> (IRI) term).findFirst())
            .orElse(null);
        final List<IRI> targetTypes = dataset.stream()
            .filter(quad -> quad.getGraphName().filter(isUserGraph.or(isServerGraph)).isPresent())
            .filter(quad -> quad.getPredicate().equals(RDF.type))
            .flatMap(quad -> quad.getObject() instanceof IRI ? Stream.of((IRI) quad.getObject()) : empty())
            .distinct().collect(toList());

        eventService.ifPresent(svc -> {
            svc.emit(new SimpleEvent(getUrl(identifier, baseUrl),
                        asList(session.getAgent()), asList(PROV.Activity, OperationType.asIRI(opType)),
                        targetTypes, inbox));
            getContainer(identifier).ifPresent(parent ->
                    emitEventsForAdjacentResources(svc, parent, session, opType, time));
        });
    }

    /**
     * The following SPARQL query is equivalent to the code below.
     *
     * <p><pre><code>
     * SELECT ?predicate ?object ?subject ?memberDate ?memberType
     * WHERE {
     *   GRAPH trellis:PreferServerManaged {
     *     PARENT rdf:type ?predicate
     *     PARENT dc:modified ?object
     *     OPTIONAL {
     *       PARENT ldp:member ?subject
     *       ?subject dc:modified ?memberDate
     *       ?subject rdf:type ? memberType
     *     }
     *   }
     * }
     * </code></pre></p>
     *
     * <p>Note: this query does not retrieve the user-managed rdf:type values
     * nor does it retrieve any ldp:inbox value.
     */
    private void emitEventsForAdjacentResources(final EventService svc, final IRI parent, final Session session,
            final OperationType opType, final Literal time) {
        final Boolean isDelete = opType == OperationType.DELETE;
        final Boolean isCreate = opType == OperationType.CREATE;
        final Var memberDate = Var.alloc("memberDate");
        final Var memberType = Var.alloc("memberType");

        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(memberDate);
        q.addResultVar(memberType);
        q.addResultVar(SUBJECT);
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb1 = new ElementPathBlock();
        epb1.addTriple(triple(rdf.asJenaNode(parent), rdf.asJenaNode(RDF.type), PREDICATE));
        epb1.addTriple(triple(rdf.asJenaNode(parent), rdf.asJenaNode(DC.modified), OBJECT));

        final ElementPathBlock epb2 = new ElementPathBlock();
        epb2.addTriple(triple(rdf.asJenaNode(parent), rdf.asJenaNode(LDP.member), SUBJECT));
        epb2.addTriple(triple(SUBJECT, rdf.asJenaNode(DC.modified), memberDate));
        epb2.addTriple(triple(SUBJECT, rdf.asJenaNode(RDF.type), memberType));

        final ElementGroup eg = new ElementGroup();
        eg.addElement(epb1);
        eg.addElement(new ElementOptional(epb2));
        q.setQueryPattern(new ElementNamedGraph(rdf.asJenaNode(PreferServerManaged), eg));
        rdfConnection.querySelect(q, qs -> {
            final IRI type = getPredicate(qs);
            final Optional<IRI> member = ofNullable(qs.get("subject")).map(RDFNode::asNode)
                .map(rdf::asRDFTerm).map(t -> (IRI) t).filter(t -> !t.equals(parent));
            final Optional<IRI> memberRdfType = ofNullable(qs.get("memberType")).map(RDFNode::asNode)
                .map(rdf::asRDFTerm).map(t -> (IRI) t);
            final Boolean memberIsModified = ofNullable(qs.get("memberDate"))
                .map(RDFNode::asNode).map(rdf::asRDFTerm).filter(time::equals).isPresent();
            if (isCreate || isDelete) {
                if (type.getIRIString().endsWith("Container")) {
                    svc.emit(new SimpleEvent(getUrl(parent, session.getProperty(TRELLIS_SESSION_BASE_URL)),
                                    asList(session.getAgent()), asList(PROV.Activity, AS.Update), asList(type), null));
                }
                if (LDP.DirectContainer.equals(type) && memberIsModified) {
                    member.ifPresent(m ->
                            svc.emit(new SimpleEvent(getUrl(m, session.getProperty(TRELLIS_SESSION_BASE_URL)),
                                        asList(session.getAgent()), asList(PROV.Activity, AS.Update),
                                        memberRdfType.map(Arrays::asList).orElseGet(Collections::emptyList), null)));
                }
            }
            if (LDP.IndirectContainer.equals(type)) {
                member.ifPresent(m -> svc.emit(new SimpleEvent(getUrl(m, session.getProperty(TRELLIS_SESSION_BASE_URL)),
                                asList(session.getAgent()), asList(PROV.Activity, AS.Update),
                                memberRdfType.map(Arrays::asList).orElseGet(Collections::emptyList), null)));
            }
        });
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
        final UpdateDeleteInsert modify = new UpdateDeleteInsert();
        modify.setWithIRI(rdf.asJenaNode(PreferServerManaged));
        modify.getDeleteAcc().addTriple(triple(member, rdf.asJenaNode(DC.modified), modified));
        modify.getInsertAcc().addTriple(triple(member, rdf.asJenaNode(DC.modified), rdf.asJenaNode(time)));
        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(triple(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.isPartOf), parent));
        epb.addTriple(triple(parent, rdf.asJenaNode(LDP.membershipResource), member));
        epb.addTriple(triple(parent, rdf.asJenaNode(LDP.hasMemberRelation), any));
        epb.addTriple(triple(member, rdf.asJenaNode(DC.modified), modified));
        modify.setElement(epb);
        return modify;
    }

    private Node getAclIRI(final IRI identifier) {
        return createURI(identifier.getIRIString() + "?ext=acl");
    }

    private Node getAuditIRI(final IRI identifier) {
        return createURI(identifier.getIRIString() + "?ext=audit");
    }

    private enum OperationType {
        DELETE, CREATE, REPLACE;

        static IRI asIRI(final OperationType opType) {
            switch (opType) {
                case DELETE:
                  return AS.Delete;
                case CREATE:
                  return AS.Create;
                case REPLACE:
                default:
                  return AS.Update;
            }
        }
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

    private String getUrl(final IRI identifier, final Optional<String> baseUrl) {
        if (baseUrl.isPresent()) {
            return toExternal(identifier, baseUrl.get()).getIRIString();
        }
        LOGGER.warn("No baseURL defined. Emitting message with resource's internal IRI: {}", identifier);
        return identifier.getIRIString();
    }

    @Override
    public Stream<Triple> scan() {
        /*
         * SELECT ?subject ?object
         * WHERE {
         *   GRAPH trellis:PreferServerManaged { ?subject rdf:type ?object }
         * }
         */
        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(SUBJECT);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(triple(SUBJECT, rdf.asJenaNode(RDF.type), OBJECT));

        final ElementNamedGraph ng = new ElementNamedGraph(rdf.asJenaNode(PreferServerManaged), epb);

        final ElementGroup elg = new ElementGroup();
        elg.addElement(ng);

        q.setQueryPattern(elg);

        final Stream.Builder<Triple> builder = builder();
        rdfConnection.querySelect(q, qs -> builder.accept(rdf.createTriple(getSubject(qs), RDF.type, getObject(qs))));
        return builder.build();
    }

    /**
     * This code is equivalent to the SPARQL queries below.
     *
     * <p><pre><code>
     * SELECT ?object WHERE {
     *   GRAPH trellis:PreferServerManaged { IDENTIFIER rdf:type ?object }
     * }
     * </code></pre></p>
     *
     * <p><pre><code>
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
     * </code></pre></p>
     */
    private void init() {
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
    public Optional<Resource> get(final IRI identifier, final Instant time) {
        return mementoService.isPresent() ? mementoService.flatMap(svc -> svc.get(identifier, time)) : get(identifier);
    }

    @Override
    public Optional<Resource> get(final IRI identifier) {
        return TriplestoreResource.findResource(rdfConnection, identifier);
    }

    @Override
    public String generateIdentifier() {
        return supplier.get();
    }

    @Override
    public Stream<IRI> purge(final IRI identifier) {
        throw new UnsupportedOperationException("purge is not supported");
    }

    @Override
    public Future<Boolean> add(final IRI id, final Session session, final Dataset dataset) {
        return supplyAsync(() -> {
            final IRI graphName = rdf.createIRI(id.getIRIString() + "?ext=audit");
            try (final Dataset data = rdf.createDataset()) {
                dataset.getGraph(PreferAudit).ifPresent(g ->
                        g.stream().forEach(t -> data.add(graphName, t.getSubject(), t.getPredicate(), t.getObject())));
                executeWrite(rdfConnection, () -> rdfConnection.loadDataset(asJenaDataset(data)));
                return true;
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
