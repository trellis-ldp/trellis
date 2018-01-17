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

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang3.Range.between;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.triplestore.TriplestoreUtils.OBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.PREDICATE;
import static org.trellisldp.triplestore.TriplestoreUtils.SUBJECT;
import static org.trellisldp.triplestore.TriplestoreUtils.getInstance;
import static org.trellisldp.triplestore.TriplestoreUtils.getObject;
import static org.trellisldp.triplestore.TriplestoreUtils.getPredicate;
import static org.trellisldp.triplestore.TriplestoreUtils.getSubject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.jena.JenaGraph;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.query.Query;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.Trellis;

/**
 * A triplestore-based implementation of the Trellis Resource API.
 */
public class TriplestoreResource implements Resource {

    private static final Logger LOGGER = getLogger(TriplestoreResource.class);
    private static final JenaRDF rdf = getInstance();

    private final IRI identifier;
    private final RDFConnection rdfConnection;
    private final JenaGraph graph = rdf.createGraph();
    private final Map<IRI, Supplier<Stream<Quad>>> graphMapper = new HashMap<>();

    /**
     * Create a Triplestore-based Resource.
     * @param rdfConnection the triplestore connector
     * @param identifier the identifier
     */
    public TriplestoreResource(final RDFConnection rdfConnection, final IRI identifier) {
        this.identifier = identifier;
        this.rdfConnection = rdfConnection;
        graphMapper.put(Trellis.PreferUserManaged, this::fetchUserQuads);
        graphMapper.put(Trellis.PreferServerManaged, this::fetchServerQuads);
        graphMapper.put(Trellis.PreferAudit, this::fetchAuditQuads);
        graphMapper.put(Trellis.PreferAccessControl, this::fetchAclQuads);
        graphMapper.put(LDP.PreferContainment, this::fetchContainmentQuads);
        graphMapper.put(LDP.PreferMembership, this::fetchMembershipQuads);
    }

    /**
     * Try to load a Trellis resource.
     * @param rdfConnection the triplestore connector
     * @param identifier the identifier
     * @return a Resource, if one exists
     */
    public static Optional<Resource> findResource(final RDFConnection rdfConnection, final IRI identifier) {
        final TriplestoreResource res = new TriplestoreResource(rdfConnection, identifier);
        res.fetchData();
        return res.exists() ? of(res) : empty();
    }

    /**
     * Test whether this resource exists.
     * @return true if this resource exists; false otherwise
     */
    protected Boolean exists() {
        return nonNull(getModified()) && nonNull(getInteractionModel());
    }

    /**
     * Fetch data for this resource.
     */
    protected void fetchData() {
        /*
         * SELECT ?predicate ?object
         * WHERE {
         *   GRAPH <http://www.trellisldp.org/ns/trellis#PreferServerManaged> {
         *     <identifier> ?predicate ?object } }
         */
        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(create(rdf.asJenaNode(identifier), PREDICATE, OBJECT));

        final ElementNamedGraph ng = new ElementNamedGraph(rdf.asJenaNode(Trellis.PreferServerManaged), epb);

        final ElementGroup elg = new ElementGroup();
        elg.addElement(ng);
        q.setQueryPattern(elg);

        rdfConnection.querySelect(q, qs ->
            graph.add(identifier, getPredicate(qs), getObject(qs)));
    }

    @Override
    public Stream<Quad> stream() {
        return graphMapper.values().stream().flatMap(Supplier::get);
    }

