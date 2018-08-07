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

import static java.lang.String.join;
import static java.util.Collections.singletonList;
import static java.util.Date.from;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
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
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_RANGES;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.domain.HttpConstants.DESCRIPTION;
import static org.trellisldp.http.domain.HttpConstants.DIGEST;
import static org.trellisldp.http.domain.HttpConstants.LINK_TEMPLATE;
import static org.trellisldp.http.domain.HttpConstants.MEMENTO_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.PATCH;
import static org.trellisldp.http.domain.HttpConstants.PREFER;
import static org.trellisldp.http.domain.HttpConstants.PREFERENCE_APPLIED;
import static org.trellisldp.http.domain.HttpConstants.RANGE;
import static org.trellisldp.http.domain.HttpConstants.WANT_DIGEST;
import static org.trellisldp.http.domain.Prefer.PREFER_MINIMAL;
import static org.trellisldp.http.domain.Prefer.PREFER_REPRESENTATION;
import static org.trellisldp.http.domain.Prefer.PREFER_RETURN;
import static org.trellisldp.http.impl.RdfUtils.buildEtagHash;
import static org.trellisldp.http.impl.RdfUtils.filterWithLDF;
import static org.trellisldp.http.impl.RdfUtils.filterWithPrefer;
import static org.trellisldp.http.impl.RdfUtils.getDefaultProfile;
import static org.trellisldp.http.impl.RdfUtils.getProfile;
import static org.trellisldp.http.impl.RdfUtils.getSyntax;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;
import static org.trellisldp.http.impl.RdfUtils.unskolemizeQuads;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.http.domain.Version;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Memento;

/**
 * The GET response builder.
 *
 * @author acoburn
 */
