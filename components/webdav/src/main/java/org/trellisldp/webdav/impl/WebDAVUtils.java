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
package org.trellisldp.webdav.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.replaceOnce;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.toDataset;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.core.PathSegment;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.slf4j.Logger;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.Session;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.SimpleEvent;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;

/**
 * Utility functions for the WebDAV API.
 */
public final class WebDAVUtils {

    private static final Logger LOGGER = getLogger(WebDAVUtils.class);
    private static final RDF rdf = RDFFactory.getInstance();

    /**
     * Recursively delete resources under the given identifier.
     * @param services the trellis services
     * @param session the session
     * @param identifier the identifier
     * @param baseUrl the baseURL
     */
    public static void recursiveDelete(final ServiceBundler services, final Session session, final IRI identifier,
            final String baseUrl) {
        final List<IRI> resources = services.getResourceService().get(identifier)
            .thenApply(res -> res.stream(LDP.PreferContainment).map(Quad::getObject).filter(IRI.class::isInstance)
                    .map(IRI.class::cast).collect(toList())).toCompletableFuture().join();
        resources.forEach(id -> recursiveDelete(services, session, id, baseUrl));
        resources.stream().parallel().map(id -> {
                final Dataset immutable = rdf.createDataset();
                services.getAuditService().creation(id, session).stream()
                    .map(skolemizeQuads(services.getResourceService(), baseUrl)).forEachOrdered(immutable::add);

                return services.getResourceService().delete(Metadata.builder(id).interactionModel(LDP.Resource)
                        .container(identifier).build())
                    .thenCompose(future -> services.getResourceService().add(id, immutable))
                    .whenComplete((a, b) -> closeDataset(immutable))
                    .thenAccept(future -> services.getEventService().emit(new SimpleEvent(externalUrl(id, baseUrl),
                                session.getAgent(), asList(PROV.Activity, AS.Delete), singletonList(LDP.Resource))));
            })
            .map(CompletionStage::toCompletableFuture).forEach(CompletableFuture::join);
    }

    /**
     * Close a dataset.
     * @param dataset the dataset
     */
    public static void closeDataset(final Dataset dataset) {
        try {
            dataset.close();
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error closing dataset", ex);
        }
    }

    /**
     * Recursively copy the resources under the provided identifier.
     * @param services the trellis services
     * @param session the session
     * @param identifier the source identifier
     * @param destination the destination identifier
     * @param baseUrl the baseURL
     * @return the next stage of completion
     */
    public static CompletionStage<Void> recursiveCopy(final ServiceBundler services, final Session session,
            final IRI identifier, final IRI destination, final String baseUrl) {
        return services.getResourceService().get(identifier)
            .thenCompose(res -> recursiveCopy(services, session, res, destination, baseUrl));
    }

    /**
     * Recursively copy the resources under the provided identifier.
     * @param services the trellis service
     * @param session the session
     * @param resource the resource
     * @param destination the destination identifier
     * @param baseUrl the baseURL
     * @return the next stage of completion
     */
    public static CompletionStage<Void> recursiveCopy(final ServiceBundler services, final Session session,
            final Resource resource, final IRI destination, final String baseUrl) {
        final List<IRI> resources = resource.stream(LDP.PreferContainment).map(Quad::getObject)
            .filter(IRI.class::isInstance).map(IRI.class::cast).collect(toList());
        resources.stream().parallel().map(id -> recursiveCopy(services, session, id,
                    mapDestination(id, resource.getIdentifier(), destination), baseUrl))
            .map(CompletionStage::toCompletableFuture).forEach(CompletableFuture::join);
        return copy(services, session, resource, destination, baseUrl);
    }

    /**
     * Copy the resources under the provided identifier.
     * @param services the trellis services
     * @param session the session
     * @param resource the source identifier
     * @param destination the destination identifier
     * @param baseUrl the baseURL
     * @return the next stage of completion
     */
    public static CompletionStage<Void> depth1Copy(final ServiceBundler services, final Session session,
            final Resource resource, final IRI destination, final String baseUrl) {
        final List<IRI> resources = resource.stream(LDP.PreferContainment).map(Quad::getObject)
            .filter(IRI.class::isInstance).map(IRI.class::cast).collect(toList());
        resources.stream().parallel()
            .map(id -> copy(services, session, id, mapDestination(id, resource.getIdentifier(), destination), baseUrl))
            .map(CompletionStage::toCompletableFuture).forEach(CompletableFuture::join);
        return copy(services, session, resource, destination, baseUrl);
    }

