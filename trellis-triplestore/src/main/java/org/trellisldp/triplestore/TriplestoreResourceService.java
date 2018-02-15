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
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Stream.builder;
import static org.apache.commons.lang3.Range.between;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.triplestore.TriplestoreUtils.OBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.PREDICATE;
import static org.trellisldp.triplestore.TriplestoreUtils.SUBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.findFirst;
import static org.trellisldp.triplestore.TriplestoreUtils.getInstance;
import static org.trellisldp.triplestore.TriplestoreUtils.getObject;
import static org.trellisldp.triplestore.TriplestoreUtils.getPredicate;
import static org.trellisldp.triplestore.TriplestoreUtils.getSubject;
import static org.trellisldp.vocabulary.Trellis.AdministratorAgent;
import static org.trellisldp.vocabulary.Trellis.DeletedResource;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferAudit;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.Range;
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
import org.trellisldp.api.AuditService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.vocabulary.XSD;

/**
 * A triplestore-based implementation of the Trellis ResourceService API.
 */
public class TriplestoreResourceService extends DefaultAuditService implements ResourceService {

    private static final Var PARENT = Var.alloc("parent");
    private static final Var MODIFIED = Var.alloc("modified");
    private static final Var MEMBER = Var.alloc("member");
    private static final Var ANY = Var.alloc("any");

    private static final Logger LOGGER = getLogger(TriplestoreResourceService.class);
    private static final JenaRDF rdf = getInstance();
    private static Optional<AuditService> auditService = findFirst(AuditService.class);

    private final Supplier<String> supplier;
    private final RDFConnection rdfConnection;
    private final Optional<EventService> eventService;
    private final Optional<MementoService> mementoService;

    /**
     * Create a triplestore-backed resource service.
     * @param rdfConnection the connection to an RDF datastore
     * @param identifierService an ID supplier service
     * @param mementoService a service for memento resources
     * @param eventService an event service
     */
    public TriplestoreResourceService(final RDFConnection rdfConnection, final IdentifierService identifierService,
            final MementoService mementoService, final EventService eventService) {
        requireNonNull(rdfConnection, "RDFConnection may not be null!");
        requireNonNull(identifierService, "IdentifierService may not be null!");
        this.rdfConnection = rdfConnection;
        this.supplier = identifierService.getSupplier();
        this.eventService = ofNullable(eventService);
        this.mementoService = ofNullable(mementoService);
        init();
    }

    private static RDFTerm getBaseIRI(final RDFTerm object) {
        if (object instanceof IRI) {
            final String iri = ((IRI) object).getIRIString().split("#")[0];
            return rdf.createIRI(iri);
        }
        return object;
    }

    @Override
    public Future<Boolean> put(final IRI identifier, final IRI ixnModel, final Dataset dataset) {
        return supplyAsync(() -> {
            final Boolean isDelete = dataset.contains(of(PreferAudit), null, RDF.type, AS.Delete);
            final Instant eventTime = now();

            // Set the LDP type
            dataset.remove(of(PreferServerManaged), identifier, RDF.type, null);
            if (isDelete) {
                LOGGER.debug("Deleting: {}", identifier);
                dataset.add(PreferServerManaged, identifier, DC.type, DeletedResource);
                dataset.add(PreferServerManaged, identifier, RDF.type, LDP.Resource);
            } else {
                LOGGER.debug("Updating: {}", identifier);
                dataset.add(PreferServerManaged, identifier, RDF.type, ixnModel);
                // Relocate some user-managed triples into the server-managed graph
                if (LDP.DirectContainer.equals(ixnModel) || LDP.IndirectContainer.equals(ixnModel)) {
                    dataset.getGraph(PreferUserManaged).ifPresent(g -> {
                        g.stream(identifier, LDP.membershipResource, null).findFirst().ifPresent(t -> {
                            // This allows for HTTP resource URL-based queries
                            dataset.add(PreferServerManaged, identifier, LDP.member, getBaseIRI(t.getObject()));
                            dataset.add(PreferServerManaged, identifier, LDP.membershipResource, t.getObject());
                        });
                        g.stream(identifier, LDP.hasMemberRelation, null).findFirst().ifPresent(t ->
                                dataset.add(PreferServerManaged, identifier, LDP.hasMemberRelation, t.getObject()));
                        g.stream(identifier, LDP.isMemberOfRelation, null).findFirst().ifPresent(t ->
                                dataset.add(PreferServerManaged, identifier, LDP.isMemberOfRelation, t.getObject()));
                        dataset.add(PreferServerManaged, identifier, LDP.insertedContentRelation,
                                g.stream(identifier, LDP.insertedContentRelation, null).map(Triple::getObject)
                                    .findFirst().orElse(LDP.MemberSubject));
                    });
                }
                // Set the parent relationship
                getContainer(identifier).ifPresent(parent ->
                        dataset.add(PreferServerManaged, identifier, DC.isPartOf, parent));

            }
            final Literal time = rdf.createLiteral(eventTime.toString(), XSD.dateTime);

            try {
                rdfConnection.update(buildUpdateRequest(identifier, time, dataset));
                emitEvents(identifier, time, dataset);
                if (!isDelete) {
                    mementoService.ifPresent(svc -> get(identifier).ifPresent(res ->
                                svc.put(identifier, eventTime, res.stream())));
                }

                return true;
            } catch (final Exception ex) {
                LOGGER.error("Could not update data: {}", ex.getMessage());
            }
            return false;
        });
    }

