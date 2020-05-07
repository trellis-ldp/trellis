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
package org.trellisldp.webdav;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.jena.util.SplitIRI.localnameXML;
import static org.apache.jena.util.SplitIRI.namespaceXML;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getContainer;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_BASE_URL;
import static org.trellisldp.http.core.TrellisExtensions.buildExtensionMapFromConfig;
import static org.trellisldp.webdav.Depth.DEPTH.INFINITY;
import static org.trellisldp.webdav.Depth.DEPTH.ONE;
import static org.trellisldp.webdav.Depth.DEPTH.ZERO;
import static org.trellisldp.webdav.impl.WebDAVUtils.closeDataset;
import static org.trellisldp.webdav.impl.WebDAVUtils.copy;
import static org.trellisldp.webdav.impl.WebDAVUtils.depth1Copy;
import static org.trellisldp.webdav.impl.WebDAVUtils.externalUrl;
import static org.trellisldp.webdav.impl.WebDAVUtils.recursiveCopy;
import static org.trellisldp.webdav.impl.WebDAVUtils.recursiveDelete;
import static org.trellisldp.webdav.impl.WebDAVUtils.skolemizeQuads;
import static org.trellisldp.webdav.xml.DavUtils.DAV_NAMESPACE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.Session;
import org.trellisldp.http.core.HttpSession;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.SimpleEvent;
import org.trellisldp.http.core.TrellisRequest;
import org.trellisldp.vocabulary.AS;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.webdav.xml.DavMultiStatus;
import org.trellisldp.webdav.xml.DavProp;
import org.trellisldp.webdav.xml.DavPropFind;
import org.trellisldp.webdav.xml.DavPropStat;
import org.trellisldp.webdav.xml.DavPropertyUpdate;
import org.trellisldp.webdav.xml.DavRemove;
import org.trellisldp.webdav.xml.DavResponse;
import org.trellisldp.webdav.xml.DavSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Implements WebDAV HTTP methods.
 */
@ApplicationScoped
@Path("{path: .*}")
public class TrellisWebDAV {

    private static final String SUCCESS = "HTTP/1.1 200 OK";
    private static final int MULTI_STATUS = 207;
    private static final RDF rdf = RDFFactory.getInstance();
    private static final Logger LOGGER = getLogger(TrellisWebDAV.class);

    private final ServiceBundler services;
    private final Map<String, IRI> extensions;
    private final String userBaseUrl;

    /**
     * Create a Trellis HTTP resource matcher.
     *
     * @param services the Trellis application bundle
     */
    @Inject
    public TrellisWebDAV(final ServiceBundler services) {
        this(services, getConfig());
    }

    /**
     * For use with RESTeasy and CDI proxies.
     *
     * @apiNote This construtor is used by CDI runtimes that require a public, no-argument constructor.
     *          It should not be invoked directly in user code.
     */
    public TrellisWebDAV() {
        this(null);
    }

    private TrellisWebDAV(final ServiceBundler services, final Config config) {
        this(services, buildExtensionMapFromConfig(config), config.getOptionalValue(CONFIG_HTTP_BASE_URL,
                    String.class).orElse(null));
    }

    /**
     * Create a Trellis HTTP resource matcher.
     *
     * @param services the Trellis application bundle
     * @param extensions the extension graphs
     * @param baseUrl the base URL
     */
    public TrellisWebDAV(final ServiceBundler services, final Map<String, IRI> extensions, final String baseUrl) {
        this.extensions = requireNonNull(extensions, "Extensions may not be null!");
        this.services = services;
        this.userBaseUrl = baseUrl;
    }

