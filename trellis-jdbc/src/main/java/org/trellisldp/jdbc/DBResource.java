/*
 * Copyright (c) Aaron Coburn and individual contributors
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

import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.jena.commonsrdf.JenaCommonsRDF.fromJena;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.Lang.NTRIPLES;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.vocabulary.RDF.type;

import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFParser;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * A db-based implementation of the Trellis Resource API.
 */
public class DBResource implements Resource {

    private static final Logger LOGGER = getLogger(DBResource.class);
    private static final RDF rdf = RDFFactory.getInstance();

    private static final String SLASH = "/";
    private static final String OBJECT = "object";
    private static final String LANG = "lang";
    private static final String DATATYPE = "datatype";
    private static final String IXN_MODEL = "interaction_model";
    private static final String SUBJECT = "subject";
    private static final String PREDICATE = "predicate";
    private static final String MEMBERSHIP_RESOURCE = "ldp_membership_resource";
    private static final String HAS_MEMBER_RELATION = "ldp_has_member_relation";
    private static final String IS_MEMBER_OF_RELATION = "ldp_is_member_of_relation";
    private static final Set<IRI> containerTypes = Set.of(LDP.Container, LDP.BasicContainer, LDP.DirectContainer,
            LDP.IndirectContainer);

    private final IRI identifier;
    private final Jdbi jdbi;
    private final boolean includeLdpType;
    private final boolean supportDirectContainment;
    private final boolean supportIndirectContainment;
    private final Map<IRI, String> extensions = new HashMap<>();
    private final Map<IRI, Supplier<Stream<Quad>>> graphMapper = new HashMap<>();

    private ResourceData data;

    /**
     * Create a DB-based Resource.
     * @param jdbi the jdbi object
     * @param identifier the identifier
     * @param extensions a map of extensions
     * @param includeLdpType whether to include the LDP type in the RDF body
     * @param supportDirectContainment whether to support direct containment
     * @param supportIndirectContainment whether to support indirect containment
     */
    protected DBResource(final Jdbi jdbi, final IRI identifier, final Map<String, IRI> extensions,
            final boolean includeLdpType, final boolean supportDirectContainment,
            final boolean supportIndirectContainment) {
        this.identifier = identifier;
        this.jdbi = jdbi;
        this.includeLdpType = includeLdpType;
        this.supportDirectContainment = supportDirectContainment;
        this.supportIndirectContainment = supportIndirectContainment;
        graphMapper.put(Trellis.PreferServerManaged, this::fetchServerManagedQuads);
        graphMapper.put(Trellis.PreferUserManaged, this::fetchUserManagedQuads);
        graphMapper.put(Trellis.PreferAudit, this::fetchAuditQuads);
        graphMapper.put(Trellis.PreferAccessControl, this::fetchAccessControlQuads);
        graphMapper.put(LDP.PreferContainment, this::fetchContainmentQuads);
        graphMapper.put(LDP.PreferMembership, this::fetchMembershipQuads);

        extensions.forEach((k, v) -> {
            if (!graphMapper.containsKey(v)) {
                this.extensions.put(v, k);
            }
        });
}

    /**
     * Try to load a Trellis resource.
     * @param ds the datasource
     * @param identifier the identifier
     * @param extensions a map of extensions
     * @param includeLdpType whether to include the LDP type in the RDF body
     * @return a Resource, if one exists
     */
    public static CompletionStage<Resource> findResource(final DataSource ds, final IRI identifier,
            final Map<String, IRI> extensions, final boolean includeLdpType) {
        return findResource(Jdbi.create(ds), identifier, extensions, includeLdpType);
    }

    /**
     * Try to load a Trellis resource.
     * @param jdbi the Jdbi object
     * @param identifier the identifier
     * @param extensions a map of extensions
     * @param includeLdpType whether to include the LDP type in the RDF body
     * @return a Resource, if one exists
     */
    public static CompletionStage<Resource> findResource(final Jdbi jdbi, final IRI identifier,
            final Map<String, IRI> extensions, final boolean includeLdpType) {
        return findResource(jdbi, identifier, extensions, includeLdpType, true, true);
    }

