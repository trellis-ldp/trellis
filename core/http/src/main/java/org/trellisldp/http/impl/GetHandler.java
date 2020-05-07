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

import static java.lang.String.join;
import static java.net.URI.create;
import static java.util.Date.from;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.ok;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.core.HttpConstants.DESCRIPTION;
import static org.trellisldp.http.core.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.http.core.HttpConstants.PATCH;
import static org.trellisldp.http.core.HttpConstants.PREFER;
import static org.trellisldp.http.core.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.core.HttpConstants.RANGE;
import static org.trellisldp.http.core.Prefer.PREFER_MINIMAL;
import static org.trellisldp.http.core.Prefer.PREFER_REPRESENTATION;
import static org.trellisldp.http.core.Prefer.PREFER_RETURN;
import static org.trellisldp.http.impl.HttpUtils.getDefaultProfile;
import static org.trellisldp.http.impl.HttpUtils.getProfile;
import static org.trellisldp.http.impl.HttpUtils.getSyntax;
import static org.trellisldp.http.impl.HttpUtils.isContainer;
import static org.trellisldp.http.impl.HttpUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.HttpUtils.triplePreferences;
import static org.trellisldp.http.impl.HttpUtils.unskolemizeTriples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.Resource;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.TrellisRequest;
import org.trellisldp.http.core.Version;
import org.trellisldp.vocabulary.LDP;

/**
 * The GET response builder.
 *
 * @author acoburn
 */