public class GetHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(GetHandler.class);

    private RDFSyntax syntax;

    /**
     * A GET response builder.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    public GetHandler(final LdpRequest req, final ServiceBundler trellis, final String baseUrl) {
        super(req, trellis, baseUrl);
    }

    /**
     * Initialize the get handler.
     * @param resource the Trellis resource
     * @return the response builder
     */
    public ResponseBuilder initialize(final Resource resource) {

        if (MISSING_RESOURCE.equals(resource)) {
            return status(NOT_FOUND);
        } else if (DELETED_RESOURCE.equals(resource)) {
            return status(GONE);
        }

        LOGGER.debug("Acceptable media types: {}", getRequest().getHeaders().getAcceptableMediaTypes());

        try {
            this.syntax = getSyntax(getServices().getIOService(),
                getRequest().getHeaders().getAcceptableMediaTypes(), resource.getBinary()
                    .filter(b -> !DESCRIPTION.equals(getRequest().getExt()))
                    .map(b -> b.getMimeType().orElse(APPLICATION_OCTET_STREAM))).orElse(null);
        } catch (final InvalidSyntaxException ex) {
            LOGGER.debug("No acceptable syntax found: {}", ex.getMessage());
            return status(NOT_ACCEPTABLE);
        }

        if (ACL.equals(getRequest().getExt()) && !resource.hasAcl()) {
            return status(NOT_FOUND);
        }

        mayContinue(true);
        setResource(resource);
        return ok();
    }

    /**
     * Get the standard headers.
     * @param builder the response builder
     * @return the response builder
     */
    public ResponseBuilder standardHeaders(final ResponseBuilder builder) {
        if (!mayContinue()) {
            return builder;
        }

        // Standard HTTP Headers
        builder.lastModified(from(getResource().getModified())).header(VARY, ACCEPT);

        final IRI model;

        if (isNull(getRequest().getExt()) || DESCRIPTION.equals(getRequest().getExt())) {
            if (nonNull(syntax)) {
                builder.header(VARY, PREFER);
                builder.type(syntax.mediaType());
            }

            model = getResource().getBinary().isPresent() && nonNull(syntax)
                ? LDP.RDFSource : getResource().getInteractionModel();
            // Link headers from User data
            getResource().getExtraLinkRelations().collect(toMap(Entry::getKey, Entry::getValue))
                .entrySet().forEach(entry -> builder.link(entry.getKey(), join(" ", entry.getValue())));
        } else {
            model = LDP.RDFSource;
        }

        // Add LDP-required headers
        addLdpHeaders(builder, model);

        // Memento-related headers
        addMementoHeaders(builder);

        return builder;
    }

    /**
     * Build the representation for the given resource.
     *
     * @param builder the response builder
     * @return the response builder
     */
    public ResponseBuilder getRepresentation(final ResponseBuilder builder) {
        if (!mayContinue()) {
            return builder;
        }

        // Add NonRDFSource-related "describe*" link headers, provided this isn't an ACL resource
        getResource().getBinary().filter(ds -> !ACL.equals(getRequest().getExt())).ifPresent(ds -> {
            final String base = getBaseBinaryIdentifier();
            final String description = base + (base.contains("?") ? "&" : "?") + "ext=description";
            if (nonNull(syntax)) {
                builder.link(description, "canonical").link(base, "describes")
                    .link(base + "#description", "alternate");
            } else {
                builder.link(base, "canonical").link(description, "describedby")
                    .type(ds.getMimeType().orElse(APPLICATION_OCTET_STREAM));
            }
        });

        // Add a "self" link header
        builder.link(getSelfIdentifier(), "self");

        // URI Template
        builder.header(LINK_TEMPLATE,
                "<" + getIdentifier() + "{?version}>; rel=\"" + Memento.Memento.getIRIString() + "\"");

        // NonRDFSources responses (strong ETags, etc)
        if (getResource().getBinary().isPresent() && isNull(syntax)) {
            return getLdpNr(builder);
        }

        // RDFSource responses (weak ETags, etc)
        final RDFSyntax rdfSyntax = ofNullable(syntax).orElse(TURTLE);
        final IRI profile = getProfile(getRequest().getHeaders().getAcceptableMediaTypes(), rdfSyntax);
        return getLdpRs(builder, rdfSyntax, profile);
    }

    /**
     * Add the memento headers.
     * @param builder the ResponseBuilder
     * @param mementos the list of memento ranges
     * @return the response builder
     */
    public ResponseBuilder addMementoHeaders(final ResponseBuilder builder, final List<Range<Instant>> mementos) {
        if (!mayContinue()) {
            return builder;
        }
        // Only show memento links for the user-managed graph (not ACL)
        if (!ACL.equals(getRequest().getExt())) {
            builder.link(getIdentifier(), "original timegate")
                .links(MementoResource.getMementoLinks(getIdentifier(), mementos).toArray(Link[]::new));
        }
        return builder;
    }

    private String getSelfIdentifier() {
        // Add any version or ext parameters
        if (nonNull(getRequest().getVersion()) || nonNull(getRequest().getExt())) {
            final List<String> query = new ArrayList<>();

            ofNullable(getRequest().getVersion()).map(Version::getInstant).map(Instant::toEpochMilli)
                .map(x -> "version=" + x).ifPresent(query::add);

            if (ACL.equals(getRequest().getExt())) {
                query.add("ext=acl");
            } else if (DESCRIPTION.equals(getRequest().getExt())) {
                query.add("ext=description");
            }
            return getIdentifier() + "?" + join("&", query);
        }
        return getIdentifier();
    }

    private String getBaseBinaryIdentifier() {
        // Add the version parameter, if present
        return getIdentifier() + ofNullable(getRequest().getVersion()).map(Version::getInstant)
            .map(Instant::toEpochMilli).map(x -> "?version=" + x).orElse("");
    }

    private void addAllowHeaders(final ResponseBuilder builder) {
        if (getResource().isMemento()) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS));
        } else if (ACL.equals(getRequest().getExt())) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH));
        } else if (getResource().getInteractionModel().equals(LDP.RDFSource)) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH, PUT, DELETE));
        } else {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH, PUT, DELETE, POST));
        }
    }

    private ResponseBuilder getLdpRs(final ResponseBuilder builder, final RDFSyntax syntax, final IRI profile) {

        final Prefer prefer = ACL.equals(getRequest().getExt()) ?
            new Prefer(PREFER_REPRESENTATION, singletonList(PreferAccessControl.getIRIString()),
                    of(PreferUserManaged, LDP.PreferContainment, LDP.PreferMembership).map(IRI::getIRIString)
                        .collect(toList()), null, null, null) : getRequest().getPrefer();

        // Check for a cache hit
        final EntityTag etag = new EntityTag(buildEtagHash(getIdentifier(), getResource().getModified(), prefer), true);
        final ResponseBuilder cache = checkCache(getResource().getModified(), etag);
        if (nonNull(cache)) {
            return cache;
        }

        builder.tag(etag);
        addAllowHeaders(builder);

        // URI Templates
        builder.header(LINK_TEMPLATE, "<" + getIdentifier() + "{?subject,predicate,object}>; rel=\""
                + LDP.RDFSource.getIRIString() + "\"");

        ofNullable(prefer).ifPresent(p -> builder.header(PREFERENCE_APPLIED, PREFER_RETURN + "=" + p.getPreference()
                    .orElse(PREFER_REPRESENTATION)));


        if (ofNullable(prefer).flatMap(Prefer::getPreference).filter(PREFER_MINIMAL::equals).isPresent()) {
            return builder.status(NO_CONTENT);
        }

        // Short circuit HEAD requests
        if (HEAD.equals(getRequest().getRequest().getMethod())) {
            return builder;
        }

        // Stream the rdf content
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream out) throws IOException {
                try (final Stream<? extends Quad> stream = getResource().stream()) {
                    getServices().getIOService().write(stream.filter(filterWithPrefer(prefer))
                        .map(unskolemizeQuads(getServices().getResourceService(), getBaseUrl()))
                        .filter(filterWithLDF(getRequest().getSubject(), getRequest().getPredicate(),
                                getRequest().getObject()))
                        .map(Quad::asTriple), out, syntax,
                            ofNullable(profile).orElseGet(() -> getDefaultProfile(syntax, getIdentifier())));
                }
            }
        };
        return builder.entity(stream);
    }

    private ResponseBuilder getLdpNr(final ResponseBuilder builder) {

        final Instant mod = getResource().getBinary().map(Binary::getModified).orElse(null);
        if (isNull(mod)) {
            LOGGER.error("Could not access binary metadata for {}", getResource().getIdentifier());
            return status(INTERNAL_SERVER_ERROR);
        }

        final EntityTag etag = new EntityTag(buildEtagHash(getIdentifier() + "BINARY", mod, null));
        final ResponseBuilder cache = checkCache(mod, etag);
        if (nonNull(cache)) {
            return cache;
        }

        // Set last-modified to be the binary's last-modified value
        builder.lastModified(from(mod));

        final IRI dsid = getResource().getBinary().map(Binary::getIdentifier).orElse(null);
        if (isNull(dsid)) {
            LOGGER.error("Could not access binary metadata for {}", getResource().getIdentifier());
            return status(INTERNAL_SERVER_ERROR);
        }

        builder.header(VARY, RANGE).header(VARY, WANT_DIGEST).header(ACCEPT_RANGES, "bytes").tag(etag);

        if (getResource().isMemento()) {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS));
        } else {
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PUT, DELETE));
        }

        // Add instance digests, if Requested and supported
        if (nonNull(getRequest().getWantDigest())) {
            final Optional<String> algorithm = getRequest().getWantDigest().getAlgorithms().stream()
                .filter(getServices().getBinaryService().supportedAlgorithms()::contains).findFirst();
            if (algorithm.isPresent()) {
                try {
                    getBinaryDigest(dsid, algorithm.get()).ifPresent(digest ->
                            builder.header(DIGEST, algorithm.get().toLowerCase() + "=" + digest));
                } catch (final IOException ex) {
                    LOGGER.error("Error computing digest on content", ex);
                    return status(INTERNAL_SERVER_ERROR);
                }
            }
        }

        // Stream the binary content
        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream out) throws IOException {
                // TODO -- with JDK 9 use InputStream::transferTo instead of IOUtils::copy
                try (final InputStream binary = getBinaryStream(dsid, getRequest())) {
                    IOUtils.copy(binary, out);
                } catch (final IOException ex) {
                    LOGGER.error("Error writing binary content", ex);
                    throw new WebApplicationException("Error processing binary content: " +
                            ex.getMessage());
                }
            }
        };

        return builder.entity(stream);
    }

    private InputStream getBinaryStream(final IRI dsid, final LdpRequest req) throws IOException {
        final Optional<InputStream> content = isNull(req.getRange())
            ? getServices().getBinaryService().getContent(dsid)
            : getServices().getBinaryService().getContent(dsid, req.getRange().getFrom(), req.getRange().getTo());
        return content.orElseThrow(() -> new IOException("Could not retrieve content from: " + dsid));
    }

    private Optional<String> getBinaryDigest(final IRI dsid, final String algorithm) throws IOException {
        final Optional<InputStream> b = getServices().getBinaryService().getContent(dsid);
        try (final InputStream is = b.orElseThrow(() -> new WebApplicationException("Couldn't fetch binary content"))) {
            return getServices().getBinaryService().digest(algorithm, is);
        }
    }

    private void addLdpHeaders(final ResponseBuilder builder, final IRI model) {
        ldpResourceTypes(model).forEach(type -> {
            builder.link(type.getIRIString(), "type");
            // Mementos don't accept POST or PATCH
            if (LDP.Container.equals(type) && !getResource().isMemento()) {
                builder.header(ACCEPT_POST, getServices().getIOService().supportedWriteSyntaxes().stream()
                        .map(RDFSyntax::mediaType).collect(joining(",")));
            } else if (LDP.RDFSource.equals(type) && !getResource().isMemento()) {
                builder.header(ACCEPT_PATCH, getServices().getIOService().supportedUpdateSyntaxes().stream()
                        .map(RDFSyntax::mediaType).collect(joining(",")));
            }
        });
    }

    private void addMementoHeaders(final ResponseBuilder builder) {
        if (getResource().isMemento()) {
            builder.header(MEMENTO_DATETIME, from(getResource().getModified()));
        } else {
            builder.header(VARY, ACCEPT_DATETIME);
        }
    }
}