    /**
     * Try to load a Trellis resource.
     * @param jdbi the Jdbi object
     * @param identifier the identifier
     * @param extensions a map of extensions
     * @param includeLdpType whether to include the LDP type in the RDF body
     * @param supportDirectContainment whether to support direct containment
     * @param supportIndirectContainment whether to support indirect containment
     * @return a Resource, if one exists
     */
    public static CompletionStage<Resource> findResource(final Jdbi jdbi, final IRI identifier,
            final Map<String, IRI> extensions, final boolean includeLdpType, final boolean supportDirectContainment,
            final boolean supportIndirectContainment) {
        return supplyAsync(() -> {
            final DBResource res = new DBResource(jdbi, identifier, extensions, includeLdpType,
                    supportDirectContainment, supportIndirectContainment);
            if (!res.fetchData()) {
                return MISSING_RESOURCE;
            }
            if (res.isDeleted()) {
                return DELETED_RESOURCE;
            }
            return res;
        });
    }

    /**
     * Identify whether this resource had previously been deleted.
     * @return true if the resource previously existed
     */
    public boolean isDeleted() {
        return data.isDeleted();
    }

    @Override
    public Stream<Quad> stream() {
        return Stream.concat(graphMapper.values().stream().flatMap(Supplier::get),
                extensions.keySet().stream().flatMap(this::fetchExtensionQuads));
    }

    @Override
    public Stream<Quad> stream(final Collection<IRI> graphNames) {
        return Stream.concat(graphNames.stream().filter(graphMapper::containsKey).map(graphMapper::get)
                    .flatMap(Supplier::get),
                graphNames.stream().filter(extensions::containsKey).flatMap(this::fetchExtensionQuads));
    }

    @Override
    public IRI getIdentifier() {
        return identifier;
    }

    @Override
    public IRI getInteractionModel() {
        return data.getInteractionModel();
    }

    @Override
    public Optional<IRI> getContainer() {
        return data.getIsPartOf();
    }

    @Override
    public Optional<IRI> getMembershipResource() {
        return data.getMembershipResource();
    }

    @Override
    public Optional<IRI> getMemberRelation() {
        return data.getHasMemberRelation();
    }

    @Override
    public Optional<IRI> getMemberOfRelation() {
        return data.getIsMemberOfRelation();
    }

    @Override
    public Optional<IRI> getInsertedContentRelation() {
        return data.getInsertedContentRelation();
    }

    @Override
    public Optional<BinaryMetadata> getBinaryMetadata() {
        return data.getBinaryMetadata();
    }

    @Override
    public Instant getModified() {
        return data.getModified();
    }

    @Override
    public boolean hasMetadata(final IRI graphName) {
        if (Trellis.PreferAccessControl.equals(graphName)) {
            return data.hasAcl();
        }
        return getMetadataGraphNames().contains(graphName);
    }

    @Override
    public Set<IRI> getMetadataGraphNames() {
        final Set<IRI> graphs = new HashSet<>(fetchExtensionGraphNames());
        if (data.hasAcl()) {
            graphs.add(Trellis.PreferAccessControl);
        }
        return unmodifiableSet(graphs);
    }

    @Override
    public Stream<Map.Entry<String,String>> getExtraLinkRelations() {
        return data.getExtra().entrySet().stream();
    }

    /**
     * Combine the various membership-related quad streams.
     */
    private Stream<Quad> fetchMembershipQuads() {
        return Stream.concat(fetchIndirectMemberQuads(),
                Stream.concat(fetchDirectMemberQuads(), fetchDirectMemberQuadsInverse()));
    }

    /**
     * Fetch a stream of user-managed quads.
     */
    private Stream<Quad> fetchUserManagedQuads() {
        return fetchQuadsFromTable("description", Trellis.PreferUserManaged);
    }

    /**
     * Fetch a stream of server-managed quads.
     */
    private Stream<Quad> fetchServerManagedQuads() {
        if (includeLdpType) {
            return Stream.of(rdf.createQuad(Trellis.PreferServerManaged,
                            adjustIdentifier(getIdentifier(), getInteractionModel()), type, getInteractionModel()));
        }
        return Stream.empty();
    }

    /**
     * Fetch a stream of webac quads.
     */
    private Stream<Quad> fetchAccessControlQuads() {
        return fetchQuadsFromTable("acl", Trellis.PreferAccessControl);
    }

    /**
     * Fetch a stream of the audit-related quads.
     */
    private Stream<Quad> fetchAuditQuads() {
        final String query = "SELECT subject, predicate, object, lang, datatype FROM log WHERE id = ?";
        return jdbi.withHandle(handle -> handle.select(query, getIdentifier().getIRIString())
                .map((rs, ctx) -> rdf.createQuad(Trellis.PreferAudit, rdf.createIRI(rs.getString(SUBJECT)),
                        rdf.createIRI(rs.getString(PREDICATE)),
                        getObject(rs.getString(OBJECT), rs.getString(LANG), rs.getString(DATATYPE))))
                .list()).stream().map(Quad.class::cast);
    }

