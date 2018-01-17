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
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.builder;
import static org.apache.jena.graph.Triple.create;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_PREFIX;
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
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
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
public class TriplestoreResourceService implements ResourceService {

    private static final String NL = "\n";
    private static final String WS = " ";
    private static final String BRR = " } ";
    private static final String BRL = " { ";

    private static final Logger LOGGER = getLogger(TriplestoreResourceService.class);
    private static final JenaRDF rdf = getInstance();
    private static Optional<AuditService> auditService = findFirst(AuditService.class);

    private final Supplier<String> supplier;
    private final Optional<EventService> eventService;
    private final RDFConnection rdfConnection;

    /**
     * Create a triplestore-backed resource service.
     * @param rdfConnection the connection to an RDF datastore
     * @param identifierService an ID supplier service
     * @param eventService an event service
     */
    public TriplestoreResourceService(final RDFConnection rdfConnection, final IdentifierService identifierService,
            final EventService eventService) {
        requireNonNull(rdfConnection, "RDFConnection may not be null!");
        requireNonNull(identifierService, "IdentifierService may not be null!");
        this.rdfConnection = rdfConnection;
        this.supplier = identifierService.getSupplier(TRELLIS_PREFIX);
        this.eventService = ofNullable(eventService);
        init();
    }

    @Override
    public Future<Boolean> put(final IRI identifier, final IRI ixnModel, final Dataset dataset) {
        return supplyAsync(() -> {
            final Boolean isDelete = dataset.contains(of(PreferAudit), null, RDF.type, AS.Delete);
            final Boolean isCreate = dataset.contains(of(PreferAudit), null, RDF.type, AS.Create);

            // Get the base URL
            final Optional<String> baseUrl = dataset.getGraph().stream(identifier, DC.isPartOf, null)
                .map(Triple::getObject).map(term -> ((IRI) term).getIRIString()).findAny();

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
                        g.stream(identifier, LDP.membershipResource, null).findFirst().ifPresent(t ->
                                dataset.add(PreferServerManaged, identifier, LDP.membershipResource, t.getObject()));
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

                // TODO save current state of resource to the versioning system
            }

            // Set the time
            final Literal time = rdf.createLiteral(now().toString(), XSD.dateTime);
            dataset.add(PreferServerManaged, identifier, DC.modified, time);

            /*
             * DELETE WHERE { GRAPH <IDENTIFIER> { ?s ?p ?o } };
             * DELETE WHERE { GRAPH <IDENTIFIER?ext=acl> { ?s ?p ?o } };
             * DELETE WHERE { GRAPH trellis:PreferServerManaged { <IDENTIFIER> ?p ?o } };
             * INSERT DATA {
             *   GRAPH <IDENTIFIER> { ... }
             *   GRAPH <IDENTIFIER?ext=acl> { ... }
             *   GRAPH trellis:PreferServerManaged { ... }
             *   GRAPH <IDENTIFIER?ext=audit> { ... }
             * };
             * // this next clause happens first in an HTTP delete operation
             * WITH trellis:PreferServerManaged
             *   DELETE { ?parent dc:modified ?x }
             *   INSERT { ?parent dc:modified <TIME> }
             *   WHERE {
             *     <IDENTIFIER> dc:isPartOf ?parent .
             *     ?parent rdf:type ldp:Container
             * }
             */
            // TODO -- start using Jena primitives instead of String concatenation
            final UpdateRequest req = new UpdateRequest();
            if (isDelete) {
                // The parent container's modified date is updated on delete and create actions.
                req.add("WITH " + PreferServerManaged
                        + "DELETE" + BRL + "?parent" + WS + DC.modified + WS + "?modified" + BRR
                        + "INSERT" + BRL + "?parent" + WS + DC.modified + WS + time + BRR
                        + "WHERE" + BRL + identifier + WS + DC.isPartOf + WS + "?parent . "
                            + "?parent" + WS + DC.modified + WS + "?modified . "
                            + "MINUS { ?parent " + RDF.type + WS + LDP.RDFSource + BRR
                            + "MINUS { ?parent " + RDF.type + WS + LDP.NonRDFSource + BRR + BRR);

                // Likewise the member resource.
                req.add("WITH  " + PreferServerManaged
                        + "DELETE { ?member " + DC.modified + WS + "?modified }"
                        + "INSERT { ?member " + DC.modified + WS + time + BRR
                        + "WHERE { " + identifier + WS + DC.isPartOf + "   ?parent . "
                            + "?parent " + LDP.membershipResource + " ?member ."
                            + "?member " + DC.modified + "  ?modified }");
            }

            req.add("DELETE WHERE { GRAPH " + identifier + " { ?s ?p ?o } };");
            req.add("DELETE WHERE { GRAPH <" + identifier.getIRIString() + "?ext=acl> { ?s ?p ?o } }");
            req.add("DELETE WHERE { GRAPH " + PreferServerManaged + " { " + identifier + " ?p ?o } }");
            if (isDelete) {
                req.add("INSERT DATA {"
                    + "GRAPH " + PreferServerManaged + " {" + dataset.getGraph(PreferServerManaged).map(Graph::stream)
                        .orElseGet(Stream::empty).map(Triple::toString).collect(joining(NL)) + "}\n"
                    + "GRAPH <" + identifier.getIRIString() + "?ext=audit> {" + dataset.getGraph(PreferAudit)
                        .map(Graph::stream).orElseGet(Stream::empty)
                        .map(Triple::toString).collect(joining(NL)) + "}}");
            } else {
                req.add("INSERT DATA {"
                    + "GRAPH " + PreferServerManaged + " {" + dataset.getGraph(PreferServerManaged).map(Graph::stream)
                        .orElseGet(Stream::empty).map(Triple::toString).collect(joining(NL)) + "}\n"
                    + "GRAPH " + identifier + " {" + dataset.getGraph(PreferUserManaged).map(Graph::stream)
                        .orElseGet(Stream::empty).map(Triple::toString).collect(joining(NL)) + "}\n"
                    + "GRAPH <" + identifier.getIRIString() + "?ext=acl> {" + dataset.getGraph(PreferAccessControl)
                        .map(Graph::stream).orElseGet(Stream::empty).map(Triple::toString).collect(joining(NL)) + "}"
                    + "GRAPH <" + identifier.getIRIString() + "?ext=audit> {" + dataset.getGraph(PreferAudit)
                        .map(Graph::stream).orElseGet(Stream::empty).map(Triple::toString)
                        .collect(joining(NL)) + "}}");
            }

            if (isCreate) {
                // The parent container's modified date is updated on delete and create actions.
                req.add("WITH  " + PreferServerManaged
                        + " DELETE { ?parent " + DC.modified + " ?modified } "
                        + " INSERT { ?parent " + DC.modified + WS + time + BRR
                        + " WHERE { " + identifier + WS + DC.isPartOf + " ?parent . "
                            + "?parent " + DC.modified + " ?modified . "
                            + "MINUS {  ?parent " + RDF.type + WS + LDP.RDFSource + BRR
                            + "MINUS {  ?parent " + RDF.type + WS + LDP.NonRDFSource + BRR + BRR);

                // Likewise the member resource.
                req.add("WITH  " + PreferServerManaged
                        + " DELETE { ?member " + DC.modified + " ?modified } "
                        + " INSERT { ?member " + DC.modified + WS + time + BRR
                        + " WHERE { " + identifier + WS + DC.isPartOf + " ?parent . "
                            + " ?parent " + LDP.membershipResource + " ?member ."
                            + " ?parent " + LDP.hasMemberRelation + " ?any ."
                            + " ?member " + DC.modified + " ?modified }");
            } else if (!isDelete) {
                // Indirect containers member resources are _always_ updated.
                req.add("WITH " + PreferServerManaged
                        + " DELETE { ?member " + DC.modified + " ?modified } "
                        + " INSERT { ?member " + DC.modified + WS + time + BRR
                        + " WHERE { " + identifier + WS + DC.isPartOf + " ?parent ."
                            + "  ?parent " + LDP.membershipResource + " ?member ."
                            + "  ?parent " + RDF.type + WS + LDP.IndirectContainer + " ."
                            + "  ?member " + DC.modified + " ?modified }");
            }

            try {
                rdfConnection.update(req);

                eventService.ifPresent(svc -> {
                    svc.emit(new SimpleEvent(getUrl(identifier, baseUrl), dataset));
                    getContainer(identifier).ifPresent(parent -> {
                        /*
                         * SELECT ?predicate ?object ?subject ?memberDate
                         * WHERE {
                         *   GRAPH trellis:PreferServerManaged {
                         *     <PARENT> rdf:type ?predicate
                         *     <PARENT> dc:modified ?object
                         *     OPTIONAL {
                         *       <PARENT> ldp:memberResource ?subject
                         *       ?subject dc:modified ?memberDate
                         *     }
                         *   }
                         * }
                         */
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
                    });
                });
                return true;
            } catch (final Exception ex) {
                LOGGER.error("Could not update data: {}", ex.getMessage());
            }
            return false;
        });
    }

    private String getUrl(final IRI identifier, final Optional<String> baseUrl) {
        if (baseUrl.isPresent()) {
            return toExternal(identifier, baseUrl.get()).getIRIString();
        }
        LOGGER.warn("No baseURL defined. Emitting message with resource's internal IRI");
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

    private void init() {
        final IRI root = rdf.createIRI(TRELLIS_PREFIX);
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
            /*
             * INSERT DATA {
             *   GRAPH trellis:PreferServerManaged {
             *     <IDENTIFIER> rdf:type ldp:Container ;
             *                  dc:modified "NOW"^^xsd:dateTime }
             *   GRAPH <IDENTIFIER?ext=audit> {
             *     <IDENTIFIER> PROV.wasGeneratedBy [
             *       rdf:type prov:Activity , as:Create ;
             *       prov:wasAssociatedWith trellis:AdministorAgent ;
             *       prov:startedAtTime "TIME"^^xsd:dateTime ] }
             *   GRAPH <IDENTIFIER?ext=acl> {
             *     <IDENTIFIER> acl:mode acl.Read , acl:Write , acl:Control ;
             *       acl:agentClass foaf:Agent ;
             *       acl:accessTo <IDENTIFIER> }
             * }
             */
            final Literal time = rdf.createLiteral(now().toString(), XSD.dateTime);
            final BlankNode bnode = rdf.createBlankNode();
            final IRI auth = rdf.createIRI(TRELLIS_PREFIX + "#auth");
            final Dataset dataset = rdf.createDataset();
            dataset.add(PreferServerManaged, root, RDF.type, LDP.Container);
            auditService.ifPresent(svc -> svc.creation(root, new SimpleSession(AdministratorAgent))
                    .forEach(dataset::add));

            final UpdateRequest update = new UpdateRequest();
            update.add("INSERT DATA { GRAPH " + PreferServerManaged + BRL
                        + rdf.createTriple(root, RDF.type, LDP.Container)
                        + rdf.createTriple(root, DC.modified, time) + BRR
                    + " GRAPH <" + root.getIRIString() + "?ext=audit> {"
                        + dataset.getGraph(PreferAudit).map(Graph::stream).orElseGet(Stream::empty)
                            .map(Triple::toString).collect(joining("\n")) + BRR
                    + " GRAPH <" + root.getIRIString() + "?ext=acl> {"
                        + rdf.createTriple(auth, ACL.mode, ACL.Read)
                        + rdf.createTriple(auth, ACL.mode, ACL.Write)
                        + rdf.createTriple(auth, ACL.mode, ACL.Control)
                        + rdf.createTriple(auth, ACL.agentClass, FOAF.Agent)
                        + rdf.createTriple(auth, ACL.accessTo, root) + BRR + BRR);

            rdfConnection.update(update);
        }
    }

    @Override
    public Optional<Resource> get(final IRI identifier, final Instant time) {
        // TODO -- add versioning support via local files
        return get(identifier);
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
