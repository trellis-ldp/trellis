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
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Response.Status.FOUND;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.trellisldp.http.core.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.core.HttpConstants.DATETIME;
import static org.trellisldp.http.core.HttpConstants.FROM;
import static org.trellisldp.http.core.HttpConstants.MEMENTO;
import static org.trellisldp.http.core.HttpConstants.ORIGINAL;
import static org.trellisldp.http.core.HttpConstants.TIMEGATE;
import static org.trellisldp.http.core.HttpConstants.TIMEMAP;
import static org.trellisldp.http.core.HttpConstants.UNTIL;
import static org.trellisldp.http.impl.HttpUtils.getProfile;
import static org.trellisldp.http.impl.HttpUtils.getSyntax;
import static org.trellisldp.vocabulary.JSONLD.compacted;
import static org.trellisldp.vocabulary.LDP.RDFSource;
import static org.trellisldp.vocabulary.LDP.Resource;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Stream;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.TrellisRequest;

/**
 * @author acoburn
 */
public final class MementoResource {

    private static final String FIRST = "first";
    private static final String LAST = "last";
    private static final String PREV = "prev";
    private static final String NEXT = "next";
    private static final String TIMEMAP_PARAM = "?ext=timemap";
    private static final String VERSION_PARAM = "?version=";

    private final ServiceBundler trellis;
    private final boolean includeMementoDates;

    /**
     * Wrap a resource in some Memento-specific response builders.
     *
     * @param trellis the Trellis application bundle
     * @param includeMementoDates whether to include memento dates in link headers
     */
    public MementoResource(final ServiceBundler trellis, final boolean includeMementoDates) {
        this.trellis = trellis;
        this.includeMementoDates = includeMementoDates;
    }

    /**
     * Create a response builder for a TimeMap response.
     *
     * @param mementos the mementos
     * @param baseUrl the base URL
     * @param req the LDP request
     * @return a response builder object
     */
    public ResponseBuilder getTimeMapBuilder(final SortedSet<Instant> mementos, final TrellisRequest req,
            final String baseUrl) {

        final List<MediaType> acceptableTypes = req.getAcceptableMediaTypes();
        final String identifier = HttpUtils.buildResourceUrl(req, baseUrl);
        final List<Link> allLinks = getMementoLinks(identifier, mementos).collect(toList());

        final ResponseBuilder builder = ok().link(identifier, ORIGINAL + " " + TIMEGATE);
        builder.links(getMementoHeaders(identifier, mementos).map(this::filterLinkParams).toArray(Link[]::new))
            .link(Resource.getIRIString(), TYPE).link(RDFSource.getIRIString(), TYPE)
            .header(ALLOW, join(",", GET, HEAD, OPTIONS));

        final RDFSyntax syntax = getSyntax(trellis.getIOService(), acceptableTypes, APPLICATION_LINK_FORMAT);

        if (syntax != null) {
            final IRI profile = getProfile(acceptableTypes, syntax);
            final IRI jsonldProfile = profile != null ? profile : compacted;

            return builder.type(syntax.mediaType()).entity((StreamingOutput) out ->
                trellis.getIOService().write(trellis.getTimemapGenerator()
                        .asRdf(identifier, allLinks), out, syntax, baseUrl, jsonldProfile));
        }

        return builder.type(APPLICATION_LINK_FORMAT)
            .entity(allLinks.stream().map(this::filterLinkParams).map(Link::toString).collect(joining(",\n")) + "\n");
    }

    /**
     * Create a response builder for a TimeGate response.
     *
     * @param mementos the list of memento ranges
     * @param req the LDP request
     * @param baseUrl the base URL
     * @return a response builder object
     */
    public ResponseBuilder getTimeGateBuilder(final SortedSet<Instant> mementos, final TrellisRequest req,
            final String baseUrl) {
        final String identifier = HttpUtils.buildResourceUrl(req, baseUrl);
        return status(FOUND)
            .location(fromUri(identifier + VERSION_PARAM + req.getDatetime().getInstant().getEpochSecond()).build())
            .link(identifier, ORIGINAL + " " + TIMEGATE)
            .links(getMementoHeaders(identifier, mementos).map(this::filterLinkParams).toArray(Link[]::new));
    }

    /**
     * Filter link parameters from a provided Link object, if configured to do so.
     * @param link the link
     * @return a Link without Memento parameters, if desired; otherwise, the original link
     */
    public Link filterLinkParams(final Link link) {
        return filterLinkParams(link, !includeMementoDates);
    }

