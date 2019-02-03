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
package org.trellisldp.webdav.impl;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.replaceOnce;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.api.TrellisUtils.toDataset;
import static org.trellisldp.api.TrellisUtils.toQuad;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.ws.rs.core.PathSegment;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Utility functions for the WebDAV API.
 */
public final class WebDAVUtils {

    private static final Logger LOGGER = getLogger(WebDAVUtils.class);
    private static final RDF rdf = getInstance();

    /**
     * Recursively delete resources under the given identifier.
     * @param service the resource service
     * @param identifier the identifier
     */
    public static void recursiveDelete(final ResourceService service, final IRI identifier) {
        final List<IRI> resources = service.get(identifier)
            .thenApply(res -> res.stream(LDP.PreferContainment).map(Triple::getObject).filter(IRI.class::isInstance)
                    .map(IRI.class::cast).collect(toList())).toCompletableFuture().join();
        resources.forEach(id -> recursiveDelete(service, id));
        resources.stream().parallel().map(id -> service.delete(Metadata.builder(id)
                    .interactionModel(LDP.Resource).container(identifier).build()))
            .map(CompletionStage::toCompletableFuture).forEach(CompletableFuture::join);
    }

    /**
     * Recursively copy the resources under the provided identifier.
     * @param service the resource service
     * @param identifier the source identifier
     * @param destination the destination identifier
     * @return the next stage of completion
     */
    public static CompletionStage<Void> recursiveCopy(final ResourceService service, final IRI identifier,
            final IRI destination) {
        return service.get(identifier).thenCompose(res -> recursiveCopy(service, res, destination));
    }

    /**
     * Recursively copy the resources under the provided identifier.
     * @param service the resource service
     * @param resource the resource
     * @param destination the destination identifier
     * @return the next stage of completion
     */
    public static CompletionStage<Void> recursiveCopy(final ResourceService service, final Resource resource,
            final IRI destination) {
        final List<IRI> resources = resource.stream(LDP.PreferContainment).map(Triple::getObject)
            .filter(IRI.class::isInstance).map(IRI.class::cast).collect(toList());
        resources.stream().parallel()
            .map(id -> recursiveCopy(service, id, mapDestination(id, resource.getIdentifier(), destination)))
            .map(CompletionStage::toCompletableFuture).forEach(CompletableFuture::join);
        return copy(service, resource, destination);
    }

    /**
     * Copy the resources under the provided identifier.
     * @param service the resource service
     * @param identifier the source identifier
     * @param destination the destination identifier
     * @return the next stage of completion
     */
    public static CompletionStage<Void> depth1Copy(final ResourceService service, final IRI identifier,
            final IRI destination) {
        return service.get(identifier).thenCompose(res -> depth1Copy(service, res, destination));
    }

    /**
     * Copy the resources under the provided identifier.
     * @param service the resource service
     * @param resource the source identifier
     * @param destination the destination identifier
     * @return the next stage of completion
     */
    public static CompletionStage<Void> depth1Copy(final ResourceService service, final Resource resource,
            final IRI destination) {
        final List<IRI> resources = resource.stream(LDP.PreferContainment).map(Triple::getObject)
            .filter(IRI.class::isInstance).map(IRI.class::cast).collect(toList());
        resources.stream().parallel()
            .map(id -> copy(service, id, mapDestination(id, resource.getIdentifier(), destination)))
            .map(CompletionStage::toCompletableFuture).forEach(CompletableFuture::join);
        return copy(service, resource, destination);
    }

    /**
     * Copy a resource to another location.
     * @param service the resource service
     * @param identifier the source identifier
     * @param destination the destination identifier
     * @return the next stage of completion
     */
    public static CompletionStage<Void> copy(final ResourceService service, final IRI identifier,
            final IRI destination) {
        return service.get(identifier).thenCompose(res -> copy(service, res, destination));
    }

    /**
     * Copy a resource to another location.
     * @param service the resource service
     * @param resource the resource
     * @param destination the destination identifier
     * @return the next stage of completion
     */
    public static CompletionStage<Void> copy(final ResourceService service, final Resource resource,
            final IRI destination) {

        final Metadata.Builder builder = Metadata.builder(destination)
            .interactionModel(resource.getInteractionModel());
        resource.getContainer().ifPresent(builder::container);
        resource.getBinaryMetadata().ifPresent(builder::binary);
        resource.getInsertedContentRelation().ifPresent(builder::insertedContentRelation);
        resource.getMemberOfRelation().ifPresent(builder::memberOfRelation);
        resource.getMemberRelation().ifPresent(builder::memberRelation);
        resource.getMembershipResource().ifPresent(builder::membershipResource);

        try (final Stream<Triple> stream = resource.stream(Trellis.PreferUserManaged)) {
            LOGGER.debug("Copying {} to {}", resource.getIdentifier(), destination);
            final TrellisDataset dataset = new TrellisDataset(stream.map(toQuad(Trellis.PreferUserManaged))
                    .collect(toDataset()));
            return service.create(builder.build(), dataset.asDataset())
                .whenComplete((a, b) -> dataset.close());
        }
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

    private static IRI mapDestination(final IRI child, final IRI parent, final IRI parentDestination) {
        final String childDestination = replaceOnce(child.getIRIString(), parent.getIRIString(),
                parentDestination.getIRIString());
        return rdf.createIRI(childDestination);
    }

    private WebDAVUtils() {
        // prevent instantiation
    }
}