    /**
     * Copy a resource.
     * @param response the async response
     * @param request the request
     * @param uriInfo the URI info
     * @param headers the headers
     * @param security the security context
     */
    @COPY
    @Timed
    public void copyResource(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers,
            @Context final SecurityContext security) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, security);
        final Session session = HttpSession.from(security);
        final IRI destination = getDestination(headers, getBaseUrl(req));
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        // Default is recursive copy as per RFC-4918
        final Depth.DEPTH depth = getDepth(headers.getHeaderString("Depth"));
        getParent(destination).thenCombine(services.getResourceService().get(destination), this::checkResources)
            .thenCompose(parent -> services.getResourceService().touch(parent.getIdentifier()))
            .thenCompose(future -> services.getResourceService().get(identifier))
            .thenApply(this::checkResource)
            .thenCompose(res -> copyTo(res, session, depth, destination, getBaseUrl(req)))
            .thenApply(future -> status(NO_CONTENT).build())
            .exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Move a resource.
     * @param response the async response
     * @param request the request
     * @param uriInfo the URI info
     * @param headers the headers
     * @param security the security context
     */
    @MOVE
    @Timed
    public void moveResource(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers,
            @Context final SecurityContext security) {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, security);
        final String baseUrl = getBaseUrl(req);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final IRI destination = getDestination(headers, baseUrl);
        final Session session = HttpSession.from(security);

        getParent(destination)
            .thenCombine(services.getResourceService().get(destination), this::checkResources)
            .thenCompose(parent -> services.getResourceService().touch(parent.getIdentifier()))
            .thenCompose(future -> services.getResourceService().get(identifier))
            .thenApply(this::checkResource)
            // Note: all MOVE operations are recursive (Depth: infinity), hence recursiveCopy
            .thenAccept(res -> recursiveCopy(services, session, res, destination, baseUrl))
            .thenAccept(future -> recursiveDelete(services, session, identifier, baseUrl))
            .thenCompose(future -> services.getResourceService().delete(Metadata.builder(identifier)
                    .interactionModel(LDP.Resource).build()))
            .thenCompose(future -> {
                final Dataset immutable = rdf.createDataset();
                services.getAuditService().creation(identifier, session).stream()
                    .map(skolemizeQuads(services.getResourceService(), baseUrl)).forEachOrdered(immutable::add);
                return services.getResourceService().add(identifier, immutable)
                    .whenComplete((a, b) -> closeDataset(immutable));
            })
            .thenAccept(future -> services.getEventService().emit(new SimpleEvent(externalUrl(identifier,
                baseUrl), session.getAgent(), asList(PROV.Activity, AS.Delete), singletonList(LDP.Resource))))
            .thenApply(future -> status(NO_CONTENT).build())
            .exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Get properties for a resource.
     * @param response the response
     * @param request the request
     * @param uriInfo the URI info
     * @param headers the headers
     * @param propfind the propfind
     * @throws ParserConfigurationException if the XML parser is not properly configured
     */
    @PROPFIND
    @Consumes({APPLICATION_XML})
    @Produces({APPLICATION_XML})
    @Timed
    public void getProperties(@Suspended final AsyncResponse response, @Context final Request request,
            @Context final UriInfo uriInfo, @Context final HttpHeaders headers, final DavPropFind propfind)
            throws ParserConfigurationException {
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final String location = fromUri(getBaseUrl(req)).path(req.getPath()).build().toString();
        final Document doc = getDocument();
        services.getResourceService().get(identifier)
            .thenApply(this::checkResource)
            .thenApply(propertiesToMultiStatus(doc, location, propfind))
            .thenApply(multistatus -> status(MULTI_STATUS).entity(multistatus).build())
            .exceptionally(this::handleException).thenApply(response::resume);
    }

    /**
     * Update properties on a resource.
     * @param response the async response
     * @param request the request
     * @param uriInfo the URI info
     * @param headers the headers
     * @param security the security context
     * @param propertyUpdate the property update request
     * @throws ParserConfigurationException if the XML parser is not properly configured
     */
    @PROPPATCH
    @Consumes({APPLICATION_XML})
    @Produces({APPLICATION_XML})
    @Timed
    public void updateProperties(@Suspended final AsyncResponse response,
            @Context final Request request, @Context final UriInfo uriInfo,
            @Context final HttpHeaders headers, @Context final SecurityContext security,
            final DavPropertyUpdate propertyUpdate) throws ParserConfigurationException {

        final Document doc = getDocument();
        final TrellisRequest req = new TrellisRequest(request, uriInfo, headers, security);
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + req.getPath());
        final String baseUrl = getBaseUrl(req);
        final String location = fromUri(baseUrl).path(req.getPath()).build().toString();
        final Session session = HttpSession.from(security);
        services.getResourceService().get(identifier)
            .thenApply(this::checkResource)
            .thenCompose(resourceToMultiStatus(doc, identifier, location, baseUrl, session, propertyUpdate))
            .thenApply(multistatus -> status(MULTI_STATUS).entity(multistatus).build())
            .exceptionally(this::handleException).thenApply(response::resume);
    }

    private static Function<Element, Stream<Quad>> elementToQuads(final IRI identifier) {
        return el -> {
            if (el.getNamespaceURI() != null) {
                final Stream.Builder<Quad> builder = Stream.builder();
                final IRI predicate = rdf.createIRI(el.getNamespaceURI() + el.getLocalName());
                // iterate over child nodes
                Node node = el.getFirstChild();
                while (node != null) {
                    if (Node.TEXT_NODE == node.getNodeType() && !isBlank(node.getNodeValue())) {
                        builder.accept(rdf.createQuad(Trellis.PreferUserManaged, identifier,
                                    predicate, rdf.createLiteral(node.getNodeValue())));
                    } else if (Node.ELEMENT_NODE == node.getNodeType()
                            && node.getNamespaceURI() != null) {
                        builder.accept(rdf.createQuad(Trellis.PreferUserManaged, identifier,
                                    predicate,
                                    rdf.createIRI(node.getNamespaceURI() + node.getLocalName())));
                    }
                    node = node.getNextSibling();
                }
                return builder.build();
            }
            return Stream.empty();
        };
    }

    private CompletionStage<? extends Resource> getParent(final IRI identifier) {
        final Optional<IRI> parent = getContainer(identifier);
        if (parent.isPresent()) {
            return services.getResourceService().get(parent.get());
        }
        return completedFuture(MISSING_RESOURCE);
    }

    private IRI getDestination(final HttpHeaders headers, final String baseUrl) {
        final String destination = headers.getHeaderString("Destination");
        if (destination == null) {
            throw new BadRequestException("Missing Destination header");
        } else if (!destination.startsWith(baseUrl)) {
            throw new BadRequestException("Out-of-domain Destination!");
        }
        return services.getResourceService().toInternal(rdf.createIRI(destination), baseUrl);
    }

    private Resource checkResources(final Resource from, final Resource to) {
        if (MISSING_RESOURCE.equals(from)) {
            throw new NotFoundException();
        } else if (DELETED_RESOURCE.equals(from)) {
            throw new ClientErrorException(GONE);
        } else if (exists(to)) {
            throw new ClientErrorException(CONFLICT);
        }
        return from;
    }

    private Resource checkResource(final Resource res) {
        if (MISSING_RESOURCE.equals(res)) {
            throw new NotFoundException();
        } else if (DELETED_RESOURCE.equals(res)) {
            throw new ClientErrorException(GONE);
        }
        return res;
    }

    private Response handleException(final Throwable err) {
        final Throwable cause = err.getCause();
        if (cause instanceof WebApplicationException) return ((WebApplicationException) cause).getResponse();
        LOGGER.error("Trellis Error:", err);
        return new WebApplicationException(err).getResponse();
    }

    private Function<Resource, CompletionStage<DavMultiStatus>> resourceToMultiStatus(final Document doc,
            final IRI identifier, final String location, final String baseUrl, final Session session,
            final DavPropertyUpdate propertyUpdate) {
        return resource -> {
            final Dataset dataset = rdf.createDataset();
            final Set<IRI> removeProperties = getRemoveProperties(propertyUpdate);
            final DavMultiStatus multistatus = new DavMultiStatus();
            final DavResponse response = new DavResponse();
            final Set<IRI> modifiedProperties = new HashSet<>();
            response.setHref(location);
            // Keep any extension data
            try (final Stream<Quad> stream = resource.stream(extensions.values())) {
                stream.forEach(dataset::add);
            }
            // Filter out any removable properties
            try (final Stream<Quad> stream = resource.stream(Trellis.PreferUserManaged)) {
                stream.forEach(quad -> {
                    if (removeProperties.contains(quad.getPredicate())) {
                        modifiedProperties.add(quad.getPredicate());
                    } else {
                        dataset.add(rdf.createQuad(Trellis.PreferUserManaged, identifier, quad.getPredicate(),
                                    quad.getObject()));
                    }
                });
            }

            final DavSet set = propertyUpdate.getSet();
            if (set != null) {
                final DavProp prop = set.getProp();
                if (prop != null) {
                    final List<Element> nodes = prop.getNodes();
                    if (nodes != null) nodes.stream().flatMap(elementToQuads(identifier)).forEach(quad -> {
                        modifiedProperties.add(quad.getPredicate());
                        dataset.add(quad);
                    });
                }
            }

            response.setPropStats(modifiedProperties.stream().map(predicate -> {
                final DavPropStat stat = new DavPropStat();
                final DavProp prop = new DavProp();
                prop.setNodes(singletonList(doc.createElementNS(namespaceXML(predicate.getIRIString()),
                                localnameXML(predicate.getIRIString()))));
                stat.setProp(prop);
                stat.setStatus(SUCCESS);
                return stat;
            }).collect(toList()));
            multistatus.setDescription("Response to property update request");
            multistatus.setResponses(singletonList(response));

            final Dataset immutable = rdf.createDataset();
            services.getAuditService().creation(resource.getIdentifier(), session).stream()
                .map(skolemizeQuads(services.getResourceService(), baseUrl)).forEachOrdered(immutable::add);

            return services.getResourceService()
                .replace(Metadata.builder(resource).build(), dataset)
                .whenComplete((a, b) -> closeDataset(dataset))
                .thenCompose(future ->
                        services.getResourceService().add(resource.getIdentifier(), immutable))
                .whenComplete((a, b) -> closeDataset(immutable))
                .thenCompose(future ->
                        services.getMementoService().put(services.getResourceService(), resource.getIdentifier()))
                .thenAccept(future -> services.getEventService().emit(new SimpleEvent(location, session.getAgent(),
                            asList(PROV.Activity, AS.Update), singletonList(resource.getInteractionModel()))))
                .thenApply(future -> multistatus);
        };
    }

    private static Document getDocument() throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setExpandEntityReferences(false);
        factory.setFeature(FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        factory.setAttribute(ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().newDocument();
    }

    private Function<Resource, DavMultiStatus> propertiesToMultiStatus(final Document doc,
            final String baseUrl, final DavPropFind propfind) {

        final Set<IRI> properties = getProperties(propfind);
        final boolean allprops = propfind.getAllProp() != null;
        final boolean propname = propfind.getPropName() != null;

        return res -> {
            final DavProp prop = new DavProp();
            prop.setNodes(getPropertyElements(doc, res, properties, allprops, propname));

            final DavPropStat propstat = new DavPropStat();
            propstat.setProp(prop);
            propstat.setStatus(SUCCESS);

            final DavResponse response = new DavResponse();
            response.setHref(externalUrl(res.getIdentifier(), baseUrl));
            response.setDescription("PROPFIND request for " + externalUrl(res.getIdentifier(), baseUrl));
            response.setPropStats(singletonList(propstat));

            final List<DavResponse> responses = new ArrayList<>();
            responses.add(response);

            try (final Stream<Quad> children = res.stream(LDP.PreferContainment)) {
                children.map(Quad::getObject).filter(IRI.class::isInstance).map(IRI.class::cast)
                    .parallel().map(id -> services.getResourceService().get(id).thenApply(r -> {
                        final DavProp childProp = new DavProp();
                        childProp.setNodes(getPropertyElements(doc, r, properties, allprops, propname));

                        final DavPropStat childPropstat = new DavPropStat();
                        childPropstat.setProp(childProp);
                        childPropstat.setStatus(SUCCESS);

                        final DavResponse childResponse = new DavResponse();
                        childResponse.setHref(externalUrl(r.getIdentifier(), baseUrl));
                        childResponse.setDescription("PROPFIND request for " + externalUrl(r.getIdentifier(), baseUrl));
                        childResponse.setPropStats(singletonList(childPropstat));

                        return childResponse;
                    })).map(CompletionStage::toCompletableFuture).map(CompletableFuture::join).forEach(responses::add);
            }

            final DavMultiStatus multistatus = new DavMultiStatus();
            multistatus.setResponses(responses);
            multistatus.setDescription("Response to property find request");
            return multistatus;
        };
    }

    private CompletionStage<Void> copyTo(final Resource resource, final Session session, final Depth.DEPTH depth,
            final IRI destination, final String baseUrl) {
        if (ZERO.equals(depth)) {
            return copy(services, session, resource, destination, baseUrl);
        } else if (ONE.equals(depth)) {
            return depth1Copy(services, session, resource, destination, baseUrl);
        }
        return recursiveCopy(services, session, resource, destination, baseUrl);
    }

    private String getBaseUrl(final TrellisRequest req) {
        if (userBaseUrl != null) {
            return userBaseUrl;
        }
        return req.getBaseUrl();
    }

    private static Set<IRI> getProperties(final DavPropFind propfind) {
        final DavProp prop = propfind.getProp();
        if (prop == null) return emptySet();
        final List<Element> nodes = prop.getNodes();
        if (nodes == null) return emptySet();
        return nodes.stream().filter(el -> el.getNamespaceURI() != null)
                        .map(el -> rdf.createIRI(el.getNamespaceURI() + el.getLocalName())).collect(toSet());
    }

    private static Set<IRI> getRemoveProperties(final DavPropertyUpdate propertyUpdate) {
        final DavRemove remove = propertyUpdate.getRemove();
        final DavSet set = propertyUpdate.getSet();
        final Set<IRI> props = new HashSet<>();
        if (remove != null) {
             props.addAll(getProperties(remove.getProp()));
        }
        // Note: also clear any set properties before adding them back
        if (set != null) {
             props.addAll(getProperties(set.getProp()));
        }
        return props;
    }

    private static Set<IRI> getProperties(final DavProp prop) {
        if (prop != null) {
            return prop.getNodes().stream().filter(el -> el.getNamespaceURI() != null)
                .map(el -> rdf.createIRI(el.getNamespaceURI() + el.getLocalName())).collect(toSet());
        }
        return emptySet();
    }

    private static Element getContentTypeElement(final Document doc, final Resource res, final boolean propname) {
        final Element el = doc.createElementNS(DAV_NAMESPACE, "getcontenttype");
        if (!propname) {
            el.setTextContent(res.getBinaryMetadata().flatMap(BinaryMetadata::getMimeType)
                .orElse("text/turtle"));
        }
        return el;
    }

    private static Element getLastModifiedElement(final Document doc, final Resource res, final boolean propname) {
        final Element el = doc.createElementNS(DAV_NAMESPACE, "getlastmodified");
        if (!propname) {
            el.setTextContent(ofInstant(res.getModified().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME));
        }
        return el;
    }

    private static Optional<Element> getResourceTypeElement(final Document doc, final Resource res,
            final boolean propname) {
        if (!propname && res.getInteractionModel().getIRIString().endsWith("Container")) {
            final Element el = doc.createElementNS(DAV_NAMESPACE, "resourcetype");
            el.appendChild(doc.createElementNS(DAV_NAMESPACE, "collection"));
            return Optional.of(el);
        }
        return empty();
    }

    private static Function<Quad, Element> quadToElement(final Document doc, final boolean propname) {
        return quad -> {
            if (quad.getObject() instanceof Literal) {
                final Element el = doc.createElementNS(namespaceXML(quad.getPredicate().getIRIString()),
                            localnameXML(quad.getPredicate().getIRIString()));
                if (!propname) {
                    el.setTextContent(((Literal) quad.getObject()).getLexicalForm());
                }
                return el;
            } else if (quad.getObject() instanceof IRI) {
                final Element el = doc.createElementNS(namespaceXML(quad.getPredicate().getIRIString()),
                            localnameXML(quad.getPredicate().getIRIString()));
                if (!propname) {
                    el.setTextContent(((IRI) quad.getObject()).getIRIString());
                }
                return el;
            }
            return null;
        };
    }

    private static List<Element> getPropertyElements(final Document doc, final Resource res,
            final Set<IRI> properties, final boolean allproperties, final boolean propname) {

        final List<Element> allProperties = new ArrayList<>();
        getResourceTypeElement(doc, res, propname).ifPresent(allProperties::add);
        allProperties.add(getContentTypeElement(doc, res, propname));
        allProperties.add(getLastModifiedElement(doc, res, propname));

        try (final Stream<Quad> stream = res.stream(Trellis.PreferUserManaged)) {
            final List<Element> elements = stream
                .filter(t -> allproperties || properties.contains(t.getPredicate()))
                .map(quadToElement(doc, propname)).filter(Objects::nonNull).collect(toList());
            allProperties.addAll(elements);
        }
        return allProperties;
    }

    private static boolean exists(final Resource res) {
        return !MISSING_RESOURCE.equals(res) && !DELETED_RESOURCE.equals(res);
    }

    private static Depth.DEPTH getDepth(final String depth) {
        if (depth != null) {
            return new Depth(depth).getDepth();
        }
        return INFINITY;
    }

}