public class GetHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(GetHandler.class);

    private final boolean weakEtags;
    private final boolean includeMementoDates;
    private final boolean isMemento;
    private final String defaultJsonLdProfile;

    private RDFSyntax syntax;

    /**
     * A GET response builder.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param extensions the extension mapping
     * @param config configuration for the get hander
     */
    public GetHandler(final TrellisRequest req, final ServiceBundler trellis, final Map<String, IRI> extensions,
            final GetConfiguration config) {
        super(req, trellis, extensions, config.getBaseUrl());
        this.isMemento = config.isMemento();
        this.weakEtags = config.useWeakEtags();
        this.includeMementoDates = config.includeMementoDates();
        this.defaultJsonLdProfile = config.defaultJsonLdProfile();
    }

    /**
     * Initialize the get handler.
     * @param resource the Trellis resource
     * @return the response builder
     */
    public ResponseBuilder initialize(final Resource resource) {

        if (MISSING_RESOURCE.equals(resource)) {
            throw new NotFoundException();
        } else if (DELETED_RESOURCE.equals(resource)) {
            throw new ClientErrorException(GONE);
        }

        // Redirect for certain trailing slash conditions
        handleTrailingSlashRedirection(resource);

        LOGGER.debug("Acceptable media types: {}", getRequest().getAcceptableMediaTypes());

        this.syntax = getSyntax(getServices().getIOService(),
            getRequest().getAcceptableMediaTypes(), resource.getBinaryMetadata()
                .filter(b -> !DESCRIPTION.equals(getRequest().getExt()))
                .map(b -> b.getMimeType().orElse(APPLICATION_OCTET_STREAM)).orElse(null));

        final IRI ext = getExtensionGraphName();
        if (ext != null && !resource.stream(ext).findAny().isPresent()) {
            LOGGER.trace("No stream for extention: {}", ext);
            throw new NotFoundException();
        }

        setResource(resource);
        return ok();
    }

    /**
     * Get the standard headers.
     * @param builder the response builder
     * @return the response builder
     */
    public ResponseBuilder standardHeaders(final ResponseBuilder builder) {

        // Standard HTTP Headers
        builder.lastModified(from(getResource().getModified()));

        if (syntax != null) {
            builder.type(syntax.mediaType());
        }

        final IRI model;
        if (getRequest().getExt() == null || DESCRIPTION.equals(getRequest().getExt())) {
            model = getResource().getBinaryMetadata().isPresent() && syntax != null
                ? LDP.RDFSource : getResource().getInteractionModel();
            // Link headers from User data
            getResource().getExtraLinkRelations().collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                    .forEach((key, value) -> builder.link(key, join(" ", value)));
        } else {
            model = LDP.RDFSource;
        }

        // Add LDP-required headers
        addLdpHeaders(builder, model);

        // Memento-related headers
        if (isMemento) {
            builder.header(MEMENTO_DATETIME, from(getResource().getModified()));
        }

        return builder;
    }

    /**
     * Build the representation for the given resource.
     *
     * @param builder the response builder
     * @return the response builder
     */
    public CompletionStage<ResponseBuilder> getRepresentation(final ResponseBuilder builder) {
        // Add NonRDFSource-related "describe*" link headers, provided this isn't an extension resource
        getResource().getBinaryMetadata().filter(ds -> getExtensionGraphName() == null).ifPresent(ds -> {
            final String base = getBaseBinaryIdentifier();
            final String description = base + (base.contains("?") ? "&" : "?") + "ext=description";
            if (syntax != null) {
                builder.link(description, "canonical").link(base, "describes")
                    .link(base + "#description", "alternate");
            } else {
                builder.link(base, "canonical").link(description, "describedby")
                    .type(ds.getMimeType().orElse(APPLICATION_OCTET_STREAM));
            }
        });

        // Add a "self" link header
        builder.link(getSelfIdentifier(), "self");

        // NonRDFSources responses (strong ETags, etc)
        if (getResource().getBinaryMetadata().isPresent() && syntax == null) {
            return getLdpNr(builder.header(VARY, buildVaryHeader(false)));
        }

        // RDFSource responses (weak ETags, etc)
        final IRI profile = getProfile(getRequest().getAcceptableMediaTypes(), syntax);
        return completedFuture(getLdpRs(builder.header(VARY, buildVaryHeader(true)), syntax, profile));
    }

    /**
     * Add the memento headers.
     * @param builder the ResponseBuilder
     * @param mementos the list of memento ranges
     * @return the response builder
     */
    public ResponseBuilder addMementoHeaders(final ResponseBuilder builder, final SortedSet<Instant> mementos) {
        // Only show memento links for the user-managed graph (not extension graphs)
        if (getExtensionGraphName() == null) {
            builder.link(getIdentifier(), "original timegate")
                .links(MementoResource.getMementoHeaders(getIdentifier(), mementos, isMemento ?
                            getResource().getModified() : null)
                        .map(link -> MementoResource.filterLinkParams(link, !includeMementoDates))
                        .toArray(Link[]::new));
        }
        return builder;
    }

    @Override
    protected String getIdentifier() {
        return HttpUtils.buildResourceUrl(getRequest(), getBaseUrl());
    }

    private String getSelfIdentifier() {
        // Add any version or ext parameters
        if (getRequest().getVersion() != null || getRequest().getExt() != null) {
            final List<String> query = new ArrayList<>();

            final Version v = getRequest().getVersion();
            if (v != null) {
                query.add("version=" + v.getInstant().getEpochSecond());
            }

            if (getExtensionGraphName() != null) {
                query.add("ext=" + getRequest().getExt());
            } else if (DESCRIPTION.equals(getRequest().getExt())) {
                query.add("ext=description");
            }
            return getIdentifier() + "?" + join("&", query);
        }
        return getIdentifier();
    }

    private String getBaseBinaryIdentifier() {
        // Add the version parameter, if present
        if (getRequest().getVersion() != null) {
            return getIdentifier() + "?version=" + getRequest().getVersion().getInstant().getEpochSecond();
        }
        return getIdentifier();
    }

    private void addAllowHeaders(final ResponseBuilder builder) {
        if (isMemento) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS));
        } else if (getExtensionGraphName() != null) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH));
        } else if (getResource().getInteractionModel().equals(LDP.RDFSource)) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH, PUT, DELETE));
        } else {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH, PUT, DELETE, POST));
        }
    }

    private ResponseBuilder getLdpRs(final ResponseBuilder builder, final RDFSyntax syntax,
            final IRI profile) {
        final Prefer prefer = getRequest().getPrefer();

        // Check for a cache hit
        final EntityTag etag = generateEtag(getResource(), weakEtags);
        checkCache(getResource().getModified(), etag);

        builder.tag(etag);
        addAllowHeaders(builder);

        if (prefer != null) {
            builder.header(PREFERENCE_APPLIED,
                    PREFER_RETURN + "=" + prefer.getPreference().orElse(PREFER_REPRESENTATION));
            if (prefer.getPreference().filter(PREFER_MINIMAL::equals).isPresent()) {
                return builder.status(NO_CONTENT);
            }
        }

        // Short circuit HEAD requests
        if (HEAD.equals(getRequest().getMethod())) {
            return builder;
        }

        // Stream the rdf content
        return builder.entity((StreamingOutput) out -> {
            try (final Stream<Quad> stream = getResource().stream(getPreferredGraphs(prefer))) {
                getServices().getIOService().write(stream.map(Quad::asTriple)
                                .map(unskolemizeTriples(getServices().getResourceService(), getBaseUrl())), out,
                        syntax, getIdentifier(), getJsonLdProfile(profile, syntax));
            }
        });
    }

    // Don't allow triples from other graphs
    private Set<IRI> getPreferredGraphs(final Prefer prefer) {
        final Set<IRI> p = triplePreferences(prefer);
        // Remove non-current extension graphs
        p.removeAll(getNonCurrentGraphNames());
        return p;
    }

    private IRI getJsonLdProfile(final IRI profile, final RDFSyntax syntax) {
        if (profile != null) {
            return profile;
        }
        return getDefaultProfile(syntax, getIdentifier(), defaultJsonLdProfile);
    }

    private CompletionStage<ResponseBuilder> getLdpNr(final ResponseBuilder builder) {

        final EntityTag etag = generateEtag(getResource());
        checkCache(getResource().getModified(), etag);

        final IRI dsid = getResource().getBinaryMetadata().map(BinaryMetadata::getIdentifier).orElse(null);

        // Add standard headers
        builder.header(ACCEPT_RANGES, "bytes").tag(etag)
            .header(ALLOW, isMemento ? join(",", GET, HEAD, OPTIONS) : join(",", GET, HEAD, OPTIONS, PUT, DELETE));

        // Short circuit HEAD requests
        if (HEAD.equals(getRequest().getMethod())) {
            return completedFuture(builder);
        }

        // Stream the binary content
        return getBinaryStream(dsid, getRequest())
                        .thenApply(in -> (StreamingOutput) out -> copy(in, out))
                        .thenApply(builder::entity);
    }

    // TODO -- with JDK 9 use InputStream::transferTo instead of IOUtils::copy
    private static void copy(final InputStream from, final OutputStream to) throws IOException {
        IOUtils.copy(from, to);
        from.close();
    }

    private CompletionStage<InputStream> getBinaryStream(final IRI dsid, final TrellisRequest req) {
        if (req.getRange() == null) {
            return getServices().getBinaryService().get(dsid).thenApply(Binary::getContent);
        }
        return getServices().getBinaryService().get(dsid)
                        .thenApply(b -> b.getContent(req.getRange().getFrom(), req.getRange().getTo()));
    }

    private String buildVaryHeader(final boolean isLdpRs) {
        final List<String> variants = new ArrayList<>();
        variants.add(ACCEPT);
        if (!isMemento) {
            variants.add(ACCEPT_DATETIME);
        }
        if (!isLdpRs) {
            variants.add(RANGE);
        } else if (getRequest().getExt() == null || DESCRIPTION.equals(getRequest().getExt())) {
            variants.add(PREFER);
        }

        return join(",", variants);
    }

    private void addLdpHeaders(final ResponseBuilder builder, final IRI model) {
        ldpResourceTypes(model).forEach(type -> {
            builder.link(type.getIRIString(), Link.TYPE);
            // Mementos don't accept POST or PATCH
            if (LDP.Container.equals(type) && !isMemento) {
                final List<RDFSyntax> rdfSyntaxes = getServices().getIOService().supportedWriteSyntaxes();
                final Stream<String> allSyntaxes = concat(rdfSyntaxes.stream().map(RDFSyntax::mediaType), of(WILDCARD));

                builder.header(ACCEPT_POST, allSyntaxes.collect(joining(",")));
            } else if (LDP.Resource.equals(type) && !isMemento) {
                builder.header(ACCEPT_PATCH, getServices().getIOService().supportedUpdateSyntaxes().stream()
                        .map(RDFSyntax::mediaType).collect(joining(",")));
            }
        });
    }

    private void handleTrailingSlashRedirection(final Resource resource) {
        if (getRequest().hasTrailingSlash() && !isContainer(resource.getInteractionModel())) {
            throw new RedirectionException(303, create(getBaseUrl() + getRequest().getPath()));
        } else if (!getRequest().hasTrailingSlash() && !getRequest().getPath().isEmpty()
                && isContainer(resource.getInteractionModel())) {
            throw new RedirectionException(303, create(getBaseUrl() + getRequest().getPath() + "/"));
        }
    }
}