    /**
     * Fetch a stream of membership quads based on indirect containment with a custom
     * ldp:insertedContentRelation value.
     */
    private Stream<Quad> fetchIndirectMemberQuads() {
        if (supportIndirectContainment) {
            final String query
                = "SELECT r2.ldp_membership_resource, r2.ldp_has_member_relation, d.object, d.lang, d.datatype "
                + "FROM resource AS r INNER JOIN resource AS r2 ON r.is_part_of = r2.subject "
                + "INNER JOIN description AS d ON r.id = d.resource_id "
                + "   AND d.predicate = r2.ldp_inserted_content_relation "
                + "WHERE r2.ldp_member = ? AND r2.interaction_model = ? AND r2.ldp_has_member_relation IS NOT NULL";

            return jdbi.withHandle(handle -> handle.select(query,
                        getIdentifier().getIRIString(), LDP.IndirectContainer.getIRIString())
                    .map((rs, ctx) -> rdf.createQuad(LDP.PreferMembership,
                                rdf.createIRI(rs.getString(MEMBERSHIP_RESOURCE)),
                                rdf.createIRI(rs.getString(HAS_MEMBER_RELATION)),
                                getObject(rs.getString(OBJECT), rs.getString(LANG), rs.getString(DATATYPE))))
                    .list()).stream().map(Quad.class::cast);
        }
        return Stream.empty();
    }

    /**
     * Fetch a stream of membership quads that are built with ldp:isMemberOfRelation
     * and either direct or indirect containment where the ldp:insertedContentRelation
     * is equal to ldp:MemberSubject.
     */
    private Stream<Quad> fetchDirectMemberQuadsInverse() {
        if (supportDirectContainment) {
            final String query
                = "SELECT r2.ldp_is_member_of_relation, r2.ldp_membership_resource "
                + "FROM resource AS r INNER JOIN resource AS r2 ON r.is_part_of = r2.subject "
                + "WHERE r.subject = ? AND r2.ldp_inserted_content_relation = ? "
                + "AND r2.ldp_is_member_of_relation IS NOT NULL";

            return jdbi.withHandle(handle -> handle.select(query,
                        getIdentifier().getIRIString(), LDP.MemberSubject.getIRIString())
                    .map((rs, ctx) -> rdf.createQuad(LDP.PreferMembership,
                            adjustIdentifier(getIdentifier(), getInteractionModel()),
                            rdf.createIRI(rs.getString(IS_MEMBER_OF_RELATION)),
                            rdf.createIRI(rs.getString(MEMBERSHIP_RESOURCE))))
                    .list()).stream().map(Quad.class::cast);
        }
        return Stream.empty();
    }

    /**
     * Fetch a stream of membership quads that are built with ldp:hasMemberRelation
     * and direct containment.
     */
    private Stream<Quad> fetchDirectMemberQuads() {
        if (supportDirectContainment) {
            final String query
                = "SELECT r.ldp_membership_resource, r.ldp_has_member_relation, r2.subject, r2.interaction_model "
                + "FROM resource AS r INNER JOIN resource AS r2 ON r.subject = r2.is_part_of "
                + "WHERE r.ldp_member = ? AND r.ldp_inserted_content_relation = ? "
                + "AND r.ldp_has_member_relation IS NOT NULL";

            return jdbi.withHandle(handle -> handle.select(query,
                        getIdentifier().getIRIString(), LDP.MemberSubject.getIRIString())
                    .map((rs, ctx) -> rdf.createQuad(LDP.PreferMembership,
                            rdf.createIRI(rs.getString(MEMBERSHIP_RESOURCE)),
                            rdf.createIRI(rs.getString(HAS_MEMBER_RELATION)),
                            rdf.createIRI(adjustIdentifier(rs.getString(SUBJECT), rs.getString(IXN_MODEL)))))
                    .list()).stream().map(Quad.class::cast);
        }
        return Stream.empty();
    }