    @Override
    public List<Range<Instant>> getMementos(final IRI identifier) {
        final List<Instant> mementos = mementoService.map(svc -> svc.list(identifier)).orElse(emptyList());
        sort(mementos);

        final List<Range<Instant>> versions = new ArrayList<>();
        Instant last = null;
        for (final Instant time : mementos) {
            if (nonNull(last)) {
                versions.add(between(last, time));
            }
            last = time;
        }
        if (nonNull(last)) {
            versions.add(between(last, now()));
        }
        return unmodifiableList(versions);
    }


    private void emitEvents(final IRI identifier, final Literal time, final Dataset dataset) {

        // Get the base URL
        final Optional<String> baseUrl = dataset.getGraph().stream(identifier, DC.isPartOf, null)
            .map(Triple::getObject).map(term -> ((IRI) term).getIRIString()).findAny();

        eventService.ifPresent(svc -> {
            svc.emit(new SimpleEvent(getUrl(identifier, baseUrl), dataset));
            getContainer(identifier).ifPresent(parent ->
                    emitEventsForAdjacentResources(svc, parent, time, baseUrl, dataset));
        });
    }

    /**
     * The following SPARQL query is equivalent to the code below.
     *
     * <p><pre><code>
     * SELECT ?predicate ?object ?subject ?memberDate
     * WHERE {
     *   GRAPH trellis:PreferServerManaged {
     *     PARENT rdf:type ?predicate
     *     PARENT dc:modified ?object
     *     OPTIONAL {
     *       PARENT ldp:memberResource ?subject
     *       ?subject dc:modified ?memberDate
     *     }
     *   }
     * }
     * </code></pre></p>
     */
    private void emitEventsForAdjacentResources(final EventService svc, final IRI parent,
            final Literal time, final Optional<String> baseUrl, final Dataset dataset) {
        final Boolean isDelete = dataset.contains(of(PreferAudit), null, RDF.type, AS.Delete);
        final Boolean isCreate = dataset.contains(of(PreferAudit), null, RDF.type, AS.Create);
        final Var memberDate = Var.alloc("memberDate");

        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(memberDate);
        q.addResultVar(SUBJECT);
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb1 = new ElementPathBlock();
        epb1.addTriple(create(rdf.asJenaNode(parent), rdf.asJenaNode(RDF.type), PREDICATE));
        epb1.addTriple(create(rdf.asJenaNode(parent), rdf.asJenaNode(DC.modified), OBJECT));

        final ElementPathBlock epb2 = new ElementPathBlock();
        epb2.addTriple(create(rdf.asJenaNode(parent), rdf.asJenaNode(LDP.membershipResource), SUBJECT));
        epb2.addTriple(create(SUBJECT, rdf.asJenaNode(DC.modified), memberDate));

        final ElementGroup eg = new ElementGroup();
        eg.addElement(epb1);
        eg.addElement(new ElementOptional(epb2));
        q.setQueryPattern(new ElementNamedGraph(rdf.asJenaNode(Trellis.PreferServerManaged), eg));
        rdfConnection.querySelect(q, qs -> {
            final IRI type = getPredicate(qs);
            final Optional<IRI> member = ofNullable(qs.get("subject")).map(RDFNode::asNode)
                .map(rdf::asRDFTerm).map(t -> (IRI) t).filter(t -> !t.equals(parent));
            final Boolean memberIsModified = ofNullable(qs.get("memberDate"))
                .map(RDFNode::asNode).map(rdf::asRDFTerm).filter(time::equals).isPresent();
            if (isCreate || isDelete) {
                if (type.getIRIString().endsWith("Container")) {
                    svc.emit(new SimpleEvent(getUrl(parent, baseUrl), dataset));
                }
                if (LDP.DirectContainer.equals(type) && memberIsModified) {
                    member.ifPresent(m -> svc.emit(new SimpleEvent(getUrl(m, baseUrl), dataset)));
                }
            }
            if (LDP.IndirectContainer.equals(type)) {
                member.ifPresent(m -> svc.emit(new SimpleEvent(getUrl(m, baseUrl), dataset)));
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
        final UpdateDeleteInsert modify = new UpdateDeleteInsert();
        modify.setWithIRI(rdf.asJenaNode(PreferServerManaged));
        modify.getDeleteAcc().addTriple(create(PARENT, rdf.asJenaNode(DC.modified), MODIFIED));
        modify.getInsertAcc().addTriple(create(PARENT, rdf.asJenaNode(DC.modified), rdf.asJenaNode(time)));
        final ElementGroup eg = new ElementGroup();
        final ElementPathBlock epb1 = new ElementPathBlock();
        epb1.addTriple(create(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.isPartOf), PARENT));
        epb1.addTriple(create(PARENT, rdf.asJenaNode(DC.modified), MODIFIED));
        eg.addElement(epb1);
        final ElementPathBlock epb2 = new ElementPathBlock();
        epb2.addTriple(create(PARENT, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.RDFSource)));
        eg.addElement(new ElementMinus(epb2));
        final ElementPathBlock epb3 = new ElementPathBlock();
        epb3.addTriple(create(PARENT, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.NonRDFSource)));
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
        final UpdateDeleteInsert modify = new UpdateDeleteInsert();
        modify.setWithIRI(rdf.asJenaNode(PreferServerManaged));
        modify.getDeleteAcc().addTriple(create(MEMBER, rdf.asJenaNode(DC.modified), MODIFIED));
        modify.getInsertAcc().addTriple(create(MEMBER, rdf.asJenaNode(DC.modified), rdf.asJenaNode(time)));
        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(create(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.isPartOf), PARENT));
        epb.addTriple(create(PARENT, rdf.asJenaNode(LDP.membershipResource), MEMBER));
        epb.addTriple(create(PARENT, rdf.asJenaNode(LDP.hasMemberRelation), ANY));
        epb.addTriple(create(MEMBER, rdf.asJenaNode(DC.modified), MODIFIED));
        modify.setElement(epb);
        return modify;
    }

    private Node getAclIRI(final IRI identifier) {
        return createURI(identifier.getIRIString() + "?ext=acl");
    }

    private Node getAuditIRI(final IRI identifier) {
        return createURI(identifier.getIRIString() + "?ext=audit");
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
    private UpdateRequest buildUpdateRequest(final IRI identifier, final Literal time, final Dataset dataset) {
        final Boolean isDelete = dataset.contains(of(PreferAudit), null, RDF.type, AS.Delete);
        final Boolean isCreate = dataset.contains(of(PreferAudit), null, RDF.type, AS.Create);

        // Set the time
        dataset.add(PreferServerManaged, identifier, DC.modified, time);

        final UpdateRequest req = new UpdateRequest();
        if (isDelete) {
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

        if (isDelete) {
            final QuadDataAcc sink = new QuadDataAcc();
            dataset.stream().filter(q -> q.getGraphName().filter(PreferServerManaged::equals).isPresent())
                    .map(rdf::asJenaQuad).forEach(sink::addQuad);
            dataset.getGraph(PreferAudit).ifPresent(g -> g.stream()
                    .map(t -> new Quad(getAuditIRI(identifier), rdf.asJenaTriple(t))).forEach(sink::addQuad));
            req.add(new UpdateDataInsert(sink));
        } else {
            final QuadDataAcc sink = new QuadDataAcc();
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

        if (isCreate) {
            // Update the parent's modification date
            req.add(getParentUpdateModificationRequest(identifier, time));

            // Likewise update the member resource.
            req.add(getMemberUpdateModificationRequest(identifier, time));

        } else if (!isDelete) {
            // Indirect containers member resources are _always_ updated.
            final UpdateDeleteInsert modify = new UpdateDeleteInsert();
            modify.setWithIRI(rdf.asJenaNode(PreferServerManaged));
            modify.getDeleteAcc().addTriple(create(MEMBER, rdf.asJenaNode(DC.modified), MODIFIED));
            modify.getInsertAcc().addTriple(create(MEMBER, rdf.asJenaNode(DC.modified), rdf.asJenaNode(time)));
            final ElementPathBlock epb = new ElementPathBlock();
            epb.addTriple(create(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.modified), MODIFIED));
            epb.addTriple(create(PARENT, rdf.asJenaNode(LDP.membershipResource), MEMBER));
            epb.addTriple(create(PARENT, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.IndirectContainer)));
            epb.addTriple(create(MEMBER, rdf.asJenaNode(DC.modified), MODIFIED));
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
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(RDF.type), OBJECT));

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
     *       prov:startedAtTime "TIME"^^xsd:dateTime ] }
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
        epb.addTriple(create(rdf.asJenaNode(root), rdf.asJenaNode(RDF.type), OBJECT));

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
            sink.addQuad(new Quad(rdf.asJenaNode(PreferServerManaged), create(rdf.asJenaNode(root),
                            rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.BasicContainer))));
            sink.addQuad(new Quad(rdf.asJenaNode(PreferServerManaged), create(rdf.asJenaNode(root),
                            rdf.asJenaNode(DC.modified), rdf.asJenaNode(time))));

            auditService.ifPresent(svc -> svc.creation(root, new SimpleSession(AdministratorAgent)).stream()
                    .map(quad -> new Quad(getAuditIRI(root), rdf.asJenaTriple(quad.asTriple())))
                    .forEach(sink::addQuad));

            sink.addQuad(new Quad(getAclIRI(root), create(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.mode),
                            rdf.asJenaNode(ACL.Read))));
            sink.addQuad(new Quad(getAclIRI(root), create(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.mode),
                            rdf.asJenaNode(ACL.Write))));
            sink.addQuad(new Quad(getAclIRI(root), create(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.mode),
                            rdf.asJenaNode(ACL.Control))));
            sink.addQuad(new Quad(getAclIRI(root), create(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.agentClass),
                            rdf.asJenaNode(FOAF.Agent))));
            sink.addQuad(new Quad(getAclIRI(root), create(rdf.asJenaNode(auth), rdf.asJenaNode(ACL.accessTo),
                            rdf.asJenaNode(root))));

            update.add(new UpdateDataInsert(sink));
            rdfConnection.update(update);
        }
    }

    @Override
    public Optional<Resource> get(final IRI identifier, final Instant time) {
        return mementoService.map(svc -> svc.get(identifier, time)).orElseGet(() -> get(identifier));
    }

    @Override
    public Optional<Resource> get(final IRI identifier) {
        return TriplestoreResource.findResource(rdfConnection, identifier);
    }

    @Override
    public Supplier<String> getIdentifierSupplier() {
        return supplier;
    }

    @Override
    public Stream<IRI> compact(final IRI identifier, final Instant from, final Instant until) {
        throw new UnsupportedOperationException("compact is not supported");
    }

    @Override
    public Stream<IRI> purge(final IRI identifier) {
        throw new UnsupportedOperationException("purge is not supported");
    }
}