    /**
     * Retrieve all of the Memento-related link headers given a collection of datetimes.
     *
     * @param identifier the public identifier for the resource
     * @param mementos a collection of memento values
     * @return a stream of link headers
     */
    public static Stream<Link> getMementoLinks(final String identifier, final SortedSet<Instant> mementos) {
        if (mementos.isEmpty()) {
            return empty();
        }
        return concat(
                of(
                    Link.fromUri(identifier).rel(TIMEGATE).rel(ORIGINAL).build(),
                    Link.fromUri(identifier + TIMEMAP_PARAM).rel(TIMEMAP).type(APPLICATION_LINK_FORMAT)
                        .param(FROM, ofInstant(mementos.first().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                        .param(UNTIL, ofInstant(mementos.last().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                        .build()),
                mementos.stream().map(time -> {
                    if (mementos.size() == 1) {
                        return mementoToLink(identifier, time, asList(FIRST, LAST));
                    } else if (time.equals(mementos.first())) {
                        return mementoToLink(identifier, time, singletonList(FIRST));
                    } else if (time.equals(mementos.last())) {
                        return mementoToLink(identifier, time, singletonList(LAST));
                    }
                    return mementoToLink(identifier, time, emptyList());
                }));
    }

    /**
     * Get the memento headers.
     * @param identifier the identifier
     * @param mementos the mementos
     * @param time the time of the current memento
     * @return a stream of link headers
     */
    public static Stream<Link> getMementoHeaders(final String identifier, final SortedSet<Instant> mementos,
            final Instant time) {
        if (mementos.isEmpty()) {
            return empty();
        } else if (mementos.size() == 1) {
            // No prev/next with only one Memento
            return of(
                // TimeMap
                Link.fromUri(identifier + TIMEMAP_PARAM).rel(TIMEMAP).type(APPLICATION_LINK_FORMAT)
                    .param(FROM, ofInstant(mementos.first().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                    .param(UNTIL, ofInstant(mementos.last().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                    .build(),
                // First, Last
                Link.fromUri(identifier + VERSION_PARAM + mementos.first().truncatedTo(SECONDS).getEpochSecond())
                    .rel(FIRST).rel(LAST).rel(MEMENTO)
                    .param(DATETIME, ofInstant(mementos.first().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                    .build());
        }

        final Stream.Builder<Link> builder = Stream.builder();
        // TimeMap link
        builder.accept(Link.fromUri(identifier + TIMEMAP_PARAM).rel(TIMEMAP).type(APPLICATION_LINK_FORMAT)
                    .param(FROM, ofInstant(mementos.first().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                    .param(UNTIL, ofInstant(mementos.last().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                    .build());

        // First link
        final Link.Builder first = Link.fromUri(identifier + VERSION_PARAM + mementos.first().truncatedTo(SECONDS)
                .getEpochSecond()).rel(FIRST).rel(MEMENTO)
                .param(DATETIME, ofInstant(mementos.first().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME));

        // Last link
        final Link.Builder last = Link.fromUri(identifier + VERSION_PARAM + mementos.last().truncatedTo(SECONDS)
                .getEpochSecond()).rel(LAST).rel(MEMENTO)
                .param(DATETIME, ofInstant(mementos.last().truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME));

        if (shouldAddPrevNextLinks(mementos, time)) {
            final Instant memento = time.truncatedTo(SECONDS);
            final SortedSet<Instant> head = mementos.headSet(memento);
            if (!head.isEmpty()) {
                final Instant prev = head.last();
                if (mementos.first().equals(prev)) {
                    // Add prev link to first
                    first.rel(PREV);
                } else {
                    // Prev link
                    builder.accept(Link.fromUri(identifier + VERSION_PARAM + prev.truncatedTo(SECONDS).getEpochSecond())
                            .rel(PREV).rel(MEMENTO)
                            .param(DATETIME, ofInstant(prev.truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                            .build());
                }
            }
            final Iterator<Instant> tail = mementos.tailSet(memento).iterator();
            // advance past the current item
            tail.next();
            if (tail.hasNext()) {
                final Instant next = tail.next();
                if (mementos.last().equals(next)) {
                    last.rel(NEXT);
                } else {
                    // Next link
                    builder.accept(Link.fromUri(identifier + VERSION_PARAM + next.truncatedTo(SECONDS)
                            .getEpochSecond()).rel(NEXT).rel(MEMENTO)
                            .param(DATETIME, ofInstant(next.truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                            .build());
                }
            }
        }

        builder.accept(first.build());
        builder.accept(last.build());

        return builder.build();
    }

    /**
     * Get the memento headers.
     * @param identifier the identifier
     * @param mementos the mementos
     * @return a stream of link headers
     */
    public static Stream<Link> getMementoHeaders(final String identifier, final SortedSet<Instant> mementos) {
        return getMementoHeaders(identifier, mementos, null);
    }

    /**
     * Filter link parameters from a provided Link object, if configured to do so.
     * @param link the link
     * @param filter whether to filter the memento parameters
     * @return a Link without Memento parameters, if desired; otherwise, the original link
     */
    public static Link filterLinkParams(final Link link, final boolean filter) {
        // from and until parameters can cause problems with downstream applications because they contain commas. This
        // method makes it possible to filter out those params, if desired. By default, they are not filtered out.
        if (filter) {
            if (TIMEMAP.equals(link.getRel())) {
                return Link.fromUri(link.getUri()).rel(TIMEMAP).type(APPLICATION_LINK_FORMAT).build();
            } else if (link.getRels().contains(MEMENTO)) {
                final Link.Builder builder = Link.fromUri(link.getUri());
                link.getRels().forEach(builder::rel);
                return builder.build();
            }
        }
        return link;
    }

    /**
     * Convert an instant to a memento link header.
     * @param identifier the identifier
     * @param time the time
     * @param rels any optional rel values
     * @return a link header
     */
    public static Link mementoToLink(final String identifier, final Instant time, final List<String> rels) {
        final Link.Builder builder = Link.fromUri(identifier + VERSION_PARAM + time.truncatedTo(SECONDS)
                .getEpochSecond()).rel(MEMENTO).param(DATETIME, ofInstant(time.truncatedTo(SECONDS), UTC)
                        .format(RFC_1123_DATE_TIME));
        rels.forEach(builder::rel);
        return builder.build();
    }

    private static boolean shouldAddPrevNextLinks(final SortedSet<Instant> mementos, final Instant time) {
        return time != null && !time.truncatedTo(SECONDS).isBefore(mementos.first().truncatedTo(SECONDS))
            && !time.truncatedTo(SECONDS).isAfter(mementos.last().truncatedTo(SECONDS));
    }
}