    /**
     * Fetch a stream of containment quads for a resource.
     */
    private Stream<Quad> fetchContainmentQuads() {
        if (getInteractionModel().getIRIString().endsWith("Container")) {
            final String query = "SELECT subject, interaction_model FROM resource WHERE is_part_of = ?";
            return jdbi.withHandle(handle -> handle.select(query,
                        getIdentifier().getIRIString())
                    .map((rs, ctx) -> rdf.createQuad(LDP.PreferContainment,
                            adjustIdentifier(getIdentifier(), getInteractionModel()),
                            LDP.contains,
                            rdf.createIRI(adjustIdentifier(rs.getString(SUBJECT), rs.getString(IXN_MODEL)))))
                    .list()).stream().map(Quad.class::cast);
        }
        return Stream.empty();
    }

    private Set<IRI> fetchExtensionGraphNames() {
        final String query = "SELECT ext FROM extension WHERE resource_id = ?";
        final Map<String, IRI> rev = extensions.entrySet().stream()
            .collect(toMap(Map.Entry::getValue, Map.Entry::getKey));
        return jdbi.withHandle(handle -> handle.select(query, data.getId())
                .map((rs, ctx) -> rs.getString("ext")).list())
            .stream().filter(rev::containsKey).map(rev::get).collect(toSet());
    }

    private Stream<Quad> fetchExtensionQuads(final IRI graphName) {
        final String query = "SELECT data FROM extension WHERE resource_id = ? AND ext = ?";
        final Model model = createDefaultModel();
        jdbi.withHandle(handle -> handle.select(query, data.getId(), extensions.get(graphName))
                .map((rs, ctx) -> rs.getString("data")).findFirst())
            .ifPresent(triples -> RDFParser.fromString(triples).lang(NTRIPLES).parse(model));
        return fromJena(model.getGraph()).stream().map(triple -> rdf.createQuad(graphName, triple.getSubject(),
                    triple.getPredicate(), triple.getObject())).map(Quad.class::cast);
    }

    private Stream<Quad> fetchQuadsFromTable(final String tableName, final IRI graphName) {
        final String query = "SELECT subject, predicate, object, lang, datatype "
                           + "FROM " + tableName + " WHERE resource_id = ?";
        return jdbi.withHandle(handle -> handle.select(query, data.getId())
                .map((rs, ctx) -> rdf.createQuad(graphName, rdf.createIRI(rs.getString(SUBJECT)),
                        rdf.createIRI(rs.getString(PREDICATE)),
                        getObject(rs.getString(OBJECT), rs.getString(LANG), rs.getString(DATATYPE))))
                .list()).stream().map(Quad.class::cast);
    }

    /**
     * Fetch data for this resource.
     * @return true if data was found; false otherwise
     */
    private boolean fetchData() {
        LOGGER.debug("Fetching data for: {}", identifier);
        final String extraQuery = "SELECT predicate, object FROM extra WHERE resource_id = ?";
        final String query
            = "SELECT id, interaction_model, modified, is_part_of, deleted, acl, "
            + "ldp_membership_resource, ldp_has_member_relation, ldp_is_member_of_relation, "
            + "ldp_inserted_content_relation, binary_location, binary_modified, binary_format "
            + "FROM resource WHERE subject = ?";
        final Optional<ResourceData> rd = jdbi.withHandle(handle -> handle.select(query, identifier.getIRIString())
                .map((rs, ctx) -> new ResourceData(rs)).findFirst());
        if (rd.isPresent()) {
            this.data = rd.get();
            final Map<String, String> extras = new HashMap<>();
            jdbi.useHandle(handle ->
                    handle.select(extraQuery, this.data.getId())
                          .map((rs, ctx) -> new SimpleImmutableEntry<>(rs.getString(OBJECT), rs.getString(PREDICATE)))
                          .forEach(entry -> extras.put(entry.getKey(), entry.getValue())));

            this.data.setExtra(extras);
            return true;
        }
        return false;
    }

    static RDFTerm getObject(final String value, final String lang, final String datatype) {
        if (lang != null) {
            return rdf.createLiteral(value, lang);
        } else if (datatype != null) {
            return rdf.createLiteral(value, rdf.createIRI(datatype));
        }
        return rdf.createIRI(value);
    }

    static String adjustIdentifier(final String identifier, final String type) {
        if (containerTypes.contains(rdf.createIRI(type)) && !identifier.endsWith(SLASH)) {
            return identifier + SLASH;
        }
        return identifier;
    }

    static IRI adjustIdentifier(final IRI identifier, final IRI type) {
        if (containerTypes.contains(type) && !identifier.getIRIString().endsWith(SLASH)) {
            return rdf.createIRI(identifier.getIRIString() + SLASH);
        }
        return identifier;
    }
}