    /**
     * Copy a resource to another location.
     * @param services the trellis services
     * @param session the session
     * @param identifier the source identifier
     * @param destination the destination identifier
     * @param baseUrl the baseURL
     * @return the next stage of completion
     */
    public static CompletionStage<Void> copy(final ServiceBundler services, final Session session,
            final IRI identifier, final IRI destination, final String baseUrl) {
        return services.getResourceService().get(identifier)
            .thenCompose(res -> copy(services, session, res, destination, baseUrl));
    }

    /**
     * Copy a resource to another location.
     * @param services the trellis services
     * @param session the session
     * @param resource the resource
     * @param destination the destination identifier
     * @param baseUrl the baseURL
     * @return the next stage of completion
     */
    public static CompletionStage<Void> copy(final ServiceBundler services, final Session session,
            final Resource resource, final IRI destination, final String baseUrl) {

        final Metadata.Builder builder = Metadata.builder(destination)
            .interactionModel(resource.getInteractionModel());
        resource.getContainer().ifPresent(builder::container);
        resource.getBinaryMetadata().ifPresent(builder::binary);
        resource.getInsertedContentRelation().ifPresent(builder::insertedContentRelation);
        resource.getMemberOfRelation().ifPresent(builder::memberOfRelation);
        resource.getMemberRelation().ifPresent(builder::memberRelation);
        resource.getMembershipResource().ifPresent(builder::membershipResource);
        builder.metadataGraphNames(resource.getMetadataGraphNames());

        try (final Stream<Quad> stream = resource.stream(Trellis.PreferUserManaged)) {
            LOGGER.debug("Copying {} to {}", resource.getIdentifier(), destination);
            final Dataset mutable = stream.collect(toDataset());

            return services.getResourceService().create(builder.build(), mutable)
                .whenComplete((a, b) -> closeDataset(mutable))
                .thenCompose(future -> {
                        final Dataset immutable = rdf.createDataset();
                        services.getAuditService().creation(resource.getIdentifier(), session).stream()
                            .map(skolemizeQuads(services.getResourceService(), baseUrl)).forEachOrdered(immutable::add);

                        return services.getResourceService().add(resource.getIdentifier(), immutable)
                            .whenComplete((a, b) -> closeDataset(immutable));
                    })
                .thenCompose(future -> services.getMementoService().put(services.getResourceService(),
                            resource.getIdentifier()))
                .thenAccept(future -> services.getEventService().emit(new SimpleEvent(externalUrl(destination, baseUrl),
                            session.getAgent(), asList(PROV.Activity, AS.Create),
                            singletonList(resource.getInteractionModel()))));
        }
    }

    /**
     * Convert quads from an external form to a skolemized form.
     *
     * @param service the resource service
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Quad, Quad> skolemizeQuads(final ResourceService service, final String baseUrl) {
        return quad -> rdf.createQuad(quad.getGraphName().orElse(Trellis.PreferUserManaged),
                (BlankNodeOrIRI) service.toInternal(service.skolemize(quad.getSubject()), baseUrl), quad.getPredicate(),
                service.toInternal(service.skolemize(quad.getObject()), baseUrl));
    }

    /**
     * Get the last path segment.
     * @param segments the path segments
     * @return the path
     */
    public static String getLastSegment(final List<PathSegment> segments) {
        if (segments.isEmpty()) {
            return "";
        }
        return segments.get(segments.size() - 1).getPath();
    }

    /**
     * From a list of segments, use all but the last item, joined in a String.
     * @param segments the path segments
     * @return the path
     */
    public static String getAllButLastSegment(final List<PathSegment> segments) {
        if (segments.isEmpty()) {
            return "";
        }
        return segments.subList(0, segments.size() - 1).stream().map(PathSegment::getPath)
                    .collect(joining("/"));
    }

    /**
     * Generate an external URL for the given location and baseURL.
     *
     * @param identifier the resource identifier
     * @param baseUrl the baseURL
     * @return the external URL
     */
    public static String externalUrl(final IRI identifier, final String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return replaceOnce(identifier.getIRIString(), TRELLIS_DATA_PREFIX, baseUrl);
        }
        return replaceOnce(identifier.getIRIString(), TRELLIS_DATA_PREFIX, baseUrl + "/");
    }

    private static IRI mapDestination(final IRI child, final IRI parent, final IRI parentDestination) {
        final String childDestination = replaceOnce(child.getIRIString(), parent.getIRIString(),
                parentDestination.getIRIString());
        return rdf.createIRI(childDestination);
    }

    private WebDAVUtils() {
        // prevent instantiation
    }
}
