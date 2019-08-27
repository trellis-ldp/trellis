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
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.http.core.HttpConstants.ACL_QUERY_PARAM;
import static org.trellisldp.http.impl.HttpUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.HttpUtils.skolemizeQuads;
import static org.trellisldp.http.impl.HttpUtils.skolemizeTriples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

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
        if (req.getPrincipalName() != null) {
            this.session = new HttpSession(getServices().getAgentService().asAgent(req.getPrincipalName()));
        } else {
            this.session = new HttpSession();
        }
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
        return getServices().getMementoService().put(getServices().getResourceService(), getInternalId())
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
        return getResource().getIdentifier();
    }

    /**
     * Read an entity into the provided {@link Dataset}.
     * @param graphName the target graph
     * @param syntax the entity syntax
     * @param dataset the dataset
     */
    protected void readEntityIntoDataset(final IRI graphName, final RDFSyntax syntax, final Dataset dataset) {
        try (final InputStream input = entity) {
            getServices().getIOService().read(input, syntax, getIdentifier())
                .map(skolemizeTriples(getServices().getResourceService(), getBaseUrl()))
                .filter(triple -> !RDF.type.equals(triple.getPredicate())
                        || !triple.getObject().ntriplesString().startsWith("<" + LDP.getNamespace()))
                .filter(triple -> !LDP.contains.equals(triple.getPredicate())).map(triple ->
                        rdf.createQuad(graphName, triple.getSubject(), triple.getPredicate(), triple.getObject()))
                .forEachOrdered(dataset::add);
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
    protected CompletionStage<Void> emitEvent(final IRI identifier, final IRI activityType, final IRI resourceType) {
        // Always notify about updates for the resource in question
        getServices().getEventService().emit(new SimpleEvent(getUrl(identifier), getSession().getAgent(),
                    asList(PROV.Activity, activityType), ldpResourceTypes(resourceType).collect(toList())));

        // Further notifications are only relevant for non-ACL resources
        if (!isAclRequest()) {
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
                                    getSession().getAgent(), asList(PROV.Activity, AS.Update),
                                    ldpResourceTypes(model).collect(toList())));
                    // If the parent's membership resource is different than the parent itself,
                    // notify about that membership resource, too (if it exists)
                    if (!parent.getMembershipResource().map(MutatingLdpHandler::removeHashFragment).filter(isEqual(id))
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

    private static IRI removeHashFragment(final IRI iri) {
        if (iri.getIRIString().contains("#")) {
            return rdf.createIRI(iri.getIRIString().split("#")[0]);
        }
        return iri;
    }

    /**
     * Check the constraints of a graph.
     * @param graph the graph
     * @param type the LDP interaction model
     * @param syntax the output syntax
     */
    protected void checkConstraint(final Optional<Graph> graph, final IRI type, final RDFSyntax syntax) {
        graph.ifPresent(g -> {
            final List<ConstraintViolation> violations = new ArrayList<>();
            getServices().getConstraintServices().forEach(svc -> svc.constrainedBy(type, g).forEach(violations::add));
            if (!violations.isEmpty()) {
                final ResponseBuilder err = status(CONFLICT);
                violations.forEach(v -> err.link(v.getConstraint().getIRIString(), LDP.constrainedBy.getIRIString()));
                final StreamingOutput stream = new StreamingOutput() {
                    @Override
                    public void write(final OutputStream out) throws IOException {
                        getServices().getIOService().write(violations.stream().flatMap(v2 -> v2.getTriples().stream()),
                                out, syntax);
                    }
                };
                throw new ClientErrorException(err.entity(stream).build());
            }
        });
    }

    protected CompletionStage<Void> persistContent(final BinaryMetadata metadata) {
        return getServices().getBinaryService().setContent(metadata, entity)
                        .whenComplete(HttpUtils.closeInputStreamAsync(entity));
    }

    protected Metadata.Builder metadataBuilder(final IRI identifier, final IRI ixnModel, final Dataset mutable) {
        final Metadata.Builder builder = Metadata.builder(identifier).interactionModel(ixnModel);
        mutable.getGraph(Trellis.PreferUserManaged).ifPresent(graph -> {
            graph.stream(identifier, LDP.membershipResource, null).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::membershipResource);
            graph.stream(identifier, LDP.hasMemberRelation, null).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::memberRelation);
            graph.stream(identifier, LDP.isMemberOfRelation, null).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::memberOfRelation);
            graph.stream(identifier, LDP.insertedContentRelation, null).findFirst().map(Triple::getObject)
                .filter(IRI.class::isInstance).map(IRI.class::cast).ifPresent(builder::insertedContentRelation);
            mutable.getGraph(Trellis.PreferAccessControl)
                .map(Graph::size)
                .map(s -> s > 0)
                .ifPresent(builder::hasAcl);
        });
        return builder;
    }

    protected CompletionStage<Void> handleResourceReplacement(final Dataset mutable, final Dataset immutable) {
        final Metadata.Builder metadata = metadataBuilder(getResource().getIdentifier(),
                getResource().getInteractionModel(), mutable);
        getResource().getContainer().ifPresent(metadata::container);
        getResource().getBinaryMetadata().ifPresent(metadata::binary);
        // update the resource
        return allOf(
            getServices().getResourceService().replace(metadata.build(), mutable).toCompletableFuture(),
            getServices().getResourceService().add(getResource().getIdentifier(),
                immutable).toCompletableFuture());
    }

    protected Stream<Quad> getAuditUpdateData() {
        return getServices().getAuditService().update(getResource().getIdentifier(), getSession()).stream()
            .map(skolemizeQuads(getServices().getResourceService(), getBaseUrl()));
    }

    /*
     * Emit update events for the membership resource, if it exists.
     */
    private CompletionStage<Void> emitMembershipUpdateEvent() {
        final IRI membershipResource = parent.getMembershipResource().map(MutatingLdpHandler::removeHashFragment)
            .orElse(null);
        if (membershipResource != null) {
            return allOf(getServices().getResourceService().touch(membershipResource).toCompletableFuture(),
                getServices().getResourceService().get(membershipResource).thenAccept(res -> {
                    if (res.getIdentifier() != null) {
                        getServices().getEventService().emit(new SimpleEvent(getUrl(res.getIdentifier()),
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
    private String getUrl(final IRI identifier) {
        final String url = getServices().getResourceService().toExternal(identifier, getBaseUrl()).getIRIString();
        return isAclRequest() ? url + ACL_QUERY_PARAM : url;
    }
}
