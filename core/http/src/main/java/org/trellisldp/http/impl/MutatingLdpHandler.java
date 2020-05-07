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
package org.trellisldp.http.impl;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_VERSIONING;
import static org.trellisldp.http.impl.HttpUtils.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.ConstraintViolation;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.Session;
import org.trellisldp.api.TrellisUtils;
import org.trellisldp.http.core.HttpSession;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.SimpleEvent;
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
    private static final Set<IRI> IGNORE = buildIgnoredGraphNames();

    private final Session session;
    private final InputStream entity;
    private final boolean versioningEnabled;

    private Resource parent;

    /**
     * Create a base handler for a mutating LDP response.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param extensions the extension mapping
     * @param baseUrl the base URL
     */
    protected MutatingLdpHandler(final TrellisRequest req, final ServiceBundler trellis,
            final Map<String, IRI> extensions, final String baseUrl) {
        this(req, trellis, extensions, baseUrl, null);
    }

    /**
     * Create a base handler for a mutating LDP response.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param extensions the extension mapping
     * @param baseUrl the base URL
     * @param entity the entity
     */
    protected MutatingLdpHandler(final TrellisRequest req, final ServiceBundler trellis,
            final Map<String, IRI> extensions, final String baseUrl, final InputStream entity) {
        super(req, trellis, extensions, baseUrl);
        this.entity = entity;
        this.session = HttpSession.from(req.getSecurityContext());
        this.versioningEnabled = getConfig().getOptionalValue(CONFIG_HTTP_VERSIONING, Boolean.class)
            .orElse(Boolean.TRUE);
    }

    protected void setParent(final Resource parent) {
        this.parent = parent;
    }

    protected IRI getParentIdentifier() {
        if (parent != null) {
            return parent.getIdentifier();
        }
        return null;
    }

    protected IRI getParentModel() {
        if (parent != null) {
            return parent.getInteractionModel();
        }
        return null;
    }

    /**
     * Update the memento resource.
     *
     * @param builder the Trellis response builder
     * @return a response builder promise
     */
    public CompletionStage<ResponseBuilder> updateMemento(final ResponseBuilder builder) {
        if (versioningEnabled) {
            return getServices().getMementoService().put(getServices().getResourceService(), getInternalId())
                .exceptionally(ex -> {
                        LOGGER.warn("Unable to store memento for {}: {}", getInternalId(), ex.getMessage());
                        return null;
                    })
                .thenApply(stage -> builder);
        }
        return completedFuture(builder);
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
        return getResource().getIdentifier();
    }

    /**
     * Read an entity into the provided {@link Dataset}.
     * @param graphName the target graph
     * @param syntax the entity syntax
     * @param dataset the dataset
     */
    protected void readEntityIntoDataset(final IRI graphName, final RDFSyntax syntax, final Dataset dataset) {
        try (final InputStream in = new ByteArrayInputStream(IOUtils.toByteArray(entity))) {
            getServices().getIOService().read(in, syntax, getIdentifier())
                .map(skolemizeTriples(getServices().getResourceService(), getBaseUrl()))
                .filter(triple -> !RDF.type.equals(triple.getPredicate())
                        || !triple.getObject().ntriplesString().startsWith("<" + LDP.getNamespace()))
                .filter(triple -> !LDP.contains.equals(triple.getPredicate())).map(triple ->
                        rdf.createQuad(graphName, triple.getSubject(), triple.getPredicate(), triple.getObject()))
                .forEachOrdered(dataset::add);
        } catch (final IOException ex) {
            throw new BadRequestException("Error handling input stream: " + ex.getMessage(), ex);
        } catch (final RuntimeTrellisException ex) {
            throw new BadRequestException("Invalid RDF content: " + ex.getMessage(), ex);
        }
    }

    /**
     * Emit events for the change.
     * @param identifier the resource identifier
     * @param activityType the activity type
     * @param resourceType the resource type
     * @return the next completion stage
     */
    protected CompletionStage<Void> emitEvent(final IRI identifier, final IRI activityType, final IRI resourceType) {
        // Always notify about updates for the resource in question
        getServices().getEventService().emit(new SimpleEvent(getUrl(identifier, resourceType), getSession().getAgent(),
                    asList(PROV.Activity, activityType), ldpResourceTypes(resourceType).collect(toList())));

        // Further notifications are only relevant for non-extension resources
        if (getExtensionGraphName() == null) {
            // If this was an update and the parent is an ldp:IndirectContainer,
            // notify about the member resource (if it exists)
            if (AS.Update.equals(activityType) && LDP.IndirectContainer.equals(getParentModel())) {
                return emitMembershipUpdateEvent();
            // If this was a creation or deletion, and the parent is some form of container,
            // notify about the parent resource, too
            } else if (AS.Create.equals(activityType) || AS.Delete.equals(activityType)) {
                final IRI model = getParentModel();
                final IRI id = getParentIdentifier();
                if (isContainer(model)) {
                    getServices().getEventService().emit(new SimpleEvent(getUrl(id, model),
                                    getSession().getAgent(), asList(PROV.Activity, AS.Update),
                                    ldpResourceTypes(model).collect(toList())));
                    // If the parent's membership resource is different than the parent itself,
                    // notify about that membership resource, too (if it exists)
                    if (!parent.getMembershipResource().map(TrellisUtils::normalizeIdentifier).filter(isEqual(id))
                            .isPresent()) {
                        return allOf(getServices().getResourceService().touch(id).toCompletableFuture(),
                                emitMembershipUpdateEvent().toCompletableFuture());
                    }
                    return getServices().getResourceService().touch(id);
                }
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
        final List<ConstraintViolation> violations = new ArrayList<>();
        getServices().getConstraintServices().forEach(svc -> svc.constrainedBy(getInternalId(), type, graph)
                .forEach(violations::add));
        if (!violations.isEmpty()) {
            final ResponseBuilder err = status(CONFLICT);
            violations.forEach(v -> err.link(v.getConstraint().getIRIString(), LDP.constrainedBy.getIRIString()));
            throw new ClientErrorException(err.entity((StreamingOutput) out ->
                    getServices().getIOService().write(violations.stream().flatMap(v2 -> v2.getTriples().stream()),
                            out, syntax, getIdentifier())).type(syntax.mediaType()).build());
        }
    }

    protected CompletionStage<Void> persistContent(final BinaryMetadata metadata) {
        return getServices().getBinaryService().setContent(metadata, entity);
    }

    protected Metadata.Builder metadataBuilder(final IRI identifier, final IRI ixnModel, final Dataset mutable) {
        final Metadata.Builder builder = Metadata.builder(identifier).interactionModel(ixnModel);
        mutable.getGraph(Trellis.PreferUserManaged).ifPresent(graph -> {
            graph.stream(null, LDP.membershipResource, null)
                .filter(triple -> matchIdentifier(triple.getSubject(), identifier)).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::membershipResource);
            graph.stream(null, LDP.hasMemberRelation, null)
                .filter(triple -> matchIdentifier(triple.getSubject(), identifier)).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::memberRelation);
            graph.stream(null, LDP.isMemberOfRelation, null)
                .filter(triple -> matchIdentifier(triple.getSubject(), identifier)).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::memberOfRelation);
            graph.stream(null, LDP.insertedContentRelation, null)
                .filter(triple -> matchIdentifier(triple.getSubject(), identifier)).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::insertedContentRelation);
        });
        builder.metadataGraphNames(mutable.getGraphNames().filter(IRI.class::isInstance).map(IRI.class::cast)
                .filter(name -> !IGNORE.contains(name)).collect(toSet()));
        return builder;
    }

    protected CompletionStage<Void> createOrReplace(final Metadata metadata, final Dataset mutable,
            final Dataset immutable) {
        if (getResource() == null) {
            LOGGER.debug("Creating new resource {}", metadata.getIdentifier());
            return handleResourceCreation(metadata, mutable, immutable);
        } else {
            LOGGER.debug("Replacing old resource {}", metadata.getIdentifier());
            return handleResourceReplacement(metadata, mutable, immutable);
        }
    }

    protected CompletionStage<Void> handleResourceCreation(final Metadata metadata, final Dataset mutable,
            final Dataset immutable) {
        return allOf(
            getServices().getResourceService().create(metadata, mutable).toCompletableFuture(),
            getServices().getResourceService().add(metadata.getIdentifier(), immutable).toCompletableFuture());
    }

    protected CompletionStage<Void> handleResourceReplacement(final Metadata metadata, final Dataset mutable,
            final Dataset immutable) {
        return allOf(
            getServices().getResourceService().replace(metadata, mutable).toCompletableFuture(),
            getServices().getResourceService().add(metadata.getIdentifier(), immutable).toCompletableFuture());
    }

    private List<Quad> auditQuads() {
        if (getResource() != null) {
            return getServices().getAuditService().update(getInternalId(), getSession());
        }
        return getServices().getAuditService().creation(getInternalId(), getSession());
    }

    protected Stream<Quad> getAuditQuadData() {
        return auditQuads().stream()
                .map(skolemizeQuads(getServices().getResourceService(), getBaseUrl()));
    }

    /*
     * Emit update events for the membership resource, if it exists.
     */
    private CompletionStage<Void> emitMembershipUpdateEvent() {
        final IRI membershipResource = parent.getMembershipResource().map(TrellisUtils::normalizeIdentifier)
            .orElse(null);
        if (membershipResource != null) {
            return allOf(getServices().getResourceService().touch(membershipResource).toCompletableFuture(),
                getServices().getResourceService().get(membershipResource).thenAccept(res -> {
                    if (res.getIdentifier() != null) {
                        getServices().getEventService()
                            .emit(new SimpleEvent(getUrl(res.getIdentifier(), res.getInteractionModel()),
                                    getSession().getAgent(), asList(PROV.Activity, AS.Update),
                                    ldpResourceTypes(res.getInteractionModel()).collect(toList())));
                    }
                }).toCompletableFuture());
        }
        return completedFuture(null);
    }

    /*
     * Convert an internal identifier to an external identifier, suitable for notifications.
     */
    private String getUrl(final IRI identifier, final IRI interactionModel) {
        final String url = getServices().getResourceService().toExternal(identifier, getBaseUrl()).getIRIString();
        final String modifiedUrl = url + (!url.endsWith("/") && isContainer(interactionModel) ? "/" : "");
        final String ext = getRequest().getExt();
        return ext != null ? modifiedUrl + "?ext=" + ext : modifiedUrl;
    }
}