    @Override
    public Stream<Triple> stream(final Collection<IRI> graphNames) {
        return graphNames.stream().filter(graphMapper::containsKey).map(graphMapper::get).flatMap(Supplier::get)
            .map(Quad::asTriple);
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public IRI getInteractionModel() {
        return (IRI) graph.stream(identifier, RDF.type, null).map(Triple::getObject).findFirst().orElse(null);
    }

    @Override
    public Optional<IRI> getMembershipResource() {
        return graph.stream(identifier, LDP.membershipResource, null).map(t -> (IRI) t.getObject()).findFirst();
    }

    @Override
    public Optional<IRI> getMemberRelation() {
        return graph.stream(identifier, LDP.hasMemberRelation, null).map(t -> (IRI) t.getObject()).findFirst();
    }

    @Override
    public Optional<IRI> getMemberOfRelation() {
        return graph.stream(identifier, LDP.isMemberOfRelation, null).map(t -> (IRI) t.getObject()).findFirst();
    }

    @Override
    public Optional<IRI> getInsertedContentRelation() {
        return graph.stream(identifier, LDP.insertedContentRelation, null).map(t -> (IRI) t.getObject()).findFirst();
    }

    @Override
    public List<Range<Instant>> getMementos() {
        // TODO -- reimplement this with a working versioning system
        final List<Instant> mementos = graph.stream(identifier, PROV.generatedAtTime, null)
            .map(triple -> (Literal) triple.getObject()).map(Literal::getLexicalForm).map(Instant::parse).sorted()
            .collect(toList());

        final List<Range<Instant>> versions = new ArrayList<>();
        Instant last = null;
        for (final Instant time : mementos) {
            if (nonNull(last)) {
                versions.add(between(last, time));
            }
            last = time;
        }
        final Instant mod = getModified();
        requireNonNull(mod, "resource modification value is null!");

        if (nonNull(last)) {
            versions.add(between(last, mod));
        }
        return unmodifiableList(versions);
    }

    @Override
    public Optional<Binary> getBinary() {
        return graph.stream(identifier, DC.hasPart, null).map(Triple::getObject).findFirst().map(id -> {
            final Instant date = graph.stream(identifier, DC.date, null).map(Triple::getObject).map(t -> (Literal) t)
                .map(Literal::getLexicalForm).map(Instant::parse).findFirst().orElse(null);
            final String mimeType = graph.stream(identifier, DC.format, null).map(Triple::getObject)
                .map(t -> (Literal) t).map(Literal::getLexicalForm).findFirst().orElse(null);
            final Long size = graph.stream(identifier, DC.extent, null).map(Triple::getObject).map(t -> (Literal) t)
                .map(Literal::getLexicalForm).map(Long::parseLong).findFirst().orElse(null);
            return new Binary((IRI) id, date, mimeType, size);
        });
    }

    @Override
    public Instant getModified() {
        return graph.stream(identifier, DC.modified, null).map(triple -> (Literal) triple.getObject())
            .map(Literal::getLexicalForm).map(Instant::parse).findFirst().orElse(null);
    }

    @Override
    public Boolean hasAcl() {
        return fetchAclQuads().findAny().isPresent();
    }

    @Override
    public Boolean isDeleted() {
        return graph.contains(identifier, DC.type, Trellis.DeletedResource);
    }

    private Stream<Quad> fetchServerQuads() {
        return graph.stream().map(triple -> rdf.createQuad(Trellis.PreferServerManaged,
                    triple.getSubject(), triple.getPredicate(), triple.getObject()));
    }

    private Stream<Quad> fetchAuditQuads() {
        /*
         * SELECT ?subject ?predicate ?object
         * WHERE { GRAPH <IDENTIFIER?ext=audit> { ?subject ?predicate ?object } }
        */
        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(SUBJECT);
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(create(SUBJECT, PREDICATE, OBJECT));

        final ElementNamedGraph ng = new ElementNamedGraph(createURI(identifier.getIRIString() + "?ext=audit"), epb);

        final ElementGroup elg = new ElementGroup();
        elg.addElement(ng);

        q.setQueryPattern(elg);

        final Stream.Builder<Quad> builder = builder();
        rdfConnection.querySelect(q, qs -> builder.accept(rdf.createQuad(Trellis.PreferAudit,
                        getSubject(qs), getPredicate(qs), getObject(qs))));
        return builder.build();
    }

    private Stream<Quad> fetchAclQuads() {
        /*
         * SELECT ?subject ?predicate ?object
         * WHERE { GRAPH <IDENTIFIER?ext=audit> { ?subject ?predicate ?object } }
         */
        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(SUBJECT);
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(create(SUBJECT, PREDICATE, OBJECT));

        final ElementNamedGraph ng = new ElementNamedGraph(createURI(identifier.getIRIString() + "?ext=acl"), epb);

        final ElementGroup elg = new ElementGroup();
        elg.addElement(ng);

        q.setQueryPattern(elg);

        final Stream.Builder<Quad> builder = builder();
        rdfConnection.querySelect(q, qs -> builder.accept(rdf.createQuad(Trellis.PreferAccessControl,
                        getSubject(qs), getPredicate(qs), getObject(qs))));
        return builder.build();
    }

    private Stream<Quad> fetchMembershipQuads() {
        return concat(
                concat(fetchIndirectMemberQuads(), fetchIndirectMemberDefaultContent()),
                concat(fetchDirectMemberQuads(), fetchDirectMemberQuadsInverse()));
    }

    private Stream<Quad> fetchIndirectMemberQuads() {
        /*
         * SELECT ?predicate ?object
         * WHERE {
         *   GRAPH trellis:PreferServerManaged {
         *      ?subject ldp:membershipResource <IDENTIFIER>
         *      AND ?subject rdf:type ldp:IndirectContainer
         *      AND ?subject ldp:membershipRelation ?predicate
         *      AND ?subject ldp:insertedContentRelation ?o
         *      AND ?s dc:isPartOf ?subject
         *   }
         *   GRAPH ?s { ?s ?o ?object }
         * }
         */

        final Var s = Var.alloc("s");
        final Var o = Var.alloc("o");

        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb1 = new ElementPathBlock();
        epb1.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.membershipResource), rdf.asJenaNode(identifier)));
        epb1.addTriple(create(SUBJECT, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.IndirectContainer)));
        epb1.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.hasMemberRelation), PREDICATE));
        epb1.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.insertedContentRelation), o));
        epb1.addTriple(create(s, rdf.asJenaNode(DC.isPartOf), SUBJECT));

        final ElementPathBlock epb2 = new ElementPathBlock();
        epb2.addTriple(create(s, o, OBJECT));

        final ElementGroup elg = new ElementGroup();
        elg.addElement(new ElementNamedGraph(rdf.asJenaNode(Trellis.PreferServerManaged), epb1));
        elg.addElement(new ElementNamedGraph(s, epb2));

        q.setQueryPattern(elg);

        final Stream.Builder<Quad> builder = builder();
        rdfConnection.querySelect(q, qs ->
            builder.accept(rdf.createQuad(LDP.PreferMembership, identifier, getPredicate(qs), getObject(qs))));
        return builder.build();
    }

    private Stream<Quad> fetchIndirectMemberDefaultContent() {
        /*
         * SELECT ?predicate ?object
         * WHERE {
         *   GRAPH trellis:PreferServerManaged {
         *      ?subject ldp:membershipResource <IDENTIFIER>
         *      AND ?subject rdf:type ldp:IndirectContainer
         *      AND ?subject ldp:membershipRelation ?predicate
         *      AND ?subject ldp:insertedContentRelation ldp:MemberSubject
         *      AND ?object dc:isPartOf ?subject
         *   }
         * }
         */
        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.membershipResource), rdf.asJenaNode(identifier)));
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.IndirectContainer)));
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.hasMemberRelation), PREDICATE));
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.insertedContentRelation), rdf.asJenaNode(LDP.MemberSubject)));
        epb.addTriple(create(OBJECT, rdf.asJenaNode(DC.isPartOf), SUBJECT));

        final ElementNamedGraph ng = new ElementNamedGraph(rdf.asJenaNode(Trellis.PreferServerManaged), epb);

        final ElementGroup elg = new ElementGroup();
        elg.addElement(ng);

        q.setQueryPattern(elg);

        final Stream.Builder<Quad> builder = builder();
        rdfConnection.querySelect(q, qs -> builder.accept(rdf.createQuad(LDP.PreferMembership, identifier,
                        getPredicate(qs), getObject(qs))));
        return builder.build();
    }

    private Stream<Quad> fetchDirectMemberQuads() {
        /*
         * SELECT ?predicate ?object
         * WHERE {
         *   GRAPH trellis:PreferServerManaged {
         *      ?subject ldp:membershipResource <IDENTIFIER>
         *      AND ?subject rdf:type ldp:DirectContainer
         *      AND ?subject ldp:hasMemberRelation ?predicate
         *      AND ?object dc:isPartOf ?subject }
         * }
         */
        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.membershipResource), rdf.asJenaNode(identifier)));
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.DirectContainer)));
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.hasMemberRelation), PREDICATE));
        epb.addTriple(create(OBJECT, rdf.asJenaNode(DC.isPartOf), SUBJECT));

        final ElementNamedGraph ng = new ElementNamedGraph(rdf.asJenaNode(Trellis.PreferServerManaged), epb);

        final ElementGroup elg = new ElementGroup();
        elg.addElement(ng);

        q.setQueryPattern(elg);

        final Stream.Builder<Quad> builder = builder();
        rdfConnection.querySelect(q, qs -> builder.accept(rdf.createQuad(LDP.PreferMembership, identifier,
                        getPredicate(qs), getObject(qs))));
        return builder.build();
    }

    private Stream<Quad> fetchDirectMemberQuadsInverse() {
        /*
         * SELECT ?predicate ?object
         * WHERE {
         *   GRAPH trellis:PreferServerManaged {
         *      <IDENTIFIER> dc:isPartOf ?subject .
         *      ?subject rdf:type ldp:DirectContainer .
         *      ?subject ldp:isMemberOfRelation ?predicate .
         *      ?subject ldp:membershipResource ?object .
         *   }
         * }
         */
        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(create(rdf.asJenaNode(identifier), rdf.asJenaNode(DC.isPartOf), SUBJECT));
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(RDF.type), rdf.asJenaNode(LDP.DirectContainer)));
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.isMemberOfRelation), PREDICATE));
        epb.addTriple(create(SUBJECT, rdf.asJenaNode(LDP.membershipResource), OBJECT));

        final ElementNamedGraph ng = new ElementNamedGraph(rdf.asJenaNode(Trellis.PreferServerManaged), epb);

        final ElementGroup elg = new ElementGroup();
        elg.addElement(ng);

        q.setQueryPattern(elg);

        final Stream.Builder<Quad> builder = builder();
        rdfConnection.querySelect(q, qs -> builder.accept(rdf.createQuad(LDP.PreferMembership, identifier,
                        getPredicate(qs), getObject(qs))));
        return builder.build();
    }

    private Stream<Quad> fetchContainmentQuads() {
        if (getInteractionModel().getIRIString().endsWith("Container")) {
            /*
             * SELECT ?object
             * WHERE {
             *   GRAPH trellis:PreferServerManaged { ?object dc:isPartOf <IDENTIFIER> }
             * }
             */
            final Query q = new Query();
            q.setQuerySelectType();
            q.addResultVar(OBJECT);

            final ElementPathBlock epb = new ElementPathBlock();
            epb.addTriple(create(OBJECT, rdf.asJenaNode(DC.isPartOf), rdf.asJenaNode(identifier)));

            final ElementNamedGraph ng = new ElementNamedGraph(rdf.asJenaNode(Trellis.PreferServerManaged), epb);

            final ElementGroup elg = new ElementGroup();
            elg.addElement(ng);
            q.setQueryPattern(elg);

            final Stream.Builder<Quad> builder = builder();
            rdfConnection.querySelect(q, qs -> builder.accept(rdf.createQuad(LDP.PreferContainment,
                            identifier, LDP.contains, getObject(qs))));
            return builder.build();
        }
        return Stream.empty();
    }

    private Stream<Quad> fetchUserQuads() {
        /*
         * SELECT ?subject ?predicate ?object
         * WHERE { GRAPH <IDENTIFIER> { ?subject ?predicate ?object } }
        */
        final Query q = new Query();
        q.setQuerySelectType();
        q.addResultVar(SUBJECT);
        q.addResultVar(PREDICATE);
        q.addResultVar(OBJECT);

        final ElementPathBlock epb = new ElementPathBlock();
        epb.addTriple(create(SUBJECT, PREDICATE, OBJECT));

        final ElementNamedGraph ng = new ElementNamedGraph(rdf.asJenaNode(identifier), epb);

        final ElementGroup elg = new ElementGroup();
        elg.addElement(ng);
        q.setQueryPattern(elg);

        final Stream.Builder<Quad> builder = builder();
        rdfConnection.querySelect(q, qs -> builder.accept(rdf.createQuad(Trellis.PreferUserManaged,
                        getSubject(qs), getPredicate(qs), getObject(qs))));
        return builder.build();
    }
}
