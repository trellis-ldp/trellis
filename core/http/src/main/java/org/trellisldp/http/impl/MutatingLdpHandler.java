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
package org.trellisldp.http.impl;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.TrellisUtils.toQuad;
import static org.trellisldp.http.impl.HttpUtils.skolemizeQuads;
import static org.trellisldp.http.impl.HttpUtils.skolemizeTriples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.api.Session;
import org.trellisldp.http.core.Digest;
import org.trellisldp.http.core.TrellisRequest;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.RDF;
import org.trellisldp.vocabulary.Trellis;

/**
 * A common base class for PUT/POST requests.
 *
 * @author acoburn
 */
class MutatingLdpHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(MutatingLdpHandler.class);

    private final Session session;

    private final InputStream entity;

    private Resource parent;

    /**
     * Create a base handler for a mutating LDP response.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    protected MutatingLdpHandler(final TrellisRequest req, final ServiceBundler trellis, final String baseUrl) {
        this(req, trellis, baseUrl, null);
    }

    /**
     * Create a base handler for a mutating LDP response.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     * @param entity the entity
     */
    protected MutatingLdpHandler(final TrellisRequest req, final ServiceBundler trellis,
            final String baseUrl, final InputStream entity) {
        super(req, trellis, baseUrl);
        this.entity = entity;
        this.session = ofNullable(req.getSecurityContext().getUserPrincipal()).map(Principal::getName)
            .map(getServices().getAgentService()::asAgent).map(HttpSession::new).orElseGet(HttpSession::new);
    }

    protected void setParent(final Resource parent) {
        this.parent = parent;
    }

    protected IRI getParentIdentifier() {
        return ofNullable(parent).map(Resource::getIdentifier).orElse(null);
    }

    protected IRI getParentModel() {
        return ofNullable(parent).map(Resource::getInteractionModel).orElse(null);
    }

    protected IRI getParentMembershipResource() {
        return ofNullable(parent).flatMap(Resource::getMembershipResource).map(IRI::getIRIString)
            .map(res -> res.split("#")[0]).map(rdf::createIRI).orElse(null);
    }

    /**
     * Update the memento resource.
     *
     * @param builder the Trellis response builder
     * @return a response builder promise
     */
    public CompletableFuture<ResponseBuilder> updateMemento(final ResponseBuilder builder) {
        return getServices().getResourceService().get(getInternalId())
            .thenCompose(getServices().getMementoService()::put)
            .exceptionally(ex -> {
                    LOGGER.warn("Unable to store memento for {}: {}", getInternalId(), ex.getMessage());
                    return null;
                })
            .thenApply(stage -> builder);
    }

    /**
     * Get the Trellis session for this interaction.
     * @return the session
     */
    protected Session getSession() {
        return session;
    }

    /**
     * Get the internal IRI for the resource.
     * @return the resource IRI
     */
    protected IRI getInternalId() {
        return ofNullable(getResource()).map(Resource::getIdentifier).orElse(null);
    }

    /**
     * Read an entity into the provided {@link TrellisDataset}.
     * @param graphName the target graph
     * @param syntax the entity syntax
     * @param dataset the dataset
     */
    protected void readEntityIntoDataset(final IRI graphName, final RDFSyntax syntax,
            final TrellisDataset dataset) {
        try (final InputStream input = entity) {
            getServices().getIOService().read(input, syntax, getIdentifier())
                .map(skolemizeTriples(getServices().getResourceService(), getBaseUrl()))
                .filter(triple -> !RDF.type.equals(triple.getPredicate())
                        || !triple.getObject().ntriplesString().startsWith("<" + LDP.getNamespace()))
                .filter(triple -> !LDP.contains.equals(triple.getPredicate()))
                .map(toQuad(graphName)).forEachOrdered(dataset::add);
        } catch (final RuntimeTrellisException ex) {
            throw new BadRequestException("Invalid RDF content: " + ex.getMessage());
        } catch (final IOException ex) {
            throw new WebApplicationException("Error processing input: " + ex.getMessage());
        }
    }

    /**
     * Emit events for the change.
     * @param identifier the resource identifier
     * @param activityType the activity type
     * @param resourceType the resource type
     * @return the next completion stage
     */
    protected CompletableFuture<Void> emitEvent(final IRI identifier, final IRI activityType, final IRI resourceType) {
        // Always notify about updates for the resource in question
        getServices().getEventService().emit(new SimpleEvent(getUrl(identifier), getSession().getAgent(),
                    asList(PROV.Activity, activityType), asList(resourceType)));
        // If this was an update and the parent is an ldp:IndirectContainer,
        // notify about the member resource (if it exists)
        if (AS.Update.equals(activityType) && LDP.IndirectContainer.equals(getParentModel())) {
            return emitMembershipUpdateEvent();
        // If this was a creation or deletion, and the parent is some form of container,
        // notify about the parent resource, too
        } else if (AS.Create.equals(activityType) || AS.Delete.equals(activityType)) {
            final IRI model = getParentModel();
            final IRI id = getParentIdentifier();
            if (HttpUtils.isContainer(model)) {
                getServices().getEventService().emit(new SimpleEvent(getUrl(id),
                                getSession().getAgent(), asList(PROV.Activity, AS.Update), asList(model)));
                // If the parent's membership resource is different than the parent itself,
                // notify about that membership resource, too (if it exists)
                if (!Objects.equals(id, getParentMembershipResource())) {
                    return allOf(getServices().getResourceService().touch(id), emitMembershipUpdateEvent());
                }
                return getServices().getResourceService().touch(id);
            }
        }
        return completedFuture(null);
    }

    /**
     * Check the constraints of a graph.
     * @param graph the graph
     * @param type the LDP interaction model
     * @param syntax the output syntax
     */
    protected void checkConstraint(final Graph graph, final IRI type, final RDFSyntax syntax) {
        ofNullable(graph).map(g ->
                constraintServices.stream().parallel().flatMap(svc -> svc.constrainedBy(type, g)).collect(toList()))
            .filter(violations -> !violations.isEmpty())
            .map(violations -> {
                final ResponseBuilder err = status(CONFLICT);
                violations.forEach(v -> err.link(v.getConstraint().getIRIString(), LDP.constrainedBy.getIRIString()));
                final StreamingOutput stream = new StreamingOutput() {
                    @Override
                    public void write(final OutputStream out) throws IOException {
                        getServices().getIOService().write(violations.stream().flatMap(v2 -> v2.getTriples().stream()),
                                out, syntax);
                    }
                };
                return err.entity(stream);
            }).ifPresent(err -> {
                throw new ClientErrorException(err.build());
            });
    }

    protected CompletableFuture<Void> persistContent(final BinaryMetadata metadata) {
        return getServices().getBinaryService().setContent(metadata, entity)
                        .whenComplete(HttpUtils.closeInputStreamAsync(entity));
    }

    protected CompletableFuture<Void> persistContent(final BinaryMetadata metadata, final Digest digest) {
        if (isNull(digest)) {
            return persistContent(metadata);
        }
        return getServices().getBinaryService().setContent(metadata, entity, digest.getAlgorithm().toUpperCase())
            .thenCompose(serverComputed -> {
                if (isNull(serverComputed)) {
                    throw new BadRequestException("Unsupported algorithm: " + digest.getAlgorithm());
                } else if (digest.getDigest().equals(serverComputed)) {
                    LOGGER.debug("Successfully persisted digest-verified bitstream: {}", metadata.getIdentifier());
                    return completedFuture(null);
                }
                return getServices().getBinaryService().purgeContent(metadata.getIdentifier()).thenAccept(future -> {
                    throw new BadRequestException("Supplied digest value does not match the server-computed digest: "
                            + serverComputed);
                });
            });
    }

    protected Metadata.Builder metadataBuilder(final IRI identifier, final IRI ixnModel, final TrellisDataset mutable) {
        final Metadata.Builder builder = Metadata.builder(identifier).interactionModel(ixnModel);
        mutable.asDataset().getGraph(Trellis.PreferUserManaged).ifPresent(graph -> {
            graph.stream(identifier, LDP.membershipResource, null).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::membershipResource);
            graph.stream(identifier, LDP.hasMemberRelation, null).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::memberRelation);
            graph.stream(identifier, LDP.isMemberOfRelation, null).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::memberOfRelation);
            graph.stream(identifier, LDP.insertedContentRelation, null).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::insertedContentRelation);
        });
        return builder;
    }

    protected CompletableFuture<Void> handleResourceReplacement(final TrellisDataset mutable,
            final TrellisDataset immutable) {
        final Metadata.Builder metadata = metadataBuilder(getResource().getIdentifier(),
                getResource().getInteractionModel(), mutable);
        getResource().getContainer().ifPresent(metadata::container);
        getResource().getBinaryMetadata().ifPresent(metadata::binary);
        // update the resource
        return allOf(
            getServices().getResourceService().replace(metadata.build(), mutable.asDataset()),
            getServices().getResourceService().add(getResource().getIdentifier(), immutable.asDataset()));
    }

    protected Stream<Quad> getAuditUpdateData() {
        return getServices().getAuditService().update(getResource().getIdentifier(), getSession()).stream()
            .map(skolemizeQuads(getServices().getResourceService(), getBaseUrl()));
    }

    /*
     * Emit update events for the membership resource, if it exists.
     */
    private CompletableFuture<Void> emitMembershipUpdateEvent() {
        final IRI membershipResource = getParentMembershipResource();
        if (nonNull(membershipResource)) {
            return allOf(getServices().getResourceService().touch(membershipResource),
                getServices().getResourceService().get(membershipResource).thenAccept(res -> {
                    if (nonNull(res.getIdentifier())) {
                        getServices().getEventService().emit(new SimpleEvent(getUrl(res.getIdentifier()),
                                    getSession().getAgent(), asList(PROV.Activity, AS.Update),
                                    asList(res.getInteractionModel())));
                    }
                }));
        }
        return completedFuture(null);
    }

    /*
     * Convert an internal identifier to an external identifier, suitable for notifications.
     */
    private String getUrl(final IRI identifier) {
        return getServices().getResourceService().toExternal(identifier, getBaseUrl()).getIRIString();
    }
}
